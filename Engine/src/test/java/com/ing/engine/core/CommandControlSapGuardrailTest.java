package com.ing.engine.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ing.datalib.settings.SapConfigRegistry;
import com.ing.datalib.settings.SapConnections;
import com.ing.datalib.settings.SapDefaults;
import com.ing.engine.commands.browser.Command;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.drivers.sap.FakeSap;
import com.ing.engine.drivers.sap.SapSessionManager;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.support.Step;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ingenious.api.status.Status;
import com.microsoft.playwright.Page;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Real {@link CommandControl#sync(Step)} routing cases replacing the print-only,
 * pre-driverless {@code SAPActionRoutingTest}: a blocked archetype (Mobile) fails fast while a
 * SAP connection is open, a SAP session action and a driverless (Synthetic Data) action both
 * still route normally - see design doc "Guardrails - what may share a SAP test case".
 */
public class CommandControlSapGuardrailTest {

    @BeforeClass
    public static void loadActions() throws Exception {
        // PluginLoader requires the plugins dir to exist (even empty) - not part of a fresh
        // checkout, only ever created by the packaged app or the installer.
        new File(com.ing.engine.constants.FilePath.getAppRoot(), "plugins").mkdirs();
        MethodInfoManager.load();
    }

    private final SapSessionManager mgr = SapSessionManager.INSTANCE;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("cc-guardrail-test");
        SapConnections store = new SapConnections(tempDir.toString());
        store.addSap("QA");
        SapDefaults defaults = new SapDefaults(tempDir.toString());
        defaults.setDefaultConnection("QA");
        SapConfigRegistry registry = new SapConfigRegistry(store, defaults);

        FakeSap.Locator locator = new FakeSap.Locator();
        locator.rotPresent = false;
        mgr.setRegistrySupplier(() -> registry);
        mgr.setLocatorFactory(() -> locator);
        mgr.resetForThread();
        mgr.initConnection("", runner());
        assertTrue("precondition: a SAP connection is open for these tests", mgr.hasConnection());
    }

    @After
    public void tearDown() throws IOException {
        mgr.closeAll(null);
        mgr.setRegistrySupplier(null);
        mgr.setLocatorFactory(null);
        mgr.resetForThread();
        Files
            .walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private static TestCaseRunner runner() {
        TestCaseRunner r = mock(TestCaseRunner.class);
        when(r.getRoot()).thenReturn(r);
        return r;
    }

    private static CommandControl newControl(TestCaseReport report) {
        return new CommandControl(null, null, null, null, report) {

            @Override
            public void execute(String com, int sub) {}

            @Override
            public void executeAction(String action) {}

            @Override
            public Object context() {
                return null;
            }
        };
    }

    @Test
    public void blockedArchetype_failsFastWithSapConnectionOpen() throws Exception {
        TestCaseReport report = mock(TestCaseReport.class);
        CommandControl cc = newControl(report);

        Step step = Step.create(1).object("Mobile").action("shake").input("");
        cc.sync(step);

        verify(report).updateTestLog(eq("shake"), contains("Mobile"), eq(Status.FAILNS));
    }

    @Test
    public void sapSessionAction_stillRoutesWhenSapConnectionOpen() throws Exception {
        TestCaseReport report = mock(TestCaseReport.class);
        CommandControl cc = newControl(report);

        Step step = Step.create(1).object("SAP").action("sapExecuteTransaction").input("VA01");
        cc.sync(step);

        verify(report, never()).updateTestLog(anyString(), anyString(), eq(Status.FAILNS));
    }

    @Test
    public void driverlessAction_stillRunsWhenSapConnectionOpen() throws Exception {
        TestCaseReport report = mock(TestCaseReport.class);
        CommandControl cc = newControl(report);

        Step step = Step.create(1).object("Synthetic Data").action("setLocale").input("en-US");
        cc.sync(step);

        verify(report, never()).updateTestLog(anyString(), anyString(), eq(Status.FAILNS));
    }

    /**
     * {@link CommandControl#isSapArchetypeAction()} must reflect the CURRENT step's own
     * archetype, not just whether a SAP connection happens to be open somewhere in the run
     * (that's {@link CommandControl#isSapMode()}, which is true for every test in this class -
     * see {@code @Before}).
     */
    @Test
    public void isSapArchetypeAction_reflectsCurrentStepArchetypeNotJustSapMode() {
        TestCaseReport report = mock(TestCaseReport.class);
        CommandControl cc = newControl(report);

        cc.Action = "sapExecuteTransaction";
        assertTrue(
            "a genuine SAP action must be recognised as SAP archetype",
            cc.isSapArchetypeAction()
        );

        cc.Action = "Click";
        assertFalse(
            "a Browser action must NOT be recognised as SAP archetype, even though " +
            "isSapMode() is true (a SAP connection is open elsewhere in the run)",
            cc.isSapArchetypeAction()
        );
    }

    /**
     * Regression test for the exact reported bug: a Browser test case that also has an open
     * SAP connection failed with a null Page ("this.page is null") on its first Browser step.
     * Root cause: {@code Command}'s constructor branched on {@code Commander.isSapMode()} (any
     * SAP connection open, run-wide) instead of the current step's own archetype, so once SAP
     * mode was on, every subsequent {@code Command} - including a plain Browser click - took
     * the SAP-only branch and never got {@code Page}/{@code AObject}/{@code Locator} wired up.
     */
    @Test
    public void browserCommand_getsPageWiredUpWhenSapConnectionOpen() throws Exception {
        PlaywrightDriverCreation pdc = new PlaywrightDriverCreation();
        pdc.page = mock(Page.class);

        TestCaseReport report = mock(TestCaseReport.class);
        CommandControl cc = new CommandControl(pdc, pdc, pdc, null, report) {

            @Override
            public void execute(String com, int sub) {}

            @Override
            public void executeAction(String action) {}

            @Override
            public Object context() {
                return runner();
            }
        };
        cc.Action = "Click"; // a genuine Browser/Playwright action, not SAP

        Command cmd = new Command(cc);

        assertNotNull(
            "Browser Command must get a live Page even while a SAP connection is open " +
            "elsewhere in the run",
            cmd.Page
        );
        assertNotNull(
            "Browser Command must get AObject wired up even while a SAP connection is open",
            cmd.AObject
        );
    }
}
