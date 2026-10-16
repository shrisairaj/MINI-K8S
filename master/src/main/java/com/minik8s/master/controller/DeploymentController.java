package com.minik8s.master.controller;

import com.minik8s.master.dto.DeploymentRequest;
import com.minik8s.master.dto.DeploymentResponse;
import com.minik8s.master.dto.ReplicaSummaryDto;
import com.minik8s.master.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeploymentResponse createDeployment(@Valid @RequestBody DeploymentRequest request) {
        return deploymentService.createOrUpdateDeployment(request);
    }

    @GetMapping
    public List<DeploymentResponse> listDeployments() {
        return deploymentService.listDeployments();
    }

    @GetMapping("/{id}")
    public DeploymentResponse getDeployment(@PathVariable("id") String id) {
        return deploymentService.getDeployment(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDeployment(@PathVariable("id") String id) {
        deploymentService.deleteDeployment(id);
    }

    @GetMapping("/{id}/replicas")
    public List<ReplicaSummaryDto> listDeploymentReplicas(@PathVariable("id") String id) {
        return deploymentService.listReplicas(id);
    }
}
