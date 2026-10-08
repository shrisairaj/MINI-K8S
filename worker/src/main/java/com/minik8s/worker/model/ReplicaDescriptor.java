package com.minik8s.worker.model;

import java.time.Instant;

public record ReplicaDescriptor(
        String replicaId,
        String deploymentId,
        String workerId,
        String image,
        long cpuMillis,
        long memoryBytes,
        String containerId,
        ReplicaStatus status,
        String message,
                Instant updatedAt) {
        public ReplicaDescriptor withStatus(ReplicaStatus next, String nextMessage) {
                return new ReplicaDescriptor(replicaId, deploymentId, workerId, image, cpuMillis, memoryBytes,
                                containerId, next, nextMessage, Instant.now());
        }
}