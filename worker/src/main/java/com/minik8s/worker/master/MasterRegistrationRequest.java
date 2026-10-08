package com.minik8s.worker.master;

public record MasterRegistrationRequest(
        String workerId,
        String host,
        int grpcPort,
        long totalCpuMillis,
        long totalMemoryBytes,
        long availableCpuMillis,
        long availableMemoryBytes) {
}