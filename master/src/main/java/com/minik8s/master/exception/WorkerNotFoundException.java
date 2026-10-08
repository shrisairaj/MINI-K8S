package com.minik8s.master.exception;

public class WorkerNotFoundException extends RuntimeException {
    public WorkerNotFoundException(String workerId) {
        super("Worker not found: " + workerId);
    }
}