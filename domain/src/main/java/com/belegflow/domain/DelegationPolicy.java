package com.belegflow.domain;
import java.time.Instant;
import java.util.UUID;
import static com.belegflow.domain.BusinessException.require;
public final class DelegationPolicy {
    private DelegationPolicy() {
    }
    public static boolean active(UUID owner, UUID delegate, UUID assigned, UUID actor, Instant from, Instant until, Instant now) {
        return owner.equals(assigned) && delegate.equals(actor) && !owner.equals(delegate) && !now.isBefore(from) && now.isBefore(until);
        // half-open interval avoids boundary overlap
    }
    public static void validate(UUID owner, UUID delegate, Instant from, Instant until) {
        require(owner != null && delegate != null && !owner.equals(delegate), "INVALID_DELEGATION");
        require(from != null && until != null && until.isAfter(from), "INVALID_DELEGATION");
    }
}
