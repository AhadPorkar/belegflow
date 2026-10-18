package com.belegflow.domain;
public enum InvoiceStage {
    RECEIVED, REVIEWED, CLARIFICATION, READY_FOR_APPROVAL, APPROVED, REJECTED, EXPORTED;
    public boolean editable() {
        return this == RECEIVED || this == REVIEWED || this == CLARIFICATION;
    }
}
