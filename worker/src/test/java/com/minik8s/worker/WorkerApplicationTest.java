package com.minik8s.worker;

import com.github.dockerjava.api.DockerClient;
import com.minik8s.worker.grpc.GrpcWorkerServer;
import com.minik8s.worker.service.WorkerHeartbeatPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "minik8s.worker.id=worker-test",
        "minik8s.worker.grpc-port=0",
        "minik8s.worker.master-url=http://127.0.0.1:1"
})
class WorkerApplicationTest {
    @MockitoBean
    private DockerClient dockerClient;

    @Autowired
    private GrpcWorkerServer grpcWorkerServer;

    @Autowired
    private WorkerHeartbeatPublisher heartbeatPublisher;

    @Test
    void startsWorkerContextAndGrpcServerWithoutDockerDaemon() {
        assertNotNull(grpcWorkerServer);
        assertNotNull(heartbeatPublisher);
    }
}