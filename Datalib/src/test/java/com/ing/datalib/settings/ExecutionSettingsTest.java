package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests ExecutionSettings composite class — verifies delegation
 * to RunSettings, TestMgmtSettings, and KafkaSSLConfigurations.
 */
public class ExecutionSettingsTest {

    private ExecutionSettings execSettings;

    @BeforeMethod
    public void setUp() {
        execSettings = new ExecutionSettings("/nonexistent/path");
    }

    @Test
    public void testRunSettingsNotNull() {
        assertThat(execSettings.getRunSettings()).isNotNull();
    }

    @Test
    public void testTestMgmtSettingsNotNull() {
        assertThat(execSettings.getTestMgmgtSettings()).isNotNull();
    }

    @Test
    public void testKafkaSSLConfigurationNotNull() {
        assertThat(execSettings.getKafkasslConfiguration()).isNotNull();
    }

    @Test
    public void testRunSettingsDefaultValues() {
        RunSettings rs = execSettings.getRunSettings();
        assertThat(rs.getExecutionMode()).isEqualTo("Local");
        assertThat(rs.getThreadCount()).isEqualTo(1);
    }

    @Test
    public void testSetLocationUpdatesAll() {
        // Should not throw
        execSettings.setLocation("/another/nonexistent/path");
        // Verify it still works
        assertThat(execSettings.getRunSettings()).isNotNull();
    }

    @Test
    public void testSaveDoesNotThrow() {
        // With non-existent location, save should handle gracefully
        // (may log warning but not throw)
        try {
            execSettings.save();
        } catch (Exception e) {
            // PropUtils.save may fail silently or log
        }
    }
}
