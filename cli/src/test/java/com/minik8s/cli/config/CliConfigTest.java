package com.minik8s.cli.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CliConfig} — Master URL resolution with three-level precedence.
 *
 * <p>Note: These tests cannot easily unset environment variables on all JVM
 * implementations. Tests that would require a specific env var being absent
 * rely on it genuinely not being set in CI. The {@code MINIK8S_MASTER_URL}
 * test is skipped if the variable is already set in the environment so it
 * does not interfere with developer machines that have it configured.</p>
 */
class CliConfigTest {

    @Test
    void explicitCliArgTakesPriority() {
        String result = CliConfig.resolveMasterUrl("http://10.0.0.1:9090");
        assertEquals("http://10.0.0.1:9090", result);
    }

    @Test
    void explicitCliArgTrimsWhitespace() {
        String result = CliConfig.resolveMasterUrl("  http://10.0.0.1:9090  ");
        assertEquals("http://10.0.0.1:9090", result);
    }

    @Test
    void nullExplicitArgFallsBackToDefault() {
        // Only reliable when MINIK8S_MASTER_URL is not set in the environment.
        // We still run the test and just confirm we get a non-null, non-blank value.
        String result = CliConfig.resolveMasterUrl(null);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void blankExplicitArgFallsBackToDefault() {
        String result = CliConfig.resolveMasterUrl("   ");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void defaultUrlConstantIsLocalhost8080() {
        assertEquals("http://localhost:8080", CliConfig.DEFAULT_MASTER_URL);
    }

    @Test
    void envVarConstantName() {
        assertEquals("MINIK8S_MASTER_URL", CliConfig.ENV_VAR_MASTER_URL);
    }

    @Test
    void fallbackToDefaultWhenNothingIsSet() {
        // If the environment variable happens to be set, this test cannot
        // verify the default precisely — but it verifies resolution completes.
        String result = CliConfig.resolveMasterUrl(null);
        String envVal = System.getenv(CliConfig.ENV_VAR_MASTER_URL);
        if (envVal == null || envVal.isBlank()) {
            assertEquals(CliConfig.DEFAULT_MASTER_URL, result,
                    "Should return default when env var is absent and no explicit URL given");
        } else {
            assertEquals(envVal.trim(), result,
                    "Should return env var value when it is set and no explicit URL given");
        }
    }
}
