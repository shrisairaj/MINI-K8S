package com.minik8s.master.config;

import com.minik8s.master.registry.InMemoryWorkerRegistry;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.master.scheduler.ResourceAwareLeastLoadedStrategy;
import com.minik8s.master.scheduler.Scheduler;
import com.minik8s.master.scheduler.SchedulingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreConfiguration {
    @Bean
    WorkerRegistry workerRegistry() {
        return new InMemoryWorkerRegistry();
    }

    @Bean
    SchedulingStrategy schedulingStrategy() {
        return new ResourceAwareLeastLoadedStrategy();
    }

    @Bean
    Scheduler scheduler(WorkerRegistry workerRegistry, SchedulingStrategy schedulingStrategy) {
        return new Scheduler(workerRegistry, schedulingStrategy);
    }
}
