package com.belegflow.domain;
public enum Role {
    ADMIN, FINANCE, APPROVER, VIEWER;
    public boolean managesFinance() {
        return this == ADMIN || this == FINANCE;
    }
    public boolean mayApprove() {
        return this == ADMIN || this == APPROVER;
    }
}
