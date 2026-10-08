package com.minik8s.worker.grpc;

import com.minik8s.protocol.worker.v1.ReplicaRequest;
import com.minik8s.protocol.worker.v1.StartReplicaRequest;
import com.minik8s.protocol.worker.v1.WorkerServiceGrpc;
import com.minik8s.protocol.worker.v1.WorkerStatusRequest;
import com.minik8s.worker.config.WorkerProperties;
import com.minik8s.worker.exception.InvalidReplicaRequestException;
import com.minik8s.worker.model.ReplicaDescriptor;
import com.minik8s.worker.model.ReplicaStatus;
import com.minik8s.worker.service.WorkerContainerService;
import com.minik8s.worker.service.WorkerResourceProvider;
import com.minik8s.worker.service.WorkerResourcesSnapshot;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerGrpcServiceTest {
    private WorkerContainerService containers;
    private WorkerResourceProvider resources;
    private WorkerProperties properties;
    private Server server;
    private ManagedChannel channel;
    private WorkerServiceGrpc.WorkerServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        containers = mock(WorkerContainerService.class);
        resources = mock(WorkerResourceProvider.class);
        properties = new WorkerProperties();
        properties.setId("worker-a");
        properties.setHost("127.0.0.1");
        properties.setGrpcPort(9191);
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new WorkerGrpcService(containers, resources, properties)).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        stub = WorkerServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void returnsWorkerResourceAndContainerStatus() {
        when(resources.snapshot()).thenReturn(new WorkerResourcesSnapshot(4000, 8_000, 3000, 6_000, 2));
        when(containers.runningCount()).thenReturn(2);

        var reply = stub.getWorkerStatus(WorkerStatusRequest.newBuilder().setWorkerId("worker-a").build());

        assertEquals("worker-a", reply.getWorkerId());
        assertEquals(4000, reply.getTotalCpuMillis());
        assertEquals(2, reply.getRunningReplicas());
    }

    @Test
    void mapsReplicaStatusAndStopCallsThroughService() {
        ReplicaDescriptor stopped = descriptor(ReplicaStatus.STOPPED);
        when(containers.stop("replica-1", "worker-a")).thenReturn(stopped);
        var reply = stub.stopReplica(ReplicaRequest.newBuilder()
                .setReplicaId("replica-1").setWorkerId("worker-a").build());

        assertTrue(reply.getAccepted());
        verify(containers).stop("replica-1", "worker-a");
    }

    @Test
    void rejectsInvalidStartAndWrongWorkerRequestsWithGrpcStatus() {
        doThrow(new InvalidReplicaRequestException("invalid request"))
                .when(containers).validateStartRequest("", "web", "worker-a", "nginx", 1, 1);
        StatusRuntimeException invalid = assertThrows(StatusRuntimeException.class,
                () -> stub.startReplica(StartReplicaRequest.newBuilder().setDeploymentId("web")
                        .setWorkerId("worker-a").setImage("nginx")
                        .setResources(com.minik8s.protocol.worker.v1.ResourceRequest.newBuilder()
                                .setCpuMillis(1).setMemoryBytes(1)).build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, invalid.getStatus().getCode());

        StatusRuntimeException wrongWorker = assertThrows(StatusRuntimeException.class,
                () -> stub.getWorkerStatus(WorkerStatusRequest.newBuilder().setWorkerId("worker-b").build()));
        assertEquals(Status.Code.FAILED_PRECONDITION, wrongWorker.getStatus().getCode());
    }

    private ReplicaDescriptor descriptor(ReplicaStatus status) {
        return new ReplicaDescriptor("replica-1", "web", "worker-a", "nginx", 1, 1,
                "docker-id", status, "test", Instant.now());
    }
}