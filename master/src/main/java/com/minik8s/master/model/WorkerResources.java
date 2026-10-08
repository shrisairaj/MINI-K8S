package com.minik8s.master.model;

public record WorkerResources(
        long totalCpuMillis,
        long totalMemoryBytes,
        long availableCpuMillis,
        long availableMemoryBytes) {

    public WorkerResources {
        if (totalCpuMillis <= 0 || totalMemoryBytes <= 0) {
            throw new IllegalArgumentException("Total CPU and memory must be positive");
        }
        if (availableCpuMillis < 0 || availableMemoryBytes < 0
                || availableCpuMillis > totalCpuMillis
                || availableMemoryBytes > totalMemoryBytes) {
            throw new IllegalArgumentException("Available resources must be within total capacity");
        }
    }

    public boolean canFit(ResourceRequest request) {
        return request.cpuMillis() <= availableCpuMillis
                && request.memoryBytes() <= availableMemoryBytes;
    }

    public WorkerResources withAvailable(long cpuMillis, long memoryBytes) {
        return new WorkerResources(totalCpuMillis, totalMemoryBytes,
                Math.min(cpuMillis, totalCpuMillis), Math.min(memoryBytes, totalMemoryBytes));
    }
}