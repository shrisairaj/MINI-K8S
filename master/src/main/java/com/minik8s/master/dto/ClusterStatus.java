package com.minik8s.master.dto;

import java.util.List;

/**
 * Represents the overall cluster status payload returned by GET /api/cluster/status.
 */
public record ClusterStatus(
        String status,
        int totalWorkers,
        int healthyWorkers,
        int unhealthyWorkers,
        int totalRunningContainers,
        List<ClusterWorkerStatus> workers) {
}
