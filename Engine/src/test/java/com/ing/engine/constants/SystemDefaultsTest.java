package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class SystemDefaultsTest {

    @AfterMethod
    public void tearDown() {
        SystemDefaults.resetAll();
        SystemDefaults.CLVars.clear();
        SystemDefaults.EnvVars.clear();
    }

    // ---- Default field values ----

    @Test
    public void testDefaultWaitTime() {
        assertThat(SystemDefaults.waitTime).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    public void testDefaultElementWaitTime() {
        assertThat(SystemDefaults.elementWaitTime).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    public void testDefaultStopExecution() {
        assertThat(SystemDefaults.stopExecution.get()).isFalse();
    }

    @Test
    public void testDefaultPauseExecution() {
        assertThat(SystemDefaults.pauseExecution.get()).isFalse();
    }

    @Test
    public void testDefaultDebugMode() {
        assertThat(SystemDefaults.debugMode.get()).isFalse();
    }

    @Test
    public void testDefaultNextStepFlag() {
        assertThat(SystemDefaults.nextStepflag.get()).isTrue();
    }

    // ---- resetAll ----

    @Test
    public void testResetAllRestoresDefaults() {
        SystemDefaults.waitTime = Duration.ofSeconds(30);
        SystemDefaults.elementWaitTime = Duration.ofSeconds(60);
        SystemDefaults.stopExecution.set(true);
        SystemDefaults.debugMode.set(true);
        SystemDefaults.nextStepflag.set(false);
        SystemDefaults.pauseExecution.set(true);

        SystemDefaults.resetAll();

        assertThat(SystemDefaults.waitTime).isEqualTo(Duration.ofSeconds(10));
        assertThat(SystemDefaults.elementWaitTime).isEqualTo(Duration.ofSeconds(10));
        assertThat(SystemDefaults.stopExecution.get()).isFalse();
        assertThat(SystemDefaults.debugMode.get()).isFalse();
        assertThat(SystemDefaults.nextStepflag.get()).isTrue();
        assertThat(SystemDefaults.pauseExecution.get()).isFalse();
    }

    // ---- canLaunchSummary ----

    @Test
    public void testCanLaunchSummaryDefault() {
        assertThat(SystemDefaults.canLaunchSummary()).isTrue();
    }

    @Test
    public void testCanLaunchSummaryWhenSuppressed() {
        SystemDefaults.CLVars.put("dontLaunchSummary", "true");
        assertThat(SystemDefaults.canLaunchSummary()).isFalse();
    }

    // ---- debug ----

    @Test
    public void testDebugDefaultFalse() {
        assertThat(SystemDefaults.debug()).isFalse();
    }

    @Test
    public void testDebugWhenDebugModeSet() {
        SystemDefaults.debugMode.set(true);
        assertThat(SystemDefaults.debug()).isTrue();
    }

    @Test
    public void testDebugWhenCLVarSet() {
        SystemDefaults.CLVars.put("debug", "true");
        assertThat(SystemDefaults.debug()).isTrue();
    }

    // ---- CLVars and EnvVars ----

    @Test
    public void testCLVarsInitEmpty() {
        assertThat(SystemDefaults.CLVars).isNotNull();
    }

    @Test
    public void testEnvVarsInitEmpty() {
        assertThat(SystemDefaults.EnvVars).isNotNull();
    }
}
