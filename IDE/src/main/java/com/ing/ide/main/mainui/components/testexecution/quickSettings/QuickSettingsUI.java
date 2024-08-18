
package com.ing.ide.main.mainui.components.testexecution.quickSettings;

import com.ing.datalib.settings.RunSettings;

/**
 *
 * 
 */
public abstract class QuickSettingsUI extends javax.swing.JPanel {

    protected PropertyListener pUp;

    /**
     * Bind the models property listener with the view
     *
     * @param pl the listener
     */
    public void setPropertyListener(PropertyListener pl) {
        this.pUp = pl;
    }

    /**
     * notify the model on prop value change
     *
     * @param prop key
     * @param value value
     */
    public void onPropertyChange(String prop, String value) {
        if (pUp != null) {
            pUp.onPropertyChange(prop, value);
        }
    }

    abstract protected void updateUI(RunSettings x);
}
