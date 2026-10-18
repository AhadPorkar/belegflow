package com.belegflow.domain;
import static com.belegflow.domain.BusinessException.require;
/** Deterministic workflow policy. No Spring, clock, database or network dependency. */
public final class WorkflowPolicy {
    public record Decision(InvoiceStage stage, String event, int approvalSlot) {
    }
    public Decision decide(WorkflowFacts f, WorkflowAction action, Actor actor, boolean activeDelegate, String note) {
        require(f != null && action != null, "INVALID_INPUT");
        boolean finance = actor.role().managesFinance();
        boolean assigned = actor.userId().equals(f.assignedApproverId()) || activeDelegate;
        switch (action) {
            case REVIEW -> {
                require(finance, "FORBIDDEN");
                require(f.stage() == InvoiceStage.RECEIVED, "INVALID_TRANSITION");
                return new Decision(InvoiceStage.REVIEWED, "INVOICE_REVIEWED", 0);
            }
            case CLARIFY -> {
                require(finance || (assigned && actor.role().mayApprove()), "FORBIDDEN");
                require(f.stage().editable() || f.stage() == InvoiceStage.READY_FOR_APPROVAL, "INVALID_TRANSITION");
                note(note);
                return new Decision(InvoiceStage.CLARIFICATION, "CLARIFICATION_REQUESTED", 0);
            }
            case RESOLVE -> {
                require(finance, "FORBIDDEN");
                require(f.stage() == InvoiceStage.CLARIFICATION, "INVALID_TRANSITION");
                require(f.openQuestions() == 0, "OPEN_QUESTIONS");
                return new Decision(InvoiceStage.REVIEWED, "CLARIFICATION_RESOLVED", 0);
            }
            case SUBMIT -> {
                require(finance, "FORBIDDEN");
                require(f.stage() == InvoiceStage.REVIEWED, "INVALID_TRANSITION");
                checkReady(f);
                require(!f.creatorId().equals(f.assignedApproverId()), "SELF_APPROVAL");
                return new Decision(InvoiceStage.READY_FOR_APPROVAL, "SUBMITTED_FOR_APPROVAL", 0);
            }
            case APPROVE -> {
                require(f.stage() == InvoiceStage.READY_FOR_APPROVAL, "INVALID_TRANSITION");
                require(actor.role().mayApprove(), "FORBIDDEN");
                require(!actor.userId().equals(f.creatorId()), "SELF_APPROVAL");
                checkReady(f);
                if (f.firstApprovedBy() == null) {
                    require(assigned, "NOT_ASSIGNED");
                    return new Decision(f.secondApprovalRequired() ? InvoiceStage.READY_FOR_APPROVAL : InvoiceStage.APPROVED, f.secondApprovalRequired() ? "FIRST_APPROVAL_RECORDED" : "INVOICE_APPROVED", 1);
                }
                require(f.secondApprovalRequired() && f.secondApprovedBy() == null, "INVALID_TRANSITION");
                require(actor.role() == Role.ADMIN, "SECOND_APPROVAL_ADMIN_ONLY");
                require(!actor.userId().equals(f.firstApprovedBy()), "DISTINCT_APPROVERS_REQUIRED");
                return new Decision(InvoiceStage.APPROVED, "INVOICE_APPROVED", 2);
            }
            case REJECT -> {
                require(finance || (assigned && actor.role().mayApprove()), "FORBIDDEN");
                require(f.stage().editable() || f.stage() == InvoiceStage.READY_FOR_APPROVAL, "INVALID_TRANSITION");
                note(note);
                return new Decision(InvoiceStage.REJECTED, "INVOICE_REJECTED", 0);
            }
            case REOPEN -> {
                require(finance, "FORBIDDEN");
                require(f.stage() == InvoiceStage.REJECTED, "INVALID_TRANSITION");
                note(note);
                return new Decision(InvoiceStage.REVIEWED, "INVOICE_REOPENED", 0);
            }
            case EXPORT -> {
                require(finance, "FORBIDDEN");
                require(f.stage() == InvoiceStage.APPROVED, "EXPORT_REQUIRES_APPROVAL");
                require(f.firstApprovedBy() != null, "EXPORT_REQUIRES_APPROVAL");
                require(!f.secondApprovalRequired() || f.secondApprovedBy() != null, "SECOND_APPROVAL_REQUIRED");
                return new Decision(InvoiceStage.EXPORTED, "ACCOUNTING_PACKAGE_CREATED", 0);
            }
        }
        throw new BusinessException("INVALID_TRANSITION");
    }
    private void checkReady(WorkflowFacts f) {
        require(f.basicChecksPassed(), "VALIDATION_FAILED");
        require(f.allocated(), "ALLOCATION_REQUIRED");
        require(!f.unresolvedDuplicate(), "DUPLICATE_UNRESOLVED");
        require(f.openQuestions() == 0, "OPEN_QUESTIONS");
        require(!f.evidenceRequired() || f.evidenceAccepted(), "EVIDENCE_REQUIRED");
    }
    private void note(String note) {
        require(note != null && !note.isBlank() && note.length() <= 2000, "NOTE_REQUIRED");
    }
}
