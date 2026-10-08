package com.minik8s.cli;

import picocli.CommandLine;
import com.minik8s.cli.command.MiniK8sCommand;

/**
 * Entry point for the Mini-K8s CLI.
 *
 * <p>Delegates entirely to Picocli. The exit code returned by Picocli is
 * forwarded to the JVM so that shell scripts and CI pipelines can detect
 * failures correctly.</p>
 *
 * <p>Usage:
 * <pre>
 *   java -jar minik8s-cli-0.1.0-SNAPSHOT.jar --help
 *   java -jar minik8s-cli-0.1.0-SNAPSHOT.jar get workers
 * </pre>
 * </p>
 */
public class CliApplication {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MiniK8sCommand())
                .setExecutionExceptionHandler(new CliExceptionHandler())
                .execute(args);
        System.exit(exitCode);
    }
}
