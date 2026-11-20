package com.ing.ide.e2e.pages;

import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.Component;
import java.util.function.Function;

/**
 * The main views of the IDE, switched with the Window menu, the side dock or the toolbar.
 */
public enum View {
    TEST_DESIGN("TestDesign", f -> f.getTestDesign().getTestDesignUI()),
    TEST_EXECUTION("TestExecution", f -> f.getTestExecution().getTestExecutionUI()),
    DASHBOARD("DashBoard", AppMainFrame::getDashBoard),
    API_WORKBENCH("APITester", f -> f.getAPITester().getAPITesterUI());

    private final String slide;
    private final Function<AppMainFrame, Component> content;

    View(final String slide, final Function<AppMainFrame, Component> content) {
        this.slide = slide;
        this.content = content;
    }

    /**
     * @return the IDE's internal name of the view
     */
    public String slide() {
        return slide;
    }

    /**
     * @param frame the main window
     * @return the view's root component
     */
    public Component content(final AppMainFrame frame) {
        return content.apply(frame);
    }
}
