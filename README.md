# Mini-K8s

Mini-K8s is an educational container-orchestration project. This repository currently implements the Member 1 Master control-plane scope: Worker Registry, resource-aware scheduling, and replica management. Worker execution and Docker integration remain on the Worker side.

## Requirements

- Java 21
- Maven 3.9+

## Build and test

From the repository root, run `mvn test`.

Run the Master application with `mvn -pl master spring-boot:run`. The Worker Registry REST API listens on port 8080 by default.

## Ownership and integration

- `WorkerRegistry` is the Master-side source of Worker lifecycle, heartbeat, resource, and reservation state.
- `Scheduler` and `SchedulingStrategy` decide placement; they do not start containers.
- `ReplicaManager` tracks desired and actual replicas and reconciles them through `WorkerClient`.
- `WorkerClient` is an integration interface. The gRPC adapter and protocol are intentionally not implemented here; Member 2 should implement the adapter against the agreed proto.
- Deployment Management should call `ReplicaManager.updateDesiredState(...)` and `reconcile(...)`. A scheduled adapter invokes reconciliation periodically.
- Health Monitoring should update Worker state through `WorkerRegistry`; each reconciliation detects `UNAVAILABLE`/`REMOVED` assigned Workers and marks their replicas failed. A monitor can also call `handleWorkerUnavailable(...)` for immediate event-driven handling and report individual replica state through `reportReplicaStatus(...)`.
- The first version reconciles replica count, not rolling updates: changing an image or resource template affects newly created replicas; replacing existing replicas for template changes belongs in a later rollout feature.

Replica operations are serialized per deployment to make scaling and reconciliation idempotent. Worker RPCs should have deadlines and idempotent semantics keyed by replica ID. A definite start rejection must use `WorkerOperationRejectedException`; transport failures with uncertain outcomes leave the replica `STARTING` until Worker status resolves them, avoiding duplicate starts. Stop operations are retried idempotently while `STOPPING`. The initial state store is in-memory and is lost when the Master restarts.

## Worker Registry API

- `POST /api/workers/register` — register or re-register a Worker. A matching registration is idempotent; a heartbeat moves it to `READY`.
- `POST /api/workers/{workerId}/heartbeat` — refresh heartbeat and reported available resources; marks the Worker `READY`.
- `GET /api/workers` — list known Workers.
- `GET /api/workers/{workerId}` — look up one Worker.
- `DELETE /api/workers/{workerId}` — mark a Worker `REMOVED`.

Registration includes Worker ID, host, gRPC port, total CPU in millicores, total memory in bytes, and currently available CPU/memory in the same units. Scheduler capacity is conservatively bounded by both reported free resources and resources not already reserved by the Master.
