
package com.ing.ide.main.mainui.components.testexecution.quickSettings;

import com.ing.datalib.settings.RunSettings;
import com.ing.ide.main.mainui.components.testexecution.testset.TestsetComponent;
import com.ing.ide.util.logging.UILogger;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JComponent;

/**
 *
 * Controller for quick settings
 *
 * <br><br><br>
 *
 *
 */
public class QuickSettings implements PropertyListener {

    private static final org.slf4j.Logger LOG = UILogger.getLogger(QuickSettings.class.getName());
    private RunSettings runSettings;
    private Action update;

    private final QuickSettingsUI uiLeft, uiRight;

    public enum Actions {
        SAVE
    }

    public QuickSettings(TestsetComponent view) {
        uiLeft = new QuickSettingsUILeft();
        uiRight = new QuickSettingsUIRight() {
            @Override
            public Object[] getEnvList() {
                return view.getProject()
                        .getTestData().getEnvironments().toArray();
            }
        };
        setListeners();
    }

    private void setListeners() {
        uiLeft.setPropertyListener((PropertyListener) this);
        uiRight.setPropertyListener((PropertyListener) this);
    }

    /**
     * update the property into the model
     *
     * @param prop key
     * @param value value
     */
    private void settingsUpdated(String prop, String value) {
       if (update != null) {
            update.actionPerformed(new ActionEvent(this, 1, prop));
        }
    }

    public JComponent getUILeft(JComponent parent) {
        return uiLeft;
    }

    public JComponent getUIRight(JComponent parent) {
        return uiRight;
    }

    /**
     * refresh the the view on model change
     * <br>
     *
     * @see QuickSettingsUILeft#updateUI()
     * @see QuickSettingsUIRight#updateUI()
     */
    public void refreshUI() {
        uiLeft.updateUI(runSettings);
        uiRight.updateUI(runSettings);
    }

    public void setOnUpdate(Action onUpdate) {
        this.update = onUpdate;
    }

    /**
     * loads the settings for the given <code>release</code> and
     * <code>testset</code> and updates the ui
     *
     * @param settings
     * @see #refreshUI()
     */
    public void loadSett(RunSettings settings) {
        this.runSettings = settings;
        refreshUI();
    }

    public void save() {
        try {
            runSettings.save();
            if (update != null) {
                update.actionPerformed(new ActionEvent(this, 1, Actions.SAVE.name()));
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void onPropertyChange(String prop, String value) {
        settingsUpdated(prop, value);
    }

}
