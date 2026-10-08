package com.minik8s.cli.command;

import com.minik8s.cli.config.CliConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Root Picocli command: {@code minik8s}.
 *
 * <p>This command is the entry point for all CLI operations. It carries the
 * global {@code --master-url} option that is inherited by every subcommand
 * via Picocli's {@code @ParentCommand} mechanism.</p>
 *
 * <h2>Usage</h2>
 * <pre>
 *   minik8s --help
 *   minik8s --version
 *   minik8s get workers
 *   minik8s --master-url http://192.168.1.100:8080 get workers
 * </pre>
 */
@Command(
        name = "minik8s",
        version = "minik8s CLI 0.1.0-SNAPSHOT",
        description = {
                "Mini Kubernetes CLI — manage your Mini-K8s cluster from the terminal.",
                "",
                "Connect to the Master at http://localhost:8080 by default, or override",
                "with --master-url or the MINIK8S_MASTER_URL environment variable."
        },
        subcommands = {
                GetCommand.class,
                DeployCommand.class,
                DeleteCommand.class,
                CommandLine.HelpCommand.class
        },
        mixinStandardHelpOptions = true,   // adds --help and --version
        sortOptions = false,
        usageHelpWidth = 80,
        headerHeading = "%n",
        synopsisHeading = "Usage: ",
        descriptionHeading = "%nDescription:%n  ",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        commandListHeading = "%nCommands:%n"
)
public class MiniK8sCommand implements Runnable {

    /**
     * Global option that every subcommand can read via {@code @ParentCommand}.
     *
     * <p>Precedence: {@code --master-url} &gt; {@code MINIK8S_MASTER_URL} env &gt; default.</p>
     */
    @Option(
            names = {"--master-url"},
            description = {
                    "URL of the Mini-K8s Master REST API.",
                    "Default: ${MINIK8S_MASTER_URL:-http://localhost:8080}"
            },
            defaultValue = Option.NULL_VALUE,
            scope = CommandLine.ScopeType.INHERIT   // propagate to all subcommands
    )
    String masterUrl;

    /**
     * Returns the effective Master URL after applying the three-level precedence.
     * Subcommands call this via their {@code @ParentCommand} reference.
     */
    public String effectiveMasterUrl() {
        return CliConfig.resolveMasterUrl(masterUrl);
    }

    /**
     * Invoked when {@code minik8s} is called with no subcommand.
     * Print usage rather than silently doing nothing.
     */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
