package com.minik8s.cli.exception;

/**
 * Thrown when the CLI cannot reach the Master (connection refused, timeout, etc.).
 *
 * <p>Distinct from {@link MasterApiException} so that callers can provide a
 * targeted "Master is not running" message rather than a generic HTTP error.</p>
 */
public class MasterUnavailableException extends RuntimeException {

    public MasterUnavailableException(String masterUrl, Throwable cause) {
        super("Unable to connect to Mini Kubernetes Master at " + masterUrl
                + ". Is the Master running?", cause);
    }
}
