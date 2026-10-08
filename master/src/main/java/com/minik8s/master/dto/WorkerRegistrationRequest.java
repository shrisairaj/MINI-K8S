package com.minik8s.master.dto;

import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.registry.WorkerRegistration;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record WorkerRegistrationRequest(
        @NotBlank String workerId,
        @NotBlank String host,
        @Min(1) @Max(65535) int grpcPort,
        @Positive long totalCpuMillis,
        @Positive long totalMemoryBytes,
        @Min(0) long availableCpuMillis,
        @Min(0) long availableMemoryBytes) {

    public WorkerRegistration toRegistration() {
        return new WorkerRegistration(workerId, host, grpcPort,
                new WorkerResources(totalCpuMillis, totalMemoryBytes, availableCpuMillis, availableMemoryBytes));
    }
}