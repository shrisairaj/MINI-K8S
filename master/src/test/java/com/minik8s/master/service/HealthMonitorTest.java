package com.minik8s.master.service;

import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.registry.InMemoryWorkerRegistry;
import com.minik8s.master.registry.WorkerRegistration;
import com.minik8s.master.replica.ReplicaManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthMonitorTest {

    private InMemoryWorkerRegistry workerRegistry;
    private WorkerClient workerClient;
    private ReplicaManager replicaManager;
    private ObjectProvider<WorkerClient> workerClientProvider;
    private ObjectProvider<ReplicaManager> replicaManagerProvider;
    private HealthMonitor healthMonitor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        workerRegistry = new InMemoryWorkerRegistry();
        workerClient = mock(WorkerClient.class);
        replicaManager = mock(ReplicaManager.class);

        workerClientProvider = mock(ObjectProvider.class);
        when(workerClientProvider.getIfAvailable()).thenReturn(workerClient);

        replicaManagerProvider = mock(ObjectProvider.class);
        when(replicaManagerProvider.getIfAvailable()).thenReturn(replicaManager);

        healthMonitor = new HealthMonitor(workerRegistry, workerClientProvider, replicaManagerProvider, 15000);
    }

    @Test
    void checkWorkersWithNoWorkersDoesNotFail() {
        healthMonitor.checkWorkers();
        verify(workerClient, never()).checkHealth(anyString(), anyString(), anyInt());
    }

    @Test
    void healthyWorkerStaysReady() {
        registerWorker("worker-1");
        when(workerClient.checkHealth(eq("worker-1"), anyString(), anyInt())).thenReturn(true);

        healthMonitor.checkWorkers();

        assertEquals(WorkerStatus.READY, workerRegistry.getWorker("worker-1").orElseThrow().status());
        verify(replicaManager, never()).handleWorkerUnavailable(anyString());
    }

    @Test
    void failedWorkerIsMarkedUnavailableAndTriggersReplicaManager() {
        registerWorker("worker-1");
        registerWorker("worker-2");

        when(workerClient.checkHealth(eq("worker-1"), anyString(), anyInt())).thenReturn(true);
        when(workerClient.checkHealth(eq("worker-2"), anyString(), anyInt())).thenThrow(new RuntimeException("gRPC Connection refused"));

        healthMonitor.checkWorkers();

        assertEquals(WorkerStatus.READY, workerRegistry.getWorker("worker-1").orElseThrow().status());
        assertEquals(WorkerStatus.UNAVAILABLE, workerRegistry.getWorker("worker-2").orElseThrow().status());
        verify(replicaManager).handleWorkerUnavailable("worker-2");
    }

    @Test
    void recoveredWorkerTransitionsBackToReady() {
        registerWorker("worker-1");
        workerRegistry.updateStatus("worker-1", WorkerStatus.UNAVAILABLE);

        when(workerClient.checkHealth(eq("worker-1"), anyString(), anyInt())).thenReturn(true);

        healthMonitor.checkWorkers();

        assertEquals(WorkerStatus.READY, workerRegistry.getWorker("worker-1").orElseThrow().status());
    }

    private void registerWorker(String workerId) {
        WorkerResources resources = new WorkerResources(4000, 8000, 4000, 8000);
        workerRegistry.registerWorker(new WorkerRegistration(workerId, "localhost", 50051, resources));
        workerRegistry.updateHeartbeat(workerId, resources);
    }
}
