package com.minik8s.master.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minik8s.master.dto.DeploymentRequest;
import com.minik8s.master.grpc.WorkerClient;
import com.minik8s.master.model.WorkerResources;
import com.minik8s.master.registry.WorkerRegistration;
import com.minik8s.master.registry.WorkerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeploymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkerRegistry workerRegistry;

    @MockitoBean
    private WorkerClient workerClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        workerRegistry.registerWorker(new WorkerRegistration(
                "worker-1",
                "127.0.0.1",
                9091,
                new WorkerResources(4000L, 8192L * 1024L * 1024L, 4000L, 8192L * 1024L * 1024L)
        ));
        workerRegistry.updateStatus("worker-1", com.minik8s.master.model.WorkerStatus.READY);
    }

    @Test
    void createAndGetDeployment() throws Exception {
        DeploymentRequest request = new DeploymentRequest(
                "nginx-dep",
                "nginx-dep",
                "nginx:alpine",
                2,
                80,
                500L,
                256L * 1024L * 1024L
        );

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("nginx-dep")))
                .andExpect(jsonPath("$.name", is("nginx-dep")))
                .andExpect(jsonPath("$.image", is("nginx:alpine")))
                .andExpect(jsonPath("$.desiredReplicas", is(2)))
                .andExpect(jsonPath("$.containerPort", is(80)))
                .andExpect(jsonPath("$.replicas", hasSize(2)));

        mockMvc.perform(get("/api/deployments/nginx-dep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("nginx-dep")))
                .andExpect(jsonPath("$.desiredReplicas", is(2)));
    }

    @Test
    void listDeploymentsAndReplicas() throws Exception {
        DeploymentRequest request = new DeploymentRequest(
                "redis-dep",
                "redis-dep",
                "redis:alpine",
                1,
                6379,
                null,
                null
        );

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/deployments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/deployments/redis-dep/replicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deploymentId", is("redis-dep")));

        mockMvc.perform(get("/api/replicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void deleteDeployment() throws Exception {
        DeploymentRequest request = new DeploymentRequest(
                "temp-dep",
                "temp-dep",
                "busybox:latest",
                1,
                8080,
                null,
                null
        );

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/deployments/temp-dep"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/deployments/temp-dep"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getNonExistentDeploymentReturns404() throws Exception {
        mockMvc.perform(get("/api/deployments/unknown-dep"))
                .andExpect(status().isNotFound());
    }
}
