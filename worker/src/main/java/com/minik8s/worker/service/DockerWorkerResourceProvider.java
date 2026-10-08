package com.minik8s.worker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.minik8s.worker.docker.ContainerRuntime;
import com.minik8s.worker.docker.RuntimeContainer;
import com.minik8s.worker.exception.ContainerOperationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DockerWorkerResourceProvider implements WorkerResourceProvider {
    private final DockerClient dockerClient;
    private final ContainerRuntime containerRuntime;

    public DockerWorkerResourceProvider(DockerClient dockerClient, ContainerRuntime containerRuntime) {
        this.dockerClient = dockerClient;
        this.containerRuntime = containerRuntime;
    }

    @Override
    public WorkerResourcesSnapshot snapshot() {
        try {
            Info info = dockerClient.infoCmd().exec();
            long totalCpu = Math.multiplyExact((long) info.getNCPU(), 1000L);
            long totalMemory = info.getMemTotal();
            if (totalCpu <= 0 || totalMemory <= 0) {
                throw new IllegalStateException("Docker Engine reported invalid resource capacity");
            }
            List<RuntimeContainer> containers = containerRuntime.list();
            long reservedCpu = containers.stream().filter(container -> "running".equalsIgnoreCase(container.status()))
                    .mapToLong(RuntimeContainer::cpuMillis).reduce(0, DockerWorkerResourceProvider::saturatedAdd);
            long reservedMemory = containers.stream().filter(container -> "running".equalsIgnoreCase(container.status()))
                    .mapToLong(RuntimeContainer::memoryBytes).reduce(0, DockerWorkerResourceProvider::saturatedAdd);
            int running = Math.toIntExact(containers.stream()
                    .filter(container -> "running".equalsIgnoreCase(container.status())).count());
            return new WorkerResourcesSnapshot(totalCpu, totalMemory,
                    Math.max(0, totalCpu - reservedCpu), Math.max(0, totalMemory - reservedMemory), running);
        } catch (Exception exception) {
            throw new ContainerOperationException("Unable to read Docker Engine resources", exception);
        }
    }

    private static long saturatedAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }
}