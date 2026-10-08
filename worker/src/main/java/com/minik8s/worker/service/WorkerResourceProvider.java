package com.minik8s.worker.service;

@FunctionalInterface
public interface WorkerResourceProvider {
    WorkerResourcesSnapshot snapshot();
}