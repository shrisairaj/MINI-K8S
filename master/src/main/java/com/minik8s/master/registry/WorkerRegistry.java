package com.minik8s.master.registry;

import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.Worker;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.model.WorkerStatus;

import java.util.List;
import java.util.Optional;

public interface WorkerRegistry {
    Worker registerWorker(WorkerRegistration registration);

    Optional<Worker> getWorker(String workerId);

    List<Worker> getAllWorkers();

    List<Worker> getAvailableWorkers();

    Worker updateStatus(String workerId, WorkerStatus status);

    Worker updateHeartbeat(String workerId, WorkerResources resources);

    Worker unregisterWorker(String workerId);

    boolean reserveResources(String workerId, String reservationId, ResourceRequest request);

    void releaseResources(String workerId, String reservationId);
}