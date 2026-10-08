package com.minik8s.master.config;

import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.grpc.WorkerGrpcClient;
import com.minik8s.master.registry.WorkerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerGrpcClientConfiguration {
    @Bean
    WorkerClient workerClient(WorkerRegistry workerRegistry,
                              @org.springframework.beans.factory.annotation.Value("${minik8s.grpc.deadline-millis:3000}")
                              long deadlineMillis) {
        return new WorkerGrpcClient(workerRegistry, deadlineMillis);
    }
}