package com.minik8s.cli.model;

public class CreateDeploymentRequest {
    private String id;
    private String name;
    private String image;
    private int replicas;
    private int containerPort;

    public CreateDeploymentRequest() {}

    public CreateDeploymentRequest(String id, String name, String image, int replicas, int containerPort) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.replicas = replicas;
        this.containerPort = containerPort;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getReplicas() { return replicas; }
    public void setReplicas(int replicas) { this.replicas = replicas; }
    public int getContainerPort() { return containerPort; }
    public void setContainerPort(int containerPort) { this.containerPort = containerPort; }
}
