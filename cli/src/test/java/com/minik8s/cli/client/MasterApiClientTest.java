package com.minik8s.cli.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.minik8s.cli.exception.MasterApiException;
import com.minik8s.cli.exception.MasterUnavailableException;
import com.minik8s.cli.model.WorkerResourcesResponse;
import com.minik8s.cli.model.WorkerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MasterApiClient}.
 *
 * <p>Uses Mockito to mock {@link HttpClient} so no real network connection
 * is needed. The tests verify that the client correctly handles all HTTP
 * status codes and network failures documented in the implementation plan.</p>
 */
@ExtendWith(MockitoExtension.class)
class MasterApiClientTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private MasterApiClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = MasterApiClient.buildObjectMapper();
        client = new MasterApiClient("http://localhost:8080", mockHttpClient, objectMapper);
    }

    // -----------------------------------------------------------------------
    // listWorkers — success cases
    // -----------------------------------------------------------------------

    @Test
    void listWorkers_returnsEmptyList() throws Exception {
        stubResponse(200, "[]");
        List<WorkerResponse> result = client.listWorkers();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listWorkers_returnsSingleWorker() throws Exception {
        String json = """
                [
                  {
                    "workerId": "worker-1",
                    "host": "localhost",
                    "grpcPort": 9091,
                    "status": "READY",
                    "resources": {
                      "totalCpuMillis": 1000,
                      "totalMemoryBytes": 1073741824,
                      "availableCpuMillis": 800,
                      "availableMemoryBytes": 536870912
                    },
                    "lastHeartbeat": "2024-01-01T12:00:00Z"
                  }
                ]
                """;
        stubResponse(200, json);

        List<WorkerResponse> result = client.listWorkers();

        assertEquals(1, result.size());
        WorkerResponse w = result.get(0);
        assertEquals("worker-1", w.getWorkerId());
        assertEquals("localhost", w.getHost());
        assertEquals(9091, w.getGrpcPort());
        assertEquals("READY", w.getStatus());
        assertNotNull(w.getResources());
        assertEquals(1000L, w.getResources().getTotalCpuMillis());
        assertEquals(800L, w.getResources().getAvailableCpuMillis());
        assertEquals(Instant.parse("2024-01-01T12:00:00Z"), w.getLastHeartbeat());
    }

    @Test
    void listWorkers_handlesMultipleWorkers() throws Exception {
        String json = """
                [
                  {"workerId":"w1","host":"h1","grpcPort":9091,"status":"READY",
                   "resources":{"totalCpuMillis":1000,"totalMemoryBytes":1073741824,
                   "availableCpuMillis":500,"availableMemoryBytes":536870912}},
                  {"workerId":"w2","host":"h2","grpcPort":9092,"status":"UNAVAILABLE",
                   "resources":{"totalCpuMillis":1000,"totalMemoryBytes":1073741824,
                   "availableCpuMillis":0,"availableMemoryBytes":0}}
                ]
                """;
        stubResponse(200, json);

        List<WorkerResponse> result = client.listWorkers();
        assertEquals(2, result.size());
        assertEquals("w1", result.get(0).getWorkerId());
        assertEquals("w2", result.get(1).getWorkerId());
    }

    @Test
    void listWorkers_ignoresUnknownJsonFields() throws Exception {
        // Master may add fields in future; we must not break
        String json = """
                [{"workerId":"w1","host":"h1","grpcPort":9091,"status":"READY",
                  "resources":{"totalCpuMillis":1000,"totalMemoryBytes":1073741824,
                  "availableCpuMillis":500,"availableMemoryBytes":536870912},
                  "futureField": "some value", "anotherNewField": 42}]
                """;
        stubResponse(200, json);

        assertDoesNotThrow(() -> client.listWorkers());
    }

    // -----------------------------------------------------------------------
    // listWorkers — HTTP error cases
    // -----------------------------------------------------------------------

    @Test
    void listWorkers_http404_throwsMasterApiException() throws Exception {
        stubResponse(404, """
                {"status":404,"title":"Not Found","detail":"Worker not found"}
                """);
        MasterApiException ex = assertThrows(MasterApiException.class, client::listWorkers);
        assertEquals(404, ex.getHttpStatus());
        assertTrue(ex.getMessage().contains("not found") || ex.getMessage().contains("Not Found")
                || ex.getMessage().contains("Worker not found"),
                "404 message should mention 'not found': " + ex.getMessage());
    }

    @Test
    void listWorkers_http400_throwsMasterApiException() throws Exception {
        stubResponse(400, """
                {"status":400,"title":"Bad Request","detail":"Invalid parameter"}
                """);
        MasterApiException ex = assertThrows(MasterApiException.class, client::listWorkers);
        assertEquals(400, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"),
                "400 message should mention invalid: " + ex.getMessage());
    }

    @Test
    void listWorkers_http409_throwsMasterApiException() throws Exception {
        stubResponse(409, """
                {"status":409,"title":"Conflict","detail":"Resource already exists"}
                """);
        MasterApiException ex = assertThrows(MasterApiException.class, client::listWorkers);
        assertEquals(409, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("conflict"),
                "409 message should mention conflict: " + ex.getMessage());
    }

    @Test
    void listWorkers_http503_throwsMasterApiException() throws Exception {
        stubResponse(503, """
                {"status":503,"title":"Service Unavailable","detail":"No workers available"}
                """);
        MasterApiException ex = assertThrows(MasterApiException.class, client::listWorkers);
        assertEquals(503, ex.getHttpStatus());
        assertTrue(ex.getMessage().toLowerCase().contains("unavailable"),
                "503 message should mention unavailable: " + ex.getMessage());
    }

    @Test
    void listWorkers_http500_throwsMasterApiException() throws Exception {
        stubResponse(500, "Internal Server Error");
        MasterApiException ex = assertThrows(MasterApiException.class, client::listWorkers);
        assertEquals(500, ex.getHttpStatus());
    }

    // -----------------------------------------------------------------------
    // Connection failure
    // -----------------------------------------------------------------------

    @Test
    void listWorkers_connectionRefused_throwsMasterUnavailableException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new ConnectException("Connection refused"));

        MasterUnavailableException ex = assertThrows(
                MasterUnavailableException.class, client::listWorkers);
        assertTrue(ex.getMessage().contains("localhost:8080"),
                "Exception should mention the master URL: " + ex.getMessage());
    }

    @Test
    void listWorkers_ioException_throwsMasterUnavailableException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection refused"));

        // IOException with "Connection refused" text maps to MasterUnavailableException
        assertThrows(MasterUnavailableException.class, client::listWorkers);
    }

    // -----------------------------------------------------------------------
    // getWorker
    // -----------------------------------------------------------------------

    @Test
    void getWorker_success() throws Exception {
        String json = """
                {
                  "workerId": "worker-42",
                  "host": "192.168.1.10",
                  "grpcPort": 9091,
                  "status": "READY",
                  "resources": {
                    "totalCpuMillis": 4000,
                    "totalMemoryBytes": 8589934592,
                    "availableCpuMillis": 3000,
                    "availableMemoryBytes": 6442450944
                  }
                }
                """;
        stubResponse(200, json);

        WorkerResponse w = client.getWorker("worker-42");
        assertEquals("worker-42", w.getWorkerId());
        assertEquals("192.168.1.10", w.getHost());
        assertEquals("READY", w.getStatus());
    }

    @Test
    void getWorker_blankId_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> client.getWorker(""));
        assertThrows(IllegalArgumentException.class, () -> client.getWorker("  "));
        assertThrows(IllegalArgumentException.class, () -> client.getWorker(null));
    }

    @Test
    void getWorker_http404_throwsMasterApiException() throws Exception {
        stubResponse(404, "{\"status\":404,\"detail\":\"Worker not found: worker-99\"}");
        MasterApiException ex = assertThrows(MasterApiException.class,
                () -> client.getWorker("worker-99"));
        assertEquals(404, ex.getHttpStatus());
    }

    // -----------------------------------------------------------------------
    // URL normalisation
    // -----------------------------------------------------------------------

    @Test
    void trailingSlashInBaseUrlIsStripped() throws Exception {
        MasterApiClient c = new MasterApiClient("http://localhost:8080/",
                mockHttpClient, objectMapper);
        stubResponse(200, "[]");
        assertDoesNotThrow(c::listWorkers);
    }

    // -----------------------------------------------------------------------
    // ObjectMapper
    // -----------------------------------------------------------------------

    @Test
    void buildObjectMapper_registersJavaTimeModule() {
        ObjectMapper mapper = MasterApiClient.buildObjectMapper();
        // Should parse ISO-8601 Instant without throwing
        assertDoesNotThrow(() -> mapper.readValue("\"2024-01-01T00:00:00Z\"", Instant.class));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubResponse(int status, String body) throws Exception {
        when(mockResponse.statusCode()).thenReturn(status);
        when(mockResponse.body()).thenReturn(body);
        org.mockito.Mockito.doReturn(mockResponse).when(mockHttpClient).send(any(HttpRequest.class), any());
    }
}
