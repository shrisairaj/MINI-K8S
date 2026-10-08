package com.minik8s.master.grpc;

/** A Worker definitively rejected an operation; it did not begin executing it. */
public class WorkerOperationRejectedException extends RuntimeException {
    public WorkerOperationRejectedException(String message) {
        super(message);
    }

    public WorkerOperationRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}