package com.minik8s.master.model;

import java.time.Instant;
import java.util.Objects;

public record Worker(
        String workerId,
        String host,
        int grpcPort,
        WorkerStatus status,
        WorkerResources resources,
        Instant lastHeartbeat) {

    public Worker {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("Worker ID must not be blank");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Worker host must not be blank");
        }
        if (grpcPort < 1 || grpcPort > 65535) {
            throw new IllegalArgumentException("gRPC port must be between 1 and 65535");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(resources, "resources");
    }

    public Worker withStatus(WorkerStatus newStatus) {
        return new Worker(workerId, host, grpcPort, newStatus, resources, lastHeartbeat);
    }

    public Worker withHeartbeat(WorkerResources newResources, Instant heartbeat, WorkerStatus newStatus) {
        return new Worker(workerId, host, grpcPort, newStatus, newResources, heartbeat);
    }

    public Worker withResources(WorkerResources newResources) {
        return new Worker(workerId, host, grpcPort, status, newResources, lastHeartbeat);
    }
}