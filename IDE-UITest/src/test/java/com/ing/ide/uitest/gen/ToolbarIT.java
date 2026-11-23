package com.ing.ide.uitest.gen;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ide.uitest.ProjectOpenUiTest;
import com.ing.ide.uitest.pages.MainWindowPage;
import com.ing.ide.uitest.pages.NewProjectDialogPage;
import com.ing.ide.uitest.pages.OpenProjectDialogPage;
import com.ing.ide.uitest.report.Scenario;
import com.ing.ide.uitest.report.StepRef;
import com.ing.ide.uitest.report.TestCase;
import com.ing.ide.uitest.support.Highlight;
import javafx.scene.control.ButtonBase;
import org.testng.annotations.Test;

/**
 * GEN-SC002 "Use the new Project and open project button from the App toolbar".
 * StepRef numbers are the printed step numbers of the RTM test cases.
 */
@Scenario(
    code = "GEN-SC002",
    description = "Use the new Project and open project button from the App toolbar"
)
public class ToolbarIT extends ProjectOpenUiTest {

    @Test(priority = 1)
    @TestCase(
        code = "GEN-SC002-TC001",
        description = "Functioning New Project Button from Menu bar"
    )
    public void newProjectFromToolbar() {
        final MainWindowPage main = mainWindow();
        final String name = newProjectName("ToolbarNew");

        final ButtonBase newProject = main.toolbarButton("New Project");
        main.click(newProject);
        final NewProjectDialogPage dialog = NewProjectDialogPage.waitFor(app.swing(), app.driver());
        evidence.verify(
            "new-project-dialog",
            StepRef.step(2),
            dialog.highlight("New Project dialog is shown"),
            () -> {}
        );

        dialog.enterNameAndPressEnter(name);
        app.waitForMainWindowWithProject(name);
        evidence.verify(
            "new-project-opened",
            StepRef.step(4),
            main.titleBar("New project " + name + " is opened"),
            () -> assertThat(main.title()).as("title bar").startsWith(name + " - ")
        );
        evidence.verify(
            "project-folder-created",
            StepRef.step(5),
            Highlight.captionOnly("Project folder Projects/" + name + " exists"),
            () -> assertThat(home.projectDir(name)).as("project folder").isDirectory()
        );
    }

    @Test(priority = 2)
    @TestCase(
        code = "GEN-SC002-TC002",
        description = "Functioning Open Project Button from Menu Bar"
    )
    public void openProjectFromToolbar() {
        final MainWindowPage main = mainWindow();
        final String name = otherProject.getFileName().toString();

        final ButtonBase openProject = main.toolbarButton("Open Project");
        evidence.verify(
            "open-project-button",
            StepRef.assertion("visible and enabled"),
            Highlight.fxNode(openProject, "\"Open Project\" is visible and enabled"),
            () ->
                assertThat(main.isVisibleAndEnabled(openProject))
                    .as("Open Project visible and enabled")
                    .isTrue()
        );

        main.click(openProject);
        final OpenProjectDialogPage chooser = OpenProjectDialogPage.waitFor(
            app.swing(),
            app.driver()
        );
        evidence.verify(
            "open-project-dialog",
            StepRef.step(2),
            chooser.highlight("Open Project dialog is displayed"),
            () -> {}
        );

        chooser.choose(otherProject);
        app.waitForMainWindowWithProject(name);
        evidence.verify(
            "project-loaded",
            StepRef.step(4),
            main.titleBar("Project " + name + " is loaded"),
            () ->
                assertThat(main.projectLocation())
                    .as("open project")
                    .isEqualTo(otherProject.toAbsolutePath())
        );
    }
}
