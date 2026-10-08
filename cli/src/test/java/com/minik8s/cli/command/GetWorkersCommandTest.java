package com.minik8s.cli.command;

import com.minik8s.cli.model.WorkerResourcesResponse;
import com.minik8s.cli.model.WorkerResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GetWorkersCommand} output formatting.
 *
 * <p>These tests call the static {@code printWorkers} method directly,
 * so no HTTP connection is needed.</p>
 */
class GetWorkersCommandTest {

    @Test
    void emptyList_printsNoWorkersMessage() {
        String output = captureOutput(() ->
                GetWorkersCommand.printWorkers(Collections.emptyList()));
        assertTrue(output.contains("no workers registered"),
                "Empty list should print 'no workers registered' message");
        assertTrue(output.contains("Total: 0 worker(s)"),
                "Should show total count of 0");
    }

    @Test
    void singleWorker_printsId() {
        WorkerResponse w = makeWorker("worker-1", "READY", 1000, 1073741824, 800, 536870912);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w)));
        assertTrue(output.contains("worker-1"),
                "Output should contain worker ID");
    }

    @Test
    void singleWorker_printsStatus() {
        WorkerResponse w = makeWorker("worker-1", "READY", 1000, 1073741824, 800, 536870912);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w)));
        assertTrue(output.contains("READY"),
                "Output should contain worker status");
    }

    @Test
    void singleWorker_printsCpuInfo() {
        WorkerResponse w = makeWorker("worker-1", "READY", 1000, 1073741824, 800, 536870912);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w)));
        // Available CPU: 800m, Total CPU: 1000m
        assertTrue(output.contains("800m"),
                "Output should contain available CPU millis");
        assertTrue(output.contains("1000m"),
                "Output should contain total CPU millis");
    }

    @Test
    void singleWorker_printsMemoryInfo() {
        // 512 MiB available, 1024 MiB total
        long totalMem = 1024L * 1024L * 1024L;
        long availMem = 512L * 1024L * 1024L;
        WorkerResponse w = makeWorker("worker-1", "READY", 1000, totalMem, 800, availMem);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w)));
        assertTrue(output.contains("512"),
                "Output should contain available memory in MiB");
        assertTrue(output.contains("1024"),
                "Output should contain total memory in MiB");
    }

    @Test
    void multipleWorkers_showsCorrectTotal() {
        WorkerResponse w1 = makeWorker("worker-1", "READY", 1000, 1073741824, 1000, 1073741824);
        WorkerResponse w2 = makeWorker("worker-2", "UNAVAILABLE", 1000, 1073741824, 0, 0);
        WorkerResponse w3 = makeWorker("worker-3", "REGISTERED", 1000, 1073741824, 500, 536870912);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w1, w2, w3)));
        assertTrue(output.contains("Total: 3 worker(s)"),
                "Should show total count of 3: " + output);
    }

    @Test
    void multipleWorkers_allIdsPresent() {
        WorkerResponse w1 = makeWorker("worker-1", "READY", 1000, 1073741824, 1000, 1073741824);
        WorkerResponse w2 = makeWorker("worker-2", "UNAVAILABLE", 1000, 1073741824, 0, 0);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w1, w2)));
        assertTrue(output.contains("worker-1"), "worker-1 should be in output");
        assertTrue(output.contains("worker-2"), "worker-2 should be in output");
    }

    @Test
    void unavailableWorker_showsUnavailableStatus() {
        WorkerResponse w = makeWorker("worker-x", "UNAVAILABLE", 1000, 1073741824, 0, 0);
        String output = captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w)));
        assertTrue(output.contains("UNAVAILABLE"), "UNAVAILABLE status should be shown");
    }

    @Test
    void workerWithNullResources_doesNotThrow() {
        WorkerResponse w = new WorkerResponse();
        w.setWorkerId("worker-null");
        w.setStatus("REGISTERED");
        w.setResources(null);

        assertDoesNotThrow(() ->
                captureOutput(() -> GetWorkersCommand.printWorkers(List.of(w))));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static WorkerResponse makeWorker(String id, String status,
                                              long totalCpu, long totalMem,
                                              long availCpu, long availMem) {
        WorkerResourcesResponse res = new WorkerResourcesResponse();
        res.setTotalCpuMillis(totalCpu);
        res.setTotalMemoryBytes(totalMem);
        res.setAvailableCpuMillis(availCpu);
        res.setAvailableMemoryBytes(availMem);

        WorkerResponse w = new WorkerResponse();
        w.setWorkerId(id);
        w.setStatus(status);
        w.setResources(res);
        return w;
    }

    private static String captureOutput(Runnable action) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return baos.toString();
    }
}
