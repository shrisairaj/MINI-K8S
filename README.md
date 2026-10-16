# Mini-K8s

Mini-K8s is an educational, Kubernetes-inspired container orchestration platform. A Spring Boot Master acts as the control plane, scheduling workloads to Worker nodes over gRPC. Workers manage containers through Docker Engine. The project includes a CLI (`minik8s`), deployment management, health monitoring, and cluster status observation.

## Implemented Features

### Master Control Plane
- **Worker Registry**: In-memory, thread-safe Worker Registry with registration, heartbeat, lifecycle state, resource tracking, and placement reservations.
- **Resource-Aware Scheduling**: Least-loaded scheduling strategy selecting workers based on available CPU and memory.
- **Replica Manager**: Desired vs. actual replica tracking, scaling up/down, reconciliation loop, worker failure failover, and container state synchronization.
- **Deployment Management (`DeploymentController`)**:
  - `POST /api/deployments` — create or scale deployments.
  - `GET /api/deployments` — list all deployments with running replica counts.
  - `GET /api/deployments/{id}` — retrieve deployment details and status.
  - `DELETE /api/deployments/{id}` — scale down to 0 and remove deployment.
  - `GET /api/replicas` and `GET /api/deployments/{id}/replicas` — inspect cluster-wide or deployment-specific containers.
- **Cluster Status API (`/api/cluster/status`)**: Aggregated cluster health report with total workers, healthy workers, and running containers.
- **Worker REST APIs**: Registration, heartbeat, lookup, listing, removal, and health status callback.

### Shared Protocol & Worker Node
- **gRPC Contract**: Protocol Buffers contract in `protocol/src/main/proto/worker.proto`.
- **Worker Node**: Spring Boot service with a gRPC server for starting, stopping, removing, inspecting, and listing replicas.
- **Docker Engine Integration**: Docker Java SDK (`docker-java`) integration behind a `ContainerRuntime` abstraction with idempotent label-based tracking (`minik8s.replica-id`).
- **Heartbeat & Status Reporter**: Periodic heartbeat to Master with live CPU millicores and memory statistics.

### CLI (`minik8s`)
- **Picocli-powered CLI**: Packaged as a shaded fat JAR and executable via `minik8s.cmd`.
- `minik8s deploy <name> --image <image> --replicas <count> --port <port>` — deploy workload.
- `minik8s get workers` — list registered workers and available capacity.
- `minik8s get deployments` — list all deployments and status.
- `minik8s get containers [-d <deploymentId>]` — list running container replicas.
- `minik8s delete <name>` — delete a deployment from the cluster.

---

## Requirements

- **Java 21+** (Java 25 compatible)
- **Maven 3.9+**
- **Docker Engine / Docker Desktop** (running on your host machine for real container execution)

---

## Build and Run

### 1. Build the Entire Project
```cmd
mvn clean test
mvn package -DskipTests
```

### 2. Start the Master Node (Terminal 1)
```cmd
start-master.bat
# Or manually: mvn -pl master spring-boot:run
```
Master REST listens on `http://localhost:8080`.

### 3. Start the Worker Node (Terminal 2)
Ensure **Docker Desktop is running**, then:
```cmd
start-worker.bat
# Or manually: mvn -pl worker spring-boot:run
```
Worker gRPC listens on port `9091` and registers with Master at `http://localhost:8080`.

### 4. Use the CLI (Terminal 3)
```cmd
# Check registered workers
minik8s get workers

# Deploy a web service (e.g. Nginx with 2 replicas)
minik8s deploy nginx --image nginx:alpine --replicas 2 --port 80

# List deployments
minik8s get deployments

# Inspect running containers
minik8s get containers

# Delete the deployment
minik8s delete nginx
```

---

## Remaining Work & Future Roadmap

- **Container Port Forwarding**: Bind host ports dynamically so containers can be reached directly via `http://localhost:<hostPort>`.
- **YAML Manifest Support**: Implement `minik8s apply -f <manifest.yaml>` to parse Kubernetes-like YAML manifests.
- **State Persistence**: Store Master state in SQLite/H2 or JSON files to survive Master restarts.
- **Rolling Updates**: Gracefully replace older containers when deployment image is updated.
- **Authentication & mTLS**: Secure gRPC transport with mutual TLS for non-local multi-machine deployments.
