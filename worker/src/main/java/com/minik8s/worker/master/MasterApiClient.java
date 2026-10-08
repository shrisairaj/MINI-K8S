package com.minik8s.worker.master;

import com.minik8s.worker.config.WorkerProperties;
import com.minik8s.worker.model.ReplicaDescriptor;
import com.minik8s.worker.service.ReplicaStatusReporter;
import com.minik8s.worker.service.WorkerResourcesSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MasterApiClient implements ReplicaStatusReporter {
    private final RestClient restClient;
    private final WorkerProperties properties;

    public MasterApiClient(RestClient.Builder builder, WorkerProperties properties) {
        this.restClient = builder.baseUrl(properties.getMasterUrl()).build();
        this.properties = properties;
    }

    public void register(WorkerResourcesSnapshot resources) {
        restClient.post().uri("/api/workers/register")
                .body(new MasterRegistrationRequest(properties.getId(), properties.getHost(),
                        properties.getGrpcPort(), resources.totalCpuMillis(), resources.totalMemoryBytes(),
                        resources.availableCpuMillis(), resources.availableMemoryBytes()))
                .retrieve().toBodilessEntity();
    }

    public void heartbeat(WorkerResourcesSnapshot resources) {
        restClient.post().uri("/api/workers/{workerId}/heartbeat", properties.getId())
                .body(new MasterHeartbeatRequest(resources.totalCpuMillis(), resources.totalMemoryBytes(),
                        resources.availableCpuMillis(), resources.availableMemoryBytes()))
                .retrieve().toBodilessEntity();
    }

        public void markUnavailable() {
                restClient.post().uri("/api/workers/{workerId}/unavailable", properties.getId())
                                .retrieve().toBodilessEntity();
        }

    @Override
    public void report(ReplicaDescriptor replica) {
        restClient.post().uri("/api/internal/workers/{workerId}/replicas/status", properties.getId())
                .body(new MasterReplicaStatusRequest(replica.deploymentId(), replica.replicaId(),
                        replica.status().name()))
                .retrieve().toBodilessEntity();
    }
}