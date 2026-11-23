package com.ing.ide.uitest;

import com.ing.ide.uitest.pages.MainWindowPage;
import com.ing.ide.uitest.support.UiTestHome;
import java.io.IOException;
import java.nio.file.Path;
import org.assertj.swing.fixture.FrameFixture;

/**
 * Base class for scenarios that start on the main window with a project open, as a user with
 * "Auto-open last project" enabled: a copy of the Tutorial project is opened on launch. A second
 * copy is available for tests that open another project.
 */
public abstract class ProjectOpenUiTest extends IngeniousUiTest {
    protected Path mainProject;
    protected Path otherProject;
    private MainWindowPage mainWindow;

    @Override
    protected void prepareHome(final UiTestHome home) throws IOException {
        final String prefix =
            "UITest-" + getClass().getSimpleName() + "-" + System.currentTimeMillis();
        mainProject = home.createFixtureProject(prefix + "-Main");
        otherProject = home.createFixtureProject(prefix + "-Other");
        home.openOnLaunch(mainProject);
    }

    /**
     * @return the main window, waiting for it on first use
     */
    protected MainWindowPage mainWindow() {
        if (mainWindow == null) {
            final FrameFixture frame = app.waitForMainWindowWithProject(
                mainProject.getFileName().toString()
            );
            mainWindow =
                new MainWindowPage(frame, app.swing(), app.driver()).waitUntilReady().activate();
        }
        return mainWindow;
    }

    /**
     * @param name a project name
     * @return a unique name for a project created during the test (cleaned up on the next run)
     */
    protected static String newProjectName(final String name) {
        return "UITest" + name + System.currentTimeMillis();
    }
}
