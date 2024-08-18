
package com.ing.ide.util;

import com.ing.ide.main.utils.toasterNotification.Toaster;
import java.awt.Component;
import javax.swing.JOptionPane;

/**
 *
 * 
 */
public class Notification {

    public static Boolean deleteConfirmation = true;
    private static final Toaster TOASTER_MANAGER = new Toaster();

    public static void show(String message) {
        show(null, message);
    }

    public static void show(Component parent, String message) {
        TOASTER_MANAGER.showToaster(parent, message);
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
