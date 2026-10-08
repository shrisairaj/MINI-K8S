package com.minik8s.cli.config;

/**
 * Resolves the Master base URL using a three-level precedence:
 *
 * <ol>
 *   <li>Explicit value passed via the {@code --master-url} CLI option.</li>
 *   <li>The {@code MINIK8S_MASTER_URL} environment variable.</li>
 *   <li>The built-in default: {@value #DEFAULT_MASTER_URL}.</li>
 * </ol>
 *
 * <p>Usage in a Picocli command:</p>
 * <pre>
 *   String url = CliConfig.resolveMasterUrl(masterUrlOption);
 *   MasterApiClient client = new MasterApiClient(url);
 * </pre>
 */
public final class CliConfig {

    /** Default Master URL used when no explicit configuration is found. */
    public static final String DEFAULT_MASTER_URL = "http://localhost:8080";

    /** Environment variable name for overriding the Master URL. */
    public static final String ENV_VAR_MASTER_URL = "MINIK8S_MASTER_URL";

    // Prevent instantiation — this is a utility class.
    private CliConfig() {}

    /**
     * Resolves the effective Master URL.
     *
     * @param explicitUrl the value provided via {@code --master-url}, or
     *                    {@code null} / blank if the option was not supplied.
     * @return the resolved URL, never null.
     */
    public static String resolveMasterUrl(String explicitUrl) {
        // 1. Explicit CLI option takes highest priority
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl.trim();
        }

        // 2. Environment variable
        String envUrl = System.getenv(ENV_VAR_MASTER_URL);
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl.trim();
        }

        // 3. Built-in default
        return DEFAULT_MASTER_URL;
    }
}
