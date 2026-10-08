package com.minik8s.master.model;

public enum WorkerStatus {
    REGISTERED,
    READY,
    UNAVAILABLE,
    DRAINING,
    REMOVED
}