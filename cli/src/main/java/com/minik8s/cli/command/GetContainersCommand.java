package com.minik8s.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Command: {@code minik8s get containers}
 *
 * <h2>Status: NOT AVAILABLE YET</h2>
 *
 * <p>This command requires a Master REST endpoint that does not yet exist.</p>
 *
 * <h2>Required Master API (CLI integration dependency)</h2>
 * <pre>
 *   GET /api/deployments/{deploymentId}/replicas
 *   — or —
 *   GET /api/replicas   (cluster-wide listing)
 *
 *   Expected response: JSON array of replica objects, each containing at minimum:
 *     - replicaId    (String)
 *     - deploymentId (String)
 *     - workerId     (String)
 *     - image        (String)
 *     - status       (String: PENDING, STARTING, RUNNING, STOPPING, STOPPED, FAILED)
 *
 *   The Replica model and ReplicaManager exist internally but there is no REST
 *   controller exposing replicas to external callers yet.
 * </pre>
 *
 * <p>Once Member 1 implements the replica/container REST API, update:
 * <ul>
 *   <li>{@code MasterApiClient} — add {@code listReplicas()} method</li>
 *   <li>{@code com.minik8s.cli.model} — add {@code ReplicaResponse} model</li>
 *   <li>This command — replace stub body with real implementation</li>
 * </ul>
 * </p>
 */
@Command(
        name = "containers",
        description = {
                "List all running containers (replicas) in the cluster.",
                "",
                "[NOT AVAILABLE YET] Requires Master replica listing API.",
                "This command will be connected once Member 1 implements the endpoint."
        },
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class GetContainersCommand implements Runnable {

    @ParentCommand
    private GetCommand getCommand;

    @Override
    public void run() {
        System.out.println();
        System.out.println("NOT AVAILABLE YET");
        System.out.println("Reason: Master container/replica listing API has not been");
        System.out.println("        implemented by Member 1 yet.");
        System.out.println();
        System.out.println("CLI integration dependency:");
        System.out.println("  Required API: GET /api/deployments/{id}/replicas");
        System.out.println("             or GET /api/replicas (cluster-wide)");
        System.out.println("  Expected response: JSON array with replicaId, deploymentId,");
        System.out.println("                     workerId, image, status");
        System.out.println();
    }
}
