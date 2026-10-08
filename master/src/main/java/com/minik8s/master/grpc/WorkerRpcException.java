package com.minik8s.master.grpc;

public class WorkerRpcException extends RuntimeException {
    public WorkerRpcException(String message, Throwable cause) {
        super(message, cause);
    }
}