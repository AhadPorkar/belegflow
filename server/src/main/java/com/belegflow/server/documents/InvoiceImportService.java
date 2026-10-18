package com.belegflow.server.documents;
import com.belegflow.domain.*;
import com.belegflow.server.audit.AuditService;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.shared.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
@Service public class InvoiceImportService {
    private final JdbcTemplate db;
    private final IdentityService identity;
    private final DocumentService documents;
    private final AuditService audit;
    private final InvoiceXmlParser parser=new InvoiceXmlParser();
    public InvoiceImportService(JdbcTemplate db,IdentityService identity,DocumentService documents,AuditService audit) {
        this.db=db;
        this.identity=identity;
        this.documents=documents;
        this.audit=audit;
    }
    public record Result(UUID id,boolean reused,boolean duplicate_warning) {
    }
    @Transactional public Result ingest(String filename,byte[] bytes,Map<String,String> metadata) {
        return ingestFor(identity.actor(),filename,bytes,metadata);
    }
    /** Internal entry point used by synthetic fixtures, not exposed as an HTTP endpoint. */
    public Result ingestFor(Actor a,String filename,byte[] bytes,Map<String,String> m) {
        a.finance();
        String type=FilePolicy.validate(filename,bytes,true);
        // Serialize per-tenant ingestion. Prevent duplicate races even for concurrent identical uploads.
        db.queryForObject("SELECT id FROM organization WHERE id=? FOR UPDATE",UUID.class,a.tenantId());
        String hash=DuplicateFingerprint.sha256(bytes);
        var same=db.queryForList("SELECT invoice_id FROM document WHERE tenant_id=? AND sha256=? AND kind='ORIGINAL'",a.tenantId(),hash);
        if(!same.isEmpty()) {
            UUID existing=Sql.uuid(same.getFirst(),"invoice_id");
            audit.append(a,existing,"IDENTICAL_IMPORT_IGNORED",filename);
            return new Result(existing,true,false);
        }
        UUID supplier=optionalUuid(m.get("supplier_id"));
        String number,description,currency,format;
        LocalDate issue,due;
        BigDecimal net,tax,gross;
        List<String> checks=new ArrayList<>();
        if(type.equals("application/xml")) {
            var parsed=parser.parse(bytes);
            number=parsed.number();
            description=parsed.description();
            currency=parsed.currency();
            format=parsed.format();
            issue=parsed.issueDate();
            due=parsed.dueDate();
            net=parsed.net();
            tax=parsed.tax();
            gross=parsed.gross();
            checks.addAll(parsed.checks());
            if(supplier==null) {
                String normalized=DuplicateFingerprint.normalize(parsed.supplier());
                var matching=db.queryForList("SELECT id FROM supplier WHERE tenant_id=? AND normalized_name=? AND active=true",a.tenantId(),normalized);
                if(matching.isEmpty()) {
                    supplier=UUID.randomUUID();
                    db.update("INSERT INTO supplier(id,tenant_id,name,normalized_name) VALUES(?,?,?,?)",supplier,a.tenantId(),parsed.supplier(),normalized);
                    audit.append(a,null,"SUPPLIER_CREATED_FROM_IMPORT",supplier.toString());
                } else supplier=Sql.uuid(matching.getFirst(),"id");
            }
        } else {
            format="PDF";
            number=Sql.text(m.get("invoice_number"),120,true);
            description=Sql.text(m.get("description"),2000,false);
            currency=Sql.text(m.getOrDefault("currency","EUR"),3,true);
            issue=LocalDate.parse(m.get("issue_date"));
            due=LocalDate.parse(m.get("due_date"));
            net=MoneyRules.amount(m.get("net"));
            tax=MoneyRules.amount(m.get("tax"));
            gross=MoneyRules.amount(m.get("gross"));
            if(!MoneyRules.validTotals(net,tax,gross,currency))checks.add("TOTALS_OR_CURRENCY_INVALID");
            if(due.isBefore(issue))checks.add("DUE_BEFORE_ISSUE");
        }
        BusinessException.require(supplier!=null&&Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM supplier WHERE tenant_id=? AND id=? AND active=true)",Boolean.class,a.tenantId(),supplier)),"INVALID_SUPPLIER");
        UUID project=optionalUuid(m.get("project_id")),costCenter=optionalUuid(m.get("cost_center_id")),approver=optionalUuid(m.get("approver_id"));
        boolean evidenceRequired=true;
        if(project!=null) {
            var p=Sql.one(db.queryForList("SELECT manager_id,evidence_required FROM project WHERE tenant_id=? AND id=? AND active=true",a.tenantId(),project));
            evidenceRequired=Sql.bool(p,"evidence_required");
            if(approver==null)approver=Sql.uuid(p,"manager_id");
        }
        if(costCenter!=null)BusinessException.require(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM cost_center WHERE tenant_id=? AND id=? AND active=true)",Boolean.class,a.tenantId(),costCenter)),"NOT_FOUND");
        if(approver!=null)identity.approverExists(a.tenantId(),approver);
        String key=DuplicateFingerprint.businessKey(supplier,number);
        boolean duplicate=Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM invoice WHERE tenant_id=? AND business_key=?)",Boolean.class,a.tenantId(),key));
        var threshold=db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",BigDecimal.class,a.tenantId());
        UUID id=UUID.randomUUID();
        db.update("""
          INSERT INTO invoice(id,tenant_id,supplier_id,invoice_number,description,issue_date,due_date,net,tax,gross,currency,source_format,basic_valid,validation_notes,project_id,cost_center_id,approver_id,evidence_required,duplicate_warning,business_key,second_required,created_by)
          VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,id,a.tenantId(),supplier,number,description,issue,due,net,tax,gross,currency,format,checks.isEmpty(),String.join(";",checks),project,costCenter,approver,evidenceRequired,duplicate,key,MoneyRules.needsSecondApproval(gross,threshold),a.userId());
        documents.insert(a,id,"ORIGINAL",filename,type,bytes);
        audit.append(a,id,"INVOICE_RECEIVED",filename);
        audit.append(a,id,checks.isEmpty()?"BASIC_CHECKS_PASSED":"BASIC_CHECKS_FAILED",String.join(";",checks));
        if(duplicate)audit.append(a,id,"POSSIBLE_DUPLICATE",key);
        return new Result(id,false,duplicate);
    }
    private UUID optionalUuid(String s) {
        return s==null||s.isBlank()?null:UUID.fromString(s);
    }
}
