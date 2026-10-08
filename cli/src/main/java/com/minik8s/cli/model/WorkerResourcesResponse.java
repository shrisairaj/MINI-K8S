package com.minik8s.cli.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * CLI-local representation of a Worker's resource information.
 *
 * <p>Mirrors the {@code WorkerResources} record from the Master:
 * <ul>
 *   <li>{@code totalCpuMillis}      — total CPU capacity in millicores</li>
 *   <li>{@code totalMemoryBytes}    — total memory capacity in bytes</li>
 *   <li>{@code availableCpuMillis}  — currently available CPU in millicores</li>
 *   <li>{@code availableMemoryBytes} — currently available memory in bytes</li>
 * </ul>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerResourcesResponse {

    private long totalCpuMillis;
    private long totalMemoryBytes;
    private long availableCpuMillis;
    private long availableMemoryBytes;

    // Default constructor required by Jackson
    public WorkerResourcesResponse() {}

    public long getTotalCpuMillis() {
        return totalCpuMillis;
    }

    public void setTotalCpuMillis(long totalCpuMillis) {
        this.totalCpuMillis = totalCpuMillis;
    }

    public long getTotalMemoryBytes() {
        return totalMemoryBytes;
    }

    public void setTotalMemoryBytes(long totalMemoryBytes) {
        this.totalMemoryBytes = totalMemoryBytes;
    }

    public long getAvailableCpuMillis() {
        return availableCpuMillis;
    }

    public void setAvailableCpuMillis(long availableCpuMillis) {
        this.availableCpuMillis = availableCpuMillis;
    }

    public long getAvailableMemoryBytes() {
        return availableMemoryBytes;
    }

    public void setAvailableMemoryBytes(long availableMemoryBytes) {
        this.availableMemoryBytes = availableMemoryBytes;
    }

    /**
     * Returns a short human-readable representation, e.g. "500m / 2048 MiB".
     */
    public String formatCpu() {
        return availableCpuMillis + "m / " + totalCpuMillis + "m";
    }

    public String formatMemory() {
        return formatMib(availableMemoryBytes) + " / " + formatMib(totalMemoryBytes) + " MiB";
    }

    private static String formatMib(long bytes) {
        return String.valueOf(bytes / (1024L * 1024L));
    }
}
