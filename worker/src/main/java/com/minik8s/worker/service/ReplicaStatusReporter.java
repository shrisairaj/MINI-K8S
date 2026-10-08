package com.minik8s.worker.service;

import com.minik8s.worker.model.ReplicaDescriptor;

@FunctionalInterface
public interface ReplicaStatusReporter {
    void report(ReplicaDescriptor replica);
}