
package com.ing.ide.util;

import com.ing.ide.main.utils.StyledConfirmDialog;
import com.ing.ide.main.utils.toasterNotification.Toaster;
import java.awt.Component;

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
    
    // Keywords that indicate error messages  
    private static final String[] ERROR_KEYWORDS = {
        "error", "failed", "couldn't", "could not", "unable", "invalid"
    };
    
    // Keywords that indicate warning messages
    private static final String[] WARNING_KEYWORDS = {
        "warning", "already present", "overwrite", "conflict"
    };

    public static void show(String message) {
        show(null, message);
    }

    public static void show(Component parent, String message) {
        // Auto-detect message type and show appropriate toast
        if (isErrorMessage(message)) {
            TOASTER_MANAGER.showErrorToaster(parent, message);
        } else if (isWarningMessage(message)) {
            TOASTER_MANAGER.showWarningToaster(parent, message);
        } else if (isSuccessMessage(message)) {
            TOASTER_MANAGER.showSuccessToaster(parent, message);
        } else {
            TOASTER_MANAGER.showInfoToaster(parent, message);
        }
    }
    
    /**
     * Check if the message is an error message based on keywords.
     */
    private static boolean isErrorMessage(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        for (String keyword : ERROR_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the message is a warning message based on keywords.
     */
    private static boolean isWarningMessage(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        for (String keyword : WARNING_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                // Don't classify as warning if it's clearly an error
                if (!isErrorMessage(message)) {
                    return true;
                }
            }
        }
        return false;
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
                if (!isErrorMessage(message) && !isWarningMessage(message)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Show a success notification with green background.
     */
    public static void showSuccess(String message) {
        showSuccess(null, message);
    }
    
    /**
     * Show a success notification with green background.
     */
    public static void showSuccess(Component parent, String message) {
        TOASTER_MANAGER.showSuccessToaster(parent, message);
    }
    
    /**
     * Show an info notification with blue background.
     */
    public static void showInfo(String message) {
        showInfo(null, message);
    }
    
    /**
     * Show an info notification with blue background.
     */
    public static void showInfo(Component parent, String message) {
        TOASTER_MANAGER.showInfoToaster(parent, message);
    }
    
    /**
     * Show a warning notification with orange background.
     */
    public static void showWarning(String message) {
        showWarning(null, message);
    }
    
    /**
     * Show a warning notification with orange background.
     */
    public static void showWarning(Component parent, String message) {
        TOASTER_MANAGER.showWarningToaster(parent, message);
    }
    
    /**
     * Show an error notification with red background.
     */
    public static void showError(String message) {
        showError(null, message);
    }
    
    /**
     * Show an error notification with red background.
     */
    public static void showError(Component parent, String message) {
        TOASTER_MANAGER.showErrorToaster(parent, message);
    }

    public static Boolean showDeleteConfirmation() {
        return showDeleteConfirmation("Are you sure want to delete?");
    }

    public static Boolean showDeleteConfirmation(String message) {
        if (deleteConfirmation) {
            return StyledConfirmDialog.showDeleteConfirm(null, message);
        }
        return true;
    }
    
    /**
     * Show a modern styled confirmation dialog.
     */
    public static boolean showConfirm(Component parent, String message, String title) {
        int result = StyledConfirmDialog.showYesNo(parent, message, title, StyledConfirmDialog.CONFIRM);
        return result == StyledConfirmDialog.YES_OPTION;
    }
    
    /**
     * Show a modern styled warning confirmation dialog.
     */
    public static boolean showWarningConfirm(Component parent, String message, String title) {
        int result = StyledConfirmDialog.showYesNo(parent, message, title, StyledConfirmDialog.WARNING);
        return result == StyledConfirmDialog.YES_OPTION;
    }

}
