package com.minik8s.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the root {@code minik8s} command: help, version, and no-args behaviour.
 */
class MiniK8sCommandTest {

    private CommandLine cli() {
        return new CommandLine(new MiniK8sCommand());
    }

    @Test
    void helpExitsZero() {
        int code = cli().execute("--help");
        assertEquals(0, code, "--help should exit with code 0");
    }

    @Test
    void versionExitsZero() {
        int code = cli().execute("--version");
        assertEquals(0, code, "--version should exit with code 0");
    }

    @Test
    void versionOutputContainsVersionString() {
        StringWriter out = new StringWriter();
        CommandLine cmd = cli();
        cmd.setOut(new PrintWriter(out));
        cmd.execute("--version");
        assertTrue(out.toString().contains("0.1.0-SNAPSHOT"),
                "Version output should contain '0.1.0-SNAPSHOT'");
    }

    @Test
    void noArgsExitsZero() {
        // Root command prints usage when called with no subcommand
        int code = cli().execute();
        assertEquals(0, code, "No-args invocation should exit with code 0");
    }

    @Test
    void getHelpExitsZero() {
        int code = cli().execute("get", "--help");
        assertEquals(0, code, "'get --help' should exit with code 0");
    }

    @Test
    void getWorkersHelpExitsZero() {
        int code = cli().execute("get", "workers", "--help");
        assertEquals(0, code, "'get workers --help' should exit with code 0");
    }

    @Test
    void deployHelpExitsZero() {
        int code = cli().execute("deploy", "--help");
        assertEquals(0, code, "'deploy --help' should exit with code 0");
    }

    @Test
    void deleteHelpExitsZero() {
        int code = cli().execute("delete", "--help");
        assertEquals(0, code, "'delete --help' should exit with code 0");
    }

    @Test
    void helpOutputContainsMasterUrlOption() {
        StringWriter out = new StringWriter();
        CommandLine cmd = cli();
        cmd.setOut(new PrintWriter(out));
        cmd.execute("--help");
        assertTrue(out.toString().contains("--master-url"),
                "Root help should document --master-url option");
    }

    @Test
    void helpOutputContainsGetSubcommand() {
        StringWriter out = new StringWriter();
        CommandLine cmd = cli();
        cmd.setOut(new PrintWriter(out));
        cmd.execute("--help");
        assertTrue(out.toString().contains("get"),
                "Root help should list 'get' subcommand");
    }

    @Test
    void helpOutputContainsDeploySubcommand() {
        StringWriter out = new StringWriter();
        CommandLine cmd = cli();
        cmd.setOut(new PrintWriter(out));
        cmd.execute("--help");
        assertTrue(out.toString().contains("deploy"),
                "Root help should list 'deploy' subcommand");
    }

    @Test
    void unknownSubcommandExitsNonZero() {
        int code = cli().execute("nonexistent-command");
        assertNotEquals(0, code, "Unknown subcommand should exit with non-zero code");
    }
}
