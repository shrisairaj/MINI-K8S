package com.minik8s.cli;

import com.minik8s.cli.exception.MasterApiException;
import com.minik8s.cli.exception.MasterUnavailableException;
import picocli.CommandLine;

/**
 * Top-level Picocli exception handler.
 *
 * <p>Converts well-known CLI exceptions into user-friendly error messages
 * without printing Java stack traces. Unknown runtime exceptions are still
 * surfaced so they are not silently swallowed during debugging.</p>
 */
public class CliExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex,
                                        CommandLine commandLine,
                                        CommandLine.ParseResult parseResult) {
        if (ex instanceof MasterUnavailableException e) {
            System.err.println("ERROR: " + e.getMessage());
        } else if (ex instanceof MasterApiException e) {
            System.err.println("ERROR: " + e.getMessage());
        } else {
            // Unexpected error — print a short message. Stack trace is suppressed
            // for a clean user experience; rethrow if you need the full trace.
            System.err.println("ERROR: Unexpected error — " + ex.getMessage());
        }
        return 1;
    }
}
