package com.belegflow.server.invoices;
import com.belegflow.domain.*;
import com.belegflow.server.shared.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
@Repository public class InvoiceRepository {
    private final JdbcTemplate db;
    public InvoiceRepository(JdbcTemplate db) {
        this.db=db;
    }
    public static final String SELECT="""
        SELECT i.*,s.name AS supplier_name,s.email AS supplier_email,s.vat_id AS supplier_vat_id,
         p.name AS project_name,p.code AS project_code,p.manager_id AS project_manager_id,
         c.name AS cost_center_name,c.code AS cost_center_code,u.display_name AS approver_name,
         author.display_name AS creator_name,ev.display_name AS evidence_accepted_name
        FROM invoice i JOIN supplier s ON s.tenant_id=i.tenant_id AND s.id=i.supplier_id
        JOIN app_user author ON author.tenant_id=i.tenant_id AND author.id=i.created_by
        LEFT JOIN project p ON p.tenant_id=i.tenant_id AND p.id=i.project_id
        LEFT JOIN cost_center c ON c.tenant_id=i.tenant_id AND c.id=i.cost_center_id
        LEFT JOIN app_user u ON u.tenant_id=i.tenant_id AND u.id=i.approver_id
        LEFT JOIN app_user ev ON ev.tenant_id=i.tenant_id AND ev.id=i.evidence_accepted_by
        """;
    public Map<String,Object> get(UUID tenant,UUID id,boolean lock) {
        return Sql.one(db.queryForList(SELECT+" WHERE i.tenant_id=? AND i.id=?"+(lock?" FOR UPDATE OF i":""),tenant,id));
    }
    public boolean delegate(Actor a,UUID owner,Instant now) {
        if(owner==null)return false;
        return Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM delegation WHERE tenant_id=? AND owner_id=? AND delegate_id=? AND active=true AND valid_from<=? AND valid_until>?)",Boolean.class,a.tenantId(),owner,a.userId(),Timestamp.from(now),Timestamp.from(now)));
    }
    public int questions(UUID tenant,UUID id) {
        return db.queryForObject("SELECT count(*) FROM discussion WHERE tenant_id=? AND invoice_id=? AND is_question=true AND resolved_at IS NULL",Integer.class,tenant,id);
    }
    public int evidenceCount(UUID tenant,UUID id) {
        return db.queryForObject("SELECT count(*) FROM document WHERE tenant_id=? AND invoice_id=? AND kind='EVIDENCE'",Integer.class,tenant,id);
    }
    public WorkflowFacts facts(Map<String,Object> row) {
        return new WorkflowFacts(InvoiceStage.valueOf(Sql.str(row,"stage")),Sql.uuid(row,"created_by"),Sql.uuid(row,"approver_id"),Sql.bool(row,"basic_valid"), row.get("project_id")!=null&&row.get("cost_center_id")!=null&&row.get("approver_id")!=null, Sql.bool(row,"duplicate_warning")&&!Sql.bool(row,"duplicate_cleared"),Sql.bool(row,"evidence_required"),row.get("evidence_accepted_by")!=null, questions(Sql.uuid(row,"tenant_id"),Sql.uuid(row,"id")),Sql.bool(row,"second_required"),Sql.uuid(row,"first_approved_by"),Sql.uuid(row,"second_approved_by"));
    }
    public void version(Map<String,Object> row,int expected) {
        BusinessException.require(Sql.integer(row,"version")==expected,"VERSION_CONFLICT");
    }
}
