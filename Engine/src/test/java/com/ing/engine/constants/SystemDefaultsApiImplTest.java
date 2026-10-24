package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link SystemDefaultsApiImpl} — verifies each method delegates
 * to the corresponding {@link SystemDefaults} static field or method.
 */
public class SystemDefaultsApiImplTest {
    private final SystemDefaultsApiImpl api = SystemDefaultsApiImpl.INSTANCE;

    @AfterMethod
    public void tearDown() {
        // Mirrors SystemDefaultsTest's teardown to avoid cross-test pollution
        // of this shared static state.
        SystemDefaults.resetAll();
        SystemDefaults.CLVars.clear();
        SystemDefaults.EnvVars.clear();
    }

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(SystemDefaultsApiImpl.INSTANCE).isNotNull();
        assertThat(SystemDefaultsApiImpl.INSTANCE).isSameAs(SystemDefaultsApiImpl.INSTANCE);
    }

    @Test
    public void timingDefaultsShouldDelegateBothWays() {
        api.setWaitTime(Duration.ofSeconds(30));
        api.setElementWaitTime(Duration.ofSeconds(60));
        assertThat(SystemDefaults.waitTime).isEqualTo(Duration.ofSeconds(30));
        assertThat(SystemDefaults.elementWaitTime).isEqualTo(Duration.ofSeconds(60));
        assertThat(api.getWaitTime()).isEqualTo(SystemDefaults.waitTime);
        assertThat(api.getElementWaitTime()).isEqualTo(SystemDefaults.elementWaitTime);
    }

    @Test
    public void executionControlFlagsShouldDelegateBothWays() {
        assertThat(api.isStopExecution()).isEqualTo(SystemDefaults.stopExecution.get());
        api.setStopExecution(true);
        assertThat(SystemDefaults.stopExecution.get()).isTrue();

        assertThat(api.isPauseExecution()).isEqualTo(SystemDefaults.pauseExecution.get());
        api.setPauseExecution(true);
        assertThat(SystemDefaults.pauseExecution.get()).isTrue();

        api.setStopCurrentIteration(true);
        assertThat(SystemDefaults.stopCurrentIteration.get()).isTrue();

        api.setNextStepFlag(false);
        assertThat(SystemDefaults.nextStepflag.get()).isFalse();

        api.setReportComplete(true);
        assertThat(SystemDefaults.reportComplete.get()).isTrue();

        api.setDebugMode(true);
        assertThat(SystemDefaults.debugMode.get()).isTrue();
        assertThat(api.isDebugMode()).isTrue();
    }

    @Test
    public void commandLineVariablesShouldDelegateToTheSharedMap() {
        api.setCommandLineVariable("debug", "true");
        assertThat(SystemDefaults.CLVars).containsEntry("debug", "true");
        assertThat(api.hasCommandLineVariable("debug")).isTrue();
        assertThat(api.getCommandLineVariable("debug")).isEqualTo("true");

        api.removeCommandLineVariable("debug");
        assertThat(SystemDefaults.CLVars).doesNotContainKey("debug");
        assertThat(api.hasCommandLineVariable("debug")).isFalse();
    }

    @Test
    public void environmentVariablesShouldDelegateToTheSharedMap() {
        api.setEnvironmentVariable("API_HOST", "localhost");
        assertThat(SystemDefaults.EnvVars).containsEntry("API_HOST", "localhost");
        assertThat(api.hasEnvironmentVariable("API_HOST")).isTrue();
        assertThat(api.getEnvironmentVariable("API_HOST")).isEqualTo("localhost");

        api.removeEnvironmentVariable("API_HOST");
        assertThat(SystemDefaults.EnvVars).doesNotContainKey("API_HOST");
    }

    @Test
    public void diagnosticsShouldDelegateToSystemDefaults() {
        assertThat(api.getBuildVersion()).isEqualTo(SystemDefaults.getBuildVersion());
        assertThat(api.canLaunchSummary()).isEqualTo(SystemDefaults.canLaunchSummary());

        api.setCommandLineVariable("dontLaunchSummary", "true");
        assertThat(api.canLaunchSummary()).isFalse();
        assertThat(SystemDefaults.canLaunchSummary()).isFalse();
    }

    @Test
    public void isDebugShouldReflectDebugModeOrCommandLineVariable() {
        assertThat(api.isDebug()).isEqualTo(SystemDefaults.debug());

        api.setDebugMode(true);
        assertThat(api.isDebug()).isTrue();
        assertThat(SystemDefaults.debug()).isTrue();
    }
}
