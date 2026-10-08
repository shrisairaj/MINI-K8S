# Mini-K8s

Mini-K8s is an educational, Kubernetes-inspired container orchestration platform. A Spring Boot Master acts as the control plane, scheduling workloads to Worker nodes over gRPC. Workers manage containers through Docker Engine. The planned project also includes a CLI, deployment management, health monitoring, and observability.

## Implemented so far

### Master control plane

- In-memory, thread-safe Worker Registry with registration, heartbeat, lifecycle state, resource information, and placement reservations.
- Resource-aware least-loaded scheduling behind a replaceable scheduling strategy.
- Replica Manager for desired/actual replica tracking, scale up/down, reconciliation, Worker failure handling, and replacement.
- Worker REST APIs for registration, heartbeat, lookup, listing, removal, and unavailable status.
- Master gRPC client implementing the existing `WorkerClient` contract.
- Worker status callback that validates the reporting Worker and updates replica state through the existing Replica Manager.

### Shared protocol and Worker node

- Shared Protocol Buffers/gRPC contract in `protocol/src/main/proto/worker.proto`.
- Worker Spring Boot module with a gRPC server for starting, stopping, removing, inspecting, and listing replicas, and reporting Worker status/resources.
- Docker Java SDK integration behind a `ContainerRuntime` abstraction. Replica IDs and Docker labels support idempotent operations and container rediscovery after a Worker restart.
- Worker registration and periodic heartbeat to the Master, plus replica status reporting.
- CPU is reported in millicores and memory in bytes, consistent with the Master models.

### Tests

- `mvn clean test` passes across the protocol, Master, and Worker modules: 41 tests, no failures or errors.
- Tests mock Docker interactions, so they do not require a Docker daemon.

## Remaining work

- Implement Deployment Management, including YAML parsing/validation and REST endpoints that call `ReplicaManager.updateDesiredState(...)`.
- Implement the CLI and connect its deploy/scale/get/delete commands to the Master REST API.
- Implement a dedicated Health Monitor for heartbeat timeouts and Worker health transitions; the Worker currently reports unavailable if Docker resource checks fail.
- Implement monitoring/observability (for example, metrics and dashboards) and connect it to Worker/replica state.
- Add end-to-end testing with Docker Engine running. Docker was unavailable during development, so real container startup has not been smoke-tested.
- Add persistence if state must survive Master restart; Master desired/actual replica state is currently in memory.
- Before non-local deployment: secure gRPC transport and internal Worker status/health endpoints, and review authentication/authorization.
- Add rolling updates and workload-template change handling; initial reconciliation maintains replica count only.

## Requirements

- Java 21
- Maven 3.9+
- Docker Engine for running Workers and starting real containers

## Build and run

From the repository root:

```sh
mvn clean test
mvn install -DskipTests
```

Start the Master and Worker in separate terminals:

```sh
mvn -pl master spring-boot:run
mvn -pl worker spring-boot:run
```

Master REST listens on port 8080. Worker gRPC listens on port 9091 by default and registers with `http://localhost:8080`. Configure the Worker with `WORKER_ID`, `WORKER_HOST`, `WORKER_GRPC_PORT`, and `MASTER_URL`. Additional local Workers need unique IDs and gRPC ports.

## Worker Registry API

- `POST /api/workers/register` — register or re-register a Worker.
- `POST /api/workers/{workerId}/heartbeat` — report heartbeat and available resources; marks a registering/unavailable Worker ready.
- `POST /api/workers/{workerId}/unavailable` — mark a Worker unavailable.
- `GET /api/workers` and `GET /api/workers/{workerId}` — list or retrieve Workers.
- `DELETE /api/workers/{workerId}` — mark a Worker removed.

Replica status reports use `POST /api/internal/workers/{workerId}/replicas/status`.

## Architecture boundaries

- The Master decides placement and desired state; it never calls Docker directly.
- `WorkerClient`/gRPC connects the Master to Worker nodes.
- Workers own Docker container lifecycle and report observed state to the Master.
- Master and Worker metadata is currently in-memory. The local gRPC channel uses plaintext and should be secured before deployment outside a trusted environment.
