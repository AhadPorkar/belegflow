package com.belegflow.server.audit;
import com.belegflow.domain.Actor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service public class AuditService {
    private final JdbcTemplate db;
    public AuditService(JdbcTemplate db) {
        this.db=db;
    }
    /** Called inside the same transaction as the business change; no after-commit audit gaps. */
    public void append(Actor actor,UUID invoice,String event,String detail) {
        db.update("INSERT INTO audit_event(tenant_id,invoice_id,actor_id,actor_name,event_type,detail) VALUES (?,?,?,?,?,?)",actor.tenantId(),invoice,actor.userId(),actor.name(),event,detail==null?"":detail);
    }
}
