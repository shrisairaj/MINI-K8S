package com.minik8s.cli.command;

import com.minik8s.cli.client.MasterApiClient;
import com.minik8s.cli.model.ReplicaResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.List;

@Command(
        name = "containers",
        description = "List containers (replicas) in the cluster.",
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class GetContainersCommand implements Runnable {

    private static final int COL_ID         = 38;
    private static final int COL_DEPLOYMENT = 18;
    private static final int COL_WORKER     = 14;
    private static final int COL_STATUS     = 12;
    private static final int COL_IMAGE      = 26;
    private static final String SEPARATOR =
            "-".repeat(COL_ID + COL_DEPLOYMENT + COL_WORKER + COL_STATUS + COL_IMAGE + 4);

    @ParentCommand
    private GetCommand getCommand;

    @Option(names = {"-d", "--deployment"}, description = "Filter containers by deployment ID")
    private String deploymentId;

    @Override
    public void run() {
        String masterUrl = getCommand.parent.effectiveMasterUrl();
        MasterApiClient client = new MasterApiClient(masterUrl);

        List<ReplicaResponse> replicas;
        if (deploymentId != null && !deploymentId.isBlank()) {
            replicas = client.listDeploymentReplicas(deploymentId);
        } else {
            replicas = client.listReplicas();
        }

        printContainers(replicas);
    }

    static void printContainers(List<ReplicaResponse> replicas) {
        System.out.println();
        System.out.println("CONTAINERS");
        System.out.println(SEPARATOR);
        System.out.printf("%-" + COL_ID + "s %-" + COL_DEPLOYMENT + "s %-" + COL_WORKER + "s %-" + COL_STATUS + "s %-" + COL_IMAGE + "s%n",
                "REPLICA ID", "DEPLOYMENT", "WORKER", "STATUS", "IMAGE");
        System.out.println(SEPARATOR);

        if (replicas == null || replicas.isEmpty()) {
            System.out.println("  (no containers found)");
        } else {
            for (ReplicaResponse r : replicas) {
                System.out.printf("%-" + COL_ID + "s %-" + COL_DEPLOYMENT + "s %-" + COL_WORKER + "s %-" + COL_STATUS + "s %-" + COL_IMAGE + "s%n",
                        truncate(r.getReplicaId(), COL_ID - 1),
                        truncate(r.getDeploymentId(), COL_DEPLOYMENT - 1),
                        truncate(r.getWorkerId(), COL_WORKER - 1),
                        safe(r.getStatus()),
                        truncate(r.getImage(), COL_IMAGE - 1));
            }
        }

        System.out.println(SEPARATOR);
        System.out.println("Total: " + (replicas == null ? 0 : replicas.size()) + " container(s)");
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
