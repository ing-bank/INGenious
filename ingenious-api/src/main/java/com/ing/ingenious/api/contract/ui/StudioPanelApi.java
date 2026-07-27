package com.ing.ingenious.api.contract.ui;

import javax.swing.JComponent;

/**
 * Contract for a plugin that contributes its own screen to the INGenious Studio UI.
 *
 * <p>Until now the plugin framework could add automation <em>actions</em> and object types,
 * but nothing that a user sees. Teams that needed a tailored surface — a test-data picker,
 * a test-case chooser wired to their own test management, an overview panel for
 * non-technical testers — had to build a separate companion application beside Studio and
 * keep it in step with every release. This interface removes that need: the surface lives
 * inside Studio, alongside Test Design and the API Workbench, and ships as an ordinary
 * plugin JAR.
 *
 * <p>Implement this on a plugin entry class (the same class listed in the JAR manifest's
 * {@code pluginEntryClasses} attribute) and it is discovered automatically at startup.
 * Implementations must have a public no-argument constructor. Plugins may declare
 * {@code studioPanelTitle}, {@code studioPanelTooltip}, {@code studioPanelOrder}, and
 * {@code studioPanelSurface} in the same manifest. The optional {@code pluginId} and
 * {@code pluginVersion} attributes provide identity and version information.
 *
 * <pre>{@code
 * public class TestDataPanel implements StudioPanelApi {
 *     public String getTitle() { return "Test Data"; }
 *     public JComponent createPanel() { return new MyPanel(); }
 * }
 * }</pre>
 *
 * <p>{@link #createPanel()} is called once, on first activation, on the Swing Event
 * Dispatch Thread. The returned component is reused for subsequent activations. Keep work
 * that would block the Event Dispatch Thread on a background thread.
 */
public interface StudioPanelApi {
    /**
     * Human-readable name for this screen. Used when {@code studioPanelTitle} is absent
     * from the manifest.
     *
     * @return the panel title, never {@code null} or blank
     */
    String getTitle();

    /**
     * Builds the screen. Called once on first activation, on the Event Dispatch Thread.
     *
     * @return the component to show, never {@code null}
     */
    JComponent createPanel();

    /**
     * Tooltip for the toolbar button when {@code studioPanelTooltip} is absent from the
     * manifest. Defaults to the title.
     *
     * @return the tooltip text
     */
    default String getTooltip() {
        return getTitle();
    }
}
