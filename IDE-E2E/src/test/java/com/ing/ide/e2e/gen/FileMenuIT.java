package com.ing.ide.e2e.gen;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ide.e2e.ProjectOpenE2ETest;
import com.ing.ide.e2e.pages.MainWindowPage;
import com.ing.ide.e2e.pages.MenuPage;
import com.ing.ide.e2e.pages.NewProjectDialogPage;
import com.ing.ide.e2e.pages.OpenProjectDialogPage;
import com.ing.ide.e2e.report.Scenario;
import com.ing.ide.e2e.report.StepRef;
import com.ing.ide.e2e.report.TestCase;
import com.ing.ide.e2e.support.Highlight;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * GEN-SC001 "Use the File menu options". Starts with a project open; the test cases share one IDE
 * and run in order. Quit (TC005) ends the IDE, so it runs in {@link FileMenuQuitIT}.
 * StepRef numbers are the printed step numbers of the RTM test cases.
 */
@Scenario(code = "GEN-SC001", description = "Use the File menu options")
public class FileMenuIT extends ProjectOpenE2ETest {

    @Test(priority = 1)
    @TestCase(
        code = "GEN-SC001-TC001",
        description = "Functioning New Project Button from File menu"
    )
    public void newProjectFromFileMenu() {
        final MainWindowPage main = mainWindow();
        final String name = newProjectName("FileMenuNew");

        final MenuPage file = main.menu("File").open();
        evidence.verify(
            "file-menu-open",
            StepRef.step(2),
            file.dropdown("File menu dropdown is displayed"),
            () -> assertThat(file.isOpen()).as("File menu open").isTrue()
        );

        file.choose("New Project");
        final NewProjectDialogPage dialog = NewProjectDialogPage.waitFor(app.swing(), app.driver());
        evidence.verify(
            "new-project-dialog",
            StepRef.step(4),
            dialog.highlight("New Project dialog is shown"),
            () -> {}
        );

        dialog.enterNameAndPressEnter(name);
        app.waitForMainWindowWithProject(name);
        evidence.verify(
            "new-project-opened",
            StepRef.step(6),
            main.titleBar("New project " + name + " is opened"),
            () -> assertThat(main.title()).as("title bar").startsWith(name + " - ")
        );
        evidence.verify(
            "project-folder-created",
            StepRef.step(7),
            Highlight.captionOnly("Project folder Projects/" + name + " exists"),
            () -> assertThat(home.projectDir(name)).as("project folder").isDirectory()
        );
    }

    @Test(priority = 2)
    @TestCase(
        code = "GEN-SC001-TC002",
        description = "Functioning Open Project Button from File menu"
    )
    public void openProjectFromFileMenu() {
        final MainWindowPage main = mainWindow();
        final String name = otherProject.getFileName().toString();

        final MenuPage file = main.menu("File").open();
        evidence.verify(
            "file-menu-open",
            StepRef.step(2),
            file.dropdown("File menu dropdown opens"),
            () -> assertThat(file.isOpen()).as("File menu open").isTrue()
        );
        evidence.verify(
            "open-project-option",
            StepRef.step(3),
            file.itemHighlight("Open Project", "\"Open Project\" is visible and enabled"),
            () ->
                assertThat(file.isVisibleAndEnabled("Open Project"))
                    .as("Open Project visible and enabled")
                    .isTrue()
        );

        file.choose("Open Project");
        final OpenProjectDialogPage chooser = OpenProjectDialogPage.waitFor(
            app.swing(),
            app.driver()
        );
        evidence.verify(
            "open-project-dialog",
            StepRef.step(5),
            chooser.highlight("Open Project dialog is displayed"),
            () -> {}
        );

        chooser.choose(otherProject);
        app.waitForMainWindowWithProject(name);
        evidence.verify(
            "project-loaded",
            StepRef.step(7),
            main.titleBar("Project " + name + " is loaded"),
            () ->
                assertThat(main.projectLocation())
                    .as("open project")
                    .isEqualTo(otherProject.toAbsolutePath())
        );
    }

    @Test(priority = 3)
    @TestCase(
        code = "GEN-SC001-TC003",
        description = "Functioning Save Project Button from File menu"
    )
    public void saveProjectFromFileMenu() throws IOException {
        final MainWindowPage main = mainWindow();
        // Saving rewrites the .project file; backdate it so the save is detectable
        final Path projectFile = main.projectLocation().resolve(".project");
        final FileTime before = FileTime.from(Instant.now().minus(1, ChronoUnit.HOURS));
        Files.setLastModifiedTime(projectFile, before);

        final MenuPage file = main.menu("File").open();
        evidence.verify(
            "file-menu-open",
            StepRef.step(2),
            file.dropdown("File menu dropdown opens"),
            () -> assertThat(file.isOpen()).as("File menu open").isTrue()
        );
        evidence.verify(
            "save-project-option",
            StepRef.step(3),
            file.itemHighlight("Save Project", "\"Save Project\" is visible and enabled"),
            () ->
                assertThat(file.isVisibleAndEnabled("Save Project"))
                    .as("Save Project visible and enabled")
                    .isTrue()
        );

        file.choose("Save Project");
        evidence.verify(
            "saved-notification",
            StepRef.step(5),
            Highlight.component(
                main.waitForNotification("Saved"),
                "\"Project saved\" notification is displayed"
            ),
            () -> {}
        );
        evidence.verify(
            "project-files-saved",
            StepRef.step(6),
            Highlight.captionOnly(
                "Project files written to " + main.projectLocation().getFileName()
            ),
            () -> assertThat(modified(projectFile)).as(".project modified").isGreaterThan(before)
        );
    }

    @Test(priority = 4)
    @TestCase(
        code = "GEN-SC001-TC004",
        description = "Functioning Restart Project Button from File menu"
    )
    public void restartFromFileMenu() {
        throw new SkipException(
            "Not automated: Restart relaunches INGenious as a separate process (ingenious.command / " +
            "ingenious.bat), outside the test's control. Reported as Blocked."
        );
    }

    private static FileTime modified(final Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            throw new AssertionError("Cannot read " + file, e);
        }
    }
}
