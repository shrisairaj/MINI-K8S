package com.minik8s.master.replica;

import com.minik8s.master.grpc.ReplicaLaunchRequest;
import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.grpc.WorkerOperationRejectedException;
import com.minik8s.master.model.ReplicaStatus;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.registry.InMemoryWorkerRegistry;
import com.minik8s.master.registry.WorkerRegistration;
import com.minik8s.master.scheduler.ResourceAwareLeastLoadedStrategy;
import com.minik8s.master.scheduler.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReplicaManagerTest {
    private InMemoryWorkerRegistry registry;
    private WorkerClient workerClient;
    private ReplicaManager replicaManager;

    @BeforeEach
    void setUp() {
        registry = new InMemoryWorkerRegistry();
        workerClient = mock(WorkerClient.class);
        Scheduler scheduler = new Scheduler(registry, new ResourceAwareLeastLoadedStrategy());
        replicaManager = new ReplicaManager(scheduler, workerClient, registry);
    }

    @Test
    void desiredEqualsActualDoesNotStartAdditionalReplicas() {
        registerReadyWorker("worker-a");
        replicaManager.updateDesiredState(workload(1));

        replicaManager.reconcile("web");
        var secondCycle = replicaManager.reconcile("web");

        assertEquals(1, secondCycle.occupyingCount());
        verify(workerClient, times(1)).startReplica(any(ReplicaLaunchRequest.class));
    }

    @Test
    void scaleUpAccountsForStartingReplicasAcrossReconciliations() {
        registerReadyWorker("worker-a");
        registerReadyWorker("worker-b");
        replicaManager.updateDesiredState(workload(3));

        var snapshot = replicaManager.reconcile("web");
        replicaManager.reconcile("web");

        assertEquals(3, snapshot.occupyingCount());
        assertEquals(3, snapshot.replicas().size());
        verify(workerClient, times(3)).startReplica(any(ReplicaLaunchRequest.class));
    }

    @Test
    void scaleDownStopsExcessReplicas() {
        registerReadyWorker("worker-a");
        replicaManager.updateDesiredState(workload(3));
        replicaManager.reconcile("web");

        replicaManager.updateDesiredState(workload(1));
        var snapshot = replicaManager.reconcile("web");

        assertEquals(1, snapshot.occupyingCount());
        assertEquals(2, snapshot.replicas().stream()
                .filter(replica -> replica.status() == ReplicaStatus.STOPPED).count());
        verify(workerClient, times(2)).stopReplica(anyString(), anyString());
    }

    @Test
    void failedReplicaIsReplacedOnNextReconciliation() {
        registerReadyWorker("worker-a");
        registerReadyWorker("worker-b");
        replicaManager.updateDesiredState(workload(1));
        var first = replicaManager.reconcile("web").replicas().getFirst();
        replicaManager.reportReplicaStatus("web", first.replicaId(), first.workerId(), ReplicaStatus.RUNNING);
        replicaManager.reportReplicaStatus("web", first.replicaId(), first.workerId(), ReplicaStatus.FAILED);

        var afterRecovery = replicaManager.reconcile("web");

        assertEquals(1, afterRecovery.occupyingCount());
        assertEquals(2, afterRecovery.replicas().size());
        verify(workerClient, times(2)).startReplica(any(ReplicaLaunchRequest.class));
    }

    @Test
    void workerFailureMarksReplicasFailedAndAllowsReplacementElsewhere() {
        registerReadyWorker("worker-a");
        registerReadyWorker("worker-b");
        replicaManager.updateDesiredState(workload(1));
        var first = replicaManager.reconcile("web").replicas().getFirst();
        registry.updateStatus(first.workerId(), com.minik8s.master.model.WorkerStatus.UNAVAILABLE);

        var recovered = replicaManager.reconcile("web");

        assertEquals(1, recovered.occupyingCount());
        assertTrue(recovered.replicas().stream().anyMatch(replica -> replica.status() == ReplicaStatus.FAILED));
        verify(workerClient, times(2)).startReplica(any(ReplicaLaunchRequest.class));
    }

    @Test
    void noSuitableWorkerLeavesDesiredReplicaUnscheduled() {
        replicaManager.updateDesiredState(workload(1));

        var snapshot = replicaManager.reconcile("web");

        assertEquals(0, snapshot.occupyingCount());
        assertTrue(snapshot.replicas().isEmpty());
        verify(workerClient, times(0)).startReplica(any(ReplicaLaunchRequest.class));
    }

    @Test
    void failedStartReleasesReservationAndKeepsReplicaFailed() {
        registerReadyWorker("worker-a");
        doThrow(new WorkerOperationRejectedException("definitive rejection"))
                .when(workerClient).startReplica(any(ReplicaLaunchRequest.class));
        replicaManager.updateDesiredState(workload(1));

        var result = replicaManager.reconcile("web");

        assertEquals(0, result.occupyingCount());
        assertEquals(ReplicaStatus.FAILED, result.replicas().getFirst().status());
        assertEquals(4000, registry.getAvailableWorkers().getFirst().resources().availableCpuMillis());
    }

    @Test
    void uncertainStartOutcomeRemainsStartingUntilWorkerReportsState() {
        registerReadyWorker("worker-a");
        doThrow(new IllegalStateException("transport timeout"))
                .when(workerClient).startReplica(any(ReplicaLaunchRequest.class));
        replicaManager.updateDesiredState(workload(1));

        var first = replicaManager.reconcile("web");
        var second = replicaManager.reconcile("web");

        assertEquals(1, first.occupyingCount());
        assertEquals(ReplicaStatus.STARTING, second.replicas().getFirst().status());
        verify(workerClient, times(1)).startReplica(any(ReplicaLaunchRequest.class));
    }

    @Test
    void retriesStopRequestsThatDidNotComplete() {
        registerReadyWorker("worker-a");
        replicaManager.updateDesiredState(workload(3));
        replicaManager.reconcile("web");
        doThrow(new IllegalStateException("temporary RPC failure"))
                .doNothing()
                .when(workerClient).stopReplica(anyString(), anyString());
        replicaManager.updateDesiredState(workload(1));

        replicaManager.reconcile("web");
        var retried = replicaManager.reconcile("web");

        assertEquals(1, retried.occupyingCount());
        assertEquals(2, retried.replicas().stream()
                .filter(replica -> replica.status() == ReplicaStatus.STOPPED).count());
        verify(workerClient, times(3)).stopReplica(anyString(), anyString());
    }

    @Test
    void concurrentReconcileCallsDoNotCreateDuplicateReplicas() {
        registerReadyWorker("worker-a");
        replicaManager.updateDesiredState(workload(1));

        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> replicaManager.reconcile("web")),
                CompletableFuture.runAsync(() -> replicaManager.reconcile("web"))).join();

        assertEquals(1, replicaManager.getSnapshot("web").occupyingCount());
        verify(workerClient, times(1)).startReplica(any(ReplicaLaunchRequest.class));
    }

    private ReplicaWorkload workload(int replicas) {
        return new ReplicaWorkload("web", "nginx:stable", replicas, new ResourceRequest(100, 100));
    }

    private void registerReadyWorker(String id) {
        WorkerResources resources = new WorkerResources(4000, 8_000, 4000, 8_000);
        registry.registerWorker(new WorkerRegistration(id, "localhost", 9091, resources));
        registry.updateHeartbeat(id, resources);
    }
}