package com.minik8s.master.registry;

import com.minik8s.master.model.WorkerResources;

public record WorkerRegistration(String workerId, String host, int grpcPort, WorkerResources resources) {
    public WorkerRegistration {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("Worker ID must not be blank");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Worker host must not be blank");
        }
        if (grpcPort < 1 || grpcPort > 65535) {
            throw new IllegalArgumentException("gRPC port must be between 1 and 65535");
        }
        if (resources == null) {
            throw new IllegalArgumentException("Worker resources are required");
        }
    }
}