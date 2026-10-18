package com.belegflow.server.notifications;
import com.belegflow.domain.*;
import com.belegflow.server.shared.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
/** Durable, deduplicated in-app reminders. Email delivery is not included in this release. */
@Service public class ReminderService {
    private final JdbcTemplate db;
    private final Clock clock;
    public ReminderService(JdbcTemplate db,Clock clock) {
        this.db=db;
        this.clock=clock;
    }
    @Scheduled(fixedDelayString="${bf.reminder-delay-ms}",initialDelayString="${bf.reminder-initial-delay-ms:30000}") @Transactional public void tick() {
        run(null);
    }
    @Transactional public int run(UUID tenant) {
        var day=ReminderPolicy.businessDate(clock.instant());
        int inserted=0;
        String sql="SELECT tenant_id,id,approver_id,first_approved_by,created_by,second_required FROM invoice WHERE stage='READY_FOR_APPROVAL' AND due_date<=? AND approver_id IS NOT NULL"+(tenant==null?"":" AND tenant_id=?");
        var rows=tenant==null?db.queryForList(sql,day.plusDays(3)):db.queryForList(sql,day.plusDays(3),tenant);
        for(var row:rows) {
            UUID t=Sql.uuid(row,"tenant_id");
            List<UUID> recipients=new ArrayList<>();
            if(row.get("first_approved_by")!=null&&Sql.bool(row,"second_required"))recipients.addAll(db.queryForList("SELECT id FROM app_user WHERE tenant_id=? AND role='ADMIN' AND active=true AND id<>? AND id<>?",UUID.class,t,row.get("first_approved_by"),row.get("created_by")));
            else {
                recipients.add(Sql.uuid(row,"approver_id"));
                recipients.addAll(db.queryForList("SELECT d.delegate_id FROM delegation d JOIN app_user u ON u.tenant_id=d.tenant_id AND u.id=d.delegate_id WHERE d.tenant_id=? AND d.owner_id=? AND d.active=true AND u.active=true AND d.valid_from<=now() AND d.valid_until>now()",UUID.class,t,row.get("approver_id")));
            }
            for(UUID u:recipients)inserted+=db.update("INSERT INTO notification(id,tenant_id,user_id,invoice_id,kind,business_date) VALUES(?,?,?,?,?,?) ON CONFLICT(tenant_id,user_id,invoice_id,kind,business_date) DO NOTHING",UUID.randomUUID(),t,u,row.get("id"),"APPROVAL_DUE",day);
        }
        return inserted;
    }
}
