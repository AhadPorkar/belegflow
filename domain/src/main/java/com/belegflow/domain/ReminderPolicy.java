package com.belegflow.domain;
import java.time.*;
public final class ReminderPolicy {
    private ReminderPolicy() {
    }
    public static LocalDate businessDate(Instant instant) {
        return instant.atZone(ZoneId.of("Europe/Berlin")).toLocalDate();
    }
    public static boolean due(InvoiceStage stage, LocalDate dueOn, Instant now) {
        return stage == InvoiceStage.READY_FOR_APPROVAL && dueOn != null && !dueOn.isAfter(businessDate(now).plusDays(3));
    }
}
