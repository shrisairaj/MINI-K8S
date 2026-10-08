package com.minik8s.master.grpc;

import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.registry.InMemoryWorkerRegistry;
import com.minik8s.master.registry.WorkerRegistration;
import com.minik8s.protocol.worker.v1.OperationReply;
import com.minik8s.protocol.worker.v1.ReplicaRequest;
import com.minik8s.protocol.worker.v1.StartReplicaRequest;
import com.minik8s.protocol.worker.v1.WorkerServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkerGrpcClientTest {
    private final AtomicInteger startCalls = new AtomicInteger();
    private final AtomicInteger stopCalls = new AtomicInteger();
    private final AtomicInteger removeCalls = new AtomicInteger();
    private Server server;
    private WorkerGrpcClient client;
    private InMemoryWorkerRegistry registry;

    @BeforeEach
    void setUp() throws Exception {
        server = ServerBuilder.forPort(0).addService(new WorkerServiceGrpc.WorkerServiceImplBase() {
            @Override
            public void startReplica(StartReplicaRequest request, StreamObserver<OperationReply> observer) {
                startCalls.incrementAndGet();
                assertEquals("replica-1", request.getReplicaId());
                assertEquals("web", request.getDeploymentId());
                assertEquals(500, request.getResources().getCpuMillis());
                observer.onNext(OperationReply.newBuilder().setAccepted(true)
                        .setReplicaId(request.getReplicaId()).setMessage("accepted").build());
                observer.onCompleted();
            }

            @Override
            public void stopReplica(ReplicaRequest request, StreamObserver<OperationReply> observer) {
                stopCalls.incrementAndGet();
                observer.onNext(OperationReply.newBuilder().setAccepted(true).setReplicaId(request.getReplicaId())
                        .setMessage("stopped").build());
                observer.onCompleted();
            }

            @Override
            public void removeReplica(ReplicaRequest request, StreamObserver<OperationReply> observer) {
                removeCalls.incrementAndGet();
                observer.onNext(OperationReply.newBuilder().setAccepted(true).setReplicaId(request.getReplicaId())
                        .setMessage("removed").build());
                observer.onCompleted();
            }
        }).build().start();

        registry = new InMemoryWorkerRegistry();
        WorkerResources resources = new WorkerResources(4000, 8_000, 4000, 8_000);
        registry.registerWorker(new WorkerRegistration("worker-a", "127.0.0.1", server.getPort(), resources));
        registry.updateHeartbeat("worker-a", resources);
        assertEquals(WorkerStatus.READY, registry.getWorker("worker-a").orElseThrow().status());
        client = new WorkerGrpcClient(registry, 2000);
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    void sendsExistingReplicaLaunchContractAndStopsThenRemoves() {
        client.startReplica(new ReplicaLaunchRequest("replica-1", "web", "worker-a", "nginx:stable",
                new ResourceRequest(500, 1_000_000)));
        client.stopReplica("worker-a", "replica-1");

        assertEquals(1, startCalls.get());
        assertEquals(1, stopCalls.get());
        assertEquals(1, removeCalls.get());
    }

    @Test
    void rejectsWorkerIdNotInRegistryBeforeSendingRpc() {
        assertThrows(WorkerOperationRejectedException.class,
                () -> client.startReplica(new ReplicaLaunchRequest("replica-1", "web", "missing", "nginx",
                        new ResourceRequest(100, 100))));
    }
}