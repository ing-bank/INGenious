package com.ing.ide.uitest;

import com.ing.ide.uitest.report.Evidence;
import com.ing.ide.uitest.report.EvidenceSource;
import com.ing.ide.uitest.report.ReportOutput;
import com.ing.ide.uitest.report.Scenario;
import com.ing.ide.uitest.report.TestIdentity;
import com.ing.ide.uitest.report.UiTestReportListener;
import com.ing.ide.uitest.select.TestSelectionInterceptor;
import com.ing.ide.uitest.support.EvidenceCollector;
import com.ing.ide.uitest.support.EvidenceMode;
import com.ing.ide.uitest.support.IngeniousApp;
import com.ing.ide.uitest.support.InputMode;
import com.ing.ide.uitest.support.UiTestHome;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * Base class for INGenious UI tests. A subclass is one {@link Scenario}; each test method is one
 * {@link com.ing.ide.uitest.report.TestCase}. Every scenario gets a fresh IDE (failsafe forks a new JVM
 * per class); results, screenshots and reports go to the run folder of {@link ReportOutput}.
 */
@Listeners({ TestSelectionInterceptor.class, UiTestReportListener.class })
public abstract class IngeniousUiTest implements EvidenceSource {
    /** First JDK without {@code java.applet}, which AssertJ Swing still references. */
    private static final int FIRST_UNSUPPORTED_JDK = 26;

    protected UiTestHome home;
    protected IngeniousApp app;
    protected EvidenceCollector evidence;

    @BeforeClass(alwaysRun = true)
    public void launchIngenious() throws IOException {
        verifySupportedJdk();
        home = UiTestHome.current();
        home.verifyPrepared();
        home.resetForFreshLaunch();
        prepareHome(home);
        final InputMode mode = InputMode.fromSystemProperty();
        evidence =
            new EvidenceCollector(mode, EvidenceMode.fromSystemProperty(), ReportOutput.runDir());
        app = IngeniousApp.launch(mode, evidence);
    }

    /**
     * Sets up the installation before the IDE launches, e.g. creates and auto-opens a project.
     * Runs after the installation is reset; the default does nothing (the startup dialog shows).
     *
     * @param home the installation
     * @throws IOException if setup fails
     */
    protected void prepareHome(final UiTestHome home) throws IOException {}

    @BeforeMethod(alwaysRun = true)
    public void startTestCase(final Method method) {
        if (evidence != null) {
            evidence.startTestCase(
                TestIdentity.of(getClass(), method).evidenceDir(ReportOutput.runDir())
            );
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeIngenious() {
        if (evidence != null) {
            evidence.complete();
        }
        if (app != null) {
            app.cleanUp();
        }
    }

    @Override
    public List<Evidence> completeEvidence() {
        return evidence != null ? evidence.complete() : List.of();
    }

    @Override
    public void captureFailure(final String step) {
        if (evidence != null) {
            evidence.captureFailure(step);
        }
    }

    private static void verifySupportedJdk() {
        final int feature = Runtime.version().feature();
        if (feature >= FIRST_UNSUPPORTED_JDK) {
            throw new IllegalStateException(
                "UI tests need JDK 17-25 (AssertJ Swing), but run on JDK " +
                feature +
                ". Pass -Duitest.jvm=/path/to/jdk-21/bin/java"
            );
        }
    }
}
