package com.minik8s.master.grpc;

import com.minik8s.master.model.ResourceRequest;

public record ReplicaLaunchRequest(
        String replicaId,
        String deploymentId,
        String workerId,
        String image,
        ResourceRequest resources) {
}