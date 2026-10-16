package com.minik8s.cli.command;

import com.minik8s.cli.client.MasterApiClient;
import com.minik8s.cli.model.DeploymentResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.List;

@Command(
        name = "deployments",
        description = "List all deployments in the cluster.",
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class GetDeploymentsCommand implements Runnable {

    private static final int COL_NAME    = 20;
    private static final int COL_DESIRED = 10;
    private static final int COL_RUNNING = 10;
    private static final int COL_STATUS  = 12;
    private static final int COL_PORT    = 8;
    private static final int COL_IMAGE   = 28;
    private static final String SEPARATOR =
            "-".repeat(COL_NAME + COL_DESIRED + COL_RUNNING + COL_STATUS + COL_PORT + COL_IMAGE + 5);

    @ParentCommand
    private GetCommand getCommand;

    @Override
    public void run() {
        String masterUrl = getCommand.parent.effectiveMasterUrl();
        MasterApiClient client = new MasterApiClient(masterUrl);
        List<DeploymentResponse> deployments = client.listDeployments();
        printDeployments(deployments);
    }

    static void printDeployments(List<DeploymentResponse> deployments) {
        System.out.println();
        System.out.println("DEPLOYMENTS");
        System.out.println(SEPARATOR);
        System.out.printf("%-" + COL_NAME + "s %-" + COL_DESIRED + "s %-" + COL_RUNNING + "s %-" + COL_STATUS + "s %-" + COL_PORT + "s %-" + COL_IMAGE + "s%n",
                "NAME", "DESIRED", "RUNNING", "STATUS", "PORT", "IMAGE");
        System.out.println(SEPARATOR);

        if (deployments == null || deployments.isEmpty()) {
            System.out.println("  (no deployments found)");
        } else {
            for (DeploymentResponse d : deployments) {
                System.out.printf("%-" + COL_NAME + "s %-" + COL_DESIRED + "s %-" + COL_RUNNING + "s %-" + COL_STATUS + "s %-" + COL_PORT + "s %-" + COL_IMAGE + "s%n",
                        truncate(d.getName(), COL_NAME - 1),
                        String.valueOf(d.getDesiredReplicas()),
                        String.valueOf(d.getRunningReplicas()),
                        safe(d.getStatus()),
                        String.valueOf(d.getContainerPort()),
                        truncate(d.getImage(), COL_IMAGE - 1));
            }
        }

        System.out.println(SEPARATOR);
        System.out.println("Total: " + (deployments == null ? 0 : deployments.size()) + " deployment(s)");
        System.out.println();
    }

    private static String safe(String value) {
        return value != null ? value : "UNKNOWN";
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() > maxLen ? value.substring(0, maxLen - 1) + "…" : value;
    }
}
