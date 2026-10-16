package com.minik8s.master.dto;

import com.minik8s.master.model.ReplicaStatus;

import java.time.Instant;

public record ReplicaSummaryDto(
        String replicaId,
        String deploymentId,
        String workerId,
        String image,
        ReplicaStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
