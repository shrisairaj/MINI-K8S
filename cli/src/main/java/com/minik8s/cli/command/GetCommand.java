package com.minik8s.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

/**
 * Subcommand group: {@code minik8s get}.
 *
 * <p>Acts as a namespace for resource-listing commands. Usage:</p>
 * <pre>
 *   minik8s get workers
 *   minik8s get deployments
 *   minik8s get containers
 * </pre>
 */
@Command(
        name = "get",
        description = "Display one or more resources from the cluster.",
        subcommands = {
                GetWorkersCommand.class,
                GetDeploymentsCommand.class,
                GetContainersCommand.class
        },
        mixinStandardHelpOptions = true,
        sortOptions = false,
        usageHelpWidth = 80,
        commandListHeading = "%nAvailable resources:%n"
)
public class GetCommand implements Runnable {

    @ParentCommand
    MiniK8sCommand parent;

    /**
     * Invoked when {@code minik8s get} is called without a resource type.
     */
    @Override
    public void run() {
        System.out.println("Specify a resource type. Run 'minik8s get --help' for available options.");
    }
}
