package com.minik8s.cli.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minik8s.cli.exception.MasterApiException;
import com.minik8s.cli.exception.MasterUnavailableException;
import com.minik8s.cli.exception.ProblemDetail;
import com.minik8s.cli.model.WorkerResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Centralized HTTP client for communicating with the Mini-K8s Master REST API.
 *
 * <p><b>All knowledge of Master endpoint paths, request formats, and response
 * parsing lives here.</b> Commands must NOT construct raw HTTP requests
 * themselves. If Member 1 changes an endpoint path or a response DTO, update
 * only this class.</p>
 *
 * <h2>Currently Implemented Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/workers}        — list all registered workers</li>
 *   <li>{@code GET /api/workers/{id}}   — get a single worker by ID</li>
 * </ul>
 *
 * <h2>Pending Endpoints (not yet implemented by Member 1)</h2>
 * <ul>
 *   <li>{@code POST /api/deployments}              — create deployment</li>
 *   <li>{@code GET  /api/deployments}              — list deployments</li>
 *   <li>{@code GET  /api/deployments/{id}}         — get deployment</li>
 *   <li>{@code DELETE /api/deployments/{id}}       — delete deployment</li>
 *   <li>{@code GET  /api/deployments/{id}/replicas} — list replicas/containers</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>Connection failures → {@link MasterUnavailableException}</li>
 *   <li>HTTP 4xx/5xx → {@link MasterApiException} with parsed ProblemDetail if available</li>
 * </ul>
 */
public class MasterApiClient {

    // -----------------------------------------------------------------------
    // Constants — all Master endpoint paths are defined here so changes to
    // Member 1's routing only require updates in this one place.
    // -----------------------------------------------------------------------

    /** Base path for the Worker Registry API. */
    private static final String PATH_WORKERS = "/api/workers";

    /** Request timeout for all HTTP calls. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a client pointing at the given Master base URL.
     *
     * @param baseUrl e.g. {@code "http://localhost:8080"}
     */
    public MasterApiClient(String baseUrl) {
        this(baseUrl, buildDefaultHttpClient(), buildObjectMapper());
    }

    /**
     * Package-private constructor for unit testing with a mock {@link HttpClient}.
     */
    MasterApiClient(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------------
    // Public API — Worker Registry (real endpoints)
    // -----------------------------------------------------------------------

    /**
     * Lists all workers registered with the Master.
     *
     * <p>Maps to: {@code GET /api/workers}</p>
     *
     * @return list of workers; empty list if none are registered.
     * @throws MasterUnavailableException if the Master cannot be reached.
     * @throws MasterApiException         if the Master returns an error response.
     */
    public List<WorkerResponse> listWorkers() {
        HttpRequest request = buildGetRequest(PATH_WORKERS);
        HttpResponse<String> response = sendRequest(request);
        checkSuccess(response);
        return parseBody(response.body(), new TypeReference<List<WorkerResponse>>() {});
    }

    /**
     * Gets a single worker by ID.
     *
     * <p>Maps to: {@code GET /api/workers/{workerId}}</p>
     *
     * @param workerId the worker identifier.
     * @return the worker.
     * @throws MasterUnavailableException if the Master cannot be reached.
     * @throws MasterApiException         if the Master returns an error response (including 404).
     */
    public WorkerResponse getWorker(String workerId) {
        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("workerId must not be blank");
        }
        HttpRequest request = buildGetRequest(PATH_WORKERS + "/" + workerId);
        HttpResponse<String> response = sendRequest(request);
        checkSuccess(response);
        return parseBody(response.body(), new TypeReference<WorkerResponse>() {});
    }

    // -----------------------------------------------------------------------
    // Future API stubs (placeholders for Member 1 integration)
    // -----------------------------------------------------------------------
    //
    // When Member 1 implements deployment endpoints, add methods here:
    //
    //   public DeploymentResponse createDeployment(CreateDeploymentRequest req) { ... }
    //   public List<DeploymentResponse> listDeployments() { ... }
    //   public void deleteDeployment(String deploymentId) { ... }
    //   public List<ReplicaResponse> listReplicas(String deploymentId) { ... }
    //
    // Keep all path constants and request/response models inside this file
    // and the model package.

    // -----------------------------------------------------------------------
    // Internal HTTP helpers
    // -----------------------------------------------------------------------

    private HttpRequest buildGetRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .header("Accept", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .build();
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException e) {
            throw new MasterUnavailableException(baseUrl, e);
        } catch (IOException e) {
            // Covers other IO issues (timeout, host unreachable, etc.)
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Connection refused")
                    || msg.contains("timed out")
                    || msg.contains("No route to host"))) {
                throw new MasterUnavailableException(baseUrl, e);
            }
            throw new MasterApiException("Network error communicating with Master: " + msg, -1, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MasterApiException("Request to Master was interrupted.", -1, e);
        }
    }

    /**
     * Checks the HTTP status code and throws a descriptive {@link MasterApiException}
     * on error. Attempts to parse a ProblemDetail body for a better message.
     */
    private void checkSuccess(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return; // success
        }

        String detail = tryParseProblemDetail(response.body());

        String message = switch (status) {
            case 400 -> "Invalid request: " + detail;
            case 404 -> "Resource not found: " + detail;
            case 409 -> "Conflict: " + detail;
            case 503 -> "Master currently unavailable: " + detail;
            default  -> "Unexpected response from Master: HTTP " + status
                        + (detail.isEmpty() ? "" : " — " + detail);
        };

        throw new MasterApiException(message, status);
    }

    /**
     * Attempts to parse the response body as a ProblemDetail and returns
     * the most useful description. Returns an empty string on failure so
     * callers never get a null or exception here.
     */
    private String tryParseProblemDetail(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            ProblemDetail pd = objectMapper.readValue(body, ProblemDetail.class);
            String d = pd.describe();
            return d != null ? d : "";
        } catch (Exception ignored) {
            // Body is not JSON ProblemDetail — return raw body truncated
            return body.length() > 200 ? body.substring(0, 200) + "..." : body;
        }
    }

    private <T> T parseBody(String body, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(body, typeRef);
        } catch (IOException e) {
            throw new MasterApiException("Could not parse Master response: " + e.getMessage(), -1, e);
        }
    }

    // -----------------------------------------------------------------------
    // Factory helpers
    // -----------------------------------------------------------------------

    private static HttpClient buildDefaultHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Required for java.time.Instant serialization/deserialization
        mapper.registerModule(new JavaTimeModule());
        // Do not write dates as timestamps (ISO-8601 string is cleaner)
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Master base URL must not be blank");
        }
        // Strip trailing slash so that paths can be appended directly
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
