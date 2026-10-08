package com.minik8s.master.model;

import java.time.Instant;
import java.util.Objects;

public record SchedulingDecision(
        String reservationId,
        Worker worker,
        ResourceRequest resources,
        Instant decidedAt) {

    public SchedulingDecision {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID must not be blank");
        }
        Objects.requireNonNull(worker, "worker");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(decidedAt, "decidedAt");
    }
}