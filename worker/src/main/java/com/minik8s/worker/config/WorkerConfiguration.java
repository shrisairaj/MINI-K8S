package com.minik8s.worker.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.minik8s.worker.docker.ContainerRuntime;
import com.minik8s.worker.docker.DockerJavaContainerRuntime;
import com.minik8s.worker.grpc.GrpcWorkerServer;
import com.minik8s.worker.grpc.WorkerGrpcService;
import com.minik8s.worker.service.WorkerContainerService;
import com.minik8s.worker.service.WorkerResourceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfiguration {
    @Bean(destroyMethod = "close")
    DockerClient dockerClient() {
        return DockerClientBuilder.getInstance().build();
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