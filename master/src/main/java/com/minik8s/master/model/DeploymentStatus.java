package com.minik8s.master.model;

public enum DeploymentStatus {
    PENDING,
    SCALING,
    RUNNING,
    DEGRADED,
    FAILED,
    STOPPED
}
