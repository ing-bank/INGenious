package com.example.panel;

import com.ing.ingenious.api.contract.ui.StudioPanelApi;
import javax.swing.JComponent;

/**
 * A panel plugin that cannot be constructed, as a plugin built against a different release or
 * missing a dependency behaves. Used to check that such a plugin is rejected on its own instead
 * of taking the rest of the toolbar with it.
 */
public class UnbuildablePanel implements StudioPanelApi {

    public UnbuildablePanel() {
        throw new IllegalStateException("this plugin cannot start here");
    }

    @Override
    public String getTitle() {
        return "never reached";
    }

    @Override
    public JComponent createPanel() {
        return null;
    }
}
