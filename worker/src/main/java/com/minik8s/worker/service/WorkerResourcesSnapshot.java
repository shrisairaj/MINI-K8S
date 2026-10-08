package com.minik8s.worker.service;

public record WorkerResourcesSnapshot(
        long totalCpuMillis,
        long totalMemoryBytes,
        long availableCpuMillis,
        long availableMemoryBytes,
        int runningReplicas) {
}