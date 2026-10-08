package com.minik8s.master.dto;

import com.minik8s.master.model.WorkerResources;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record WorkerHeartbeatRequest(
        @Positive long totalCpuMillis,
        @Positive long totalMemoryBytes,
        @Min(0) long availableCpuMillis,
        @Min(0) long availableMemoryBytes) {

    public WorkerResources toResources() {
        return new WorkerResources(totalCpuMillis, totalMemoryBytes, availableCpuMillis, availableMemoryBytes);
    }
}