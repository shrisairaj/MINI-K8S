package com.minik8s.master.controller;

import com.minik8s.master.dto.WorkerHeartbeatRequest;
import com.minik8s.master.dto.WorkerRegistrationRequest;
import com.minik8s.master.exception.WorkerNotFoundException;
import com.minik8s.master.model.Worker;
import com.minik8s.master.registry.WorkerRegistry;
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
@RequestMapping("/api/workers")
public class WorkerController {
    private final WorkerRegistry workerRegistry;

    public WorkerController(WorkerRegistry workerRegistry) {
        this.workerRegistry = workerRegistry;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Worker register(@Valid @RequestBody WorkerRegistrationRequest request) {
        return workerRegistry.registerWorker(request.toRegistration());
    }

    @PostMapping("/{workerId}/heartbeat")
    public Worker heartbeat(@PathVariable("workerId") String workerId,
                            @Valid @RequestBody WorkerHeartbeatRequest request) {
        return workerRegistry.updateHeartbeat(workerId, request.toResources());
    }

    @GetMapping
    public List<Worker> listWorkers() {
        return workerRegistry.getAllWorkers();
    }

    @GetMapping("/{workerId}")
    public Worker getWorker(@PathVariable("workerId") String workerId) {
        return workerRegistry.getWorker(workerId).orElseThrow(() -> new WorkerNotFoundException(workerId));
    }

    @DeleteMapping("/{workerId}")
    public Worker removeWorker(@PathVariable("workerId") String workerId) {
        return workerRegistry.unregisterWorker(workerId);
    }

    @PostMapping("/{workerId}/unavailable")
    public Worker markUnavailable(@PathVariable("workerId") String workerId) {
        return workerRegistry.updateStatus(workerId, com.minik8s.master.model.WorkerStatus.UNAVAILABLE);
    }
}