package com.minik8s.master.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DeploymentRequest(
        String id,
        @NotBlank(message = "Deployment name cannot be blank")
        String name,
        @NotBlank(message = "Container image cannot be blank")
        String image,
        @Min(value = 0, message = "Replicas count cannot be negative")
        int replicas,
        @Min(value = 1, message = "Container port must be between 1 and 65535")
        @Max(value = 65535, message = "Container port must be between 1 and 65535")
        int containerPort,
        Long cpuMillis,
        Long memoryBytes
) {
    public DeploymentRequest {
        if (id == null || id.isBlank()) {
            id = name;
        }
    }
}
