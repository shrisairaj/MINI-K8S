package com.minik8s.cli.command;

import com.minik8s.cli.client.MasterApiClient;
import com.minik8s.cli.model.CreateDeploymentRequest;
import com.minik8s.cli.model.DeploymentResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "deploy",
        description = "Deploy a workload to the Mini-K8s cluster.",
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
        String masterUrl = parent.effectiveMasterUrl();
        MasterApiClient client = new MasterApiClient(masterUrl);

        CreateDeploymentRequest request = new CreateDeploymentRequest(name, name, image, replicas, port);
        DeploymentResponse response = client.createDeployment(request);

        System.out.println();
        System.out.printf("Deployment '%s' successfully submitted.%n", response.getName());
        System.out.printf("  Image:            %s%n", response.getImage());
        System.out.printf("  Desired Replicas: %d%n", response.getDesiredReplicas());
        System.out.printf("  Port:             %d%n", response.getContainerPort());
        System.out.printf("  Status:           %s%n", response.getStatus());
        System.out.println();
    }
}
