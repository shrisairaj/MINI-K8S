package com.minik8s.worker.exception;

public class InvalidReplicaRequestException extends RuntimeException {
    public InvalidReplicaRequestException(String message) {
        super(message);
    }
}