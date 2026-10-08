package com.minik8s.worker.grpc;

import com.minik8s.worker.config.WorkerProperties;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GrpcWorkerServer {
    private static final Logger logger = LoggerFactory.getLogger(GrpcWorkerServer.class);
    private final WorkerProperties properties;
    private final WorkerGrpcService service;
    private Server server;

    public GrpcWorkerServer(WorkerProperties properties, WorkerGrpcService service) {
        this.properties = properties;
        this.service = service;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(properties.getGrpcPort()).addService(service).build().start();
        logger.info("Worker {} gRPC server started on {}:{}", properties.getId(), properties.getHost(),
                properties.getGrpcPort());
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown();
            if (!server.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                server.shutdownNow();
            }
        }
    }
}