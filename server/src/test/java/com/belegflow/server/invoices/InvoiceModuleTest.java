package com.belegflow.server.invoices;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoiceModuleTest extends ServiceFixture {
    final InvoiceService service=new InvoiceService(db,identity,audit,repo,clock);
    @Test void reviewLocksAndChecksVersionBeforeWritingAndAuditing(){
        var row=row("RECEIVED");service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.REVIEW,"Checked"));
        var order=inOrder(repo,db,audit);order.verify(repo).get(tenant,invoiceId,true);order.verify(repo).version(row,3);
        assertEquals("REVIEWED",arg("UPDATE invoice SET stage",1));verify(audit).append(actor,invoiceId,"INVOICE_REVIEWED","Checked");
    }
    @Test void staleVersionStopsAllWrites(){
        var row=row("RECEIVED");doThrow(new BusinessException("VERSION_CONFLICT")).when(repo).version(row,2);
        assertEquals("VERSION_CONFLICT",assertThrows(BusinessException.class,()->service.command(invoiceId,new InvoiceService.Command(2,WorkflowAction.REVIEW,""))).code());assertTrue(writes().isEmpty());verifyNoInteractions(audit);
    }
    @Test void directExportCommandIsRejected(){row("APPROVED");assertEquals("USE_EXPORT_ENDPOINT",assertThrows(BusinessException.class,()->service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.EXPORT,""))).code());assertTrue(writes().isEmpty());}
    @Test void submitSnapshotsThresholdAndRequiresEvidence(){
        row("REVIEWED");when(db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",BigDecimal.class,tenant)).thenReturn(new BigDecimal("10000"));
        assertEquals("EVIDENCE_REQUIRED",assertThrows(BusinessException.class,()->service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.SUBMIT,""))).code());assertTrue(writes().isEmpty());
    }
    @Test void validSubmissionRecalculatesThreshold(){
        var row=row("REVIEWED");row.put("evidence_accepted_by",approverId);when(db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",BigDecimal.class,tenant)).thenReturn(new BigDecimal("100"));
        service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.SUBMIT,""));assertEquals(true,arg("UPDATE invoice SET stage",4));verify(identity).approverExists(tenant,approverId);
    }
    @Test void clarificationClearsBothApprovalsAndCreatesQuestion(){
        var row=row("READY_FOR_APPROVAL");row.put("first_approved_by",approverId);row.put("second_approved_by",UUID.randomUUID());
        service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.CLARIFY,"Confirm additional work"));
        assertNull(arg("UPDATE invoice SET stage",2));assertNull(arg("UPDATE invoice SET stage",3));assertEquals("Confirm additional work",arg("INSERT INTO discussion",5));
    }
    @Test void firstApprovalPersistsTheActualActor(){
        as(Role.APPROVER);var row=row("READY_FOR_APPROVAL");row.put("approver_id",actorId);row.put("evidence_accepted_by",actorId);
        service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.APPROVE,"Verified"));assertEquals(actorId,arg("UPDATE invoice SET stage",2));assertEquals("APPROVED",arg("UPDATE invoice SET stage",1));
    }
    @Test void secondApprovalMustBeIndependent(){
        as(Role.ADMIN);var row=row("READY_FOR_APPROVAL");row.put("first_approved_by",actorId);row.put("second_required",true);row.put("evidence_accepted_by",actorId);
        assertEquals("DISTINCT_APPROVERS_REQUIRED",assertThrows(BusinessException.class,()->service.command(invoiceId,new InvoiceService.Command(3,WorkflowAction.APPROVE,""))).code());assertTrue(writes().isEmpty());
    }
    @Test void duplicateResolutionRequiresReasonAndWarning(){
        var row=row("REVIEWED");row.put("duplicate_warning",true);
        assertThrows(BusinessException.class,()->service.clearDuplicate(invoiceId,new InvoiceService.Note(3," ")));
        service.clearDuplicate(invoiceId,new InvoiceService.Note(3,"Different purchase order"));verify(audit).append(actor,invoiceId,"DUPLICATE_CLEARED","Different purchase order");
    }
    @Test void managerAcceptanceRequiresEvidenceAndNotCreator(){
        as(Role.APPROVER);var row=row("REVIEWED");row.put("project_manager_id",actorId);when(repo.evidenceCount(tenant,invoiceId)).thenReturn(0);
        assertEquals("EVIDENCE_REQUIRED",assertThrows(BusinessException.class,()->service.acceptEvidence(invoiceId,new InvoiceService.Note(3,"Completed"))).code());assertTrue(writes().isEmpty());
    }
    @Test void validServiceAcceptanceIsAudited(){
        as(Role.APPROVER);var row=row("REVIEWED");row.put("project_manager_id",actorId);when(repo.evidenceCount(tenant,invoiceId)).thenReturn(1);
        service.acceptEvidence(invoiceId,new InvoiceService.Note(3,"Completed and inspected"));assertEquals(actorId,arg("UPDATE invoice SET evidence_accepted_by",1));verify(audit).append(actor,invoiceId,"SERVICE_EVIDENCE_ACCEPTED","Completed and inspected");
    }
    @Test void financeCannotPretendToBeProjectManager(){row("REVIEWED");assertEquals("NOT_ASSIGNED",assertThrows(BusinessException.class,()->service.acceptEvidence(invoiceId,new InvoiceService.Note(3,"Completed"))).code());assertTrue(writes().isEmpty());}
    @Test void commentsRequireNonblankTextAndKeepInvoiceScope(){row("REVIEWED");UUID id=service.comment(invoiceId,new InvoiceService.Comment("  Please check  "));assertEquals("Please check",arg("INSERT INTO discussion",5));verify(audit).append(actor,invoiceId,"COMMENT_ADDED",id.toString());}
    @Test void viewerCannotComment(){as(Role.VIEWER);assertThrows(BusinessException.class,()->service.comment(invoiceId,new InvoiceService.Comment("Hi")));verifyNoInteractions(repo);assertTrue(writes().isEmpty());}
    @Test void onlyClarificationQuestionsCanBeResolved(){row("REVIEWED");assertThrows(BusinessException.class,()->service.resolveQuestion(invoiceId,UUID.randomUUID(),new InvoiceService.Comment("Yes")));assertTrue(writes().isEmpty());}
    @Test void resolvingQuestionAdvancesVersionAndAudit(){row("CLARIFICATION");service.resolveQuestion(invoiceId,UUID.randomUUID(),new InvoiceService.Comment("Confirmed"));assertEquals(2,writes().size());verify(audit).append(actor,invoiceId,"QUESTION_RESOLVED","Confirmed");}
    @Test void missingQuestionDoesNotAdvanceVersion(){row("CLARIFICATION");when(db.update(anyString(),any(Object[].class))).thenReturn(0);assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->service.resolveQuestion(invoiceId,UUID.randomUUID(),new InvoiceService.Comment("Yes"))).code());assertEquals(1,writes().size());verifyNoInteractions(audit);}
    @Test void invalidPagingRejectedBeforeQueries(){assertThrows(BusinessException.class,()->service.list("","",-1,50));assertThrows(BusinessException.class,()->service.list("","",0,101));verifyNoInteractions(db);}
    @Test void unknownStageIsNotTurnedIntoRawSql(){assertThrows(IllegalArgumentException.class,()->service.list("","BAD_STAGE",0,50));verifyNoInteractions(db);}
    @Test void allocationChangeClearsEarlierServiceAcceptance(){
        var row=row("REVIEWED");row.put("evidence_accepted_by",approverId);UUID nextProject=UUID.randomUUID();
        when(db.queryForList("SELECT id,evidence_required FROM project WHERE tenant_id=? AND id=? AND active=true",tenant,nextProject)).thenReturn(List.of(Map.of("id",nextProject,"evidence_required",true)));
        when(db.queryForObject("SELECT EXISTS(SELECT 1 FROM cost_center WHERE tenant_id=? AND id=? AND active=true)",Boolean.class,tenant,costId)).thenReturn(true);
        service.allocate(invoiceId,new InvoiceService.Allocation(3,nextProject,costId,approverId,LocalDate.of(2026,10,31)));
        assertNull(arg("UPDATE invoice SET project_id",8));assertNull(arg("UPDATE invoice SET project_id",9));verify(audit).append(eq(actor),eq(invoiceId),eq("ALLOCATION_UPDATED"),anyString());
    }
    @Test void allocationWithEarlierDueDateDoesNotWrite(){
        row("REVIEWED");when(db.queryForList("SELECT id,evidence_required FROM project WHERE tenant_id=? AND id=? AND active=true",tenant,projectId)).thenReturn(List.of(Map.of("id",projectId,"evidence_required",true)));
        when(db.queryForObject("SELECT EXISTS(SELECT 1 FROM cost_center WHERE tenant_id=? AND id=? AND active=true)",Boolean.class,tenant,costId)).thenReturn(true);
        assertEquals("INVALID_DUE_DATE",assertThrows(BusinessException.class,()->service.allocate(invoiceId,new InvoiceService.Allocation(3,projectId,costId,approverId,LocalDate.of(2026,9,30)))).code());assertTrue(writes().isEmpty());
    }
}
