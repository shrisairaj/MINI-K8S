package com.minik8s.cli.command;

import com.minik8s.cli.client.MasterApiClient;
import com.minik8s.cli.model.WorkerResponse;
import com.minik8s.cli.model.WorkerResourcesResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Command: {@code minik8s get workers}
 *
 * <p>Lists all workers registered with the Master using the real
 * {@code GET /api/workers} endpoint.</p>
 *
 * <h2>Status: IMPLEMENTED</h2>
 * <p>Backed by the existing Master Worker Registry API.</p>
 *
 * <h2>Example output</h2>
 * <pre>
 * WORKERS
 * -----------------------------------------------------------------------
 * ID              STATUS      CPU (avail/total)   MEMORY (avail/total)
 * worker-1        READY       800m / 1000m        512 / 1024 MiB
 * worker-2        UNAVAILABLE 0m / 1000m          0 / 1024 MiB
 * -----------------------------------------------------------------------
 * Total: 2 worker(s)
 * </pre>
 */
@Command(
        name = "workers",
        description = {
                "List all workers registered with the Mini-K8s Master.",
                "",
                "Displays worker ID, status, and available resources."
        },
        mixinStandardHelpOptions = true,
        usageHelpWidth = 80
)
public class GetWorkersCommand implements Runnable {

    // Column widths for aligned table output
    private static final int COL_ID      = 20;
    private static final int COL_STATUS  = 14;
    private static final int COL_CPU     = 22;
    private static final int COL_MEM     = 26;
    private static final String SEPARATOR =
            "-".repeat(COL_ID + COL_STATUS + COL_CPU + COL_MEM + 3);

    private static final DateTimeFormatter HEARTBEAT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    @ParentCommand
    private GetCommand getCommand;

    @Override
    public void run() {
        String masterUrl = getCommand.parent.effectiveMasterUrl();
        MasterApiClient client = new MasterApiClient(masterUrl);
        List<WorkerResponse> workers = client.listWorkers();
        printWorkers(workers);
    }

    // -----------------------------------------------------------------------
    // Output formatting
    // -----------------------------------------------------------------------

    static void printWorkers(List<WorkerResponse> workers) {
        System.out.println();
        System.out.println("WORKERS");
        System.out.println(SEPARATOR);
        System.out.printf("%-" + COL_ID + "s %-" + COL_STATUS + "s %-" + COL_CPU + "s %-" + COL_MEM + "s%n",
                "ID", "STATUS", "CPU (avail/total)", "MEMORY (avail/total)");
        System.out.println(SEPARATOR);

        if (workers.isEmpty()) {
            System.out.println("  (no workers registered)");
        } else {
            for (WorkerResponse w : workers) {
                String cpuCol  = formatCpu(w.getResources());
                String memCol  = formatMemory(w.getResources());
                System.out.printf("%-" + COL_ID + "s %-" + COL_STATUS + "s %-" + COL_CPU + "s %-" + COL_MEM + "s%n",
                        truncate(w.getWorkerId(), COL_ID - 1),
                        safeStatus(w.getStatus()),
                        cpuCol,
                        memCol);
            }
        }

        System.out.println(SEPARATOR);
        System.out.println("Total: " + workers.size() + " worker(s)");
        System.out.println();
    }

    private static String formatCpu(WorkerResourcesResponse r) {
        if (r == null) return "N/A";
        return r.getAvailableCpuMillis() + "m / " + r.getTotalCpuMillis() + "m";
    }

    private static String formatMemory(WorkerResourcesResponse r) {
        if (r == null) return "N/A";
        long availMib = r.getAvailableMemoryBytes() / (1024L * 1024L);
        long totalMib = r.getTotalMemoryBytes() / (1024L * 1024L);
        return availMib + " / " + totalMib + " MiB";
    }

    private static String safeStatus(String status) {
        return (status != null) ? status : "UNKNOWN";
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() > maxLen ? value.substring(0, maxLen - 1) + "…" : value;
    }
}
