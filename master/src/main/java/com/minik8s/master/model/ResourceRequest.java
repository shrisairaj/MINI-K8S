package com.minik8s.master.model;

public record ResourceRequest(long cpuMillis, long memoryBytes) {
    public ResourceRequest {
        if (cpuMillis < 0 || memoryBytes < 0) {
            throw new IllegalArgumentException("Resource requests cannot be negative");
        }
    }
}