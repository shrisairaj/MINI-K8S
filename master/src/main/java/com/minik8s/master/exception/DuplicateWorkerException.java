package com.minik8s.master.exception;

public class DuplicateWorkerException extends RuntimeException {
    public DuplicateWorkerException(String workerId) {
        super("Worker ID is already registered with a different endpoint or was removed: " + workerId);
    }
}