package com.minik8s.master.service;

import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.model.Worker;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.master.replica.ReplicaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Periodically checks the health of registered Worker nodes over gRPC and updates their state in WorkerRegistry.
 */
@Service
public class HealthMonitor {
    private static final Logger logger = LoggerFactory.getLogger(HealthMonitor.class);

    private final WorkerRegistry workerRegistry;
    private final ObjectProvider<WorkerClient> workerClientProvider;
    private final ObjectProvider<ReplicaManager> replicaManagerProvider;
    private final long heartbeatTimeoutMillis;

    public HealthMonitor(WorkerRegistry workerRegistry,
                         ObjectProvider<WorkerClient> workerClientProvider,
                         ObjectProvider<ReplicaManager> replicaManagerProvider,
                         @Value("${health.monitor.heartbeat-timeout:${minik8s.health.monitor.heartbeat-timeout:15000}}") long heartbeatTimeoutMillis) {
        this.workerRegistry = workerRegistry;
        this.workerClientProvider = workerClientProvider;
        this.replicaManagerProvider = replicaManagerProvider;
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
    }

    @Scheduled(fixedDelayString = "${health.monitor.interval:${minik8s.health.monitor.interval:5000}}")
    public void checkWorkers() {
        List<Worker> workers = workerRegistry.getAllWorkers().stream()
                .filter(w -> w.status() != WorkerStatus.REMOVED)
                .toList();

        if (workers.isEmpty()) {
            logger.debug("[HealthMonitor] No registered workers to check.");
            return;
        }

        WorkerClient workerClient = workerClientProvider.getIfAvailable();
        ReplicaManager replicaManager = replicaManagerProvider.getIfAvailable();

        for (Worker worker : workers) {
            checkSingleWorker(worker, workerClient, replicaManager);
        }
    }

    private void checkSingleWorker(Worker worker, WorkerClient workerClient, ReplicaManager replicaManager) {
        String workerId = worker.workerId();
        logger.info("[HealthMonitor] Checking {}", workerId);

        try {
            boolean isHealthy = true;

            // 1. Perform gRPC health check call if WorkerClient is present
            if (workerClient != null) {
                try {
                    isHealthy = workerClient.checkHealth(workerId, worker.host(), worker.grpcPort());
                } catch (Exception e) {
                    logger.warn("[HealthMonitor] Failed to contact {}: {}", workerId, e.getMessage());
                    isHealthy = false;
                }
            }

            // 2. Check for heartbeat staleness if gRPC check succeeded
            if (isHealthy && worker.lastHeartbeat() != null) {
                long elapsed = Duration.between(worker.lastHeartbeat(), Instant.now()).toMillis();
                if (elapsed > heartbeatTimeoutMillis) {
                    logger.warn("[HealthMonitor] Heartbeat for {} is stale ({} ms old)", workerId, elapsed);
                    isHealthy = false;
                }
            }

            if (isHealthy) {
                if (worker.status() == WorkerStatus.UNAVAILABLE) {
                    workerRegistry.updateStatus(workerId, WorkerStatus.READY);
                }
                logger.info("[HealthMonitor] {} is HEALTHY", workerId);
            } else {
                if (worker.status() != WorkerStatus.UNAVAILABLE) {
                    workerRegistry.updateStatus(workerId, WorkerStatus.UNAVAILABLE);
                    if (replicaManager != null) {
                        replicaManager.handleWorkerUnavailable(workerId);
                    }
                }
                logger.warn("[HealthMonitor] {} is UNHEALTHY", workerId);
            }

        } catch (Exception ex) {
            logger.error("[HealthMonitor] Failed to contact {}: {}", workerId, ex.getMessage());
        }
    }
}
