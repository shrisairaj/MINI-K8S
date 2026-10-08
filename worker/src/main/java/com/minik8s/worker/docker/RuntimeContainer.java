package com.minik8s.worker.docker;

import java.time.Instant;

public record RuntimeContainer(
        String replicaId,
        String deploymentId,
        String image,
        String containerId,
        String status,
                Instant updatedAt,
                long cpuMillis,
                long memoryBytes) {
        public RuntimeContainer(String replicaId, String deploymentId, String image, String containerId,
                                                        String status, Instant updatedAt) {
                this(replicaId, deploymentId, image, containerId, status, updatedAt, 0, 0);
        }
}