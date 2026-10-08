package com.minik8s.master.grpc;

import com.minik8s.master.model.Worker;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.protocol.worker.v1.ReplicaRequest;
import com.minik8s.protocol.worker.v1.StartReplicaRequest;
import com.minik8s.protocol.worker.v1.WorkerServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class WorkerGrpcClient implements WorkerClient {
    private final WorkerRegistry workerRegistry;
    private final long deadlineMillis;
    private final ConcurrentMap<String, ChannelEntry> channels = new ConcurrentHashMap<>();

    public WorkerGrpcClient(WorkerRegistry workerRegistry,
                            @Value("${minik8s.grpc.deadline-millis:3000}") long deadlineMillis) {
        this.workerRegistry = workerRegistry;
        this.deadlineMillis = deadlineMillis;
    }

    @Override
    public void startReplica(ReplicaLaunchRequest request) {
        Worker worker = requireWorker(request.workerId());
        var protoRequest = StartReplicaRequest.newBuilder()
                .setReplicaId(request.replicaId()).setDeploymentId(request.deploymentId())
                .setWorkerId(request.workerId()).setImage(request.image())
                .setResources(com.minik8s.protocol.worker.v1.ResourceRequest.newBuilder()
                        .setCpuMillis(request.resources().cpuMillis())
                        .setMemoryBytes(request.resources().memoryBytes()).build())
                .build();
        try {
            var reply = stub(worker).startReplica(protoRequest);
            if (!reply.getAccepted()) {
                throw new WorkerOperationRejectedException(reply.getMessage());
            }
        } catch (StatusRuntimeException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void stopReplica(String workerId, String replicaId) {
        Worker worker = requireWorker(workerId);
        ReplicaRequest request = ReplicaRequest.newBuilder().setWorkerId(workerId).setReplicaId(replicaId).build();
        try {
            var stub = stub(worker);
            var reply = stub.stopReplica(request);
            if (!reply.getAccepted()) {
                throw new WorkerOperationRejectedException(reply.getMessage());
            }
            var removeReply = stub.removeReplica(request);
            if (!removeReply.getAccepted()) {
                throw new WorkerOperationRejectedException(removeReply.getMessage());
            }
        } catch (StatusRuntimeException exception) {
            throw translate(exception);
        }
    }

    @PreDestroy
    public void close() {
        channels.values().forEach(entry -> entry.channel().shutdownNow());
        channels.clear();
    }

    private Worker requireWorker(String workerId) {
        return workerRegistry.getWorker(workerId)
                .orElseThrow(() -> new WorkerOperationRejectedException("Unknown worker: " + workerId));
    }

    private WorkerServiceGrpc.WorkerServiceBlockingStub stub(Worker worker) {
        ChannelEntry entry = channels.compute(worker.workerId(), (id, current) -> {
            if (current != null && current.host().equals(worker.host()) && current.port() == worker.grpcPort()) {
                return current;
            }
            if (current != null) {
                current.channel().shutdownNow();
            }
            ManagedChannel channel = ManagedChannelBuilder.forAddress(worker.host(), worker.grpcPort())
                    .usePlaintext().build();
            return new ChannelEntry(worker.host(), worker.grpcPort(), channel);
        });
        return WorkerServiceGrpc.newBlockingStub(entry.channel())
                .withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS);
    }

    private RuntimeException translate(StatusRuntimeException exception) {
        Status.Code code = exception.getStatus().getCode();
        if (code == Status.Code.INVALID_ARGUMENT || code == Status.Code.FAILED_PRECONDITION
                || code == Status.Code.RESOURCE_EXHAUSTED || code == Status.Code.PERMISSION_DENIED
                || code == Status.Code.NOT_FOUND) {
            return new WorkerOperationRejectedException(exception.getStatus().getDescription(), exception);
        }
        return new WorkerRpcException("Worker gRPC call outcome is unknown", exception);
    }

    private record ChannelEntry(String host, int port, ManagedChannel channel) { }
}