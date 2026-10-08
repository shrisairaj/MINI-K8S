package com.minik8s.worker.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InfoCmd;
import com.github.dockerjava.api.model.Info;
import com.minik8s.worker.docker.ContainerRuntime;
import com.minik8s.worker.docker.RuntimeContainer;
import com.minik8s.worker.exception.ContainerOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DockerWorkerResourceProviderTest {
    private DockerClient dockerClient;
    private ContainerRuntime runtime;
    private DockerWorkerResourceProvider provider;

    @BeforeEach
    void setUp() {
        dockerClient = mock(DockerClient.class);
        runtime = mock(ContainerRuntime.class);
        provider = new DockerWorkerResourceProvider(dockerClient, runtime);
    }

    @Test
    void reportsDockerCapacityMinusRunningReplicaRequests() {
        InfoCmd infoCommand = mock(InfoCmd.class);
        Info info = mock(Info.class);
        when(dockerClient.infoCmd()).thenReturn(infoCommand);
        when(infoCommand.exec()).thenReturn(info);
        when(info.getNCPU()).thenReturn(4);
        when(info.getMemTotal()).thenReturn(8_000L);
        when(runtime.list()).thenReturn(List.of(
                new RuntimeContainer("running", "web", "nginx", "id1", "running", Instant.now(), 500, 1000),
                new RuntimeContainer("stopped", "web", "nginx", "id2", "exited", Instant.now(), 500, 2000)));

        WorkerResourcesSnapshot snapshot = provider.snapshot();

        assertEquals(4000, snapshot.totalCpuMillis());
        assertEquals(8_000, snapshot.totalMemoryBytes());
        assertEquals(3500, snapshot.availableCpuMillis());
        assertEquals(7000, snapshot.availableMemoryBytes());
        assertEquals(1, snapshot.runningReplicas());
    }

    @Test
    void propagatesDockerUnavailableAsAWorkerResourceFailure() {
        InfoCmd infoCommand = mock(InfoCmd.class);
        when(dockerClient.infoCmd()).thenReturn(infoCommand);
        when(infoCommand.exec()).thenThrow(new IllegalStateException("daemon unavailable"));

        assertThrows(ContainerOperationException.class, () -> provider.snapshot());
    }
}