package com.minik8s.master.config;

import com.minik8s.master.replica.ReplicaManager;
import org.springframework.scheduling.annotation.Scheduled;

public class ReplicaReconciliationJob {
    private final ReplicaManager replicaManager;

    public ReplicaReconciliationJob(ReplicaManager replicaManager) {
        this.replicaManager = replicaManager;
    }

    @Scheduled(fixedDelayString = "${minik8s.reconciliation.fixed-delay:5000}")
    public void reconcile() {
        replicaManager.reconcileAll();
    }
}