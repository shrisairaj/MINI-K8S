package com.minik8s.master.registry;

import com.minik8s.master.exception.DuplicateWorkerException;
import com.minik8s.master.exception.WorkerNotFoundException;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.Worker;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.model.WorkerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryWorkerRegistryTest {
    private InMemoryWorkerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryWorkerRegistry();
    }

    @Test
    void registersWorkerAndHeartbeatMakesItReady() {
        Worker registered = registry.registerWorker(registration("worker-a", "host-a", 9091));

        assertEquals(WorkerStatus.REGISTERED, registered.status());
        assertTrue(registry.getAvailableWorkers().isEmpty());

        Worker ready = registry.updateHeartbeat("worker-a", resources(4000, 8_000));

        assertEquals(WorkerStatus.READY, ready.status());
        assertEquals("worker-a", registry.getAvailableWorkers().getFirst().workerId());
    }

    @Test
    void permitsIdempotentRegistrationButRejectsDifferentEndpoint() {
        registry.registerWorker(registration("worker-a", "host-a", 9091));

        assertEquals("host-a", registry.registerWorker(registration("worker-a", "host-a", 9091)).host());
        assertThrows(DuplicateWorkerException.class,
                () -> registry.registerWorker(registration("worker-a", "host-b", 9091)));
    }

    @Test
    void updatesStatusAndUnregistersWorker() {
        registry.registerWorker(registration("worker-a", "host-a", 9091));
        registry.updateHeartbeat("worker-a", resources(4000, 8_000));

        registry.updateStatus("worker-a", WorkerStatus.UNAVAILABLE);
        assertTrue(registry.getAvailableWorkers().isEmpty());

        Worker removed = registry.unregisterWorker("worker-a");
        assertEquals(WorkerStatus.REMOVED, removed.status());
        assertThrows(IllegalStateException.class,
                () -> registry.updateStatus("worker-a", WorkerStatus.READY));
    }

    @Test
    void listsWorkersInStableOrderAndThrowsWhenMissing() {
        registry.registerWorker(registration("worker-b", "host-b", 9092));
        registry.registerWorker(registration("worker-a", "host-a", 9091));

        assertEquals("worker-a", registry.getAllWorkers().getFirst().workerId());
        assertTrue(registry.getWorker("missing").isEmpty());
        assertThrows(WorkerNotFoundException.class,
                () -> registry.updateStatus("missing", WorkerStatus.READY));
    }

    @Test
    void reservationsReduceCapacityAndCanBeReleasedIdempotently() {
        registry.registerWorker(registration("worker-a", "host-a", 9091));
        registry.updateHeartbeat("worker-a", resources(4000, 8_000));

        assertTrue(registry.reserveResources("worker-a", "replica-1", new ResourceRequest(3000, 6_000)));
        assertFalse(registry.reserveResources("worker-a", "replica-2", new ResourceRequest(2000, 1_000)));
        assertEquals(1000, registry.getAvailableWorkers().getFirst().resources().availableCpuMillis());

        registry.releaseResources("worker-a", "replica-1");
        registry.releaseResources("worker-a", "replica-1");
        assertEquals(4000, registry.getAvailableWorkers().getFirst().resources().availableCpuMillis());
    }

    @Test
    void concurrentReservationsDoNotOversubscribeWorker() throws Exception {
        registry.registerWorker(registration("worker-a", "host-a", 9091));
        registry.updateHeartbeat("worker-a", resources(1000, 1000));
        int contenders = 12;
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(contenders)) {
            for (int i = 0; i < contenders; i++) {
                int id = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        if (registry.reserveResources("worker-a", "r-" + id,
                                new ResourceRequest(250, 250))) {
                            accepted.incrementAndGet();
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
        }

        assertEquals(4, accepted.get());
    }

    private WorkerRegistration registration(String id, String host, int port) {
        return new WorkerRegistration(id, host, port, resources(4000, 8_000));
    }

    private WorkerResources resources(long cpu, long memory) {
        return new WorkerResources(cpu, memory, cpu, memory);
    }
}