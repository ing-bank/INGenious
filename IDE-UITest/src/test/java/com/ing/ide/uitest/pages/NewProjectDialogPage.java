package com.ing.ide.uitest.pages;

import com.ing.ide.main.ui.NewProject;
import com.ing.ide.uitest.support.Highlight;
import com.ing.ide.uitest.support.Timeouts;
import com.ing.ide.uitest.support.UiDriver;
import java.util.concurrent.TimeUnit;
import javax.swing.JTextField;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;

/**
 * Page object for the Swing "New Project" dialog opened from the File menu or the toolbar.
 */
public final class NewProjectDialogPage {
    private final DialogFixture dialog;
    private final Robot robot;
    private final UiDriver driver;

    private NewProjectDialogPage(
        final DialogFixture dialog,
        final Robot robot,
        final UiDriver driver
    ) {
        this.dialog = dialog;
        this.robot = robot;
        this.driver = driver;
    }

    /**
     * Waits for the dialog to appear.
     *
     * @param robot the AssertJ Swing robot
     * @param driver performs the user input
     * @return the dialog
     */
    public static NewProjectDialogPage waitFor(final Robot robot, final UiDriver driver) {
        final DialogFixture dialog = WindowFinder
            .findDialog(
                new GenericTypeMatcher<>(NewProject.class) {

                    @Override
                    protected boolean isMatching(final NewProject d) {
                        return d.isShowing();
                    }
                }
            )
            .withTimeout(Timeouts.STEP.toMillis(), TimeUnit.MILLISECONDS)
            .using(robot);
        return new NewProjectDialogPage(dialog, robot, driver);
    }

    /**
     * @param label caption
     * @return highlight of the whole dialog
     */
    public Highlight highlight(final String label) {
        return Highlight.component(dialog.target(), label);
    }

    /**
     * Types the project name and presses Enter, which creates the project.
     *
     * @param name the project name
     */
    public void enterNameAndPressEnter(final String name) {
        // The editable name field is the first enabled, editable text field of the dialog
        final JTextField nameField = robot
            .finder()
            .find(
                dialog.target(),
                new GenericTypeMatcher<>(JTextField.class) {

                    @Override
                    protected boolean isMatching(final JTextField f) {
                        return f.isShowing() && f.isEditable() && f.isEnabled();
                    }
                }
            );
        driver.typeAndEnter(nameField, name);
    }
}
