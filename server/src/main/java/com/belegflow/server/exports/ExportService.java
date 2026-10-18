package com.belegflow.server.exports;
import com.belegflow.domain.*;
import com.belegflow.server.audit.AuditService;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.invoices.InvoiceRepository;
import com.belegflow.server.shared.Sql;
import com.belegflow.server.documents.DocumentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
/** Persistent, idempotent accounting packages. No live accounting, DATEV, bank or payment connector. */
@Service public class ExportService {
    private final JdbcTemplate db;
    private final IdentityService identity;
    private final InvoiceRepository repo;
    private final AuditService audit;
    private final Clock clock;
    public ExportService(JdbcTemplate db,IdentityService identity,InvoiceRepository repo,AuditService audit,Clock clock) {
        this.db=db;
        this.identity=identity;
        this.repo=repo;
        this.audit=audit;
        this.clock=clock;
    }
    public record Entry(UUID id,int version) {
    }
    public record Request(UUID idempotency_key,List<Entry> invoices) {
    }
    public List<Map<String,Object>> list() {
        var a=identity.actor();
        a.finance();
        return Sql.api(db.queryForList("SELECT e.id,e.filename,e.sha256,e.created_at,u.display_name AS created_by_name,(SELECT count(*) FROM export_invoice x WHERE x.tenant_id=e.tenant_id AND x.export_id=e.id) AS invoice_count FROM accounting_export e JOIN app_user u ON u.tenant_id=e.tenant_id AND u.id=e.created_by WHERE e.tenant_id=? ORDER BY e.created_at DESC LIMIT 100",a.tenantId()));
    }
    @Transactional public UUID create(Request b) {
        var a=identity.actor();
        a.finance();
        BusinessException.require(b.idempotency_key()!=null&&b.invoices()!=null&&!b.invoices().isEmpty()&&b.invoices().size()<=20,"INVALID_INPUT");
        BusinessException.require(b.invoices().stream().allMatch(e->e!=null&&e.id()!=null&&e.version()>=0),"INVALID_INPUT");
        var entries=b.invoices().stream().sorted(Comparator.comparing(e->e.id().toString())).toList();
        BusinessException.require(entries.stream().map(Entry::id).distinct().count()==entries.size(),"INVALID_INPUT");
        String fingerprint=DuplicateFingerprint.sha256(entries.stream().map(e->e.id()+":"+e.version()).reduce("",(x,y)->x+"|"+y).getBytes(StandardCharsets.UTF_8));
        // Stable lock ordering: tenant first, then invoices. Retry with the same key returns identical bytes.
        db.queryForObject("SELECT id FROM organization WHERE id=? FOR UPDATE",UUID.class,a.tenantId());
        var previous=db.queryForList("SELECT id,request_fingerprint FROM accounting_export WHERE tenant_id=? AND idempotency_key=?",a.tenantId(),b.idempotency_key());
        if(!previous.isEmpty()) {
            BusinessException.require(fingerprint.equals(previous.getFirst().get("request_fingerprint")),"IDEMPOTENCY_CONFLICT");
            return Sql.uuid(previous.getFirst(),"id");
        }
        List<Map<String,Object>> invoices=new ArrayList<>();
        long totalBytes=0;
        for(var e:entries) {
            var r=repo.get(a.tenantId(),e.id(),true);
            repo.version(r,e.version());
            new WorkflowPolicy().decide(repo.facts(r),WorkflowAction.EXPORT,a,false,"");
            invoices.add(r);
            totalBytes+=db.queryForObject("SELECT coalesce(sum(size_bytes),0) FROM document WHERE tenant_id=? AND invoice_id=?",Long.class,a.tenantId(),e.id());
        }
        BusinessException.require(totalBytes<=50L*1024*1024,"EXPORT_TOO_LARGE");
        UUID export=UUID.randomUUID();
        String filename="belegflow-"+ReminderPolicy.businessDate(clock.instant())+"-"+export.toString().substring(0,8)+".zip";
        List<List<Object>> rows=new ArrayList<>();
        rows.add(List.of("invoice_id","supplier","invoice_number","issue_date","due_date","currency","net","tax","gross","project","cost_center","first_approved_by","second_approved_by"));
        List<List<Object>> events=new ArrayList<>();
        events.add(List.of("invoice_id","event","actor","created_at","detail"));
        List<List<Object>> manifest=new ArrayList<>();
        manifest.add(List.of("path","sha256","bytes"));
        try(var bytes=new ByteArrayOutputStream();var zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)) {
            for(var r:invoices) {
                UUID id=Sql.uuid(r,"id");
                rows.add(Arrays.asList(id,r.get("supplier_name"),r.get("invoice_number"),r.get("issue_date"),r.get("due_date"),r.get("currency"),r.get("net"),r.get("tax"),r.get("gross"),r.get("project_code"),r.get("cost_center_code"),r.get("first_approved_by"),r.get("second_approved_by")));
                db.update("UPDATE invoice SET stage='EXPORTED',version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",a.tenantId(),id);
                audit.append(a,id,"ACCOUNTING_PACKAGE_CREATED",export.toString());
                for(var d:db.queryForList("SELECT id,kind,filename,sha256,content FROM document WHERE tenant_id=? AND invoice_id=? ORDER BY kind,id",a.tenantId(),id)) {
                    String original=Sql.str(d,"filename"),extension=original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
                    String path=Sql.str(d,"kind").equals("ORIGINAL")?"originals/"+id+extension:"evidence/"+id+"/"+d.get("id")+extension;
                    byte[] content=(byte[])d.get("content");
                    add(zip,path,content);
                    manifest.add(List.of(path,d.get("sha256"),content.length));
                }
                for(var ev:db.queryForList("SELECT event_type,actor_name,created_at,detail FROM audit_event WHERE tenant_id=? AND invoice_id=? ORDER BY id",a.tenantId(),id))events.add(Arrays.asList(id,ev.get("event_type"),ev.get("actor_name"),ev.get("created_at"),ev.get("detail")));
            }
            addManifest(zip,"invoices.csv",SafeCsv.encode(rows),manifest);
            addManifest(zip,"audit.csv",SafeCsv.encode(events),manifest);
            String notice="BelegFlow accounting handoff v1\nUTF-8 BOM; semicolon-delimited CSV; decimal point; ISO dates.\nOriginals are preserved byte-for-byte. SHA-256 values are in manifest.csv.\nThis is NOT a DATEV import format or proof of accounting, payment or legal compliance.\nVerify account mappings and tax treatment with your accountant before import.\n";
            addManifest(zip,"README.en.txt",notice.getBytes(StandardCharsets.UTF_8),manifest);
            addManifest(zip,"README.de.txt",("BelegFlow Buchhaltungspaket v1\nUTF-8 mit BOM; Semikolon als Trennzeichen; Dezimalpunkt; ISO-Daten.\nOriginale sind bytegetreu erhalten. SHA-256-Werte stehen in manifest.csv.\nKein DATEV-Importformat und kein Nachweis einer Buchung, Zahlung oder Rechtskonformitaet.\nKontierung und steuerliche Behandlung vor dem Import fachlich pruefen.\n").getBytes(StandardCharsets.UTF_8),manifest);
            add(zip,"manifest.csv",SafeCsv.encode(manifest));
            zip.finish();
            byte[] content=bytes.toByteArray();
            db.update("INSERT INTO accounting_export(id,tenant_id,idempotency_key,request_fingerprint,created_by,filename,sha256,content) VALUES(?,?,?,?,?,?,?,?)",export,a.tenantId(),b.idempotency_key(),fingerprint,a.userId(),filename,DuplicateFingerprint.sha256(content),content);
            for(var e:entries)db.update("INSERT INTO export_invoice(tenant_id,export_id,invoice_id) VALUES(?,?,?)",a.tenantId(),export,e.id());
            return export;
        } catch(IOException e) {
            throw new IllegalStateException("Unable to create accounting package",e);
        }
    }
    public DocumentService.Download download(UUID id) {
        var a=identity.actor();
        a.finance();
        var r=Sql.one(db.queryForList("SELECT filename,sha256,content FROM accounting_export WHERE tenant_id=? AND id=?",a.tenantId(),id));
        return new DocumentService.Download(Sql.str(r,"filename"),"application/zip",Sql.str(r,"sha256"),(byte[])r.get("content"));
    }
    private static void add(ZipOutputStream zip,String path,byte[] content)throws IOException {
        ZipEntry entry=new ZipEntry(path);
        entry.setTime(0);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }
    private static void addManifest(ZipOutputStream zip,String path,byte[] content,List<List<Object>> manifest)throws IOException {
        add(zip,path,content);
        manifest.add(List.of(path,DuplicateFingerprint.sha256(content),content.length));
    }
}
