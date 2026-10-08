package com.minik8s.worker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.minik8s.worker.docker.ContainerRuntime;
import com.minik8s.worker.docker.DockerJavaContainerRuntime;
import com.minik8s.worker.grpc.GrpcWorkerServer;
import com.minik8s.worker.grpc.WorkerGrpcService;
import com.minik8s.worker.service.WorkerContainerService;
import com.minik8s.worker.service.WorkerResourceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class WorkerConfiguration {
    @Bean(destroyMethod = "close")
    DockerClient dockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean
    ContainerRuntime containerRuntime(DockerClient dockerClient) {
        return new DockerJavaContainerRuntime(dockerClient);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    GrpcWorkerServer grpcWorkerServer(WorkerProperties properties, WorkerContainerService containers,
                                      WorkerResourceProvider resources) {
        return new GrpcWorkerServer(properties, new WorkerGrpcService(containers, resources, properties));
    }
}