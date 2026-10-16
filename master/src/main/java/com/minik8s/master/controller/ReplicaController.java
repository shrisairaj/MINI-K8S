package com.minik8s.master.controller;

import com.minik8s.master.dto.ReplicaSummaryDto;
import com.minik8s.master.service.DeploymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/replicas")
public class ReplicaController {

    private final DeploymentService deploymentService;

    public ReplicaController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping
    public List<ReplicaSummaryDto> listAllReplicas() {
        return deploymentService.listReplicas(null);
    }
}
