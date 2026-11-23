package com.ing.ide.uitest.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.ing.ide.uitest.ProjectOpenUiTest;
import com.ing.ide.uitest.pages.MainWindowPage;
import com.ing.ide.uitest.pages.MenuPage;
import com.ing.ide.uitest.pages.View;
import com.ing.ide.uitest.report.Scenario;
import com.ing.ide.uitest.report.StepRef;
import com.ing.ide.uitest.report.TestCase;
import com.ing.ide.uitest.support.Highlight;
import com.ing.ide.uitest.support.Waits;
import java.awt.Dialog;
import java.awt.Window;
import java.util.Arrays;
import javax.swing.AbstractButton;
import javax.swing.JSplitPane;
import org.testng.annotations.Test;

/**
 * GEN-SC003 "Use the Window menu options". Starts with a project open; each test case first puts
 * the IDE in its precondition view through the IDE's API, then acts through the UI.
 * StepRef numbers are the printed step numbers of the RTM test cases.
 */
@Scenario(code = "GEN-SC003", description = "Use the Window menu options")
public class WindowMenuIT extends ProjectOpenUiTest {
    /** Divider position {@code TestDesignUI.adjustUI()} restores for the outer Test Design split. */
    private static final double DEFAULT_SPLIT = 0.70;

    @Test(priority = 1)
    @TestCase(
        code = "GEN-SC003-TC001",
        description = "Verify navigation to Test Design via Window menu"
    )
    public void navigateToTestDesign() {
        navigateViaWindowMenu(View.TEST_EXECUTION, "Test Design", View.TEST_DESIGN);
    }

    @Test(priority = 2)
    @TestCase(
        code = "GEN-SC003-TC002",
        description = "Verify navigation to Test Execution via Window menu"
    )
    public void navigateToTestExecution() {
        navigateViaWindowMenu(View.TEST_DESIGN, "Test Execution", View.TEST_EXECUTION);
    }

    @Test(priority = 3)
    @TestCase(
        code = "GEN-SC003-TC003",
        description = "Verify navigation to Dashboard via Window menu"
    )
    public void navigateToDashboard() {
        navigateViaWindowMenu(View.TEST_DESIGN, "Dashboard", View.DASHBOARD);
    }

    @Test(priority = 4)
    @TestCase(
        code = "GEN-SC003-TC004",
        description = "Verify navigation to API Workbench via Window menu"
    )
    public void navigateToApiWorkbench() {
        navigateViaWindowMenu(View.TEST_DESIGN, "API Workbench", View.API_WORKBENCH);
    }

    @Test(priority = 5)
    @TestCase(
        code = "GEN-SC003-TC005",
        description = "Verify AdjustUI resets application UI partition dimensions via Window menu"
    )
    public void adjustUiResetsPartitions() {
        final MainWindowPage main = mainWindow();
        main.arrangeView(View.TEST_DESIGN);
        // Precondition: a partition resized away from its default
        final JSplitPane split = main.testDesignSplitPane();
        Waits.swing(
            () -> {
                split.setDividerLocation(0.40);
                return null;
            }
        );
        Waits.until("divider to move", () -> Math.abs(proportion(split) - 0.40) < 0.02);
        evidence.take("partition-resized");

        final MenuPage window = openWindowMenu(main, 2);
        checkItemWithShortcut(window, "AdjustUI", 3, 4);

        window.choose("AdjustUI");
        Waits.within(() -> Math.abs(proportion(split) - DEFAULT_SPLIT) < 0.02, 5000);
        evidence.verify(
            "partitions-reset",
            StepRef.step(6),
            Highlight.component(split, "Partitions are reset to their default layout"),
            () ->
                assertThat(proportion(split))
                    .as("Test Design split position")
                    .isCloseTo(DEFAULT_SPLIT, within(0.02))
        );
        evidence.verify(
            "no-errors",
            StepRef.step(7),
            Highlight.captionOnly("No error dialog is shown"),
            () -> assertThat(showingDialogs()).as("dialogs shown after AdjustUI").isZero()
        );
    }

    @Test(priority = 6)
    @TestCase(
        code = "GEN-SC003-TC006",
        description = "Verify Window menu closes after selection or loss of focus"
    )
    public void windowMenuCloses() {
        final MainWindowPage main = mainWindow();
        main.arrangeView(View.TEST_DESIGN);

        final MenuPage window = openWindowMenu(main, 2);
        window.clickOutside(main.statusBar());
        window.waitUntilClosed();
        evidence.verify(
            "closed-after-click-outside",
            StepRef.assertion("focus is moved outside"),
            Highlight.captionOnly("Window menu closed after clicking outside it"),
            () -> assertThat(window.isOpen()).as("Window menu open").isFalse()
        );

        openWindowMenu(main, 2);
        window.choose("Test Execution");
        window.waitUntilClosed();
        evidence.verify(
            "closed-after-selection",
            StepRef.step(4),
            Highlight.captionOnly("Window menu closed after choosing an option"),
            () -> assertThat(window.isOpen()).as("Window menu open").isFalse()
        );
    }

    @Test(priority = 7)
    @TestCase(
        code = "GEN-SC003-TC007",
        description = "Navigate to the API workbench via the side view selector"
    )
    public void navigateToApiWorkbenchViaViewSelector() {
        final MainWindowPage main = mainWindow();
        main.arrangeView(View.TEST_DESIGN);

        main.hover(main.leftEdge());
        final AbstractButton apiWorkbench = main.viewSelectorButton("API Workbench");
        evidence.take("view-selector-shown", StepRef.step(1));

        main.click(apiWorkbench);
        main.waitForView(View.API_WORKBENCH);
        evidence.verify(
            "api-workbench-shown",
            StepRef.step(3),
            main.viewContent(View.API_WORKBENCH, "API Workbench is shown"),
            () -> assertThat(main.isShowing(View.API_WORKBENCH)).as("API Workbench shown").isTrue()
        );
    }

    // Shared flow of TC001-TC004: Window menu -> item with shortcut -> view is shown
    private void navigateViaWindowMenu(final View from, final String item, final View to) {
        final MainWindowPage main = mainWindow();
        main.arrangeView(from);

        final MenuPage window = openWindowMenu(main, 2);
        checkItemWithShortcut(window, item, 3, 4);

        window.choose(item);
        main.waitForView(to);
        evidence.verify(
            "navigated",
            StepRef.step(6),
            main.titleBar("Current view is " + item),
            () -> assertThat(main.currentView()).as("current view").isEqualTo(to.slide())
        );
        evidence.verify(
            "view-elements-shown",
            StepRef.step(7),
            main.viewContent(to, item + " UI elements are displayed"),
            () -> assertThat(main.isShowing(to)).as(item + " content showing").isTrue()
        );
    }

    private MenuPage openWindowMenu(final MainWindowPage main, final int dropdownStep) {
        final MenuPage window = main.menu("Window").open();
        evidence.verify(
            "window-menu-open",
            StepRef.step(dropdownStep),
            window.dropdown("Window menu dropdown is displayed"),
            () -> assertThat(window.isOpen()).as("Window menu open").isTrue()
        );
        return window;
    }

    private void checkItemWithShortcut(
        final MenuPage window,
        final String item,
        final int visibleStep,
        final int shortcutStep
    ) {
        evidence.verify(
            "option-visible",
            StepRef.step(visibleStep),
            window.itemHighlight(item, "\"" + item + "\" is visible"),
            () -> assertThat(window.isVisibleAndEnabled(item)).as(item + " visible").isTrue()
        );
        final String shortcut = window.shownShortcut(item);
        evidence.verify(
            "option-shortcut",
            StepRef.step(shortcutStep),
            window.itemHighlight(item, "\"" + item + "\" shows keyboard shortcut " + shortcut),
            () -> assertThat(shortcut).as(item + " shortcut").isNotBlank()
        );
    }

    private static double proportion(final JSplitPane split) {
        return Waits.swing(
            () ->
                (double) split.getDividerLocation() /
                Math.max(1, split.getWidth() - split.getDividerSize())
        );
    }

    private static long showingDialogs() {
        return Waits.swing(
            () ->
                Arrays
                    .stream(Window.getWindows())
                    .filter(w -> w instanceof Dialog && w.isShowing())
                    .count()
        );
    }
}
