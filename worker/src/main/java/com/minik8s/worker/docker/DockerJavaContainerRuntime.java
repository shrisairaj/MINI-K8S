package com.minik8s.worker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.minik8s.worker.exception.ContainerOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DockerJavaContainerRuntime implements ContainerRuntime {
    private static final Logger logger = LoggerFactory.getLogger(DockerJavaContainerRuntime.class);
    private static final String REPLICA_LABEL = "minik8s.replica-id";
    private static final String DEPLOYMENT_LABEL = "minik8s.deployment-id";
    private static final String CPU_LABEL = "minik8s.cpu-millis";
    private static final String MEMORY_LABEL = "minik8s.memory-bytes";

    private final DockerClient dockerClient;

    public DockerJavaContainerRuntime(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public RuntimeContainer start(ContainerSpec spec) {
        try {
            Optional<RuntimeContainer> existing = inspect(spec.replicaId());
            if (existing.isPresent()) {
                if ("running".equals(existing.get().status())) {
                    return existing.get();
                }
                String id = existing.get().containerId();
                dockerClient.startContainerCmd(id).exec();
                return inspect(spec.replicaId()).orElseThrow(() -> new IllegalStateException(
                        "Container disappeared after start: " + spec.replicaId()));
            }

            ensureImageAvailable(spec.image());
            HostConfig hostConfig = HostConfig.newHostConfig();
            if (spec.memoryBytes() > 0) {
                hostConfig.withMemory(spec.memoryBytes());
            }
            if (spec.cpuMillis() > 0) {
                hostConfig.withCpuPeriod(100_000L)
                        .withCpuQuota(Math.multiplyExact(spec.cpuMillis(), 100L));
            }
            Map<String, String> labels = Map.of(
                    REPLICA_LABEL, spec.replicaId(),
                    DEPLOYMENT_LABEL, spec.deploymentId(),
                    CPU_LABEL, Long.toString(spec.cpuMillis()),
                    MEMORY_LABEL, Long.toString(spec.memoryBytes()));
            CreateContainerResponse created = dockerClient.createContainerCmd(spec.image())
                    .withName(containerName(spec.replicaId()))
                    .withLabels(labels)
                    .withHostConfig(hostConfig)
                    .exec();
            dockerClient.startContainerCmd(created.getId()).exec();
            return inspect(spec.replicaId()).orElseGet(() -> new RuntimeContainer(
                    spec.replicaId(), spec.deploymentId(), spec.image(), created.getId(), "running", Instant.now()));
        } catch (Exception exception) {
            if (exception instanceof ContainerOperationException operationException) {
                throw operationException;
            }
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ContainerOperationException("Docker failed to start replica " + spec.replicaId(), exception);
        }
    }

    @Override
    public void stop(String replicaId) {
        try {
            Optional<RuntimeContainer> container = inspect(replicaId);
            if (container.isPresent() && "running".equals(container.get().status())) {
                dockerClient.stopContainerCmd(container.get().containerId()).exec();
            }
        } catch (Exception exception) {
            throw new ContainerOperationException("Docker failed to stop replica " + replicaId, exception);
        }
    }

    @Override
    public void remove(String replicaId) {
        try {
            Optional<RuntimeContainer> container = inspect(replicaId);
            if (container.isPresent()) {
                dockerClient.removeContainerCmd(container.get().containerId()).withForce(true).exec();
            }
        } catch (Exception exception) {
            throw new ContainerOperationException("Docker failed to remove replica " + replicaId, exception);
        }
    }

    @Override
    public Optional<RuntimeContainer> inspect(String replicaId) {
        try {
            return dockerClient.listContainersCmd().withShowAll(true).exec().stream()
                    .filter(container -> replicaId.equals(labels(container).get(REPLICA_LABEL)))
                    .findFirst()
                    .map(this::toRuntimeContainer);
        } catch (Exception exception) {
            throw new ContainerOperationException("Docker failed to inspect replica " + replicaId, exception);
        }
    }

    @Override
    public List<RuntimeContainer> list() {
        try {
            return dockerClient.listContainersCmd().withShowAll(true).exec().stream()
                    .filter(container -> labels(container).containsKey(REPLICA_LABEL))
                    .map(this::toRuntimeContainer)
                    .toList();
        } catch (Exception exception) {
            throw new ContainerOperationException("Docker failed to list Mini-K8s containers", exception);
        }
    }

    private void ensureImageAvailable(String image) throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (com.github.dockerjava.api.exception.NotFoundException missingImage) {
            logger.info("Pulling Docker image {}", image);
            PullImageResultCallback callback = new PullImageResultCallback();
            dockerClient.pullImageCmd(image).exec(callback);
            if (!callback.awaitCompletion(5, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Timed out pulling image " + image);
            }
        }
    }

    private RuntimeContainer toRuntimeContainer(Container container) {
        Map<String, String> labels = labels(container);
        String status = container.getState() == null ? "unknown" : container.getState().toLowerCase();
        return new RuntimeContainer(labels.get(REPLICA_LABEL), labels.get(DEPLOYMENT_LABEL),
                container.getImage(), container.getId(), status, Instant.now(),
                parseLong(labels.get(CPU_LABEL)), parseLong(labels.get(MEMORY_LABEL)));
    }

    private long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Map<String, String> labels(Container container) {
        return container.getLabels() == null ? Map.of() : container.getLabels();
    }

    private String containerName(String replicaId) {
        return "minik8s-" + replicaId.replaceAll("[^a-zA-Z0-9_.-]", "-");
    }
}