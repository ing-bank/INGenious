package com.ing.ide.e2e.pages;

import com.ing.ide.e2e.support.Highlight;
import com.ing.ide.e2e.support.Timeouts;
import com.ing.ide.e2e.support.UiDriver;
import com.ing.ide.main.mainui.QuitConfirmationDialog;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.TimeUnit;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;

/**
 * Page object for the Swing main window ({@code AppMainFrame}).
 */
public final class MainWindowPage {
    private final FrameFixture frame;
    private final Robot robot;
    private final UiDriver driver;

    /**
     * @param frame the main window fixture
     * @param robot the AssertJ Swing robot, used to find windows and components
     * @param driver performs the user input
     */
    public MainWindowPage(final FrameFixture frame, final Robot robot, final UiDriver driver) {
        this.frame = frame;
        this.robot = robot;
        this.driver = driver;
    }

    /**
     * @return the window title as currently shown in the title bar
     */
    public String title() {
        return GuiActionRunner.execute(() -> frame.target().getTitle());
    }

    /**
     * Marks the title bar for an annotated screenshot.
     *
     * @param label what is checked in the title bar
     * @return the title bar area; empty when the window has no native title bar (e.g. no window manager)
     */
    public Highlight titleBar(final String label) {
        final Rectangle bounds = GuiActionRunner.execute(
            () -> {
                final Point location = frame.target().getLocationOnScreen();
                return new Rectangle(
                    location.x,
                    location.y,
                    frame.target().getWidth(),
                    frame.target().getInsets().top
                );
            }
        );
        return Highlight.onScreen(bounds, label);
    }

    /**
     * Closes the window like clicking the title-bar close button, then answers the quit
     * confirmation with "Don't Save", and waits until the window is gone.
     */
    public void quitWithoutSaving() {
        driver.requestClose(frame.target());
        final DialogFixture quitDialog = WindowFinder
            .findDialog(
                new GenericTypeMatcher<>(QuitConfirmationDialog.class) {

                    @Override
                    protected boolean isMatching(final QuitConfirmationDialog dialog) {
                        return dialog.isShowing();
                    }
                }
            )
            .withTimeout(Timeouts.STEP.toMillis(), TimeUnit.MILLISECONDS)
            .using(robot);
        driver.click(
            quitDialog.button(JButtonMatcher.withText("Don't Save").andShowing()).target()
        );
        Pause.pause(
            new Condition("main window to close") {

                @Override
                public boolean test() {
                    return !frame.target().isShowing();
                }
            },
            Timeout.timeout(Timeouts.STEP.toMillis())
        );
    }

    /**
     * @return whether the main window is still on screen
     */
    public boolean isShowing() {
        return GuiActionRunner.execute(() -> frame.target().isShowing());
    }
}
