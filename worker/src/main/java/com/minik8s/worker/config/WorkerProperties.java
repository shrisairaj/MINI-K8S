package com.minik8s.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minik8s.worker")
public class WorkerProperties {
    private String id = "worker-1";
    private String host = "127.0.0.1";
    private int grpcPort = 9091;
    private String masterUrl = "http://localhost:8080";
    private long heartbeatDelay = 5000;
    private long grpcDeadlineMillis = 3000;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getGrpcPort() { return grpcPort; }
    public void setGrpcPort(int grpcPort) { this.grpcPort = grpcPort; }
    public String getMasterUrl() { return masterUrl; }
    public void setMasterUrl(String masterUrl) { this.masterUrl = masterUrl; }
    public long getHeartbeatDelay() { return heartbeatDelay; }
    public void setHeartbeatDelay(long heartbeatDelay) { this.heartbeatDelay = heartbeatDelay; }
    public long getGrpcDeadlineMillis() { return grpcDeadlineMillis; }
    public void setGrpcDeadlineMillis(long grpcDeadlineMillis) { this.grpcDeadlineMillis = grpcDeadlineMillis; }
}