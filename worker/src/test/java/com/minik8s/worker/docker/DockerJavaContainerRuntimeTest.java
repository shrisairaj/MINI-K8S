package com.minik8s.worker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.minik8s.worker.exception.ContainerOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DockerJavaContainerRuntimeTest {
    private DockerClient docker;
    private DockerJavaContainerRuntime runtime;
    private ListContainersCmd listCommand;

    @BeforeEach
    void setUp() {
        docker = mock(DockerClient.class);
        runtime = new DockerJavaContainerRuntime(docker);
        listCommand = mock(ListContainersCmd.class);
        when(docker.listContainersCmd()).thenReturn(listCommand);
        when(listCommand.withShowAll(true)).thenReturn(listCommand);
    }

    @Test
    void createsAndStartsLabeledContainerWithResourceConstraints() {
        when(listCommand.exec()).thenReturn(List.of());
        InspectImageCmd inspectImage = mock(InspectImageCmd.class);
        when(docker.inspectImageCmd("nginx:latest")).thenReturn(inspectImage);
        when(inspectImage.exec()).thenReturn(mock(InspectImageResponse.class));

        CreateContainerCmd create = mock(CreateContainerCmd.class);
        when(docker.createContainerCmd("nginx:latest")).thenReturn(create);
        when(create.withName(anyString())).thenReturn(create);
        when(create.withLabels(anyMap())).thenReturn(create);
        when(create.withHostConfig(any())).thenReturn(create);
        CreateContainerResponse response = mock(CreateContainerResponse.class);
        when(response.getId()).thenReturn("docker-id");
        when(create.exec()).thenReturn(response);
        StartContainerCmd start = mock(StartContainerCmd.class);
        when(docker.startContainerCmd("docker-id")).thenReturn(start);
        doNothing().when(start).exec();

        RuntimeContainer result = runtime.start(new ContainerSpec("replica-1", "web",
                "nginx:latest", 500, 1_000_000));

        assertEquals("docker-id", result.containerId());
        assertEquals("running", result.status());
        verify(create).withLabels(org.mockito.ArgumentMatchers.argThat(labels ->
                "replica-1".equals(labels.get("minik8s.replica-id"))));
        verify(start).exec();
    }

    @Test
    void stopsAndRemovesExistingContainer() {
        Container container = dockerContainer("running");
        when(listCommand.exec()).thenReturn(List.of(container));
        StopContainerCmd stop = mock(StopContainerCmd.class);
        when(docker.stopContainerCmd("docker-id")).thenReturn(stop);
        doNothing().when(stop).exec();
        RemoveContainerCmd remove = mock(RemoveContainerCmd.class);
        when(docker.removeContainerCmd("docker-id")).thenReturn(remove);
        when(remove.withForce(true)).thenReturn(remove);
        doNothing().when(remove).exec();

        runtime.stop("replica-1");
        runtime.remove("replica-1");

        verify(stop).exec();
        verify(remove).exec();
    }

    @Test
    void inspectsContainerAndWrapsDockerEngineFailure() {
        Container container = dockerContainer("running");
        when(listCommand.exec()).thenReturn(List.of(container));
        assertEquals("running", runtime.inspect("replica-1").orElseThrow().status());

        when(listCommand.exec()).thenThrow(new IllegalStateException("daemon down"));
        assertThrows(ContainerOperationException.class, () -> runtime.list());
    }

    private Container dockerContainer(String state) {
        Container container = mock(Container.class);
        when(container.getLabels()).thenReturn(Map.of(
                "minik8s.replica-id", "replica-1",
                "minik8s.deployment-id", "web"));
        when(container.getState()).thenReturn(state);
        when(container.getImage()).thenReturn("nginx:latest");
        when(container.getId()).thenReturn("docker-id");
        return container;
    }
}