package com.minik8s.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Command: {@code minik8s delete <name>}
 *
 * <h2>Status: NOT AVAILABLE YET</h2>
 *
 * <p>This command requires a Master REST endpoint that does not yet exist.</p>
 *
 * <h2>Required Master API (CLI integration dependency)</h2>
 * <pre>
 *   DELETE /api/deployments/{deploymentId}
 *
 *   Expected behaviour:
 *     - Scales desiredReplicas to 0 and removes the deployment from the cluster.
 *     - Returns 204 No Content on success, or a ProblemDetail body on error.
 *
 *   Internally, Member 1's ReplicaManager.updateDesiredState() could be
 *   called with desiredReplicas=0, but there is no REST endpoint yet.
 * </pre>
 *
 * <p>Once Member 1 implements the delete endpoint, update:
 * <ul>
 *   <li>{@code MasterApiClient} — add {@code deleteDeployment(String deploymentId)} method</li>
 *   <li>This command — replace stub body with real implementation</li>
 * </ul>
 * </p>
 */
@Command(
        name = "delete",
        description = {
                "Delete a deployment from the Mini-K8s cluster.",
                "",
                "[NOT AVAILABLE YET] Requires Master deployment deletion API",
                "(DELETE /api/deployments/{name}). This command will be connected",
                "once Member 1 implements the endpoint."
        },
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class DeleteCommand implements Runnable {

    @ParentCommand
    private MiniK8sCommand parent;

    /**
     * The deployment name / ID. Captured now so {@code --help} shows the
     * argument, but not used until the Master API is available.
     */
    @Parameters(
            index = "0",
            paramLabel = "<name>",
            description = "Name / ID of the deployment to delete.",
            arity = "0..1"
    )
    private String name;

    @Override
    public void run() {
        System.out.println();
        System.out.println("NOT AVAILABLE YET");
        System.out.println("Reason: Master deployment deletion API (DELETE /api/deployments/{id})");
        System.out.println("        has not been implemented by Member 1 yet.");
        System.out.println();
        System.out.println("CLI integration dependency:");
        System.out.println("  Required API: DELETE /api/deployments/{deploymentId}");
        System.out.println("  Expected response: 204 No Content on success");
        System.out.println();
    }
}
