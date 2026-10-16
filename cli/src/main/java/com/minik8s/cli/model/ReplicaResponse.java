package com.minik8s.cli.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReplicaResponse {
    private String replicaId;
    private String deploymentId;
    private String workerId;
    private String image;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public ReplicaResponse() {}

    public String getReplicaId() { return replicaId; }
    public void setReplicaId(String replicaId) { this.replicaId = replicaId; }
    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
