package com.ing.ide.uitest.pages;

import com.ing.ide.main.mainui.QuitConfirmationDialog;
import com.ing.ide.uitest.support.Highlight;
import com.ing.ide.uitest.support.Timeouts;
import com.ing.ide.uitest.support.UiDriver;
import java.util.concurrent.TimeUnit;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;

/**
 * Page object for the "Quit INGenious" confirmation dialog.
 */
public final class QuitDialogPage {
    private final DialogFixture dialog;
    private final UiDriver driver;

    private QuitDialogPage(final DialogFixture dialog, final UiDriver driver) {
        this.dialog = dialog;
        this.driver = driver;
    }

    /**
     * Waits for the dialog to appear.
     *
     * @param robot the AssertJ Swing robot
     * @param driver performs the user input
     * @return the dialog
     */
    public static QuitDialogPage waitFor(final Robot robot, final UiDriver driver) {
        final DialogFixture dialog = WindowFinder
            .findDialog(
                new GenericTypeMatcher<>(QuitConfirmationDialog.class) {

                    @Override
                    protected boolean isMatching(final QuitConfirmationDialog d) {
                        return d.isShowing();
                    }
                }
            )
            .withTimeout(Timeouts.STEP.toMillis(), TimeUnit.MILLISECONDS)
            .using(robot);
        return new QuitDialogPage(dialog, driver);
    }

    /**
     * @param label caption
     * @return highlight of the whole dialog
     */
    public Highlight highlight(final String label) {
        return Highlight.component(dialog.target(), label);
    }

    /**
     * Clicks "Don't Save".
     */
    public void dontSave() {
        driver.click(dialog.button(JButtonMatcher.withText("Don't Save").andShowing()).target());
    }
}
