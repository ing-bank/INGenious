
package com.ing.ide.util;

import com.ing.ide.main.utils.toasterNotification.Toaster;
import com.ing.ide.main.utils.toasterNotification.ToasterDialog;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * 
 */
public class Notification {

    public static Boolean deleteConfirmation = true;
    private static final Toaster TOASTER_MANAGER = new Toaster();
    
    // Keywords that indicate success messages
    private static final String[] SUCCESS_KEYWORDS = {
        "saved", "created", "success", "added", "done", "copied", 
        "renamed", "migration is done", "loaded"
    };

    public static void show(String message) {
        show(null, message);
    }

    public static void show(Component parent, String message) {
        // Auto-detect success messages based on keywords
        if (isSuccessMessage(message)) {
            TOASTER_MANAGER.showSuccessToaster(parent, message);
        } else {
            TOASTER_MANAGER.showInfoToaster(parent, message);
        }
    }
    
    /**
     * Check if the message is a success message based on keywords.
     */
    private static boolean isSuccessMessage(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        for (String keyword : SUCCESS_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                // Make sure it's not a negative/failure context
                if (!lowerMessage.contains("couldn't") && 
                    !lowerMessage.contains("could not") &&
                    !lowerMessage.contains("failed") &&
                    !lowerMessage.contains("not ") &&
                    !lowerMessage.contains("error") &&
                    !lowerMessage.contains("already present")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Show a success notification with translucent green background.
     */
    public static void showSuccess(String message) {
        showSuccess(null, message);
    }
    
    /**
     * Show a success notification with translucent green background.
     */
    public static void showSuccess(Component parent, String message) {
        TOASTER_MANAGER.showSuccessToaster(parent, message);
    }
    
    /**
     * Show an info notification with translucent blue background.
     */
    public static void showInfo(String message) {
        showInfo(null, message);
    }
    
    /**
     * Show an info notification with translucent blue background.
     */
    public static void showInfo(Component parent, String message) {
        TOASTER_MANAGER.showInfoToaster(parent, message);
    }

    public static Boolean showDeleteConfirmation() {
        return showDeleteConfirmation("Are you sure want to delete?");
    }

    public static Boolean showDeleteConfirmation(String message) {
        if (deleteConfirmation) {
            int value = JOptionPane.showConfirmDialog(null, message, "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return value == JOptionPane.YES_OPTION;
        }
        return true;
    }

}
