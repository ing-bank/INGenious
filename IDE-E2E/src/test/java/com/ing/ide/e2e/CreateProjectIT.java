package com.ing.ide.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ide.e2e.pages.MainWindowPage;
import com.ing.ide.e2e.pages.StartupDialogPage;
import com.ing.ide.e2e.report.Scenario;
import com.ing.ide.e2e.report.TestCase;
import org.assertj.swing.fixture.FrameFixture;
import org.testng.annotations.Test;

/**
 * A user launches INGenious, creates a new project, sees the release version in the title bar
 * and quits.
 */
@Scenario(code = "SC-001", description = "Create a new project from the startup dialog")
public class CreateProjectIT extends IngeniousE2ETest {

    @Test
    @TestCase(
        code = "TC-001-01",
        description = "Title bar shows the project name and the INGenious version from the pom"
    )
    public void titleBarShowsPomVersionAfterCreatingProject() {
        final String expectedVersion = System.getProperty("ingenious.expectedVersion");
        assertThat(expectedVersion)
            .as("ingenious.expectedVersion system property (set from the pom by failsafe)")
            .isNotBlank();
        final String projectName = "E2EProject" + System.currentTimeMillis();

        app.waitForStartupDialog();
        evidence.take("startup-dialog");

        final StartupDialogPage startup = new StartupDialogPage(app.fx(), app.driver())
            .openNewProjectPanel()
            .enterProjectName(projectName);
        assertThat(startup.projectName()).isEqualTo(projectName);
        evidence.take("new-project-form");
        startup.clickCreateProject();

        final FrameFixture frame = app.waitForMainWindowWithProject(projectName);
        app.preventSystemExitOnQuit(frame);
        final MainWindowPage mainWindow = new MainWindowPage(frame, app.swing(), app.driver());
        evidence.take("project-created");

        final String expectedTitle = projectName + " - INGenious " + expectedVersion;
        evidence.verify(
            "assert-title-bar-version",
            mainWindow.titleBar("title bar shows version " + expectedVersion + " from the pom"),
            () -> assertThat(mainWindow.title()).as("title bar").isEqualTo(expectedTitle)
        );
        assertThat(home.projectDir(projectName)).as("project folder on disk").isDirectory();

        mainWindow.quitWithoutSaving();
        assertThat(mainWindow.isShowing()).as("main window after quit").isFalse();
        evidence.take("closed");
    }
}
