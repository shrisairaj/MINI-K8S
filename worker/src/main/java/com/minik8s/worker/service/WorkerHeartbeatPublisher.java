package com.minik8s.worker.service;

import com.minik8s.worker.config.WorkerProperties;
import com.minik8s.worker.exception.ContainerOperationException;
import com.minik8s.worker.master.MasterApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WorkerHeartbeatPublisher {
    private static final Logger logger = LoggerFactory.getLogger(WorkerHeartbeatPublisher.class);

    private final MasterApiClient master;
    private final WorkerResourceProvider resources;
    private final WorkerContainerService containers;
    private final WorkerProperties properties;
    private final AtomicBoolean registered = new AtomicBoolean();

    public WorkerHeartbeatPublisher(MasterApiClient master, WorkerResourceProvider resources,
                                    WorkerContainerService containers, WorkerProperties properties) {
        this.master = master;
        this.resources = resources;
        this.containers = containers;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerAtStartup() {
        register();
    }

    @Scheduled(fixedDelayString = "${minik8s.worker.heartbeat-delay:5000}")
    public void sendHeartbeat() {
        if (!registered.get()) {
            register();
            return;
        }
        try {
            WorkerResourcesSnapshot snapshot = currentResources();
            master.heartbeat(snapshot);
            containers.publishStatuses();
        } catch (ContainerOperationException exception) {
            registered.set(false);
            try {
                master.markUnavailable();
            } catch (RestClientException reportingFailure) {
                exception.addSuppressed(reportingFailure);
            }
            logger.warn("Worker {} marked unavailable because Docker Engine is not healthy",
                    properties.getId(), exception);
        } catch (RestClientException exception) {
            registered.set(false);
            logger.warn("Worker {} could not reach Master heartbeat endpoint", properties.getId(), exception);
        }
    }

    private void register() {
        try {
            master.register(currentResources());
            registered.set(true);
            logger.info("Worker {} registered with Master at {}", properties.getId(), properties.getMasterUrl());
        } catch (RestClientException exception) {
            logger.warn("Worker {} could not register with Master; will retry", properties.getId(), exception);
        } catch (ContainerOperationException exception) {
            logger.warn("Worker {} cannot register until Docker Engine is healthy", properties.getId(), exception);
        }
    }

    private WorkerResourcesSnapshot currentResources() {
        WorkerResourcesSnapshot host = resources.snapshot();
        int running;
        try {
            running = containers.runningCount();
        } catch (RuntimeException exception) {
            logger.warn("Worker {} could not query running container count", properties.getId(), exception);
            running = 0;
        }
        return new WorkerResourcesSnapshot(host.totalCpuMillis(), host.totalMemoryBytes(),
                host.availableCpuMillis(), host.availableMemoryBytes(), running);
    }
}