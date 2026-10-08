package com.minik8s.worker.exception;

public class ContainerOperationException extends RuntimeException {
    public ContainerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}