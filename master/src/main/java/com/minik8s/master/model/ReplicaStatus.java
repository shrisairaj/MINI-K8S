package com.minik8s.master.model;

public enum ReplicaStatus {
    PENDING,
    STARTING,
    RUNNING,
    FAILED,
    STOPPING,
    STOPPED
}