package com.minik8s.master.replica;

import com.minik8s.master.model.ResourceRequest;

import java.util.Objects;

public record ReplicaWorkload(
        String deploymentId,
        String image,
        int desiredReplicas,
        ResourceRequest resources) {

    public ReplicaWorkload {
        if (deploymentId == null || deploymentId.isBlank()) {
            throw new IllegalArgumentException("Deployment ID must not be blank");
        }
        if (image == null || image.isBlank()) {
            throw new IllegalArgumentException("Image must not be blank");
        }
        if (desiredReplicas < 0) {
            throw new IllegalArgumentException("Desired replica count cannot be negative");
        }
        Objects.requireNonNull(resources, "resources");
    }
}