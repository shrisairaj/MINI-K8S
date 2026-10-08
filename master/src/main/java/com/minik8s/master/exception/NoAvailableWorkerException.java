package com.minik8s.master.exception;

public class NoAvailableWorkerException extends RuntimeException {
    public NoAvailableWorkerException() {
        super("No ready worker can satisfy the requested resources");
    }
}