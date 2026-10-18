package com.belegflow.server.notifications;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationModuleTest extends ServiceFixture {
    final ReminderService reminders=new ReminderService(db,clock);
    final String candidates="SELECT tenant_id,id,approver_id,first_approved_by,created_by,second_required FROM invoice WHERE stage='READY_FOR_APPROVAL' AND due_date<=? AND approver_id IS NOT NULL";
    void candidates(Map<String,Object> r){when(db.queryForList(candidates+" AND tenant_id=?",LocalDate.of(2026,10,21),tenant)).thenReturn(List.of(r));}
    @Test void firstApprovalNotifiesAssignedApprover(){var r=row("READY_FOR_APPROVAL");candidates(r);assertEquals(1,reminders.run(tenant));assertEquals(approverId,arg("INSERT INTO notification",3));assertEquals(LocalDate.of(2026,10,18),arg("INSERT INTO notification",6));}
    @Test void secondApprovalTargetsIndependentAdministrators(){
        var r=row("READY_FOR_APPROVAL");r.put("first_approved_by",approverId);r.put("second_required",true);candidates(r);
        when(db.queryForList("SELECT id FROM app_user WHERE tenant_id=? AND role='ADMIN' AND active=true AND id<>? AND id<>?",UUID.class,tenant,approverId,creatorId)).thenReturn(List.of(actorId));
        assertEquals(1,reminders.run(tenant));assertEquals(actorId,arg("INSERT INTO notification",3));
    }
    @Test void duplicateNotificationConflictContributesZero(){candidates(row("READY_FOR_APPROVAL"));when(db.update(anyString(),any(Object[].class))).thenReturn(0);assertEquals(0,reminders.run(tenant));assertTrue(write("INSERT INTO notification").getArgument(0,String.class).contains("ON CONFLICT"));}
    @Test void scheduledTickIncludesAllTenants(){when(db.queryForList(candidates,LocalDate.of(2026,10,21))).thenReturn(List.of());reminders.tick();verify(db).queryForList(candidates,LocalDate.of(2026,10,21));}
    @Test void markReadScopesToBothTenantAndRecipient(){UUID id=UUID.randomUUID();new NotificationController(identity,db,reminders).read(id);verify(db).update("UPDATE notification SET is_read=true WHERE tenant_id=? AND user_id=? AND id=?",tenant,actorId,id);}
    @Test void anotherUsersNotificationReturnsNotFound(){when(db.update(anyString(),any(Object[].class))).thenReturn(0);assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->new NotificationController(identity,db,reminders).read(UUID.randomUUID())).code());}
    @Test void financeCannotManuallyRunReminderJob(){var runner=mock(ReminderService.class);assertThrows(BusinessException.class,()->new NotificationController(identity,db,runner).run());verifyNoInteractions(runner);}
    @Test void adminReminderJobIsTenantScoped(){as(Role.ADMIN);var runner=mock(ReminderService.class);when(runner.run(tenant)).thenReturn(4);assertEquals(4,new NotificationController(identity,db,runner).run().get("created"));verify(runner).run(tenant);}
    @Test void notificationListUsesRecipientScope(){new NotificationController(identity,db,reminders).list();verify(db).queryForList("SELECT n.id,n.invoice_id,n.kind,n.is_read,n.created_at,i.invoice_number FROM notification n JOIN invoice i ON i.tenant_id=n.tenant_id AND i.id=n.invoice_id WHERE n.tenant_id=? AND n.user_id=? ORDER BY n.created_at DESC LIMIT 100",tenant,actorId);}
}
