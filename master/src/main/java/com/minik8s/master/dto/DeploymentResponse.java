package com.minik8s.master.dto;

import com.minik8s.master.model.DeploymentStatus;

import java.util.List;

public record DeploymentResponse(
        String id,
        String name,
        String image,
        int desiredReplicas,
        int runningReplicas,
        int containerPort,
        DeploymentStatus status,
        List<ReplicaSummaryDto> replicas
) {}
