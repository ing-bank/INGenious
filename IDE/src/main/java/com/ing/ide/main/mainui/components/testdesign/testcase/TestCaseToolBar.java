package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.engine.core.RunManager;
import com.ing.engine.drivers.PlaywrightDriverFactory;
import com.ing.ide.main.utils.SearchBox;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.settings.IconSettings;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.UIManager;

/**
 * Toolbar for the TestCase panel providing quick access to run, debug, record,
 * save, add/remove rows, move rows, and reload actions. Tooltips display
 * platform-appropriate shortcut labels (Ctrl on Windows, ⌘ on Mac).
 */
public class TestCaseToolBar extends JToolBar {
    private final TestCaseComponent testCaseComp;

    private JButton saveButton;
    private SearchBox searchField;

    private JButton consoleButton;

    private JButton runButton;

    private JButton debugButton;

    private JButton record;

    private JPopupMenu browsersMenu;

    private ButtonGroup browserSelectButtonGroup;

    public TestCaseToolBar(TestCaseComponent testCaseComp) {
        this.testCaseComp = testCaseComp;
        setFloatable(false);
        setOpaque(false);
        setBorder(
            javax.swing.BorderFactory.createMatteBorder(
                0,
                0,
                1,
                0,
                UIManager.getColor("Separator.foreground")
            )
        );
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        init();
    }

    public void setSave(Boolean flag) {
        saveButton.setEnabled(flag);
    }

    public void focusSearch() {
        searchField.focus();
        searchField.setText("");
    }

    private void init() {
        browsersMenu = new JPopupMenu();
        browserSelectButtonGroup = new ButtonGroup();

        searchField = new SearchBox(testCaseComp);
        add(searchField);
        addSeparator();

        add(consoleButton = Utils.createButton("Console", "console", null, testCaseComp));

        record = Utils.createButton("Record", testCaseComp);
        record.setText(null);
        record.setToolTipText("Start Recording [" + Keystroke.format(Keystroke.RECORD) + "]");
        record.setIcon(IconSettings.getIconSettings().getRecordStartIcon());

        record.addActionListener(e -> toggleRecording());
        add(record);

        add(
            runButton =
                Utils.createButton("Run", "run", Keystroke.format(Keystroke.F6), testCaseComp)
        );
        add(
            debugButton =
                Utils.createButton(
                    "Debug",
                    "debug",
                    Keystroke.format(Keystroke.CTRLF6),
                    testCaseComp
                )
        );

        runButton.setComponentPopupMenu(browsersMenu);
        debugButton.setComponentPopupMenu(browsersMenu);
        addSeparator();

        JButton addRowButton = Utils.createButton(
            "Add Row",
            "add",
            "<html>" +
            Keystroke.format(Keystroke.ADD_ROW) +
            " to add a row at last" +
            "<br>" +
            Keystroke.format(Keystroke.INSERT_ROW) +
            " to insert a row before the selected row" +
            "<br>" +
            Keystroke.format(Keystroke.REPLICATE_ROW) +
            " to replicate the row" +
            "</html>",
            testCaseComp
        );
        add(addRowButton);
        JButton removeRow = Utils.createButton(
            "Delete Rows",
            "remove",
            Keystroke.format(Keystroke.REMOVE_ROW),
            testCaseComp
        );
        add(removeRow);
        addSeparator();

        add(Utils.createButton("Move Rows Up", "up", Keystroke.format(Keystroke.UP), testCaseComp));
        add(
            Utils.createButton(
                "Move Rows Down",
                "down",
                Keystroke.format(Keystroke.DOWN),
                testCaseComp
            )
        );
        addSeparator();

        add(
            saveButton =
                Utils.createButton("Save", "save", Keystroke.format(Keystroke.SAVE), testCaseComp)
        );
        add(Utils.createButton("Reload", "reload", Keystroke.format(Keystroke.F5), testCaseComp));
        add(
            Utils.createButton(
                "Open with System Editor",
                "openwithsystemeditor",
                Keystroke.format(Keystroke.OPEN),
                testCaseComp
            )
        );
        saveButton.setEnabled(false);

        setConsoleVisible(false);
    }

    void setConsoleVisible(Boolean flag) {
        consoleButton.setVisible(flag);
    }

    void loadBrowsers(List<String> emulators) {
        browsersMenu.removeAll();

        // Add Playwright browsers first
        List<String> browsers = PlaywrightDriverFactory.Browser.getValuesAsList();
        setBrowserListPopupMenu(browsers);

        // Extract SAP and add it with separator
        List<String> emulatorsCopy = new ArrayList<>(emulators);
        boolean hasSAP = emulatorsCopy.remove("SAP");

        if (hasSAP) {
            browsersMenu.addSeparator();
            setBrowserListPopupMenu(List.of("SAP"));
        }

        // Add remaining emulators
        if (!emulatorsCopy.isEmpty()) {
            browsersMenu.addSeparator();
            setBrowserListPopupMenu(emulatorsCopy);
        }

        selectABrowser();
    }

    String getSelectedBrowser() {
        if (browserSelectButtonGroup.getSelection() != null) {
            return browserSelectButtonGroup.getSelection().getActionCommand();
        }
        return "Chrome";
    }

    private void setBrowserListPopupMenu(List<String> browsers) {
        JRadioButtonMenuItem browserMenuItem;

        for (String browser : browsers) {
            browsersMenu.add(browserMenuItem = new JRadioButtonMenuItem(browser));
            browserMenuItem.setActionCommand(browser);
            browserMenuItem.setFont(UIManager.getFont("TableMenu.font"));
            browserMenuItem.addItemListener(
                (ItemEvent ie) -> {
                    if (ie.getStateChange() == ItemEvent.SELECTED) {
                        String selBrowser =
                            ((JRadioButtonMenuItem) ie.getSource()).getText() +
                            ". Right Click to change the browser";
                        if (
                            ((JRadioButtonMenuItem) ie.getSource()).getText()
                                .equalsIgnoreCase("ProtractorJS")
                        ) {
                            selBrowser = "Ensure that ProtractorJS is installed globally";
                            runButton.setToolTipText(selBrowser);
                            debugButton.setToolTipText(selBrowser);
                        } else {
                            runButton.setToolTipText(
                                "Run [" + Keystroke.format(Keystroke.F6) + "] - with " + selBrowser
                            );
                            debugButton.setToolTipText(
                                "Debug [" +
                                Keystroke.format(Keystroke.CTRLF6) +
                                "] - with " +
                                selBrowser
                            );
                        }
                    }
                }
            );
            browserSelectButtonGroup.add(browserMenuItem);
        }
    }

    private String getPreviouslySelectedBrowser() {
        if (browserSelectButtonGroup.getSelection() != null) {
            return browserSelectButtonGroup.getSelection().getActionCommand();
        }
        return null;
    }

    private void selectABrowser() {
        String browser = getPreviouslySelectedBrowser();
        if (browser == null) {
            browser = RunManager.getGlobalSettings().getBrowser();
        }
        if (browser != null) {
            for (
                Enumeration<AbstractButton> buttons = browserSelectButtonGroup.getElements();
                buttons.hasMoreElements();
            ) {
                AbstractButton button = buttons.nextElement();
                if (button.getActionCommand().equals(browser)) {
                    button.setSelected(true);
                }
            }
        } else {
            browserSelectButtonGroup.setSelected(
                browserSelectButtonGroup.getElements().nextElement().getModel(),
                true
            );
        }
    }

    void setPlaceHolderText(String text, String toolTip) {
        searchField.setPlaceHolder(text, toolTip);
    }

    void startMode() {
        runButton.setActionCommand("Run");
        runButton.setIcon(Utils.getIconByResourceName("/ui/resources/run"));
    }

    void stopMode() {
        runButton.setActionCommand("StopRun");
        runButton.setIcon(Utils.getIconByResourceName("/ui/resources/stop"));
    }

    void toggleRecording() {
        try {
            testCaseComp.record();
        } catch (IOException ex) {
            java
                .util.logging.Logger.getLogger(TestCaseComponent.class.getName())
                .log(java.util.logging.Level.SEVERE, null, ex);
        }
    }

    public void enableRecordButton() {
        record.setEnabled(true);
<<<<<<< HEAD
        record.setToolTipText(isRecording ? "Stop Recording" : "Start Recording");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecordingState(boolean recording) {
        isRecording = recording;
        record.setIcon(
            recording
                ? IconSettings.getIconSettings().getRecordStopIcon()
                : IconSettings.getIconSettings().getRecordStartIcon()
        );
        record.setToolTipText(recording ? "Stop Recording" : "Start Recording");
        record.setEnabled(true);
=======
        record.setToolTipText("Start Recording [" + Keystroke.format(Keystroke.RECORD) + "]");
>>>>>>> cbca25f9 (Shortcut Key Fixes for Start Recording Reload)
    }
}
