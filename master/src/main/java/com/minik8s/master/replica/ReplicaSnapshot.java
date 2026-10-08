package com.minik8s.master.replica;

import com.minik8s.master.model.Replica;
import com.minik8s.master.model.ReplicaStatus;

import java.util.List;

public record ReplicaSnapshot(String deploymentId, int desiredReplicas, List<Replica> replicas) {
    public ReplicaSnapshot {
        replicas = List.copyOf(replicas);
    }

    public long runningCount() {
        return replicas.stream().filter(replica -> replica.status() == ReplicaStatus.RUNNING).count();
    }

    public long occupyingCount() {
        return replicas.stream().filter(replica -> switch (replica.status()) {
            case PENDING, STARTING, RUNNING, STOPPING -> true;
            case FAILED, STOPPED -> false;
        }).count();
    }
}