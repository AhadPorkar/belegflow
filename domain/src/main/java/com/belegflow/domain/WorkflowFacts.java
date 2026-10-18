package com.belegflow.domain;
import java.util.UUID;
/** Facts are loaded by the application from locked, tenant-scoped rows, never from the browser. */
public record WorkflowFacts( InvoiceStage stage, UUID creatorId, UUID assignedApproverId, boolean basicChecksPassed, boolean allocated, boolean unresolvedDuplicate, boolean evidenceRequired, boolean evidenceAccepted, int openQuestions, boolean secondApprovalRequired, UUID firstApprovedBy, UUID secondApprovedBy ) {
}
