package com.minik8s.master.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Represents the desired application deployment state within the Master service.
 */
public class Deployment {

    @NotBlank(message = "Deployment ID cannot be blank")
    private String id;

    @NotBlank(message = "Deployment name cannot be blank")
    private String name;

    @NotBlank(message = "Container image cannot be blank")
    private String image;

    @Min(value = 0, message = "Replicas count cannot be negative")
    private int replicas;

    @Min(value = 1, message = "Container port must be between 1 and 65535")
    @Max(value = 65535, message = "Container port must be between 1 and 65535")
    private int containerPort;

    @NotNull(message = "Deployment status cannot be null")
    private DeploymentStatus status = DeploymentStatus.PENDING;

    public Deployment() {
    }

    public Deployment(String id, String name, String image, int replicas, int containerPort) {
        this(id, name, image, replicas, containerPort, DeploymentStatus.PENDING);
    }

    public Deployment(String id, String name, String image, int replicas, int containerPort, DeploymentStatus status) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.replicas = replicas;
        this.containerPort = containerPort;
        this.status = status != null ? status : DeploymentStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public int getContainerPort() {
        return containerPort;
    }

    public void setContainerPort(int containerPort) {
        this.containerPort = containerPort;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Deployment that = (Deployment) o;
        return replicas == that.replicas &&
                containerPort == that.containerPort &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(image, that.image) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, image, replicas, containerPort, status);
    }

    @Override
    public String toString() {
        return "Deployment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", replicas=" + replicas +
                ", containerPort=" + containerPort +
                ", status=" + status +
                '}';
    }
}
