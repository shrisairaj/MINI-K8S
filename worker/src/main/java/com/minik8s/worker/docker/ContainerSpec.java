package com.minik8s.worker.docker;

public record ContainerSpec(String replicaId, String deploymentId, String image, long cpuMillis, long memoryBytes) {
}