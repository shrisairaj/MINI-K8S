package com.minik8s.master.service;

import com.minik8s.master.dto.DeploymentRequest;
import com.minik8s.master.dto.DeploymentResponse;
import com.minik8s.master.dto.ReplicaSummaryDto;
import com.minik8s.master.exception.DeploymentNotFoundException;
import com.minik8s.master.model.Deployment;
import com.minik8s.master.model.DeploymentStatus;
import com.minik8s.master.model.Replica;
import com.minik8s.master.model.ReplicaStatus;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.replica.ReplicaManager;
import com.minik8s.master.replica.ReplicaSnapshot;
import com.minik8s.master.replica.ReplicaWorkload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DeploymentService {
    private static final long DEFAULT_CPU_MILLIS = 500L;
    private static final long DEFAULT_MEMORY_BYTES = 256L * 1024L * 1024L; // 256 MiB

    private final ReplicaManager replicaManager;
    private final ConcurrentMap<String, Deployment> deployments = new ConcurrentHashMap<>();

    public DeploymentService(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    public DeploymentResponse createOrUpdateDeployment(DeploymentRequest request) {
        String id = request.id();
        long cpu = request.cpuMillis() != null && request.cpuMillis() > 0 ? request.cpuMillis() : DEFAULT_CPU_MILLIS;
        long memory = request.memoryBytes() != null && request.memoryBytes() > 0 ? request.memoryBytes() : DEFAULT_MEMORY_BYTES;

        Deployment deployment = deployments.compute(id, (k, existing) -> {
            if (existing == null) {
                return new Deployment(id, request.name(), request.image(), request.replicas(), request.containerPort(), DeploymentStatus.PENDING);
            }
            existing.setName(request.name());
            existing.setImage(request.image());
            existing.setReplicas(request.replicas());
            existing.setContainerPort(request.containerPort());
            return existing;
        });

        ReplicaWorkload workload = new ReplicaWorkload(id, request.image(), request.replicas(), new ResourceRequest(cpu, memory));
        replicaManager.updateDesiredState(workload);
        ReplicaSnapshot snapshot = replicaManager.reconcile(id);

        DeploymentStatus status = computeStatus(deployment.getReplicas(), snapshot);
        deployment.setStatus(status);

        return toResponse(deployment, snapshot);
    }

    public DeploymentResponse getDeployment(String deploymentId) {
        Deployment deployment = deployments.get(deploymentId);
        if (deployment == null) {
            throw new DeploymentNotFoundException(deploymentId);
        }
        ReplicaSnapshot snapshot = replicaManager.getSnapshot(deploymentId);
        DeploymentStatus status = computeStatus(deployment.getReplicas(), snapshot);
        deployment.setStatus(status);
        return toResponse(deployment, snapshot);
    }

    public List<DeploymentResponse> listDeployments() {
        return deployments.values().stream()
                .sorted(Comparator.comparing(Deployment::getId))
                .map(deployment -> {
                    ReplicaSnapshot snapshot;
                    try {
                        snapshot = replicaManager.getSnapshot(deployment.getId());
                    } catch (Exception e) {
                        snapshot = new ReplicaSnapshot(deployment.getId(), deployment.getReplicas(), List.of());
                    }
                    DeploymentStatus status = computeStatus(deployment.getReplicas(), snapshot);
                    deployment.setStatus(status);
                    return toResponse(deployment, snapshot);
                })
                .toList();
    }

    public void deleteDeployment(String deploymentId) {
        Deployment deployment = deployments.remove(deploymentId);
        if (deployment == null) {
            throw new DeploymentNotFoundException(deploymentId);
        }
        replicaManager.deleteDeployment(deploymentId);
    }

    public List<ReplicaSummaryDto> listReplicas(String deploymentId) {
        if (deploymentId != null && !deploymentId.isBlank()) {
            Deployment deployment = deployments.get(deploymentId);
            if (deployment == null) {
                throw new DeploymentNotFoundException(deploymentId);
            }
            ReplicaSnapshot snapshot = replicaManager.getSnapshot(deploymentId);
            return snapshot.replicas().stream().map(this::toReplicaSummary).toList();
        }

        List<ReplicaSummaryDto> allReplicas = new ArrayList<>();
        for (ReplicaSnapshot snapshot : replicaManager.getAllSnapshots()) {
            for (Replica replica : snapshot.replicas()) {
                allReplicas.add(toReplicaSummary(replica));
            }
        }
        return allReplicas.stream().sorted(Comparator.comparing(ReplicaSummaryDto::replicaId)).toList();
    }

    private DeploymentStatus computeStatus(int desiredReplicas, ReplicaSnapshot snapshot) {
        if (desiredReplicas == 0) {
            return DeploymentStatus.STOPPED;
        }
        long running = snapshot.runningCount();
        if (running == desiredReplicas) {
            return DeploymentStatus.RUNNING;
        }
        boolean hasFailed = snapshot.replicas().stream().anyMatch(r -> r.status() == ReplicaStatus.FAILED);
        if (hasFailed && running == 0) {
            return DeploymentStatus.FAILED;
        }
        if (hasFailed) {
            return DeploymentStatus.DEGRADED;
        }
        if (running > 0 && running < desiredReplicas) {
            return DeploymentStatus.SCALING;
        }
        return DeploymentStatus.PENDING;
    }

    private DeploymentResponse toResponse(Deployment deployment, ReplicaSnapshot snapshot) {
        List<ReplicaSummaryDto> replicaDtos = snapshot.replicas().stream()
                .map(this::toReplicaSummary)
                .toList();

        return new DeploymentResponse(
                deployment.getId(),
                deployment.getName(),
                deployment.getImage(),
                deployment.getReplicas(),
                Math.toIntExact(snapshot.runningCount()),
                deployment.getContainerPort(),
                deployment.getStatus(),
                replicaDtos
        );
    }

    private ReplicaSummaryDto toReplicaSummary(Replica replica) {
        return new ReplicaSummaryDto(
                replica.replicaId(),
                replica.deploymentId(),
                replica.workerId(),
                replica.image(),
                replica.status(),
                replica.createdAt(),
                replica.updatedAt()
        );
    }
}
