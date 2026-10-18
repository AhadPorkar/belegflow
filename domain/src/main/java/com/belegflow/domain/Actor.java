package com.belegflow.domain;
import java.util.UUID;
import java.util.Objects;
public record Actor(UUID tenantId, UUID userId, String name, Role role) {
    public Actor {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(role);
    }
    public void finance() {
        BusinessException.require(role.managesFinance(), "FORBIDDEN");
    }
    public void admin() {
        BusinessException.require(role == Role.ADMIN, "FORBIDDEN");
    }
}
