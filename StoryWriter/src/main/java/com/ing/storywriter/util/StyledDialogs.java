package com.ing.storywriter.util;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * Drop-in replacement for JOptionPane with modern styled dialogs.
 * Provides convenience methods that match JOptionPane signatures for easy migration.
 */
public class StyledDialogs {
    
    /**
     * Show a confirmation dialog (Yes/No).
     * Drop-in replacement for JOptionPane.showConfirmDialog.
     */
    public static int showConfirmDialog(Component parent, String message, String title, 
                                       int optionType, int messageType) {
        int dialogType = convertMessageType(messageType);
        int result = StyledConfirmDialog.showYesNo(parent, message, title, dialogType);
        return result; // Returns YES_OPTION, NO_OPTION, or CANCEL_OPTION
    }
    
    /**
     * Show a confirmation dialog (Yes/No) with default title.
     */
    public static int showConfirmDialog(Component parent, String message) {
        return showConfirmDialog(parent, message, "Confirm", 
                               JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }
    
    /**
     * Show a message dialog (OK only) - uses toast notification instead.
     */
    public static void showMessageDialog(Component parent, String message) {
        Notification.show(parent, message);
    }
    
    /**
     * Show a message dialog with title - uses toast notification instead.
     */
    public static void showMessageDialog(Component parent, String message, String title, int messageType) {
        // Use appropriate notification type based on message type
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                Notification.showError(parent, message);
                break;
            case JOptionPane.WARNING_MESSAGE:
                Notification.showWarning(parent, message);
                break;
            case JOptionPane.INFORMATION_MESSAGE:
                Notification.showInfo(parent, message);
                break;
            default:
                Notification.show(parent, message);
        }
    }
    
    /**
     * Show an input dialog.
     * Note: This still uses JOptionPane as it requires text input.
     */
    public static String showInputDialog(Component parent, String message, String title, int messageType) {
        return JOptionPane.showInputDialog(parent, message, title, messageType);
    }
    
    /**
     * Show an input dialog.
     */
    public static String showInputDialog(Component parent, String message) {
        return JOptionPane.showInputDialog(parent, message);
    }
    
    /**
     * Show an input dialog with options (dropdown).
     */
    public static Object showInputDialog(Component parent, String message, String title, 
                                        int messageType, Object icon, Object[] options, 
                                        Object initialValue) {
        return JOptionPane.showInputDialog(parent, message, title, messageType, 
                                          (javax.swing.Icon) icon, options, initialValue);
    }
    
    /**
     * Convert JOptionPane message type to StyledConfirmDialog type.
     */
    private static int convertMessageType(int jOptionPaneType) {
        switch (jOptionPaneType) {
            case JOptionPane.ERROR_MESSAGE:
                return StyledConfirmDialog.ERROR;
            case JOptionPane.WARNING_MESSAGE:
                return StyledConfirmDialog.WARNING;
            case JOptionPane.INFORMATION_MESSAGE:
                return StyledConfirmDialog.INFO;
            case JOptionPane.QUESTION_MESSAGE:
            default:
                return StyledConfirmDialog.CONFIRM;
        }
    }
    
    // JOptionPane constants for compatibility
    public static final int YES_OPTION = JOptionPane.YES_OPTION;
    public static final int NO_OPTION = JOptionPane.NO_OPTION;
    public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    public static final int OK_OPTION = JOptionPane.OK_OPTION;
    public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
    public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
    public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
    public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
    public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
    public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
    public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
}
