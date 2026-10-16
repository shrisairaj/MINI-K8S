package com.minik8s.cli.command;

import com.minik8s.cli.client.MasterApiClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "delete",
        description = "Delete a deployment from the Mini-K8s cluster.",
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class DeleteCommand implements Runnable {

    @ParentCommand
    private MiniK8sCommand parent;

    @Parameters(
            index = "0",
            paramLabel = "<name>",
            description = "Name / ID of the deployment to delete."
    )
    private String name;

    @Override
    public void run() {
        if (name == null || name.isBlank()) {
            System.err.println("Error: Deployment name must be specified.");
            return;
        }

        String masterUrl = parent.effectiveMasterUrl();
        MasterApiClient client = new MasterApiClient(masterUrl);

        client.deleteDeployment(name);
        System.out.println();
        System.out.printf("Deployment '%s' successfully deleted.%n", name);
        System.out.println();
    }
}
