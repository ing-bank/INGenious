package com.example.panel;

import com.ing.ingenious.api.contract.data.ProjectTestDataApi;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A panel plugin exactly as a third party would write one, packed into a generated JAR by
 * {@code StudioPanelPluginsTest} and loaded through the production plugin class loader.
 *
 * <p>The static counters are read back through reflection on the loaded class, so a test can
 * see how often this plugin was instantiated and what was handed to it. They are per class
 * loader, which is what makes them usable as evidence that two plugins are isolated.
 */
public class SamplePanel implements com.ing.ingenious.api.contract.ui.StudioPanelApi {
    /** How often this class has been instantiated inside its own class loader. */
    public static int instances;

    /** How often {@link #createPanel()} has been called inside its own class loader. */
    public static int panelsCreated;

    /** The last test data handed over, or {@code null} when none was. */
    public static Object lastTestData;

    /** Set to {@code true} to make {@link #createPanel()} throw, as a broken screen would. */
    public static boolean failToCreatePanel;

    public SamplePanel() {
        instances++;
    }

    @Override
    public String getTitle() {
        return "Title from the class";
    }

    @Override
    public String getTooltip() {
        return "Tooltip from the class";
    }

    @Override
    public JComponent createPanel() {
        panelsCreated++;
        if (failToCreatePanel) {
            throw new IllegalStateException("this screen cannot be built");
        }
        return new JPanel();
    }

    @Override
    public void setProjectTestData(ProjectTestDataApi testData) {
        lastTestData = testData;
    }
}
