package com.minik8s.master.scheduler;

import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.model.Worker;

import java.util.List;
import java.util.Optional;

public interface SchedulingStrategy {
    Optional<Worker> selectWorker(List<Worker> eligibleWorkers, ResourceRequest request);
}