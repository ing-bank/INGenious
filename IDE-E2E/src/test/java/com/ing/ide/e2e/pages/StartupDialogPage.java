package com.ing.ide.e2e.pages;

import com.ing.ide.e2e.support.Timeouts;
import com.ing.ide.e2e.support.UiDriver;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

/**
 * Page object for the JavaFX startup dialog ({@code FXStartUp}) that opens or creates projects.
 * Nodes are located by the style classes and prompt text the dialog already defines.
 */
public final class StartupDialogPage {
    private static final String NEW_PROJECT_NAV = ".startup-nav-new";
    private static final String CREATE_BUTTON = ".startup-create-btn";
    private static final String PROJECT_NAME_PROMPT = "Enter project name...";

    private final FxRobot fx;
    private final UiDriver driver;

    /**
     * @param fx the TestFX robot, used to look up nodes
     * @param driver performs the user input
     */
    public StartupDialogPage(final FxRobot fx, final UiDriver driver) {
        this.fx = fx;
        this.driver = driver;
    }

    /**
     * Opens the "New Project" panel from the sidebar.
     *
     * @return this page
     */
    public StartupDialogPage openNewProjectPanel() {
        waitFor(NEW_PROJECT_NAV);
        driver.click(fx.lookup(NEW_PROJECT_NAV).<Node>query());
        return this;
    }

    /**
     * Types the project name like a user would.
     *
     * @param name the project name
     * @return this page
     */
    public StartupDialogPage enterProjectName(final String name) {
        waitUntil(() -> projectNameField() != null);
        driver.type(projectNameField(), name);
        return this;
    }

    /**
     * @return the text currently in the project name field
     */
    public String projectName() {
        return projectNameField().getText();
    }

    /**
     * Clicks "Create Project".
     */
    public void clickCreateProject() {
        driver.click(fx.lookup(CREATE_BUTTON).<Node>query());
    }

    private TextField projectNameField() {
        return fx
            .lookup(
                (Node node) ->
                    node instanceof TextField &&
                    PROJECT_NAME_PROMPT.equals(((TextField) node).getPromptText()) &&
                    node.isVisible()
            )
            .<TextField>tryQuery()
            .orElse(null);
    }

    private void waitFor(final String query) {
        waitUntil(() -> fx.lookup(query).tryQuery().isPresent());
    }

    private static void waitUntil(final Callable<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(Timeouts.STEP.toSeconds(), TimeUnit.SECONDS, condition);
        } catch (TimeoutException e) {
            throw new AssertionError("Startup dialog did not reach the expected state in time", e);
        }
    }
}
