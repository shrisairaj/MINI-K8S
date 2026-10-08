package com.minik8s.master.scheduler;

import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.Worker;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ResourceAwareLeastLoadedStrategy implements SchedulingStrategy {
    @Override
    public Optional<Worker> selectWorker(List<Worker> eligibleWorkers, ResourceRequest request) {
        return eligibleWorkers.stream()
                .filter(worker -> worker.resources().canFit(request))
                .min(Comparator.comparingDouble((Worker worker) -> projectedPeakUtilization(worker, request))
                        .thenComparing(Worker::workerId));
    }

    private double projectedPeakUtilization(Worker worker, ResourceRequest request) {
        double cpu = (double) (worker.resources().totalCpuMillis()
                - worker.resources().availableCpuMillis() + request.cpuMillis())
                / worker.resources().totalCpuMillis();
        double memory = (double) (worker.resources().totalMemoryBytes()
                - worker.resources().availableMemoryBytes() + request.memoryBytes())
                / worker.resources().totalMemoryBytes();
        return Math.max(cpu, memory);
    }
}