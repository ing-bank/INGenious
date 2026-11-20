package com.ing.ide.e2e.gen;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ide.e2e.ProjectOpenE2ETest;
import com.ing.ide.e2e.pages.MainWindowPage;
import com.ing.ide.e2e.pages.MenuPage;
import com.ing.ide.e2e.pages.QuitDialogPage;
import com.ing.ide.e2e.report.Scenario;
import com.ing.ide.e2e.report.StepRef;
import com.ing.ide.e2e.report.TestCase;
import com.ing.ide.e2e.support.Highlight;
import com.ing.ide.e2e.support.Waits;
import java.awt.Window;
import java.util.Arrays;
import org.testng.annotations.Test;

/**
 * GEN-SC001-TC005 Quit, in its own JVM because it ends the IDE.
 * <p>
 * The IDE runs inside the test JVM, so the harness turns the IDE's {@code System.exit} into a
 * plain dispose. "No process remains" is therefore checked as: the IDE requested exit, and no
 * IDE window is left on screen.
 */
@Scenario(code = "GEN-SC001", description = "Use the File menu options")
public class FileMenuQuitIT extends ProjectOpenE2ETest {

    @Test
    @TestCase(
        code = "GEN-SC001-TC005",
        description = "Functioning Quit Project Button from File menu"
    )
    public void quitFromFileMenu() {
        final MainWindowPage main = mainWindow();

        final MenuPage file = main.menu("File").open();
        evidence.verify(
            "file-menu-open",
            StepRef.step(2),
            file.dropdown("File menu dropdown opens"),
            () -> assertThat(file.isOpen()).as("File menu open").isTrue()
        );
        evidence.verify(
            "quit-option",
            StepRef.step(3),
            file.itemHighlight("Quit", "\"Quit\" is visible and enabled"),
            () ->
                assertThat(file.isVisibleAndEnabled("Quit")).as("Quit visible and enabled").isTrue()
        );

        file.choose("Quit");
        final QuitDialogPage quit = QuitDialogPage.waitFor(app.swing(), app.driver());
        evidence.verify(
            "quit-popup",
            StepRef.step(5),
            quit.highlight("\"Quit INGenious\" popup appears"),
            () -> {}
        );

        quit.dontSave();
        main.waitUntilClosed();
        evidence.verify(
            "shutdown-started",
            StepRef.step(7),
            Highlight.captionOnly("INGenious closed its window and requested exit"),
            () -> {
                assertThat(main.isShowing()).as("main window showing").isFalse();
                assertThat(app.exitRequested()).as("exit requested").isTrue();
            }
        );
        evidence.verify(
            "nothing-left-running",
            StepRef.step(9),
            Highlight.captionOnly("No INGenious window remains"),
            () ->
                assertThat(
                        Waits.swing(
                            () ->
                                Arrays.stream(Window.getWindows()).filter(Window::isShowing).count()
                        )
                    )
                    .as("windows still showing")
                    .isZero()
        );
    }
}
