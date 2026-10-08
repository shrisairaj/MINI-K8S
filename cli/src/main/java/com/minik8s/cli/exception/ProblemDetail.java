package com.minik8s.cli.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal representation of Spring's RFC 9457 ProblemDetail response body.
 *
 * <p>The Master uses {@code ProblemDetail.forStatusAndDetail(status, message)}
 * which always sets at least {@code status} and {@code detail}. The other
 * fields are optional and may be absent in some responses.</p>
 *
 * <p>Jackson fields are snake_case-compatible because Jackson's default
 * naming uses the exact Java field name — which already matches Spring's
 * ProblemDetail serialisation (camelCase for most fields).</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProblemDetail {

    private int status;
    private String title;
    private String detail;
    private String instance;

    // Default constructor required by Jackson
    public ProblemDetail() {}

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    /**
     * Returns the most useful human-readable description available, falling
     * back gracefully if {@code detail} is null.
     */
    public String describe() {
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        if (title != null && !title.isBlank()) {
            return title;
        }
        return "(no detail provided)";
    }
}
