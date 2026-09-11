package com.ing.engine.core;

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
import com.ing.engine.drivers.sap.FakeSap;
import com.ing.engine.drivers.sap.SapSessionManager;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.support.Step;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ingenious.api.status.Status;
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
}
