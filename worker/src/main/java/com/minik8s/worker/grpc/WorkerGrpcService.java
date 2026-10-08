package com.minik8s.worker.grpc;

import com.minik8s.protocol.worker.v1.ListReplicasReply;
import com.minik8s.protocol.worker.v1.ListReplicasRequest;
import com.minik8s.protocol.worker.v1.OperationReply;
import com.minik8s.protocol.worker.v1.ReplicaRequest;
import com.minik8s.protocol.worker.v1.ReplicaStatusReply;
import com.minik8s.protocol.worker.v1.StartReplicaRequest;
import com.minik8s.protocol.worker.v1.WorkerServiceGrpc;
import com.minik8s.protocol.worker.v1.WorkerStatusReply;
import com.minik8s.protocol.worker.v1.WorkerStatusRequest;
import com.minik8s.worker.config.WorkerProperties;
import com.minik8s.worker.docker.RuntimeContainer;
import com.minik8s.worker.exception.ContainerOperationException;
import com.minik8s.worker.exception.InvalidReplicaRequestException;
import com.minik8s.worker.exception.ReplicaNotFoundException;
import com.minik8s.worker.model.ReplicaDescriptor;
import com.minik8s.worker.service.WorkerContainerService;
import com.minik8s.worker.service.WorkerResourceProvider;
import com.minik8s.worker.service.WorkerResourcesSnapshot;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class WorkerGrpcService extends WorkerServiceGrpc.WorkerServiceImplBase {
    private static final Logger logger = LoggerFactory.getLogger(WorkerGrpcService.class);

    private final WorkerContainerService containers;
    private final WorkerResourceProvider resourceProvider;
    private final WorkerProperties properties;

    public WorkerGrpcService(WorkerContainerService containers, WorkerResourceProvider resourceProvider,
                             WorkerProperties properties) {
        this.containers = containers;
        this.resourceProvider = resourceProvider;
        this.properties = properties;
    }

    @Override
    public void startReplica(StartReplicaRequest request, StreamObserver<OperationReply> observer) {
        try {
            long cpu = request.hasResources() ? request.getResources().getCpuMillis() : 0;
            long memory = request.hasResources() ? request.getResources().getMemoryBytes() : 0;
            containers.validateStartRequest(request.getReplicaId(), request.getDeploymentId(), request.getWorkerId(),
                    request.getImage(), cpu, memory);
            CompletableFuture.runAsync(() -> {
                try {
                    containers.start(request.getReplicaId(), request.getDeploymentId(), request.getWorkerId(),
                            request.getImage(), cpu, memory);
                } catch (RuntimeException exception) {
                    logger.warn("Asynchronous start failed for replica {}", request.getReplicaId(), exception);
                }
            });
            observer.onNext(OperationReply.newBuilder().setAccepted(true).setReplicaId(request.getReplicaId())
                    .setMessage("Start accepted; container status will be reported asynchronously").build());
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception).withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void stopReplica(ReplicaRequest request, StreamObserver<OperationReply> observer) {
        try {
            ReplicaDescriptor stopped = containers.stop(request.getReplicaId(), request.getWorkerId());
            observer.onNext(OperationReply.newBuilder().setAccepted(true).setReplicaId(request.getReplicaId())
                    .setMessage(stopped.message()).build());
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception).withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void removeReplica(ReplicaRequest request, StreamObserver<OperationReply> observer) {
        try {
            ReplicaDescriptor removed = containers.remove(request.getReplicaId(), request.getWorkerId());
            observer.onNext(OperationReply.newBuilder().setAccepted(true).setReplicaId(request.getReplicaId())
                    .setMessage(removed.message()).build());
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception).withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void getReplicaStatus(ReplicaRequest request, StreamObserver<ReplicaStatusReply> observer) {
        try {
            observer.onNext(toReply(containers.get(request.getReplicaId(), request.getWorkerId())));
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception).withDescription(exception.getMessage()).withCause(exception).asRuntimeException());
        }
    }

    @Override
    public void listReplicas(ListReplicasRequest request, StreamObserver<ListReplicasReply> observer) {
        if (!properties.getId().equals(request.getWorkerId())) {
            observer.onError(Status.FAILED_PRECONDITION.withDescription("Request addressed to a different Worker")
                    .asRuntimeException());
            return;
        }
            try {
                ListReplicasReply reply = ListReplicasReply.newBuilder()
                    .addAllReplicas(containers.list().stream().map(this::toReply).toList()).build();
                observer.onNext(reply);
                observer.onCompleted();
            } catch (RuntimeException exception) {
                observer.onError(toStatus(exception).withDescription(exception.getMessage()).withCause(exception)
                    .asRuntimeException());
            }
    }

    @Override
    public void getWorkerStatus(WorkerStatusRequest request, StreamObserver<WorkerStatusReply> observer) {
        if (!properties.getId().equals(request.getWorkerId())) {
            observer.onError(Status.FAILED_PRECONDITION.withDescription("Request addressed to a different Worker")
                    .asRuntimeException());
            return;
        }
        try {
            WorkerResourcesSnapshot resources = resourceProvider.snapshot();
            WorkerStatusReply reply = WorkerStatusReply.newBuilder()
                .setWorkerId(properties.getId()).setHost(properties.getHost()).setGrpcPort(properties.getGrpcPort())
                .setTotalCpuMillis(resources.totalCpuMillis()).setTotalMemoryBytes(resources.totalMemoryBytes())
                .setAvailableCpuMillis(resources.availableCpuMillis())
                .setAvailableMemoryBytes(resources.availableMemoryBytes())
                .setRunningReplicas(containers.runningCount()).setObservedAtEpochMillis(Instant.now().toEpochMilli())
                .build();
            observer.onNext(reply);
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception).withDescription(exception.getMessage()).withCause(exception)
                .asRuntimeException());
        }
    }

    private ReplicaStatusReply toReply(ReplicaDescriptor descriptor) {
        return ReplicaStatusReply.newBuilder().setReplicaId(descriptor.replicaId())
                .setDeploymentId(descriptor.deploymentId()).setWorkerId(descriptor.workerId())
                .setImage(descriptor.image()).setStatus(descriptor.status().name())
                .setMessage(descriptor.message() == null ? "" : descriptor.message())
                .setUpdatedAtEpochMillis(descriptor.updatedAt().toEpochMilli()).build();
    }

    private Status toStatus(RuntimeException exception) {
        if (exception instanceof InvalidReplicaRequestException) {
            return Status.INVALID_ARGUMENT;
        }
        if (exception instanceof ReplicaNotFoundException) {
            return Status.NOT_FOUND;
        }
        if (exception instanceof ContainerOperationException) {
            return Status.UNAVAILABLE;
        }
        return Status.INTERNAL;
    }
}