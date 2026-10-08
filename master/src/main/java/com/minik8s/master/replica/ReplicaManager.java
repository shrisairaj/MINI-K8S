package com.minik8s.master.replica;

import com.minik8s.master.exception.DeploymentNotFoundException;
import com.minik8s.master.exception.NoAvailableWorkerException;
import com.minik8s.master.grpc.ReplicaLaunchRequest;
import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.grpc.WorkerOperationRejectedException;
import com.minik8s.master.model.Replica;
import com.minik8s.master.model.ReplicaStatus;
import com.minik8s.master.model.SchedulingDecision;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.master.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ReplicaManager {
    private static final Logger logger = LoggerFactory.getLogger(ReplicaManager.class);

    private final Scheduler scheduler;
    private final WorkerClient workerClient;
    private final WorkerRegistry workerRegistry;
    private final ConcurrentMap<String, DeploymentState> deployments = new ConcurrentHashMap<>();

    public ReplicaManager(Scheduler scheduler, WorkerClient workerClient, WorkerRegistry workerRegistry) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.workerClient = Objects.requireNonNull(workerClient, "workerClient");
        this.workerRegistry = Objects.requireNonNull(workerRegistry, "workerRegistry");
    }

    public void updateDesiredState(ReplicaWorkload workload) {
        Objects.requireNonNull(workload, "workload");
        DeploymentState state = deployments.computeIfAbsent(workload.deploymentId(), ignored -> new DeploymentState());
        synchronized (state) {
            state.workload = workload;
        }
    }

    public ReplicaSnapshot reconcile(String deploymentId) {
        DeploymentState state = requireDeployment(deploymentId);
        synchronized (state) {
            return reconcileLocked(deploymentId, state);
        }
    }

    public List<ReplicaSnapshot> reconcileAll() {
        return deployments.keySet().stream().sorted().map(this::reconcile).toList();
    }

    public ReplicaSnapshot getSnapshot(String deploymentId) {
        DeploymentState state = requireDeployment(deploymentId);
        synchronized (state) {
            return snapshot(deploymentId, state);
        }
    }

    public ReplicaSnapshot reportReplicaStatus(String deploymentId, String replicaId, String workerId,
                                               ReplicaStatus reportedStatus) {
        Objects.requireNonNull(reportedStatus, "reportedStatus");
        DeploymentState state = requireDeployment(deploymentId);
        synchronized (state) {
            Replica replica = state.replicas.get(replicaId);
            if (replica == null) {
                throw new IllegalArgumentException("Replica not found: " + replicaId);
            }
            if (!replica.workerId().equals(workerId)) {
                throw new IllegalArgumentException("Replica status reporter does not match assigned worker");
            }
            if (replica.status() == ReplicaStatus.FAILED || replica.status() == ReplicaStatus.STOPPED) {
                return snapshot(deploymentId, state);
            }
            if (reportedStatus == ReplicaStatus.STARTING || reportedStatus == replica.status()) {
                return snapshot(deploymentId, state);
            }
            if (reportedStatus != ReplicaStatus.RUNNING && reportedStatus != ReplicaStatus.FAILED
                    && reportedStatus != ReplicaStatus.STOPPED) {
                throw new IllegalArgumentException("Worker cannot report replica state " + reportedStatus);
            }
            if (replica.status() == ReplicaStatus.STOPPING && reportedStatus == ReplicaStatus.RUNNING) {
                return snapshot(deploymentId, state);
            }
            Replica updated = replica.transitionTo(reportedStatus);
            state.replicas.put(replicaId, updated);
            if (reportedStatus == ReplicaStatus.FAILED || reportedStatus == ReplicaStatus.STOPPED) {
                workerRegistry.releaseResources(replica.workerId(), replica.replicaId());
            }
            return snapshot(deploymentId, state);
        }
    }

    public void handleWorkerUnavailable(String workerId) {
        for (var deploymentEntry : deployments.entrySet()) {
            DeploymentState state = deploymentEntry.getValue();
            synchronized (state) {
                failReplicasOnWorker(state, workerId);
            }
        }
    }

    private ReplicaSnapshot reconcileLocked(String deploymentId, DeploymentState state) {
        // This lock is per deployment and deliberately spans short idempotent Worker RPCs so
        // scale and reconcile operations cannot issue conflicting start/stop commands.
        ReplicaWorkload workload = state.workload;
        markUnavailableReplicas(state);
        List<Replica> stopping = state.replicas.values().stream()
            .filter(replica -> replica.status() == ReplicaStatus.STOPPING)
            .toList();
        stopping.forEach(replica -> requestStop(state, replica));

        long occupying = state.replicas.values().stream().filter(ReplicaManager::occupiesSlot).count();
        if (occupying > workload.desiredReplicas()) {
            int excess = Math.toIntExact(occupying - workload.desiredReplicas());
            long stopInProgress = state.replicas.values().stream()
                .filter(replica -> replica.status() == ReplicaStatus.STOPPING).count();
            int additionalStops = Math.max(0, excess - Math.toIntExact(stopInProgress));
            List<Replica> toStop = state.replicas.values().stream()
                    .filter(replica -> replica.status() == ReplicaStatus.RUNNING
                            || replica.status() == ReplicaStatus.STARTING
                            || replica.status() == ReplicaStatus.PENDING)
                    .sorted(Comparator.comparing(Replica::createdAt).reversed())
                .limit(additionalStops)
                    .toList();
            for (Replica replica : toStop) {
            beginStop(state, replica);
            }
        } else if (occupying < workload.desiredReplicas()
            && state.replicas.values().stream().noneMatch(replica -> replica.status() == ReplicaStatus.STOPPING)) {
            long missing = workload.desiredReplicas() - occupying;
            for (long i = 0; i < missing; i++) {
                if (!startReplica(deploymentId, state, workload)) {
                    break;
                }
            }
        }
        return snapshot(deploymentId, state);
    }

    private boolean startReplica(String deploymentId, DeploymentState state, ReplicaWorkload workload) {
        String replicaId = UUID.randomUUID().toString();
        SchedulingDecision decision;
        try {
            decision = scheduler.schedule(replicaId, workload.resources());
        } catch (NoAvailableWorkerException exception) {
            logger.info("Cannot reconcile deployment {} yet: {}", deploymentId, exception.getMessage());
            return false;
        }

        Instant now = Instant.now();
        Replica pending = new Replica(replicaId, deploymentId, workload.image(),
                decision.worker().workerId(), workload.resources(), ReplicaStatus.PENDING, now, now);
        Replica starting = pending.transitionTo(ReplicaStatus.STARTING);
        state.replicas.put(replicaId, starting);
        try {
            workerClient.startReplica(new ReplicaLaunchRequest(replicaId, deploymentId,
                    decision.worker().workerId(), workload.image(), workload.resources()));
            return true;
        } catch (WorkerOperationRejectedException exception) {
            state.replicas.put(replicaId, starting.transitionTo(ReplicaStatus.FAILED));
            workerRegistry.releaseResources(decision.worker().workerId(), replicaId);
            logger.warn("Worker {} rejected replica {} for deployment {}", decision.worker().workerId(),
                    replicaId, deploymentId, exception);
            return false;
        } catch (RuntimeException exception) {
            // The RPC may have reached the Worker. Keep the reservation and STARTING state until
            // a status report resolves the outcome; retrying with a new ID could create a duplicate.
            logger.warn("Start outcome for replica {} is unknown; awaiting Worker status", replicaId, exception);
            return true;
        }
    }

    private void beginStop(DeploymentState state, Replica replica) {
        Replica stopping = replica.transitionTo(ReplicaStatus.STOPPING);
        state.replicas.put(replica.replicaId(), stopping);
        requestStop(state, stopping);
    }

    private void requestStop(DeploymentState state, Replica stopping) {
        try {
            workerClient.stopReplica(stopping.workerId(), stopping.replicaId());
            state.replicas.put(stopping.replicaId(), stopping.transitionTo(ReplicaStatus.STOPPED));
            workerRegistry.releaseResources(stopping.workerId(), stopping.replicaId());
        } catch (RuntimeException exception) {
            // Keep STOPPING so the next reconciliation retries the idempotent stop request.
            logger.warn("Failed to stop replica {}; it will be retried", stopping.replicaId(), exception);
        }
    }

    private void markUnavailableReplicas(DeploymentState state) {
        for (Replica replica : new ArrayList<>(state.replicas.values())) {
            boolean unavailable = workerRegistry.getWorker(replica.workerId())
                    .map(worker -> worker.status() == WorkerStatus.UNAVAILABLE
                            || worker.status() == WorkerStatus.REMOVED)
                    .orElse(true);
            if (unavailable && occupiesSlot(replica)) {
                failReplicasOnWorker(state, replica.workerId());
            }
        }
    }

    private void failReplicasOnWorker(DeploymentState state, String workerId) {
        for (Replica replica : new ArrayList<>(state.replicas.values())) {
            if (replica.workerId().equals(workerId) && occupiesSlot(replica)) {
                state.replicas.put(replica.replicaId(), replica.transitionTo(ReplicaStatus.FAILED));
                workerRegistry.releaseResources(workerId, replica.replicaId());
            }
        }
    }

    private DeploymentState requireDeployment(String deploymentId) {
        DeploymentState state = deployments.get(deploymentId);
        if (state == null) {
            throw new DeploymentNotFoundException(deploymentId);
        }
        return state;
    }

    private ReplicaSnapshot snapshot(String deploymentId, DeploymentState state) {
        return new ReplicaSnapshot(deploymentId, state.workload.desiredReplicas(),
                state.replicas.values().stream().sorted(Comparator.comparing(Replica::createdAt).thenComparing(Replica::replicaId)).toList());
    }

    private static boolean occupiesSlot(Replica replica) {
        return switch (replica.status()) {
            case PENDING, STARTING, RUNNING, STOPPING -> true;
            case FAILED, STOPPED -> false;
        };
    }

    private static final class DeploymentState {
        private ReplicaWorkload workload;
        private final ConcurrentMap<String, Replica> replicas = new ConcurrentHashMap<>();
    }
}