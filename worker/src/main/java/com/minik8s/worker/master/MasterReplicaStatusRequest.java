package com.minik8s.worker.master;

public record MasterReplicaStatusRequest(String deploymentId, String replicaId, String status) {
}