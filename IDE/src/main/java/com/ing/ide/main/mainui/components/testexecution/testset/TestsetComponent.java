
package com.ing.ide.main.mainui.components.testexecution.testset;

import com.ing.datalib.component.ExecutionStep;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.RunManager;
import com.ing.ide.main.mainui.EngineConfig;
import com.ing.ide.main.mainui.components.testexecution.TestExecution;
import com.ing.ide.main.mainui.components.testexecution.quickSettings.QuickSettings;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.main.utils.table.TableCheckBoxColumn;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Notification;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

/**
 *
 * 
 */
public class TestsetComponent extends JPanel implements ActionListener {

    private final TestExecution testExecution;

    private final TestSetToolBar toolBar;
    private final TestSetPopupMenu popupMenu;
    private final TestSetValidator validator;

    private final QuickSettings quickSettings;

    private final XTable testSetTable;

    private final ExecutePopupMenu executePopupMenu;

    private TestSetAutoSuggest tsAutoSuggest;

    private SaveListener saveListener;

    private Thread runner;

    public TestsetComponent(TestExecution testExecution) {
        this.testExecution = testExecution;
        testSetTable = new XTable() {

            @Override
            public Component prepareEditor(TableCellEditor editor, int row, int column) {
                Component c = super.prepareEditor(editor, row, column);
                if (c instanceof JCheckBox) {
                    JCheckBox b = (JCheckBox) c;
                    b.setBackground(getSelectionBackground());
                    b.setBorderPainted(true);
                }
                return c;
            }

        };
        toolBar = new TestSetToolBar(this);
        validator = new TestSetValidator(testSetTable);
        popupMenu = new TestSetPopupMenu(this);
        quickSettings = new QuickSettings(this);
        executePopupMenu = new ExecutePopupMenu();
        init();
    }

    private void init() {
        new StatusSorter();
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(testSetTable), BorderLayout.CENTER);
        testSetTable.setComponentPopupMenu(popupMenu);
        initTableListeners();
        initRunner();
        quickSettings.setOnUpdate(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (QuickSettings.Actions.SAVE.name().equals(ae.getActionCommand())) {
                    changeSave(true);
                } else {
                    changeSave(false);
                }
            }
        });
    }

    public JTable getTestSetTable() {
        return testSetTable;
    }

    public void loadTableModelForSelection(Object obj) {
        if (obj instanceof TestSet) {
            TestSet tc = (TestSet) obj;
            tc.setSaveListener(saveListener);
            getTestSetTable().setModel(testExecution.getProject().getTableModelFor(tc));
            TableCheckBoxColumn.installFor(testSetTable, executePopupMenu, 0);
            tsAutoSuggest.installForTestSet();
            validator.init();
            changeSave(tc.isSaved());
            reloadSettings();
            refreshTitle();
        }
    }

    public void reloadSettings() {
        if (getCurrentTestSet() != null) {
            quickSettings.loadSett(getCurrentTestSet().getExecSettings().getRunSettings());
        }
    }

    public void resetTable() {
        getTestSetTable().setModel(new DefaultTableModel());
        toolBar.setPlaceHolderText("");
        changeSave(true);
    }

    public void refreshTitle() {
        toolBar.setPlaceHolderText(getCurrentTestSet().getRelease().getName() + " - " + getCurrentTestSet().getName());
    }

    public void load() {
        tsAutoSuggest = new TestSetAutoSuggest(testExecution.getProject(), testSetTable);
        loadBrowsers();
    }

    public void loadBrowsers() {
        popupMenu.loadBrowsers(testExecution.getProject().getProjectSettings().getEmulators().getEmulatorNames());
        tsAutoSuggest.loadBrowsers();
    }

    private void initTableListeners() {
        testSetTable.setActionFor("Insert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertRow();
            }
        });
        testSetTable.setActionFor("Add", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addRow();
            }
        });
        testSetTable.setActionFor("Delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedRows();
            }
        });
        testSetTable.setActionFor("Clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                clearValues();
            }
        });
        testSetTable.setActionFor("Replicate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                replicateRow();
            }
        });
        testSetTable.setActionFor("Save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        testSetTable.setActionFor("Reload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });
        testSetTable.setActionFor("Open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openWithSystemEditor();
            }

        });
        testSetTable.setActionFor("Search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toolBar.focusSearch();
            }
        });

        testSetTable.setActionFor("Copy Above", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyAbove();
            }
        });

        testSetTable.setActionFor("MoveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowUp();
            }
        });
        testSetTable.setActionFor("MoveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowDown();
            }
        });
        testSetTable.setKeyStrokeFor("RunTestSet", Keystroke.F6);
        testSetTable.setActionFor("RunTestSet", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run();
            }
        });
        saveListener = new SaveListener() {
            @Override
            public void onSave(Boolean bln) {
                changeSave(bln);
            }
        };

        testSetTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isLeftMouseButton(me) && me.isAltDown()) {
                    goToSelectedTestCase();
                }
            }

        });

    }

    private void initRunner() {
        runner = new Thread(new Runnable() {
            @Override
            public void run() {
                toolBar.stopMode();
                RunManager.getGlobalSettings().setFor(getCurrentTestSet());
                testExecution.getTestExecutionUI().getConsolePanel().start();
                EngineConfig.runProject(testExecution.getProject());
                toolBar.startMode();
            }
        });
    }

    private void changeSave(Boolean bln) {
        toolBar.setSave(!bln);
        popupMenu.setSave(!bln);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (getCurrentTestSet() != null) {
            switch (ae.getActionCommand()) {
                case "Open with System Editor":
                    openWithSystemEditor();
                    break;
                case "Add Row":
                    insertRowBelow();
                    break;
                case "Delete Rows":
                    deleteSelectedRows();
                    break;
                case "Save":
                    save();
                    break;
                case "Reload":
                    reload();
                    break;
                case "Search":
                    testSetTable.searchFor(((JTextField) ae.getSource()).getText());
                    break;
                case "GoToNextSearch":
                    testSetTable.goToNextSearch();
                    break;
                case "GoToPrevoiusSearch":
                    testSetTable.goToPrevoiusSearch();
                    break;
                case "ChangeBrowser":
                    changeBrowser(((JMenuItem) ae.getSource()).getText());
                    break;
                case "Cut":
                case "Copy":
                case "Paste":
                    ccp(ae.getActionCommand());
                    break;
                case "Move Rows Up":
                    moveRowUp();
                    break;
                case "Move Rows Down":
                    moveRowDown();
                    break;
                case "Console":
                    toggleConsole(ae.getSource());
                    break;
                case "Run":
                    run();
                    break;
                case "StopRun":
                    stopExecution();
                    break;
                case "Go To TestCase":
                    goToSelectedTestCase();
                    break;
                case "Change Status":
                    changeStatus(((JMenuItem) ae.getSource()).getText());
                    break;
                case "ExecuteAll":
                    selectByStatus(".*");
                    break;
                case "ExecuteNone":
                    selectByStatus("$$$");
                    break;
                case "ExecutePassed":
                    selectByStatus("Passed");
                    break;
                case "ExecuteFailed":
                    selectByStatus("Failed");
                    break;
                case "ExecuteNoRun":
                    selectByStatus("NoRun");
                    break;
                default:
                    throw new UnsupportedOperationException(ae.getActionCommand());
            }
        } else {
            Notification.show("Please select a valid TestSet");
        }
    }

    public TestSet getCurrentTestSet() {
        if (testSetTable.getModel() instanceof TestSet) {
            return (TestSet) testSetTable.getModel();
        }
        return null;
    }

    private void stopCellEditing() {
        if (testSetTable.getCellEditor() != null) {
            testSetTable.getCellEditor().stopCellEditing();
        }
    }

    private void changeBrowser(String browserName) {
        stopCellEditing();
        if (testSetTable.getSelectedRow() != -1) {
            for (int row : testSetTable.getSelectedRows()) {
                getCurrentTestSet().getTestSteps().get(row).setBrowser(browserName);
                getCurrentTestSet().fireTableCellUpdated(row, ExecutionStep.HEADERS.Browser.getIndex());
            }
        }
    }

    private void insertRow() {
        stopCellEditing();
        if (testSetTable.getSelectedRow() != -1) {
            getCurrentTestSet().addNewStepAt(testSetTable.getSelectedRow());
        }
    }

    public ExecutionStep getSelectedStep() {
        if (testSetTable.getSelectedRow() != -1) {
            return getCurrentTestSet().getTestSteps().get(testSetTable.getSelectedRow());
        }
        if (testSetTable.getRowCount() > 0) {
            return getCurrentTestSet().getTestSteps().get(testSetTable.getRowCount() - 1);
        }
        return null;
    }

    public ExecutionStep insertRowBelow() {
        stopCellEditing();
        if (testSetTable.getSelectedRow() != -1
                && testSetTable.getSelectedRow() + 1 < testSetTable.getRowCount()) {
            return getCurrentTestSet().addNewStepAt(testSetTable.getSelectedRow() + 1);
        } else {
            return getCurrentTestSet().addNewStep();
        }
    }

    private ExecutionStep addRow() {
        stopCellEditing();
        return getCurrentTestSet().addNewStep();
    }

    private void replicateRow() {
        stopCellEditing();
        if (testSetTable.getSelectedRow() != -1) {
            getCurrentTestSet().replicateStepAt(testSetTable.getSelectedRow());
        }
    }

    private void copyAbove() {
        stopCellEditing();
        int row = testSetTable.getSelectedRow();
        if (row > 0) {
            for (int col : testSetTable.getSelectedColumns()) {
                String value = Objects.toString(testSetTable.getValueAt(row - 1, col), "");
                testSetTable.setValueAt(value, row, col);
            }
        }
    }

    private void moveRowUp() {
        stopCellEditing();
        if (testSetTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testSetTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            if (getCurrentTestSet().moveRowsUp(from, to)) {
                testSetTable.getSelectionModel().setSelectionInterval(from - 1, to - 1);
            }
        }
    }

    private void moveRowDown() {
        stopCellEditing();
        if (testSetTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testSetTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            if (getCurrentTestSet().moveRowsDown(from, to)) {
                testSetTable.getSelectionModel().setSelectionInterval(from + 1, to + 1);
            }
        }
    }

    private void clearValues() {
        stopCellEditing();
        if (testSetTable.getSelectedRowCount() > 0) {
            getCurrentTestSet().clearValues(
                    testSetTable.getSelectedRows(),
                    testSetTable.getSelectedColumns());
        }
    }

    private void deleteSelectedRows() {
        stopCellEditing();
        if (testSetTable.getSelectedRows().length > 0) {
            getCurrentTestSet().removeSteps(Utils.getReverseSorted(testSetTable.getSelectedRows()));
        }
    }

    private void openWithSystemEditor() {
        save();
        Utils.openWithSystemEditor(getCurrentTestSet().getLocation());
    }

    private void toggleConsole(Object source) {
        testExecution.getTestExecutionUI().toggleConsolePanel(source);
    }

    private void changeStatus(String status) {
        stopCellEditing();
        if (testSetTable.getSelectedRows().length > 0) {
            for (int row : testSetTable.getSelectedRows()) {
                getCurrentTestSet().getTestSteps().get(row).setStatus(status);
                getCurrentTestSet().fireTableCellUpdated(row,
                        ExecutionStep.HEADERS.Status.getIndex());
            }
        }
    }

    private void selectByStatus(String status) {
        for (ExecutionStep step : getCurrentTestSet().getTestSteps()) {
            if (step.getStatus().matches(status)) {
                step.setExecute("true");
            } else {
                step.setExecute("false");
            }
        }
        getCurrentTestSet().fireTableDataChanged();
    }

    private void save() {
        stopCellEditing();
        if (validator.validateSteps()) {
            quickSettings.save();
            getCurrentTestSet().save();
        }
    }

    private void reload() {
        stopCellEditing();
        getCurrentTestSet().reload();
        tsAutoSuggest.installForTestSet();
        TableCheckBoxColumn.installFor(testSetTable, executePopupMenu, 0);
        validator.init();
    }

    private void goToSelectedTestCase() {
        if (testSetTable.getSelectedRow() != -1) {
            TestCase testCase = getSelectedStep().getTestCase();
            if (testCase != null) {
                testExecution.getsMainFrame().showTestDesign();
                testExecution.getsMainFrame().
                        getTestDesign().loadTableModelForSelection(testCase);
            } else {
                Notification.show("Testcase not present in the project");
            }
        }
    }

    private void ccp(String operation) {
        switch (operation) {
            case "Cut":
                testSetTable.cut();
                break;
            case "Copy":
                testSetTable.copy();
                break;
            case "Paste":
                testSetTable.paste();
                break;
        }
    }

    private void run() {
        if (!runner.isAlive()) {
            save();
            if (getCurrentTestSet().getRowCount() > 0) {
                if (!getCurrentTestSet().getExecutableSteps().isEmpty()) {
                    getCurrentTestSet().getProject().save();
                    SystemDefaults.debugMode.set(false);
                    initRunner();
                    runner.start();
                } else {
                    Notification.show("No Executable Steps are present in the testSet");
                    testExecution.getsMainFrame().getLoader().showIDontCare();
                }
            } else {
                Notification.show("No Steps are present in the testSet");
            }
        } else {
            Notification.show("Already Running");
        }
    }

    private void stopExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(false);
            SystemDefaults.stopCurrentIteration.set(true);
            SystemDefaults.stopExecution.set(true);
        }
    }

    public void pullTestCases(List<TestCase> testCases) {
        for (TestCase testCase : testCases) {
            ExecutionStep step = addRow();
            step.setTestScenario(testCase.getScenario().getName());
            step.setTestCase(testCase.getName());
            step.setBrowser(testExecution.getDefaultBrowser());
        }
    }

    public Project getProject() {
        return testExecution.getProject();
    }

    public QuickSettings getQuickSettings() {
        return quickSettings;
    }

    class ExecutePopupMenu extends JPopupMenu {

        public ExecutePopupMenu() {
            add(create("Select All", "ExecuteAll"));
            add(create("Select None", "ExecuteNone"));
            addSeparator();
            add(create("Select Passed", "ExecutePassed"));
            add(create("Select Failed", "ExecuteFailed"));
            add(create("Select NoRun", "ExecuteNoRun"));
        }

        private JMenuItem create(String text, String actionCommand) {
            JMenuItem menuItem = Utils.createMenuItem(text, TestsetComponent.this);
            menuItem.setActionCommand(actionCommand);
            return menuItem;
        }

    }

    class StatusSorter {

        String[] statuses = new String[]{"NoRun", "Passed", "Failed", ".*"};
        int currentStatus = 0;

        public StatusSorter() {
            testSetTable.getTableHeader().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    TableColumnModel columnModel = testSetTable.getColumnModel();
                    int vci = columnModel.getColumnIndexAtX(me.getX());
                    int mci = testSetTable.convertColumnIndexToModel(vci);
                    if (mci == ExecutionStep.HEADERS.Status.getIndex()) {
                        sort();
                    }
                }
            });
        }

        private void sort() {
            Collections.sort(getCurrentTestSet().getTestSteps(), getStepComparator());
            Collections.rotate(getCurrentTestSet().getTestSteps(), getStatusIndex());
            getCurrentTestSet().fireTableDataChanged();
            currentStatus = ++currentStatus % statuses.length;
        }

        private int getStatusIndex() {
            for (int i = 0; i < getCurrentTestSet().getRowCount(); i++) {
                ExecutionStep step = getCurrentTestSet().getTestSteps().get(i);
                if (step.getStatus().matches(statuses[currentStatus])) {
                    return -i;
                }
            }
            return 0;
        }

        private Comparator getStepComparator() {
            return new Comparator<ExecutionStep>() {
                @Override
                public int compare(ExecutionStep t, ExecutionStep t1) {
                    return t.getStatus().compareTo(t1.getStatus());
                }
            };
        }
    }
}
