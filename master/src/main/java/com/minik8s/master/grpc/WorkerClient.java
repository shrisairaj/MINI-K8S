package com.minik8s.master.grpc;

public interface WorkerClient {
    /** Returns when the Worker has accepted the idempotent start request, not when the container is running.
     * Throw WorkerOperationRejectedException only for a definite rejection. Other failures have an unknown
     * outcome and must be resolved by an eventual Worker status report, using replicaId as the idempotency key.
     */
    void startReplica(ReplicaLaunchRequest request);

    /** Must be safe to retry for the same replica ID. */
    void stopReplica(String workerId, String replicaId);

    /**
     * Sends a health/ping request over gRPC to check if the target worker node is responsive.
     * Returns true if worker ping succeeds, false otherwise.
     */
    default boolean checkHealth(String workerId, String host, int grpcPort) {
        return true;
    }
}