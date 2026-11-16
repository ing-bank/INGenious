package com.ing.ide.e2e;

import com.ing.ide.e2e.report.E2EReportListener;
import com.ing.ide.e2e.report.Evidence;
import com.ing.ide.e2e.report.EvidenceSource;
import com.ing.ide.e2e.report.ReportOutput;
import com.ing.ide.e2e.report.Scenario;
import com.ing.ide.e2e.report.TestIdentity;
import com.ing.ide.e2e.support.E2EHome;
import com.ing.ide.e2e.support.EvidenceCollector;
import com.ing.ide.e2e.support.EvidenceMode;
import com.ing.ide.e2e.support.IngeniousApp;
import com.ing.ide.e2e.support.InputMode;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * Base class for INGenious E2E tests. A subclass is one {@link Scenario}; each test method is one
 * {@link com.ing.ide.e2e.report.TestCase}. Every scenario gets a fresh IDE (failsafe forks a new JVM
 * per class); results, screenshots and reports go to the run folder of {@link ReportOutput}.
 */
@Listeners(E2EReportListener.class)
public abstract class IngeniousE2ETest implements EvidenceSource {
    /** First JDK without {@code java.applet}, which AssertJ Swing still references. */
    private static final int FIRST_UNSUPPORTED_JDK = 26;

    protected E2EHome home;
    protected IngeniousApp app;
    protected EvidenceCollector evidence;

    @BeforeClass(alwaysRun = true)
    public void launchIngenious() throws IOException {
        verifySupportedJdk();
        home = E2EHome.current();
        home.verifyPrepared();
        home.resetForFreshLaunch();
        final InputMode mode = InputMode.fromSystemProperty();
        evidence =
            new EvidenceCollector(mode, EvidenceMode.fromSystemProperty(), ReportOutput.runDir());
        app = IngeniousApp.launch(mode, evidence);
    }

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
                "E2E tests need JDK 17-25 (AssertJ Swing), but run on JDK " +
                feature +
                ". Pass -De2e.jvm=/path/to/jdk-21/bin/java"
            );
        }
    }
}
