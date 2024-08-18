
package com.ing.storywriter.util;

import java.awt.Component;
import javax.swing.JOptionPane;
import com.ing.storywriter.bdd.ui.UIControl;
import com.ing.storywriter.util.toaster.Toaster;

/**
 *
 */
public class Notification {

    public static Boolean deleteConfirmation = true;
    private static final Toaster t = new Toaster();

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
        t.showToaster(parent, message);
    }

    public static Boolean showDeleteConfirmation() {
        return showDeleteConfirmation("Are you sure want to delete?");
    }

    public static Boolean showDeleteConfirmation(String message) {
        if (deleteConfirmation) {
            int value = JOptionPane.showConfirmDialog(UIControl.ctrl.ui, message, "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            return value == JOptionPane.YES_OPTION;
        }
        return true;
    }

}
