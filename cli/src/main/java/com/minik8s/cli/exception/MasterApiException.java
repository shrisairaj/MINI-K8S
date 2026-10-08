package com.minik8s.cli.exception;

/**
 * Thrown when the Master REST API returns an unexpected or error HTTP response.
 *
 * <p>The {@code httpStatus} field carries the numeric HTTP status code when
 * one is available. Use {@code -1} to indicate that no HTTP response was
 * received at all (e.g., connection refused).</p>
 */
public class MasterApiException extends RuntimeException {

    private final int httpStatus;

    public MasterApiException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public MasterApiException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * Returns the HTTP status code, or {@code -1} if no HTTP response was received.
     */
    public int getHttpStatus() {
        return httpStatus;
    }
}
