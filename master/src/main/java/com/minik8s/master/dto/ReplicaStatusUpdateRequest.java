package com.minik8s.master.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplicaStatusUpdateRequest(
        @NotBlank String deploymentId,
        @NotBlank String replicaId,
        @NotBlank String status) {
}