package com.belegflow.server.audit;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.shared.Sql;
import com.belegflow.domain.BusinessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@RestController @RequestMapping("/api/audit") public class AuditController {
    private final IdentityService identity;
    private final JdbcTemplate db;
    public AuditController(IdentityService identity,JdbcTemplate db) {
        this.identity=identity;
        this.db=db;
    }
    @GetMapping public Map<String,Object> events(@RequestParam(defaultValue="0") long before) {
        var a=identity.actor();
        BusinessException.require(before>=0,"INVALID_INPUT");
        var rows=Sql.api(db.queryForList("SELECT e.id,e.invoice_id,e.actor_name,e.event_type,e.detail,e.created_at,i.invoice_number FROM audit_event e LEFT JOIN invoice i ON i.tenant_id=e.tenant_id AND i.id=e.invoice_id WHERE e.tenant_id=? AND (?=0 OR e.id<?) ORDER BY e.id DESC LIMIT 100",a.tenantId(),before,before));
        return Map.of("items",rows,"next",rows.isEmpty()?0:rows.getLast().get("id"));
    }
}
