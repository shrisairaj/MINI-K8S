package com.minik8s.worker.exception;

public class ReplicaNotFoundException extends RuntimeException {
    public ReplicaNotFoundException(String replicaId) {
        super("Replica not found: " + replicaId);
    }
}