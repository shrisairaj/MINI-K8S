package com.minik8s.worker.master;

public record MasterHeartbeatRequest(
        long totalCpuMillis,
        long totalMemoryBytes,
        long availableCpuMillis,
        long availableMemoryBytes) {
}