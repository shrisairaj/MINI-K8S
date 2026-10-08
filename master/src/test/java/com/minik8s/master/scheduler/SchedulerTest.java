package com.minik8s.master.scheduler;

import com.minik8s.master.exception.NoAvailableWorkerException;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.registry.InMemoryWorkerRegistry;
import com.minik8s.master.registry.WorkerRegistration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchedulerTest {
    private InMemoryWorkerRegistry registry;
    private Scheduler scheduler;

    @BeforeEach
    void setUp() {
        registry = new InMemoryWorkerRegistry();
        scheduler = new Scheduler(registry, new ResourceAwareLeastLoadedStrategy());
    }

    @Test
    void failsWhenNoWorkersAreAvailable() {
        assertThrows(NoAvailableWorkerException.class,
                () -> scheduler.schedule("replica-1", new ResourceRequest(100, 100)));
    }

    @Test
    void ignoresUnreadyAndInsufficientWorkers() {
        register("worker-unready", 4000, 8000, false);
        register("worker-small", 500, 500, true);
        register("worker-large", 4000, 8000, true);

        var decision = scheduler.schedule("replica-1", new ResourceRequest(1000, 1000));

        assertEquals("worker-large", decision.worker().workerId());
    }

    @Test
    void picksWorkerWithLowestProjectedPeakUtilization() {
        register("worker-a", 8000, 16_000, true);
        registry.updateHeartbeat("worker-a", new WorkerResources(8000, 16_000, 4000, 8000));
        register("worker-b", 8000, 16_000, true);
        registry.updateHeartbeat("worker-b", new WorkerResources(8000, 16_000, 7000, 14_000));

        var decision = scheduler.schedule("replica-1", new ResourceRequest(500, 1000));

        assertEquals("worker-b", decision.worker().workerId());
    }

    @Test
    void resourceReservationsPreventBackToBackOversubscription() {
        register("worker-a", 1000, 1000, true);
        ResourceRequest request = new ResourceRequest(700, 700);

        assertEquals("worker-a", scheduler.schedule("replica-1", request).worker().workerId());
        assertThrows(NoAvailableWorkerException.class, () -> scheduler.schedule("replica-2", request));
    }

    private void register(String id, long cpu, long memory, boolean ready) {
        registry.registerWorker(new WorkerRegistration(id, "localhost", 9091,
                new WorkerResources(cpu, memory, cpu, memory)));
        if (ready) {
            registry.updateHeartbeat(id, new WorkerResources(cpu, memory, cpu, memory));
        } else {
            registry.updateStatus(id, WorkerStatus.REGISTERED);
        }
    }
}