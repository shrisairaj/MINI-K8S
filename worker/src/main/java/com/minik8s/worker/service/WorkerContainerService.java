package com.minik8s.worker.service;

import com.minik8s.worker.config.WorkerProperties;
import com.minik8s.worker.docker.ContainerRuntime;
import com.minik8s.worker.docker.ContainerSpec;
import com.minik8s.worker.docker.RuntimeContainer;
import com.minik8s.worker.exception.InvalidReplicaRequestException;
import com.minik8s.worker.exception.ReplicaNotFoundException;
import com.minik8s.worker.model.ReplicaDescriptor;
import com.minik8s.worker.model.ReplicaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class WorkerContainerService {
    private static final Logger logger = LoggerFactory.getLogger(WorkerContainerService.class);

    private final ContainerRuntime runtime;
    private final WorkerProperties properties;
    private final ReplicaStatusReporter statusReporter;
    private final ConcurrentMap<String, ReplicaDescriptor> replicas = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> replicaLocks = new ConcurrentHashMap<>();

    public WorkerContainerService(ContainerRuntime runtime, WorkerProperties properties,
                                  ReplicaStatusReporter statusReporter) {
        this.runtime = runtime;
        this.properties = properties;
        this.statusReporter = statusReporter;
    }

    public ReplicaDescriptor start(String replicaId, String deploymentId, String workerId,
                                   String image, long cpuMillis, long memoryBytes) {
        validateStart(replicaId, deploymentId, workerId, image, cpuMillis, memoryBytes);
        synchronized (replicaLocks.computeIfAbsent(replicaId, ignored -> new Object())) {
            ReplicaDescriptor existing = replicas.get(replicaId);
            if (existing != null && !sameWorkload(existing, deploymentId, image, cpuMillis, memoryBytes)) {
                throw new InvalidReplicaRequestException("Replica ID is already used by a different workload");
            }
            if (existing != null && (existing.status() == ReplicaStatus.RUNNING
                    || existing.status() == ReplicaStatus.STOPPED
                    || existing.status() == ReplicaStatus.REMOVED)) {
                return existing;
            }

            ReplicaDescriptor starting = new ReplicaDescriptor(replicaId, deploymentId, workerId, image,
                    cpuMillis, memoryBytes, existing == null ? null : existing.containerId(),
                    ReplicaStatus.STARTING, "Container start requested", Instant.now());
            replicas.put(replicaId, starting);
            report(starting);
            try {
                RuntimeContainer container = runtime.start(new ContainerSpec(
                        replicaId, deploymentId, image, cpuMillis, memoryBytes));
                ReplicaDescriptor running = descriptor(container, cpuMillis, memoryBytes, ReplicaStatus.RUNNING,
                        "Container is running");
                replicas.put(replicaId, running);
                report(running);
                return running;
            } catch (RuntimeException exception) {
                ReplicaDescriptor failed = starting.withStatus(ReplicaStatus.FAILED,
                        exception.getMessage() == null ? "Container start failed" : exception.getMessage());
                replicas.put(replicaId, failed);
                report(failed);
                throw exception;
            }
        }
    }

    public void validateStartRequest(String replicaId, String deploymentId, String workerId, String image,
                                     long cpuMillis, long memoryBytes) {
        validateStart(replicaId, deploymentId, workerId, image, cpuMillis, memoryBytes);
        ReplicaDescriptor existing = replicas.get(replicaId);
        if (existing != null && !sameWorkload(existing, deploymentId, image, cpuMillis, memoryBytes)) {
            throw new InvalidReplicaRequestException("Replica ID is already used by a different workload");
        }
    }

    public ReplicaDescriptor stop(String replicaId, String workerId) {
        validateWorker(workerId);
        synchronized (replicaLocks.computeIfAbsent(replicaId, ignored -> new Object())) {
            ReplicaDescriptor current = replicas.get(replicaId);
            if (current == null) {
                current = runtime.inspect(replicaId)
                        .map(container -> descriptor(container, 0, 0, statusFromRuntime(container.status()),
                                "Recovered from Docker"))
                        .orElseGet(() -> new ReplicaDescriptor(replicaId, "", workerId, "", 0, 0, null,
                                ReplicaStatus.STOPPED, "Container already absent", Instant.now()));
                replicas.put(replicaId, current);
            }
            if (current.status() == ReplicaStatus.STOPPED || current.status() == ReplicaStatus.REMOVED) {
                return current;
            }
            try {
                runtime.stop(replicaId);
                ReplicaDescriptor stopped = current.withStatus(ReplicaStatus.STOPPED, "Container stopped");
                replicas.put(replicaId, stopped);
                report(stopped);
                return stopped;
            } catch (RuntimeException exception) {
                logger.warn("Stop outcome for replica {} is uncertain; preserving its prior state", replicaId,
                    exception);
                throw exception;
            }
        }
    }

    public ReplicaDescriptor remove(String replicaId, String workerId) {
        validateWorker(workerId);
        synchronized (replicaLocks.computeIfAbsent(replicaId, ignored -> new Object())) {
            ReplicaDescriptor current = replicas.get(replicaId);
            if (current != null && current.status() == ReplicaStatus.REMOVED) {
                return current;
            }
            if (current == null && runtime.inspect(replicaId).isEmpty()) {
                return new ReplicaDescriptor(replicaId, "", workerId, "", 0, 0, null,
                        ReplicaStatus.REMOVED, "Container already absent", Instant.now());
            }
            if (current == null) {
                current = descriptor(runtime.inspect(replicaId).orElseThrow(), 0, 0,
                        ReplicaStatus.STOPPED, "Container found");
            }
            try {
                runtime.remove(replicaId);
                ReplicaDescriptor removed = current.withStatus(ReplicaStatus.REMOVED, "Container removed");
                replicas.put(replicaId, removed);
                return removed;
            } catch (RuntimeException exception) {
                logger.warn("Failed to remove container for replica {}", replicaId, exception);
                throw exception;
            }
        }
    }

    public ReplicaDescriptor get(String replicaId) {
        ReplicaDescriptor cached = replicas.get(replicaId);
        if (cached != null && cached.status() == ReplicaStatus.REMOVED) {
            return cached;
        }
        RuntimeContainer runtimeContainer = runtime.inspect(replicaId).orElse(null);
        if (runtimeContainer == null) {
            if (cached != null && cached.status() == ReplicaStatus.RUNNING) {
                ReplicaDescriptor failed = cached.withStatus(ReplicaStatus.FAILED,
                        "Container is no longer present in Docker");
                replicas.put(replicaId, failed);
                report(failed);
                return failed;
            }
            throw new ReplicaNotFoundException(replicaId);
        }
        ReplicaDescriptor descriptor = fromRuntime(runtimeContainer, cached);
        replicas.put(replicaId, descriptor);
        return descriptor;
    }

    public ReplicaDescriptor get(String replicaId, String workerId) {
        validateWorker(workerId);
        return get(replicaId);
    }

    public List<ReplicaDescriptor> list() {
        Map<String, ReplicaDescriptor> result = new ConcurrentHashMap<>(replicas);
        java.util.Set<String> observedIds = ConcurrentHashMap.newKeySet();
        for (RuntimeContainer runtimeContainer : runtime.list()) {
            ReplicaDescriptor known = replicas.get(runtimeContainer.replicaId());
            ReplicaDescriptor current = fromRuntime(runtimeContainer, known);
            result.put(current.replicaId(), current);
            replicas.put(current.replicaId(), current);
            observedIds.add(current.replicaId());
        }
        for (ReplicaDescriptor known : new ArrayList<>(replicas.values())) {
            if (known.status() == ReplicaStatus.RUNNING && !observedIds.contains(known.replicaId())) {
                ReplicaDescriptor failed = known.withStatus(ReplicaStatus.FAILED,
                        "Container is no longer present in Docker");
                replicas.put(failed.replicaId(), failed);
                result.put(failed.replicaId(), failed);
            }
        }
        return result.values().stream().sorted(Comparator.comparing(ReplicaDescriptor::replicaId)).toList();
    }

    public List<ReplicaDescriptor> publishStatuses() {
        List<ReplicaDescriptor> current = list();
        current.stream()
                .filter(replica -> replica.status() == ReplicaStatus.RUNNING
                        || replica.status() == ReplicaStatus.FAILED
                        || replica.status() == ReplicaStatus.STOPPED)
                .forEach(this::report);
        return current;
    }

    public int runningCount() {
        return Math.toIntExact(list().stream().filter(replica -> replica.status() == ReplicaStatus.RUNNING).count());
    }

    private void validateStart(String replicaId, String deploymentId, String workerId, String image,
                               long cpuMillis, long memoryBytes) {
        if (replicaId == null || replicaId.isBlank() || deploymentId == null || deploymentId.isBlank()
                || image == null || image.isBlank()) {
            throw new InvalidReplicaRequestException("Replica ID, deployment ID, and image are required");
        }
        validateWorker(workerId);
        if (cpuMillis < 0 || memoryBytes < 0) {
            throw new InvalidReplicaRequestException("CPU and memory requests cannot be negative");
        }
    }

    private void validateWorker(String workerId) {
        if (!properties.getId().equals(workerId)) {
            throw new InvalidReplicaRequestException("Request is addressed to a different Worker");
        }
    }

    private ReplicaDescriptor find(String replicaId) {
        ReplicaDescriptor current = replicas.get(replicaId);
        if (current != null) {
            return current;
        }
        RuntimeContainer fromDocker = runtime.inspect(replicaId).orElseThrow(() -> new ReplicaNotFoundException(replicaId));
        current = descriptor(fromDocker, 0, 0, statusFromRuntime(fromDocker.status()), "Recovered from Docker");
        replicas.put(replicaId, current);
        return current;
    }

    private ReplicaDescriptor fromRuntime(RuntimeContainer container, ReplicaDescriptor known) {
        long cpu = known == null || known.cpuMillis() == 0 ? container.cpuMillis() : known.cpuMillis();
        long memory = known == null || known.memoryBytes() == 0 ? container.memoryBytes() : known.memoryBytes();
        ReplicaStatus observedStatus = statusFromRuntime(container.status());
        if (observedStatus == ReplicaStatus.STOPPED
            && (known == null || known.status() == ReplicaStatus.RUNNING)) {
            observedStatus = ReplicaStatus.FAILED;
        }
        return new ReplicaDescriptor(container.replicaId(), container.deploymentId(), properties.getId(),
            container.image(), cpu, memory, container.containerId(), observedStatus,
                "Docker state: " + container.status(), container.updatedAt());
    }

    private ReplicaDescriptor descriptor(RuntimeContainer container, long cpuMillis, long memoryBytes,
                                         ReplicaStatus status, String message) {
        return new ReplicaDescriptor(container.replicaId(), container.deploymentId(), properties.getId(),
                container.image(), cpuMillis, memoryBytes, container.containerId(), status, message,
                container.updatedAt());
    }

    private ReplicaStatus statusFromRuntime(String status) {
        return switch (status == null ? "" : status.toLowerCase()) {
            case "running" -> ReplicaStatus.RUNNING;
            case "created" -> ReplicaStatus.CREATED;
            case "removed" -> ReplicaStatus.REMOVED;
            default -> ReplicaStatus.STOPPED;
        };
    }

    private boolean sameWorkload(ReplicaDescriptor existing, String deploymentId, String image,
                                 long cpuMillis, long memoryBytes) {
        return existing.deploymentId().equals(deploymentId) && existing.image().equals(image)
                && existing.cpuMillis() == cpuMillis && existing.memoryBytes() == memoryBytes;
    }

    private void report(ReplicaDescriptor descriptor) {
        try {
            statusReporter.report(descriptor);
        } catch (RuntimeException exception) {
            logger.warn("Could not report replica {} status to Master; next heartbeat will retry",
                    descriptor.replicaId(), exception);
        }
    }
}