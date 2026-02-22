
package com.ing.storywriter.util;

import java.awt.Component;
import com.ing.storywriter.bdd.ui.UIControl;
import com.ing.storywriter.util.toaster.Toaster;
import com.ing.storywriter.util.StyledConfirmDialog;

/**
 *
 */
public class Notification {

    public static Boolean deleteConfirmation = true;
    private static final Toaster t = new Toaster();
    
    // Keywords that indicate success messages
    private static final String[] SUCCESS_KEYWORDS = {
        "saved", "created", "success", "added", "done", "copied", 
        "renamed", "complete", "export complete", "loaded"
    };
    
    // Keywords that indicate error messages  
    private static final String[] ERROR_KEYWORDS = {
        "error", "failed", "couldn't", "could not", "unable", "invalid"
    };
    
    // Keywords that indicate warning messages
    private static final String[] WARNING_KEYWORDS = {
        "warning", "already", "overwrite", "conflict"
    };

    public class Msg {

        public static final String INVALID_PROJ = "Invalid Project Name!!";
        public static final String PROJ_EXIST = "Project Already Exist!!";
        public static final String NO_STORY = "story not Exist/Selected!!";
        public static final String SAVE_STEP = "Steps saved to scenario.!";
        public static final String SELECT_SCN = "Pls select a scenario.!";
        public static final String SAVE_EX = "Examples saved to scenario.!";
        public static final String C_SAVE = "Save Complete!";
        public static final String C_EXPORT = "Export Complete!";
        public static final String NO_EXPORT = "Nothing to export!";
        public static final String SOS = "SOS ";
    }

    public static void show(String message) {
        if (message != null) {
            if (message.length() < 30) {
                message = "\n\n" + message;
            }
            show(UIControl.ctrl.ui, message);
        }
    }

    public static void show(Component parent, String message) {
        // Auto-detect message type and show appropriate toast
        if (isErrorMessage(message)) {
            t.showErrorToaster(parent, message);
        } else if (isWarningMessage(message)) {
            t.showWarningToaster(parent, message);
        } else if (isSuccessMessage(message)) {
            t.showSuccessToaster(parent, message);
        } else {
            t.showInfoToaster(parent, message);
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
     * Show a success notification.
     */
    public static void showSuccess(Component parent, String message) {
        t.showSuccessToaster(parent, message);
    }
    
    /**
     * Show an info notification.
     */
    public static void showInfo(Component parent, String message) {
        t.showInfoToaster(parent, message);
    }
    
    /**
     * Show a warning notification.
     */
    public static void showWarning(Component parent, String message) {
        t.showWarningToaster(parent, message);
    }
    
    /**
     * Show an error notification.
     */
    public static void showError(Component parent, String message) {
        t.showErrorToaster(parent, message);
    }

    public static Boolean showDeleteConfirmation() {
        return showDeleteConfirmation("Are you sure want to delete?");
    }

    public static Boolean showDeleteConfirmation(String message) {
        if (deleteConfirmation) {
            return StyledConfirmDialog.showDeleteConfirm(UIControl.ctrl.ui, message);
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
