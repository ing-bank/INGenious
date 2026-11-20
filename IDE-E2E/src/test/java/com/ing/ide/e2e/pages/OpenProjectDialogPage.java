package com.ing.ide.e2e.pages;

import com.ing.ide.e2e.support.Highlight;
import com.ing.ide.e2e.support.Timeouts;
import com.ing.ide.e2e.support.UiDriver;
import com.ing.ide.main.utils.INGeniousFileChooser;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.swing.JFileChooser;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;

/**
 * Page object for the "Open Project" file chooser dialog.
 */
public final class OpenProjectDialogPage {
    private final DialogFixture dialog;
    private final UiDriver driver;

    private OpenProjectDialogPage(final DialogFixture dialog, final UiDriver driver) {
        this.dialog = dialog;
        this.driver = driver;
    }

    /**
     * Waits for the project chooser dialog to appear.
     *
     * @param robot the AssertJ Swing robot
     * @param driver performs the user input
     * @return the dialog
     */
    public static OpenProjectDialogPage waitFor(final Robot robot, final UiDriver driver) {
        final DialogFixture dialog = WindowFinder
            .findDialog(
                new GenericTypeMatcher<>(javax.swing.JDialog.class) {

                    @Override
                    protected boolean isMatching(final javax.swing.JDialog d) {
                        return (
                            d.isShowing() &&
                            "Open Project".equals(d.getTitle()) &&
                            chooserIn(d) != null
                        );
                    }
                }
            )
            .withTimeout(Timeouts.STEP.toMillis(), TimeUnit.MILLISECONDS)
            .using(robot);
        return new OpenProjectDialogPage(dialog, driver);
    }

    /**
     * @param label caption
     * @return highlight of the whole dialog
     */
    public Highlight highlight(final String label) {
        return Highlight.component(dialog.target(), label);
    }

    /**
     * Selects a project folder and confirms.
     *
     * @param project the project folder
     */
    public void choose(final Path project) {
        driver.chooseFile(chooserIn(dialog.target()), project.toFile());
    }

    private static JFileChooser chooserIn(final java.awt.Container container) {
        for (final java.awt.Component c : container.getComponents()) {
            if (c instanceof INGeniousFileChooser) {
                return (JFileChooser) c;
            }
            if (c instanceof java.awt.Container) {
                final JFileChooser nested = chooserIn((java.awt.Container) c);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
