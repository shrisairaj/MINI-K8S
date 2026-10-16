package com.minik8s.cli.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentResponse {
    private String id;
    private String name;
    private String image;
    private int desiredReplicas;
    private int runningReplicas;
    private int containerPort;
    private String status;
    private List<ReplicaResponse> replicas;

    public DeploymentResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getDesiredReplicas() { return desiredReplicas; }
    public void setDesiredReplicas(int desiredReplicas) { this.desiredReplicas = desiredReplicas; }
    public int getRunningReplicas() { return runningReplicas; }
    public void setRunningReplicas(int runningReplicas) { this.runningReplicas = runningReplicas; }
    public int getContainerPort() { return containerPort; }
    public void setContainerPort(int containerPort) { this.containerPort = containerPort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<ReplicaResponse> getReplicas() { return replicas; }
    public void setReplicas(List<ReplicaResponse> replicas) { this.replicas = replicas; }
}
