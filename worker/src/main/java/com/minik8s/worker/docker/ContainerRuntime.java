package com.minik8s.worker.docker;

import java.util.List;
import java.util.Optional;

public interface ContainerRuntime {
    RuntimeContainer start(ContainerSpec spec);

    void stop(String replicaId);

    void remove(String replicaId);

    Optional<RuntimeContainer> inspect(String replicaId);

    List<RuntimeContainer> list();
}