package com.minik8s.master.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minik8s.master.model.WorkerStatus;
import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.model.ResourceRequest;
import com.minik8s.master.registry.WorkerRegistry;
import com.minik8s.master.registry.WorkerRegistration;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.replica.ReplicaManager;
import com.minik8s.master.replica.ReplicaWorkload;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkerControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRegistry workerRegistry;

        @Autowired
        private ReplicaManager replicaManager;

        @MockitoBean
        private WorkerClient workerClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationAndHeartbeatMoveWorkerToReady() throws Exception {
        String registration = """
                {"workerId":"integration-worker","host":"localhost","grpcPort":9191,
                 "totalCpuMillis":4000,"totalMemoryBytes":8000,
                 "availableCpuMillis":4000,"availableMemoryBytes":8000}
                """;

        mockMvc.perform(post("/api/workers/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated());

        String heartbeat = """
                {"totalCpuMillis":4000,"totalMemoryBytes":8000,
                 "availableCpuMillis":3500,"availableMemoryBytes":7000}
                """;
        String response = mockMvc.perform(post("/api/workers/integration-worker/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON).content(heartbeat))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        assertEquals(WorkerStatus.READY.name(), body.get("status").asText());
        assertEquals(WorkerStatus.READY, workerRegistry.getWorker("integration-worker").orElseThrow().status());
    }

    @Test
    void rejectsInvalidResourceCapacity() throws Exception {
        String invalid = """
                {"workerId":"invalid-worker","host":"localhost","grpcPort":9192,
                 "totalCpuMillis":1000,"totalMemoryBytes":1000,
                 "availableCpuMillis":2000,"availableMemoryBytes":1000}
                """;

        mockMvc.perform(post("/api/workers/register")
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

        @Test
        void workerReplicaStatusCallbackUpdatesExistingReplicaManager() throws Exception {
                WorkerResources resources = new WorkerResources(4000, 8_000, 4000, 8_000);
                workerRegistry.registerWorker(new WorkerRegistration("status-worker", "localhost", 9193, resources));
                workerRegistry.updateHeartbeat("status-worker", resources);
                replicaManager.updateDesiredState(new ReplicaWorkload("status-deployment", "nginx:stable", 1,
                                new ResourceRequest(100, 100)));
                var pending = replicaManager.reconcile("status-deployment").replicas().getFirst();

                mockMvc.perform(post("/api/internal/workers/status-worker/replicas/status")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {"deploymentId":"status-deployment","replicaId":"%s","status":"RUNNING"}
                                                                """.formatted(pending.replicaId())))
                                .andExpect(status().isAccepted());

                assertEquals(1, replicaManager.getSnapshot("status-deployment").runningCount());
        }
}