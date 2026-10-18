package com.belegflow.server.notifications;
import com.belegflow.domain.BusinessException;
import com.belegflow.server.identity.IdentityService;
import com.belegflow.server.shared.Sql;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@RestController @RequestMapping("/api/notifications") public class NotificationController {
    private final IdentityService identity;
    private final JdbcTemplate db;
    private final ReminderService reminders;
    public NotificationController(IdentityService identity,JdbcTemplate db,ReminderService reminders) {
        this.identity=identity;
        this.db=db;
        this.reminders=reminders;
    }
    @GetMapping public List<Map<String,Object>> list() {
        var a=identity.actor();
        return Sql.api(db.queryForList("SELECT n.id,n.invoice_id,n.kind,n.is_read,n.created_at,i.invoice_number FROM notification n JOIN invoice i ON i.tenant_id=n.tenant_id AND i.id=n.invoice_id WHERE n.tenant_id=? AND n.user_id=? ORDER BY n.created_at DESC LIMIT 100",a.tenantId(),a.userId()));
    }
    @PostMapping("/{id}/read") public void read(@PathVariable UUID id) {
        var a=identity.actor();
        BusinessException.require(db.update("UPDATE notification SET is_read=true WHERE tenant_id=? AND user_id=? AND id=?",a.tenantId(),a.userId(),id)==1,"NOT_FOUND");
    }
    @PostMapping("/run") public Map<String,Integer> run() {
        var a=identity.actor();
        a.admin();
        return Map.of("created",reminders.run(a.tenantId()));
    }
}
