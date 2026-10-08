package com.minik8s.master.controller;

import com.minik8s.master.dto.ReplicaStatusUpdateRequest;
import com.minik8s.master.model.ReplicaStatus;
import com.minik8s.master.replica.ReplicaManager;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/workers/{workerId}/replicas")
public class WorkerReplicaStatusController {
    private final ReplicaManager replicaManager;

    public WorkerReplicaStatusController(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    @PostMapping("/status")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reportStatus(@PathVariable("workerId") String workerId,
                             @Valid @RequestBody ReplicaStatusUpdateRequest request) {
        ReplicaStatus status;
        try {
            status = ReplicaStatus.valueOf(request.status());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported replica status: " + request.status(), exception);
        }
        replicaManager.reportReplicaStatus(request.deploymentId(), request.replicaId(), workerId, status);
    }
}