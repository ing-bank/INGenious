
package com.ing.ide.main.mainui.components.testexecution.quickSettings;

/**
 *
 * Listener for settings update.
 * <br>
 * Interfaces the model and view.
 * 
 * 
 * 
 * <br><br><br>
 *
 */
public interface PropertyListener {
/**
 * notifies when change happens to the property from view
 * 
 * 
 * @param prop key
 * @param value value
 */
    public void onPropertyChange(String prop, String value);
}
