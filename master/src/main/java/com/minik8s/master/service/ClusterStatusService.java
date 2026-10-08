package com.minik8s.master.service;

import com.minik8s.master.dto.ClusterStatus;
import com.minik8s.master.dto.ClusterWorkerStatus;
import com.minik8s.master.model.ReplicaStatus;
import com.minik8s.master.model.Worker;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.master.replica.ReplicaManager;
import com.minik8s.master.replica.ReplicaSnapshot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives current cluster status by aggregating information from WorkerRegistry and ReplicaManager.
 */
@Service
public class ClusterStatusService {

    private final WorkerRegistry workerRegistry;
    private final ObjectProvider<ReplicaManager> replicaManagerProvider;

    public ClusterStatusService(WorkerRegistry workerRegistry, ObjectProvider<ReplicaManager> replicaManagerProvider) {
        this.workerRegistry = workerRegistry;
        this.replicaManagerProvider = replicaManagerProvider;
    }

    public ClusterStatus getClusterStatus() {
        List<Worker> activeWorkers = workerRegistry.getAllWorkers().stream()
                .filter(w -> w.status() != WorkerStatus.REMOVED)
                .toList();

        Map<String, Integer> runningContainersPerWorker = getRunningContainersCountPerWorker();

        List<ClusterWorkerStatus> workerStatuses = activeWorkers.stream().map(worker -> {
            boolean isHealthy = (worker.status() == WorkerStatus.READY || worker.status() == WorkerStatus.REGISTERED);
            String healthString = isHealthy ? "HEALTHY" :
                    (worker.status() == WorkerStatus.UNAVAILABLE ? "UNHEALTHY" : worker.status().name());

            int runningCount = runningContainersPerWorker.getOrDefault(worker.workerId(), 0);

            return new ClusterWorkerStatus(
                    worker.workerId(),
                    worker.host(),
                    worker.grpcPort(),
                    healthString,
                    runningCount,
                    worker.lastHeartbeat()
            );
        }).toList();

        int healthyCount = 0;
        int unhealthyCount = 0;
        int totalRunningContainers = 0;

        for (ClusterWorkerStatus ws : workerStatuses) {
            if ("HEALTHY".equals(ws.status())) {
                healthyCount++;
            } else {
                unhealthyCount++;
            }
            totalRunningContainers += ws.runningContainers();
        }

        int totalWorkers = activeWorkers.size();
        String overallStatus;
        if (totalWorkers == 0) {
            overallStatus = "NO_WORKERS";
        } else if (unhealthyCount == 0) {
            overallStatus = "HEALTHY";
        } else if (healthyCount > 0) {
            overallStatus = "DEGRADED";
        } else {
            overallStatus = "UNHEALTHY";
        }

        return new ClusterStatus(
                overallStatus,
                totalWorkers,
                healthyCount,
                unhealthyCount,
                totalRunningContainers,
                workerStatuses
        );
    }

    private Map<String, Integer> getRunningContainersCountPerWorker() {
        ReplicaManager replicaManager = replicaManagerProvider.getIfAvailable();
        if (replicaManager == null) {
            return Collections.emptyMap();
        }

        Map<String, Integer> counts = new HashMap<>();
        try {
            List<ReplicaSnapshot> snapshots = replicaManager.reconcileAll();
            for (ReplicaSnapshot snapshot : snapshots) {
                for (var replica : snapshot.replicas()) {
                    if (replica.status() == ReplicaStatus.RUNNING || replica.status() == ReplicaStatus.STARTING) {
                        counts.merge(replica.workerId(), 1, Integer::sum);
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback gracefully if reconciliation/snapshot cannot be evaluated
        }
        return counts;
    }
}
