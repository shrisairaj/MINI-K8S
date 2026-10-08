package com.minik8s.master.controller;

import com.minik8s.master.dto.ClusterStatus;
import com.minik8s.master.service.ClusterStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Endpoint for querying the overall cluster status.
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterStatusController {

    private final ClusterStatusService clusterStatusService;

    public ClusterStatusController(ClusterStatusService clusterStatusService) {
        this.clusterStatusService = clusterStatusService;
    }

    @GetMapping("/status")
    public ClusterStatus getClusterStatus() {
        return clusterStatusService.getClusterStatus();
    }
}
