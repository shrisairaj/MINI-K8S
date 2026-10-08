package com.minik8s.cli.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * CLI-local representation of a Worker as returned by the Master REST API.
 *
 * <p>This class mirrors the fields of the Master's {@code Worker} record but
 * is deliberately kept separate so that changes to the Master's model do not
 * automatically break the CLI. Adapt the field mappings here when Member 1
 * changes the Worker model.</p>
 *
 * <p>Fields (as serialised by Spring Jackson defaults):
 * <ul>
 *   <li>{@code workerId}   — unique worker identifier</li>
 *   <li>{@code host}       — hostname or IP address</li>
 *   <li>{@code grpcPort}   — gRPC port on the worker</li>
 *   <li>{@code status}     — one of REGISTERED, READY, UNAVAILABLE, DRAINING, REMOVED</li>
 *   <li>{@code resources}  — resource capacity and availability</li>
 *   <li>{@code lastHeartbeat} — ISO-8601 timestamp of the last heartbeat</li>
 * </ul>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerResponse {

    private String workerId;
    private String host;
    private int grpcPort;
    private String status;
    private WorkerResourcesResponse resources;
    private Instant lastHeartbeat;

    // Default constructor required by Jackson
    public WorkerResponse() {}

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getGrpcPort() {
        return grpcPort;
    }

    public void setGrpcPort(int grpcPort) {
        this.grpcPort = grpcPort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public WorkerResourcesResponse getResources() {
        return resources;
    }

    public void setResources(WorkerResourcesResponse resources) {
        this.resources = resources;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }
}
