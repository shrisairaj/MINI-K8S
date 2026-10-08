package com.minik8s.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Command: {@code minik8s deploy <name>}
 *
 * <h2>Status: NOT AVAILABLE YET</h2>
 *
 * <p>This command requires a Master REST endpoint that does not yet exist.</p>
 *
 * <h2>Required Master API (CLI integration dependency)</h2>
 * <pre>
 *   POST /api/deployments
 *
 *   Expected request body (JSON) based on Master's new Deployment model:
 *     {
 *       "id":            "nginx",
 *       "name":          "nginx",
 *       "image":         "nginx:latest",
 *       "replicas":      2,
 *       "containerPort": 80
 *     }
 *
 *   Expected response: deployment object with status.
 * </pre>
 *
 * <p>Once Member 1 implements the deployment REST API, update:
 * <ul>
 *   <li>{@code MasterApiClient} — add {@code createDeployment(DeploymentRequest)} method</li>
 *   <li>This command — remove stub body and call API</li>
 * </ul>
 * </p>
 */
@Command(
        name = "deploy",
        description = {
                "Deploy a workload to the Mini-K8s cluster.",
                "",
                "[NOT AVAILABLE YET] Requires Master deployment API (POST /api/deployments).",
                "This command is fully prepared to accept arguments, but execution will",
                "be blocked until Member 1 implements the endpoint."
        },
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class DeployCommand implements Runnable {

    @ParentCommand
    private MiniK8sCommand parent;

    @Parameters(
            index = "0",
            paramLabel = "<name>",
            description = "Name / ID of the deployment."
    )
    private String name;

    @Option(names = {"-i", "--image"}, required = true, description = "Container image to deploy")
    private String image;

    @Option(names = {"-r", "--replicas"}, defaultValue = "1", description = "Number of replicas (default: ${DEFAULT-VALUE})")
    private int replicas;

    @Option(names = {"-p", "--port"}, defaultValue = "80", description = "Container port (default: ${DEFAULT-VALUE})")
    private int port;

    @Override
    public void run() {
        System.out.println();
        System.out.println("NOT AVAILABLE YET");
        System.out.println("Reason: Master deployment API (POST /api/deployments) has not been");
        System.out.println("        implemented by Member 1 yet.");
        System.out.println();
        System.out.println("CLI integration dependency:");
        System.out.println("  Required API: POST /api/deployments");
        System.out.println("  Prepared Request Body based on arguments:");
        System.out.printf("    { \"id\": \"%s\", \"name\": \"%s\", \"image\": \"%s\",%n", name, name, image);
        System.out.printf("      \"replicas\": %d, \"containerPort\": %d }%n", replicas, port);
        System.out.println();
    }
}
