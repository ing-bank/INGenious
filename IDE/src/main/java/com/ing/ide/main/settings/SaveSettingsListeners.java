package com.ing.ide.main.settings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 *
 * @author Julie Ann Ayap
 */
public class SaveSettingsListeners {
    public final JButton saveSettings;

    public SaveSettingsListeners(JButton saveButton) {
        this.saveSettings = saveButton;
    }

    public class SaveDocListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            saveSettings.setEnabled(true);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            saveSettings.setEnabled(true);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            saveSettings.setEnabled(true);
        }
    }

    public class SaveItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            saveSettings.setEnabled(true);
        }
    }

    public class SaveChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            saveSettings.setEnabled(true);
        }
    }

    public class SaveTableModelListener implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent e) {
            saveSettings.setEnabled(true);
        }
    }
}
