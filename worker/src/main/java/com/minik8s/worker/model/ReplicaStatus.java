package com.minik8s.worker.model;

public enum ReplicaStatus {
    CREATED,
    STARTING,
    RUNNING,
    STOPPED,
    FAILED,
    REMOVED
}