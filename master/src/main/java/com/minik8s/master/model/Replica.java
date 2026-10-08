package com.minik8s.master.model;

import java.time.Instant;
import java.util.Objects;

public record Replica(
        String replicaId,
        String deploymentId,
        String image,
        String workerId,
        ResourceRequest resources,
        ReplicaStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public Replica {
        if (replicaId == null || replicaId.isBlank()
                || deploymentId == null || deploymentId.isBlank()
                || image == null || image.isBlank()
                || workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("Replica identity and image fields must not be blank");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public Replica transitionTo(ReplicaStatus nextStatus) {
        if (!isAllowedTransition(status, nextStatus)) {
            throw new IllegalStateException("Invalid replica transition: " + status + " -> " + nextStatus);
        }
        return new Replica(replicaId, deploymentId, image, workerId, resources,
                nextStatus, createdAt, Instant.now());
    }

    private static boolean isAllowedTransition(ReplicaStatus current, ReplicaStatus next) {
        return switch (current) {
                case PENDING -> next == ReplicaStatus.STARTING || next == ReplicaStatus.FAILED
                    || next == ReplicaStatus.STOPPING || next == ReplicaStatus.STOPPED;
            case STARTING -> next == ReplicaStatus.RUNNING || next == ReplicaStatus.FAILED || next == ReplicaStatus.STOPPING;
            case RUNNING -> next == ReplicaStatus.FAILED || next == ReplicaStatus.STOPPING;
            case STOPPING -> next == ReplicaStatus.STOPPED || next == ReplicaStatus.FAILED;
            case FAILED, STOPPED -> false;
        };
    }
}