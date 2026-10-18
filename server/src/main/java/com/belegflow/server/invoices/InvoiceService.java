package com.belegflow.server.invoices;
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
@Service public class InvoiceService {
    private final JdbcTemplate db;
    private final IdentityService identity;
    private final AuditService audit;
    private final InvoiceRepository repo;
    private final Clock clock;
    private final WorkflowPolicy policy=new WorkflowPolicy();
    public InvoiceService(JdbcTemplate db,IdentityService identity,AuditService audit,InvoiceRepository repo,Clock clock) {
        this.db=db;
        this.identity=identity;
        this.audit=audit;
        this.repo=repo;
        this.clock=clock;
    }
    public Map<String,Object> list(String search,String stage,int page,int size) {
        var a=identity.actor();
        BusinessException.require(page>=0&&page<=10000&&size>=1&&size<=100,"INVALID_INPUT");
        String q="%"+Sql.text(search,200,false)+"%",s=Sql.text(stage,30,false);
        if(!s.isEmpty())InvoiceStage.valueOf(s);
        String where=" WHERE i.tenant_id=? AND (?='' OR i.stage=?) AND (i.invoice_number ILIKE ? OR s.name ILIKE ? OR i.description ILIKE ?)";
        var rows=db.queryForList(InvoiceRepository.SELECT+where+" ORDER BY i.received_at DESC,i.id LIMIT ? OFFSET ?",a.tenantId(),s,s,q,q,q,size,page*size);
        Long count=db.queryForObject("SELECT count(*) FROM invoice i JOIN supplier s ON s.tenant_id=i.tenant_id AND s.id=i.supplier_id"+where,Long.class,a.tenantId(),s,s,q,q,q);
        return Map.of("items",Sql.api(rows),"total",count,"page",page,"size",size);
    }
    public Map<String,Object> summary() {
        var a=identity.actor();
        var date=ReminderPolicy.businessDate(clock.instant());
        return Sql.api(db.queryForMap("""
            SELECT count(*) AS total,
             count(*) FILTER (WHERE stage IN ('RECEIVED','REVIEWED','CLARIFICATION')) AS review,
             count(*) FILTER (WHERE stage='READY_FOR_APPROVAL') AS awaiting,
             count(*) FILTER (WHERE stage='APPROVED') AS approved,
             count(*) FILTER (WHERE stage='EXPORTED') AS exported,
             count(*) FILTER (WHERE stage NOT IN ('EXPORTED','REJECTED') AND due_date < ?) AS overdue,
             coalesce(sum(gross) FILTER (WHERE currency='EUR' AND stage NOT IN ('EXPORTED','REJECTED')),0) AS open_value,
             coalesce(sum(gross) FILTER (WHERE currency='EUR' AND stage='APPROVED'),0) AS approved_value
            FROM invoice WHERE tenant_id=?
            """,date,a.tenantId()));
    }
    public Map<String,Object> detail(UUID id) {
        var a=identity.actor();
        var row=repo.get(a.tenantId(),id,false);
        Map<String,Object> result=Sql.api(row);
        result.put("documents",Sql.api(db.queryForList("SELECT id,kind,filename,content_type,sha256,size_bytes,created_at FROM document WHERE tenant_id=? AND invoice_id=? ORDER BY created_at",a.tenantId(),id)));
        result.put("discussion",Sql.api(db.queryForList("SELECT d.id,d.body,d.is_question,d.resolved_at,d.resolution,d.created_at,u.display_name AS author_name FROM discussion d JOIN app_user u ON u.tenant_id=d.tenant_id AND u.id=d.author_id WHERE d.tenant_id=? AND d.invoice_id=? ORDER BY d.created_at",a.tenantId(),id)));
        result.put("audit",Sql.api(db.queryForList("SELECT id,actor_name,event_type,detail,created_at FROM audit_event WHERE tenant_id=? AND invoice_id=? ORDER BY id DESC LIMIT 100",a.tenantId(),id)));
        result.put("possible_duplicates",Sql.api(db.queryForList("SELECT id,invoice_number,stage,gross,received_at FROM invoice WHERE tenant_id=? AND business_key=? AND id<>? ORDER BY received_at",a.tenantId(),row.get("business_key"),id)));
        var f=repo.facts(row);
        List<String> actions=new ArrayList<>();
        boolean delegate=repo.delegate(a,Sql.uuid(row,"approver_id"),clock.instant());
        for(var action:WorkflowAction.values()) {
            try {
                policy.decide(f,action,a,delegate,"Permission check");
                actions.add(action.name());
            } catch(BusinessException ignored) {
            }
        }
        result.put("allowed_actions",actions);
        boolean assigned=a.userId().equals(row.get("approver_id"))||delegate;
        boolean manager=a.userId().equals(row.get("project_manager_id"))||repo.delegate(a,Sql.uuid(row,"project_manager_id"),clock.instant());
        result.put("can_edit",a.role().managesFinance()&&f.stage().editable());
        result.put("can_upload_evidence",f.stage().editable()&&(a.role().managesFinance()||(assigned&&a.role().mayApprove())));
        result.put("can_accept_evidence",f.stage().editable()&&manager&&a.role().mayApprove()&&!a.userId().equals(f.creatorId())&&repo.evidenceCount(a.tenantId(),id)>0);
        result.put("open_questions",f.openQuestions());
        return result;
    }
    public record Command(int version,WorkflowAction action,String note) {
    }
    @Transactional public void command(UUID id,Command command) {
        var a=identity.actor();
        var row=repo.get(a.tenantId(),id,true);
        repo.version(row,command.version());
        BusinessException.require(command.action()!=WorkflowAction.EXPORT,"USE_EXPORT_ENDPOINT");
        // Snapshot the current threshold at submission; future rule changes do not rewrite an existing approval.
        if(command.action()==WorkflowAction.SUBMIT) {
            var threshold=db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",BigDecimal.class,a.tenantId());
            row.put("second_required",MoneyRules.needsSecondApproval((BigDecimal)row.get("gross"),threshold));
            identity.approverExists(a.tenantId(),Sql.uuid(row,"approver_id"));
        }
        var decision=policy.decide(repo.facts(row),command.action(),a,repo.delegate(a,Sql.uuid(row,"approver_id"),clock.instant()),command.note());
        UUID first=Sql.uuid(row,"first_approved_by"),second=Sql.uuid(row,"second_approved_by");
        if(decision.approvalSlot()==1)first=a.userId();
        if(decision.approvalSlot()==2)second=a.userId();
        if(command.action()==WorkflowAction.CLARIFY||command.action()==WorkflowAction.REOPEN) {
            first=null;
            second=null;
        }
        db.update("UPDATE invoice SET stage=?,first_approved_by=?,second_approved_by=?,second_required=?,version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",decision.stage().name(),first,second,row.get("second_required"),a.tenantId(),id);
        if(command.action()==WorkflowAction.CLARIFY) db.update("INSERT INTO discussion(id,tenant_id,invoice_id,author_id,body,is_question) VALUES(?,?,?,?,?,true)",UUID.randomUUID(),a.tenantId(),id,a.userId(),Sql.text(command.note(),2000,true));
        audit.append(a,id,decision.event(),Sql.text(command.note(),2000,false));
    }
    public record Allocation(int version,UUID project_id,UUID cost_center_id,UUID approver_id,LocalDate due_date) {
    }
    @Transactional public void allocate(UUID id,Allocation body) {
        var a=identity.actor();
        a.finance();
        var row=repo.get(a.tenantId(),id,true);
        repo.version(row,body.version());
        BusinessException.require(InvoiceStage.valueOf(Sql.str(row,"stage")).editable(),"INVALID_TRANSITION");
        var project=Sql.one(db.queryForList("SELECT id,evidence_required FROM project WHERE tenant_id=? AND id=? AND active=true",a.tenantId(),body.project_id()));
        BusinessException.require(Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS(SELECT 1 FROM cost_center WHERE tenant_id=? AND id=? AND active=true)",Boolean.class,a.tenantId(),body.cost_center_id())),"NOT_FOUND");
        identity.approverExists(a.tenantId(),body.approver_id());
        BusinessException.require(body.due_date()!=null&&!body.due_date().isBefore(((java.sql.Date)row.get("issue_date")).toLocalDate()),"INVALID_DUE_DATE");
        boolean projectChanged=!Objects.equals(row.get("project_id"),body.project_id());
        String notes=Sql.str(row,"validation_notes").replace("DUE_DATE_REQUIRED","").replace("DUE_BEFORE_ISSUE","").replaceAll("^;+|;+$","");
        boolean valid=notes.isBlank()&&MoneyRules.validTotals((BigDecimal)row.get("net"),(BigDecimal)row.get("tax"),(BigDecimal)row.get("gross"),Sql.str(row,"currency"));
        db.update("UPDATE invoice SET project_id=?,cost_center_id=?,approver_id=?,evidence_required=?,due_date=?,basic_valid=?,validation_notes=?,evidence_accepted_by=?,evidence_accepted_at=?,version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",body.project_id(),body.cost_center_id(),body.approver_id(),project.get("evidence_required"),body.due_date(),valid,notes,projectChanged?null:row.get("evidence_accepted_by"),projectChanged?null:row.get("evidence_accepted_at"),a.tenantId(),id);
        audit.append(a,id,"ALLOCATION_UPDATED",body.project_id()+" / "+body.cost_center_id()+" / "+body.approver_id());
    }
    public record Note(int version,String note) {
    }
    @Transactional public void clearDuplicate(UUID id,Note body) {
        var a=identity.actor();
        a.finance();
        var row=repo.get(a.tenantId(),id,true);
        repo.version(row,body.version());
        BusinessException.require(InvoiceStage.valueOf(Sql.str(row,"stage")).editable()&&Sql.bool(row,"duplicate_warning"),"INVALID_TRANSITION");
        String note=Sql.text(body.note(),2000,true);
        db.update("UPDATE invoice SET duplicate_cleared=true,version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",a.tenantId(),id);
        audit.append(a,id,"DUPLICATE_CLEARED",note);
    }
    @Transactional public void acceptEvidence(UUID id,Note body) {
        var a=identity.actor();
        var row=repo.get(a.tenantId(),id,true);
        repo.version(row,body.version());
        BusinessException.require(InvoiceStage.valueOf(Sql.str(row,"stage")).editable(),"INVALID_TRANSITION");
        boolean manager=a.userId().equals(row.get("project_manager_id"))||repo.delegate(a,Sql.uuid(row,"project_manager_id"),clock.instant());
        BusinessException.require(manager&&a.role().mayApprove(),"NOT_ASSIGNED");
        BusinessException.require(!a.userId().equals(row.get("created_by")),"SELF_APPROVAL");
        BusinessException.require(repo.evidenceCount(a.tenantId(),id)>0,"EVIDENCE_REQUIRED");
        String note=Sql.text(body.note(),2000,true);
        db.update("UPDATE invoice SET evidence_accepted_by=?,evidence_accepted_at=now(),evidence_note=?,version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",a.userId(),note,a.tenantId(),id);
        audit.append(a,id,"SERVICE_EVIDENCE_ACCEPTED",note);
    }
    public record Comment(String body) {
    }
    @Transactional public UUID comment(UUID id,Comment b) {
        var a=identity.actor();
        BusinessException.require(a.role()!=Role.VIEWER,"FORBIDDEN");
        repo.get(a.tenantId(),id,true);
        UUID comment=UUID.randomUUID();
        db.update("INSERT INTO discussion(id,tenant_id,invoice_id,author_id,body) VALUES(?,?,?,?,?)",comment,a.tenantId(),id,a.userId(),Sql.text(b.body(),2000,true));
        audit.append(a,id,"COMMENT_ADDED",comment.toString());
        return comment;
    }
    @Transactional public void resolveQuestion(UUID id,UUID question,Comment body) {
        var a=identity.actor();
        a.finance();
        var row=repo.get(a.tenantId(),id,true);
        BusinessException.require(Sql.str(row,"stage").equals("CLARIFICATION"),"INVALID_TRANSITION");
        String resolution=Sql.text(body.body(),2000,true);
        BusinessException.require(db.update("UPDATE discussion SET resolved_by=?,resolved_at=now(),resolution=? WHERE tenant_id=? AND invoice_id=? AND id=? AND is_question=true AND resolved_at IS NULL",a.userId(),resolution,a.tenantId(),id,question)==1,"NOT_FOUND");
        db.update("UPDATE invoice SET version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",a.tenantId(),id);
        audit.append(a,id,"QUESTION_RESOLVED",resolution);
    }
}
