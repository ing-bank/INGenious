package com.ing.engine.drivers.sap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Smoke tests against a real, already-running SAP GUI. Excluded from the default build
 * (class name ends {@code LiveTest} - see {@code Engine/pom.xml}'s surefire config); run
 * with {@code mvn test -pl Engine -Psap-live} on a Windows machine that has SAP GUI open
 * and scripting enabled (see the design doc's "Known limitation" section).
 *
 * <p>Every test self-skips (rather than fails) when the environment isn't ready, so an
 * accidental {@code -Psap-live} run elsewhere degrades gracefully instead of red-flagging.
 */
public class JacobSapEngineLocatorLiveTest {
    private JacobSapEngineLocator locator;

    @Before
    public void setUp() {
        // sap-live tests require Windows with a running, scripting-enabled SAP GUI.
        assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
        locator = new JacobSapEngineLocator();
        assumeTrue(locator.isRunningObjectTablePresent() && locator.isScriptingEnabled());
    }

    @Test
    public void attachReturnsAnEngine() {
        assertNotNull(locator.attach());
    }

    @Test
    public void openConnectionNamesDoesNotThrow() {
        assertNotNull(locator.attach().openConnectionNames());
    }
}
