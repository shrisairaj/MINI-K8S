package com.minik8s.worker.service;

import com.minik8s.worker.config.WorkerProperties;
import com.minik8s.worker.docker.ContainerRuntime;
import com.minik8s.worker.docker.ContainerSpec;
import com.minik8s.worker.docker.RuntimeContainer;
import com.minik8s.worker.exception.ContainerOperationException;
import com.minik8s.worker.exception.InvalidReplicaRequestException;
import com.minik8s.worker.exception.ReplicaNotFoundException;
import com.minik8s.worker.model.ReplicaStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerContainerServiceTest {
    private ContainerRuntime runtime;
    private ReplicaStatusReporter reporter;
    private WorkerContainerService service;

    @BeforeEach
    void setUp() {
        runtime = mock(ContainerRuntime.class);
        reporter = mock(ReplicaStatusReporter.class);
        WorkerProperties properties = new WorkerProperties();
        properties.setId("worker-a");
        service = new WorkerContainerService(runtime, properties, reporter);
    }

    @Test
    void createsAndStartsReplicaWithRequestedResourceLimits() {
        when(runtime.start(any())).thenReturn(container("running"));

        var result = service.start("replica-1", "web", "worker-a", "nginx:stable", 500, 1024);

        assertEquals(ReplicaStatus.RUNNING, result.status());
        assertEquals("docker-id", result.containerId());
        verify(runtime).start(new ContainerSpec("replica-1", "web", "nginx:stable", 500, 1024));
        verify(reporter).report(result);
    }

    @Test
    void repeatedStartForSameReplicaDoesNotCreateAnotherContainer() {
        when(runtime.start(any())).thenReturn(container("running"));

        service.start("replica-1", "web", "worker-a", "nginx:stable", 500, 1024);
        var repeated = service.start("replica-1", "web", "worker-a", "nginx:stable", 500, 1024);

        assertEquals(ReplicaStatus.RUNNING, repeated.status());
        verify(runtime).start(any());
    }

    @Test
    void stopAndRemoveAreIdempotentAndUpdateState() {
        when(runtime.start(any())).thenReturn(container("running"));
        when(runtime.inspect("replica-1")).thenReturn(Optional.of(container("running")));
        service.start("replica-1", "web", "worker-a", "nginx:stable", 500, 1024);

        assertEquals(ReplicaStatus.STOPPED, service.stop("replica-1", "worker-a").status());
        assertEquals(ReplicaStatus.STOPPED, service.stop("replica-1", "worker-a").status());
        assertEquals(ReplicaStatus.REMOVED, service.remove("replica-1", "worker-a").status());
        assertEquals(ReplicaStatus.REMOVED, service.remove("replica-1", "worker-a").status());

        verify(runtime).stop("replica-1");
        verify(runtime).remove("replica-1");
    }

    @Test
    void readsStatusAndListsRuntimeContainers() {
        when(runtime.inspect("replica-1")).thenReturn(Optional.of(container("running")));
        when(runtime.list()).thenReturn(List.of(container("running")));

        assertEquals(ReplicaStatus.RUNNING, service.get("replica-1", "worker-a").status());
        assertEquals(1, service.list().size());
        assertEquals(1, service.runningCount());
    }

    @Test
    void rejectsInvalidAndForeignWorkerRequests() {
        assertThrows(InvalidReplicaRequestException.class,
                () -> service.start("", "web", "worker-a", "nginx", 100, 100));
        assertThrows(InvalidReplicaRequestException.class,
                () -> service.start("replica-1", "web", "worker-b", "nginx", 100, 100));
        assertThrows(InvalidReplicaRequestException.class,
                () -> service.start("replica-1", "web", "worker-a", "nginx", -1, 100));
        verify(runtime, never()).start(any());
    }

    @Test
    void reportsDockerStartFailureAndPropagatesIt() {
        ContainerOperationException failure = new ContainerOperationException("Docker unavailable",
                new IllegalStateException("daemon down"));
        when(runtime.start(any())).thenThrow(failure);

        assertThrows(ContainerOperationException.class,
                () -> service.start("replica-1", "web", "worker-a", "nginx", 100, 100));

        verify(reporter).report(org.mockito.ArgumentMatchers.argThat(replica ->
                replica.status() == ReplicaStatus.FAILED));
    }

    @Test
    void reportsNotFoundAndPreservesRunningStatusWhenStopOutcomeFails() {
        when(runtime.inspect("missing")).thenReturn(Optional.empty());
        assertThrows(ReplicaNotFoundException.class, () -> service.get("missing", "worker-a"));

        when(runtime.start(any())).thenReturn(container("running"));
        when(runtime.inspect("replica-1")).thenReturn(Optional.of(container("running")));
        service.start("replica-1", "web", "worker-a", "nginx", 100, 100);
        doThrow(new ContainerOperationException("stop failed", new IllegalStateException()))
                .when(runtime).stop("replica-1");
        assertThrows(ContainerOperationException.class, () -> service.stop("replica-1", "worker-a"));
        assertEquals(ReplicaStatus.RUNNING, service.get("replica-1").status());
    }

    private RuntimeContainer container(String status) {
        return new RuntimeContainer("replica-1", "web", "nginx:stable", "docker-id", status, Instant.now());
    }
}