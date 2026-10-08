package com.minik8s.master.config;

import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.master.replica.ReplicaManager;
import com.minik8s.master.scheduler.Scheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReplicaManagementConfiguration {
    @Bean
    @ConditionalOnBean(WorkerClient.class)
    ReplicaManager replicaManager(Scheduler scheduler, WorkerClient workerClient, WorkerRegistry workerRegistry) {
        return new ReplicaManager(scheduler, workerClient, workerRegistry);
    }

    @Bean
    @ConditionalOnBean(ReplicaManager.class)
    @ConditionalOnProperty(name = "minik8s.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
    ReplicaReconciliationJob replicaReconciliationJob(ReplicaManager replicaManager) {
        return new ReplicaReconciliationJob(replicaManager);
    }
}