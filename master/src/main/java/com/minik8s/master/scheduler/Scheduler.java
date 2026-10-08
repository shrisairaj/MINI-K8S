package com.minik8s.master.scheduler;

import com.minik8s.master.exception.NoAvailableWorkerException;
import com.minik8s.master.exception.WorkerNotFoundException;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.SchedulingDecision;
import com.minik8s.master.model.Worker;
import com.minik8s.master.registry.WorkerRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Scheduler {
    private final WorkerRegistry workerRegistry;
    private final SchedulingStrategy strategy;

    public Scheduler(WorkerRegistry workerRegistry, SchedulingStrategy strategy) {
        this.workerRegistry = Objects.requireNonNull(workerRegistry, "workerRegistry");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    public SchedulingDecision schedule(String reservationId, ResourceRequest request) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID must not be blank");
        }
        Objects.requireNonNull(request, "request");
        List<Worker> candidates = new ArrayList<>(workerRegistry.getAvailableWorkers());
        while (!candidates.isEmpty()) {
            Worker candidate = strategy.selectWorker(List.copyOf(candidates), request)
                    .orElseThrow(NoAvailableWorkerException::new);
            try {
                if (workerRegistry.reserveResources(candidate.workerId(), reservationId, request)) {
                    return new SchedulingDecision(reservationId, candidate, request, Instant.now());
                }
            } catch (WorkerNotFoundException ignored) {
                // The worker was removed between the snapshot and the reservation attempt.
            }
            candidates.removeIf(worker -> worker.workerId().equals(candidate.workerId()));
        }
        throw new NoAvailableWorkerException();
    }
}