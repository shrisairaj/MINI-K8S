package com.minik8s.master.exception;

public class DeploymentNotFoundException extends RuntimeException {
    public DeploymentNotFoundException(String deploymentId) {
        super("Deployment not found: " + deploymentId);
    }
}