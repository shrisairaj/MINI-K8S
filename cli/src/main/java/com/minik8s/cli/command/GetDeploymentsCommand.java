package com.minik8s.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Command: {@code minik8s get deployments}
 *
 * <h2>Status: NOT AVAILABLE YET</h2>
 *
 * <p>This command requires a Master REST endpoint that does not yet exist.</p>
 *
 * <h2>Required Master API (CLI integration dependency)</h2>
 * <pre>
 *   GET /api/deployments
 *
 *   Expected response: JSON array of deployment objects, each containing at minimum:
 *     - deploymentId  (String)
 *     - image         (String)
 *     - desiredReplicas (int)
 *     - replicas      (array of replica summaries)
 *
 *   The ReplicaManager.getSnapshot(deploymentId) method exists internally
 *   but is not exposed via any REST controller yet.
 * </pre>
 *
 * <p>Once Member 1 implements the deployment REST API, update:
 * <ul>
 *   <li>{@code MasterApiClient} — add {@code listDeployments()} method</li>
 *   <li>{@code com.minik8s.cli.model} — add {@code DeploymentResponse} model</li>
 *   <li>This command — replace stub body with real implementation</li>
 * </ul>
 * </p>
 */
@Command(
        name = "deployments",
        description = {
                "List all deployments in the cluster.",
                "",
                "[NOT AVAILABLE YET] Requires Master deployment API (GET /api/deployments).",
                "This command will be connected once Member 1 implements the endpoint."
        },
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class GetDeploymentsCommand implements Runnable {

    @ParentCommand
    private GetCommand getCommand;

    @Override
    public void run() {
        System.out.println();
        System.out.println("NOT AVAILABLE YET");
        System.out.println("Reason: Master deployment API (GET /api/deployments) has not been");
        System.out.println("        implemented by Member 1 yet.");
        System.out.println();
        System.out.println("CLI integration dependency:");
        System.out.println("  Required API: GET /api/deployments");
        System.out.println("  Expected response: JSON array with deploymentId, image,");
        System.out.println("                     desiredReplicas, replicas[]");
        System.out.println();
    }
}
