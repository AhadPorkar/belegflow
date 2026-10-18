package com.belegflow.server.documents;
import com.belegflow.domain.*;
import com.belegflow.server.audit.AuditService;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.invoices.InvoiceRepository;
import com.belegflow.server.shared.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Clock;
/** Byte-preserving database-backed document store. Suitable for this bounded initial release.
 * Object-storage migration belongs behind this boundary, not in workflow policy. */
@Service public class DocumentService {
    private final JdbcTemplate db;
    private final IdentityService identity;
    private final InvoiceRepository repo;
    private final AuditService audit;
    private final Clock clock;
    public DocumentService(JdbcTemplate db,IdentityService identity,InvoiceRepository repo,AuditService audit,Clock clock) {
        this.db=db;
        this.identity=identity;
        this.repo=repo;
        this.audit=audit;
        this.clock=clock;
    }
    public record Download(String filename,String contentType,String sha256,byte[] bytes) {
    }
    public Download download(UUID id) {
        var a=identity.actor();
        var r=Sql.one(db.queryForList("SELECT filename,content_type,sha256,content FROM document WHERE tenant_id=? AND id=?",a.tenantId(),id));
        return new Download(Sql.str(r,"filename"),Sql.str(r,"content_type"),Sql.str(r,"sha256"),(byte[])r.get("content"));
    }
    public UUID insert(Actor a,UUID invoice,String kind,String filename,String type,byte[] bytes) {
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO document(id,tenant_id,invoice_id,kind,filename,content_type,sha256,size_bytes,content,uploaded_by) VALUES(?,?,?,?,?,?,?,?,?,?)",id,a.tenantId(),invoice,kind,filename,type,DuplicateFingerprint.sha256(bytes),bytes.length,bytes,a.userId());
        return id;
    }
    @Transactional public UUID evidence(UUID invoice,int version,String filename,byte[] bytes) {
        var a=identity.actor();
        var row=repo.get(a.tenantId(),invoice,true);
        repo.version(row,version);
        BusinessException.require(InvoiceStage.valueOf(Sql.str(row,"stage")).editable(),"INVALID_TRANSITION");
        boolean assigned=a.userId().equals(row.get("approver_id"))||repo.delegate(a,Sql.uuid(row,"approver_id"),clock.instant());
        BusinessException.require(a.role().managesFinance()||(assigned&&a.role().mayApprove()),"FORBIDDEN");
        String type=FilePolicy.validate(filename,bytes,false);
        BusinessException.require(repo.evidenceCount(a.tenantId(),invoice)<20,"DOCUMENT_LIMIT");
        UUID id=insert(a,invoice,"EVIDENCE",filename,type,bytes);
        // Adding evidence invalidates an earlier acceptance. The reviewer must explicitly accept the updated set.
        db.update("UPDATE invoice SET evidence_accepted_by=null,evidence_accepted_at=null,evidence_note='',version=version+1,updated_at=now() WHERE tenant_id=? AND id=?",a.tenantId(),invoice);
        audit.append(a,invoice,"EVIDENCE_UPLOADED",filename);
        return id;
    }
}
