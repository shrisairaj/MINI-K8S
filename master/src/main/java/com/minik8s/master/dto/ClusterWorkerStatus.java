package com.minik8s.master.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Detailed status summary for an individual worker node within the cluster status response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClusterWorkerStatus(
        String workerId,
        String host,
        int grpcPort,
        String status,
        int runningContainers,
        Instant lastHeartbeat) {
}
