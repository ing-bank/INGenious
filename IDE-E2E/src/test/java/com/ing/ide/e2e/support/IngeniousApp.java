package com.ing.ide.e2e.support;

import com.ing.ide.main.Main;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.ui.FXStartUp;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.testfx.api.FxRobot;

/**
 * Launches the INGenious IDE in the test JVM and gives tests the robots that drive it:
 * an AssertJ Swing {@link Robot} for Swing windows and a TestFX {@link FxRobot} for embedded JavaFX.
 */
public final class IngeniousApp {
    private static final String EXIT_GUARD = "e2e.exitGuard";

    private final Robot swing;
    private final FxRobot fx;
    private final InputMode mode;
    private final UiDriver driver;
    private volatile boolean exitRequested;

    private IngeniousApp(final Robot swing, final InputMode mode, final ActionObserver observer) {
        this.swing = swing;
        this.fx = new FxRobot();
        this.mode = mode;
        final UiDriver raw = mode == InputMode.ROBOT
            ? new RobotUiDriver(swing, fx)
            : new DirectUiDriver(swing);
        this.driver = new ObservedUiDriver(raw, observer, mode);
    }

    /**
     * Starts the IDE exactly like the {@code ingenious} launcher does (no arguments = GUI).
     *
     * @param mode how user input is delivered
     * @param observer notified before every user action, e.g. to show it in a recording
     * @return the running application
     */
    public static IngeniousApp launch(final InputMode mode, final ActionObserver observer) {
        final Robot robot = BasicRobot.robotWithNewAwtHierarchy();
        robot.settings().delayBetweenEvents(50);
        Main.main(new String[0]);
        return new IngeniousApp(robot, mode, observer);
    }

    /**
     * @return the driver page objects use for all user input
     */
    public UiDriver driver() {
        return driver;
    }

    /**
     * @return how user input is delivered
     */
    public InputMode mode() {
        return mode;
    }

    /**
     * @return the AssertJ Swing robot, for finding components (input goes through {@link #driver()})
     */
    public Robot swing() {
        return swing;
    }

    /**
     * @return the TestFX robot, for looking up JavaFX nodes (input goes through {@link #driver()})
     */
    public FxRobot fx() {
        return fx;
    }

    /**
     * Waits for the startup (project picker) dialog shown when no recent project is auto-opened.
     *
     * @return the startup dialog
     */
    public DialogFixture waitForStartupDialog() {
        return WindowFinder
            .findDialog(showing(FXStartUp.class))
            .withTimeout(Timeouts.LAUNCH.toMillis(), TimeUnit.MILLISECONDS)
            .using(swing);
    }

    /**
     * Waits for the main window to show a loaded project.
     *
     * @param projectName the expected project name
     * @return the main window
     */
    public FrameFixture waitForMainWindowWithProject(final String projectName) {
        final GenericTypeMatcher<AppMainFrame> matcher = new GenericTypeMatcher<>(
            AppMainFrame.class
        ) {

            @Override
            protected boolean isMatching(final AppMainFrame frame) {
                return frame.isShowing() && frame.getTitle().startsWith(projectName + " - ");
            }
        };
        final FrameFixture frame = WindowFinder
            .findFrame(matcher)
            .withTimeout(Timeouts.LAUNCH.toMillis(), TimeUnit.MILLISECONDS)
            .using(swing);
        preventSystemExitOnQuit(frame);
        return frame;
    }

    /**
     * Keeps the test JVM alive when the IDE quits.
     * <p>
     * On quit, {@link AppMainFrame} switches itself to {@code EXIT_ON_CLOSE}, and {@link JFrame} calls
     * {@code System.exit} after all window listeners ran. This listener is registered after the
     * IDE's own, so it runs last and turns that exit into a plain dispose.
     *
     * @param mainWindow the main window
     */
    public void preventSystemExitOnQuit(final FrameFixture mainWindow) {
        final JFrame frame = (JFrame) mainWindow.target();
        GuiActionRunner.execute(
            () -> {
                if (frame.getRootPane().getClientProperty(EXIT_GUARD) != null) {
                    return;
                }
                frame.getRootPane().putClientProperty(EXIT_GUARD, Boolean.TRUE);
                frame.addWindowListener(
                    new WindowAdapter() {

                        @Override
                        public void windowClosing(final WindowEvent e) {
                            if (frame.getDefaultCloseOperation() == JFrame.EXIT_ON_CLOSE) {
                                exitRequested = true;
                                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            }
                        }
                    }
                );
            }
        );
    }

    /**
     * @return whether the IDE asked to exit the JVM (blocked by {@link #preventSystemExitOnQuit})
     */
    public boolean exitRequested() {
        return exitRequested;
    }

    /**
     * Releases the robots and disposes any remaining windows.
     */
    public void cleanUp() {
        swing.cleanUp();
    }

    private static <T extends Window> GenericTypeMatcher<T> showing(final Class<T> type) {
        return new GenericTypeMatcher<>(type) {

            @Override
            protected boolean isMatching(final T window) {
                return window.isShowing();
            }
        };
    }
}
