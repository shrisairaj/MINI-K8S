package com.minik8s.master.registry;

import com.minik8s.master.exception.DuplicateWorkerException;
import com.minik8s.master.exception.WorkerNotFoundException;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.Worker;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.model.WorkerStatus;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryWorkerRegistry implements WorkerRegistry {
    private final ConcurrentMap<String, Entry> workers = new ConcurrentHashMap<>();

    @Override
    public Worker registerWorker(WorkerRegistration registration) {
        Entry entry = workers.compute(registration.workerId(), (workerId, existing) -> {
            if (existing == null) {
                return new Entry(new Worker(workerId, registration.host(), registration.grpcPort(),
                        WorkerStatus.REGISTERED, registration.resources(), null));
            }
            synchronized (existing) {
                Worker current = existing.worker;
                if (current.status() == WorkerStatus.REMOVED) {
                    throw new DuplicateWorkerException(workerId);
                }
                if (!current.host().equals(registration.host()) || current.grpcPort() != registration.grpcPort()) {
                    throw new DuplicateWorkerException(workerId);
                }
                existing.worker = new Worker(workerId, registration.host(), registration.grpcPort(),
                        WorkerStatus.REGISTERED, registration.resources(), current.lastHeartbeat());
                return existing;
            }
        });
        return entry.snapshot();
    }

    @Override
    public Optional<Worker> getWorker(String workerId) {
        Entry entry = workers.get(workerId);
        return entry == null ? Optional.empty() : Optional.of(entry.snapshot());
    }

    @Override
    public List<Worker> getAllWorkers() {
        return workers.values().stream()
                .map(Entry::snapshot)
                .sorted(Comparator.comparing(Worker::workerId))
                .toList();
    }

    @Override
    public List<Worker> getAvailableWorkers() {
        return workers.values().stream()
                .map(Entry::schedulableSnapshot)
                .filter(worker -> worker != null && worker.status() == WorkerStatus.READY)
                .sorted(Comparator.comparing(Worker::workerId))
                .toList();
    }

    @Override
    public Worker updateStatus(String workerId, WorkerStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Worker status is required");
        }
        Entry entry = requireEntry(workerId);
        synchronized (entry) {
            if (entry.worker.status() == WorkerStatus.REMOVED && status != WorkerStatus.REMOVED) {
                throw new IllegalStateException("A removed worker cannot transition to " + status);
            }
            entry.worker = entry.worker.withStatus(status);
            return entry.worker;
        }
    }

    @Override
    public Worker updateHeartbeat(String workerId, WorkerResources resources) {
        if (resources == null) {
            throw new IllegalArgumentException("Heartbeat resources are required");
        }
        Entry entry = requireEntry(workerId);
        synchronized (entry) {
            Worker current = entry.worker;
            WorkerStatus nextStatus = switch (current.status()) {
                case REGISTERED, UNAVAILABLE -> WorkerStatus.READY;
                default -> current.status();
            };
            entry.worker = current.withHeartbeat(resources, Instant.now(), nextStatus);
            return entry.worker;
        }
    }

    @Override
    public Worker unregisterWorker(String workerId) {
        return updateStatus(workerId, WorkerStatus.REMOVED);
    }

    @Override
    public boolean reserveResources(String workerId, String reservationId, ResourceRequest request) {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("Reservation ID must not be blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("Resource request is required");
        }
        Entry entry = requireEntry(workerId);
        synchronized (entry) {
            if (entry.worker.status() != WorkerStatus.READY) {
                return false;
            }
            ResourceRequest previous = entry.reservations.get(reservationId);
            if (previous != null) {
                return previous.equals(request);
            }
            WorkerResources effective = entry.effectiveResources();
            if (!effective.canFit(request)) {
                return false;
            }
            entry.reservations.put(reservationId, request);
            return true;
        }
    }

    @Override
    public void releaseResources(String workerId, String reservationId) {
        Entry entry = workers.get(workerId);
        if (entry != null && reservationId != null) {
            synchronized (entry) {
                entry.reservations.remove(reservationId);
            }
        }
    }

    private Entry requireEntry(String workerId) {
        Entry entry = workers.get(workerId);
        if (entry == null) {
            throw new WorkerNotFoundException(workerId);
        }
        return entry;
    }

    private static final class Entry {
        private volatile Worker worker;
        private final ConcurrentMap<String, ResourceRequest> reservations = new ConcurrentHashMap<>();

        private Entry(Worker worker) {
            this.worker = worker;
        }

        private Worker snapshot() {
            return worker;
        }

        private synchronized Worker schedulableSnapshot() {
            if (worker.status() != WorkerStatus.READY) {
                return null;
            }
            WorkerResources effective = effectiveResources();
            return worker.withResources(effective);
        }

        private WorkerResources effectiveResources() {
            WorkerResources reported = worker.resources();
            long reservedCpu = 0;
            long reservedMemory = 0;
            for (ResourceRequest reservation : reservations.values()) {
                reservedCpu = Math.addExact(reservedCpu, reservation.cpuMillis());
                reservedMemory = Math.addExact(reservedMemory, reservation.memoryBytes());
            }
            long unreservedCpu = Math.max(0, reported.totalCpuMillis() - reservedCpu);
            long unreservedMemory = Math.max(0, reported.totalMemoryBytes() - reservedMemory);
            return reported.withAvailable(
                    Math.min(reported.availableCpuMillis(), unreservedCpu),
                    Math.min(reported.availableMemoryBytes(), unreservedMemory));
        }
    }
}