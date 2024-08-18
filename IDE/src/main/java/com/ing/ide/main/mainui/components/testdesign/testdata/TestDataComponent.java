
package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.TabTitleEditListener;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.JtableUtils;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Utility;
import com.ing.ide.util.Validator;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *
 *
 */
public class TestDataComponent extends JPanel implements ChangeListener, ActionListener {

    private static final ImageIcon ADD_NEW_TAB_ICON = new ImageIcon(TestDataComponent.class.getResource("/ui/resources/testdesign/testdata/add.png"));

    private final TestDesign testDesign;

    private final XJTabbedPane envTab;

    private final TestDataToolBar toolBar;

    private final TestDataPopupMenu popupMenu;

    private final TestDataTabPopup testDataTabPopup;

    private final TestDataEnvPopup testDataEnvPopup;

    private SaveListener saveListener;

    private final Environment environmentPanel;

    public TestDataComponent(TestDesign sProxy) {
        this.testDesign = sProxy;
        envTab = new XJTabbedPane();
        toolBar = new TestDataToolBar(this);
        popupMenu = new TestDataPopupMenu(this);
        environmentPanel = new Environment(this);
        testDataTabPopup = new TestDataTabPopup();
        testDataEnvPopup = new TestDataEnvPopup();
        init();
    }

    private void init() {
        envTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        envTab.setComponentPopupMenu(testDataEnvPopup);

        TabTitleEditListener l = new TabTitleEditListener(envTab, onTestDataEnvRenameAction());
        envTab.addChangeListener(l);
        envTab.addMouseListener(l);

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(envTab, BorderLayout.CENTER);

        saveListener = new SaveListener() {

            @Override
            public void onSave(Boolean bln) {
                changeSave(bln);
            }
        };
    }

    private Action onTestDataEnvRenameAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String newName = getValue("newValue").toString();
                Boolean returnVal = false;
                if (Validator.isValidName(newName)) {
                    returnVal = renameEnvironment(newName);
                }
                putValue("rename", returnVal);
            }
        };
    }

    private Action onTestDataRenameAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String newName = getValue("newValue").toString();
                Boolean returnVal = false;
                if (Validator.isValidName(newName)) {
                    TestDataTablePanel panel = getSelectedData();
                    if (panel != null) {
                        returnVal = panel.rename(getValue("newValue").toString());
                    }
                }
                putValue("rename", returnVal);
            }
        };
    }

    private Action onCloseAction() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                TestDataTablePanel panel = getSelectedData();
                if (panel != null) {
                    closeTestData(envTab.getSelectedComponent());
                }
            }
        };
    }

    public void load() {
        environmentPanel.reset();
        envTab.removeAll();
        loadTestData();
    }

    private void loadTestData() {
        if (testDesign.getProject() != null) {
            for (TestData sTestData : testDesign.getProject().getTestData().getAllEnvironments()) {
                envTab.addTab(sTestData.getEnviroment(), createNewTestDataTab(sTestData));
            }
            addAddNewTab();
        }
    }

    private JTabbedPane createNewTestDataTab(TestData sTestData) {
        JTabbedPane testdataTab = new JTabbedPane();
        testdataTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        testdataTab.setTabPlacement(JTabbedPane.BOTTOM);
        addToTab(testdataTab, sTestData.getGlobalData(), true);
        for (AbstractDataModel std : sTestData.getTestDataList()) {
            addToTab(testdataTab, std, false);
        }

        JLabel label = new JLabel("Click + to Add New TestData");
        testdataTab.addTab("", ADD_NEW_TAB_ICON, label);
        label.setHorizontalAlignment(JLabel.CENTER);
        TabTitleEditListener l = new TabTitleEditListener(testdataTab, onTestDataRenameAction(), 0);
        l.setOnMiddleClickAction(onCloseAction());
        testdataTab.addChangeListener(l);
        testdataTab.addMouseListener(l);
        testdataTab.addChangeListener(this);
        testdataTab.addMouseListener(onAddNewTDTab());
        testdataTab.setComponentPopupMenu(testDataTabPopup);
        return testdataTab;
    }

    private MouseAdapter onAddNewTDTab() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                JTabbedPane tabbedPane = (JTabbedPane) me.getSource();
                if (tabbedPane.getSelectedIndex() != -1 && getSelectedData() == null) {
                    Rectangle rect = tabbedPane.getUI().
                            getTabBounds(tabbedPane, tabbedPane.getSelectedIndex());
                    if (rect.contains(me.getPoint())) {
                        tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() - 1);
                        addNewTestData(tabbedPane);
                    }
                }
            }
        };
    }

    private void addAddNewTab() {
        envTab.addTab("", ADD_NEW_TAB_ICON, new JScrollPane(environmentPanel));
    }

    private TestDataTablePanel addToTab(JTabbedPane testdataTab, AbstractDataModel std, Boolean isGlobalData) {
        TestDataTablePanel tdPanel = new TestDataTablePanel(std, isGlobalData);
        testdataTab.addTab(std.getName(), tdPanel);
        return tdPanel;
    }

    private TestDataTablePanel addToLastTab(JTabbedPane testdataTab, AbstractDataModel std) {
        TestDataTablePanel tdPanel = new TestDataTablePanel(std);
        testdataTab.insertTab(std.getName(), null, tdPanel, null, testdataTab.getTabCount() - 1);
        testdataTab.setSelectedIndex(testdataTab.getTabCount() - 2);
        return tdPanel;
    }

    public JTabbedPane getTestdataTab() {
        return envTab;
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        onTestDataChange(ce);
    }

    private void onTestDataChange(ChangeEvent ce) {
        TestDataTablePanel panel = getSelectedData();
        if (panel != null) {
            panel.load();
            toolBar.switchOptionsForGlobalData(!panel.isGlobalData);
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            toolBar.setSearchText(panel.std.getName(), envName);
        }
    }

    private void addNewTestData(Object source) {
        JTabbedPane tab = (JTabbedPane) source;
        TestDataModel model = testDesign.getProject().getTestData()
                .getTestDataFor(envTab.getTitleAt(envTab.getSelectedIndex())).addTestData();
        TestCase testcase = testDesign.getTestCaseComp().getCurrentTestCase();
        if (testcase != null) {
            model.addRecord();
            model.getRecords().get(0).setScenario(testcase.getScenario().getName());
            model.getRecords().get(0).setTestcase(testcase.getName());
            model.getRecords().get(0).setIteration("1");
            model.getRecords().get(0).setSubIteration("1");
        }
        addToLastTab(tab, model);
    }

    public void testDataAdded(String env, TestDataModel tdModel) {
        for (int i = 0; i < envTab.getTabCount(); i++) {
            if (envTab.getTitleAt(i).equals(env)) {
                JTabbedPane tab = (JTabbedPane) envTab.getComponentAt(i);
                addToLastTab(tab, tdModel);
            }
        }
    }

    private void searchTestData(Object source) {
        JTabbedPane tab = (JTabbedPane) source;
        List<String> tabs = new ArrayList<>();
        for (int i = 0; i < tab.getTabCount() - 1; i++) {
            tabs.add(tab.getTitleAt(i));
        }
        JComboBox combo = new JComboBox(tabs.toArray());
        int option = JOptionPane.showConfirmDialog(null, combo,
                "Go To TestData",
                JOptionPane.DEFAULT_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            tab.setSelectedIndex(tabs.indexOf(combo.getSelectedItem().toString()));
        }
    }

    private void closeTestData(Object source) {
        JTabbedPane tab = (JTabbedPane) source;
        TestDataTablePanel panel = getSelectedData();
        if (!panel.isGlobalData) {
            int index = tab.getSelectedIndex();
            tab.setSelectedIndex(index - 1);
            tab.removeTabAt(index);
        }
    }

    private void deleteTestData(Object source) {
        JTabbedPane tab = (JTabbedPane) source;
        TestDataTablePanel panel = getSelectedData();
        if (!panel.isGlobalData) {
            int index = tab.getSelectedIndex();
            String name = tab.getTitleAt(index);
            int option = JOptionPane.showConfirmDialog(null, "Are you sure want to delete the TestData [" + name + "]", "Delete TestData", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                Boolean flag = testDesign.getProject().getTestData()
                        .getTestDataFor(envTab.getTitleAt(envTab.getSelectedIndex()))
                        .deleteTestData(name);
                if (flag) {
                    tab.setSelectedIndex(index - 1);
                    tab.removeTabAt(index);
                } else {
                    Notification.show("Couldn't Delete Testdata - '" + name + "'");
                }
            }
        }
    }

    private void reopenTestData() {
        int index = envTab.getSelectedIndex();
        String envName = envTab.getTitleAt(index);
        envTab.removeTabAt(index);
        TestData sTestData = testDesign.getProject().getTestData().getTestDataFor(envName);
        envTab.insertTab(sTestData.getEnviroment(), null, createNewTestDataTab(sTestData), null, index);
        envTab.setSelectedIndex(index);
    }

    private TestDataTablePanel getSelectedData() {
        if (envTab.getSelectedComponent() instanceof JTabbedPane) {
            JTabbedPane tab = (JTabbedPane) envTab.getSelectedComponent();
            if (tab.getTabCount() > 0 && tab.getSelectedComponent() != null
                    && tab.getSelectedComponent() instanceof TestDataTablePanel) {
                return (TestDataTablePanel) tab.getSelectedComponent();
            }
        }
        return null;
    }

    private TestData getCurrentEnviromentData() {
        if (envTab.getSelectedComponent() instanceof JTabbedPane) {
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            return testDesign.getProject().getTestData().getTestDataFor(envName);
        }
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        TestDataTablePanel tdPanel = getSelectedData();
        if (tdPanel != null) {
            switch (ae.getActionCommand()) {
                case "Open with System Editor":
                    tdPanel.openWithSystemEditor();
                    break;
                case "Add Row":
                    tdPanel.insertRowBelow();
                    break;
                case "Delete Rows":
                    tdPanel.deleteSelectedRows();
                    break;
                case "Add Column":
                    tdPanel.addColumn();
                    break;
                case "Delete Columns":
                    tdPanel.deleteSelectedColumns();
                    break;
                case "Create Selected Column's In All Env":
                    addColumnInOtherEnvironement(tdPanel);
                    break;
                case "Create Selected Rows's In All Env":
                    addRowInOtherEnvironement(tdPanel);
                    break;
                case "Save":
                    tdPanel.save();
                    break;
                case "Reload":
                    tdPanel.reload();
                    break;
                case "Search":
                    tdPanel.table.searchFor(((JTextField) ae.getSource()).getText());
                    break;
                case "GoToNextSearch":
                    tdPanel.table.goToNextSearch();
                    break;
                case "GoToPrevoiusSearch":
                    tdPanel.table.goToPrevoiusSearch();
                    break;
                case "Global Data":
                    makeAsGlobalData(tdPanel);
                    break;
                case "Encrypt":
                    tdPanel.encrypt();
                    break;
                case "Cut":
                case "Copy":
                case "Paste":
                    tdPanel.ccp(ae.getActionCommand());
                    break;
                case "Move Rows Up":
                    tdPanel.moveRowUp();
                    break;
                case "Move Rows Down":
                    tdPanel.moveRowDown();
                    break;
                case "Move Column Left":
                    tdPanel.moveColumnLeft();
                    break;
                case "Move Column Right":
                    tdPanel.moveColumnRight();
                    break;
                case "Add New TestData":
                    addNewTestData(envTab.getSelectedComponent());
                    break;
                case "Add In All Env":
                    addInAllEnvironement();
                    break;
                case "Search TestData":
                    searchTestData(envTab.getSelectedComponent());
                    break;
                case "Close TestData":
                    closeTestData(envTab.getSelectedComponent());
                    break;
                case "Delete TestData":
                    deleteTestData(envTab.getSelectedComponent());
                    break;
                case "Reopen Closed":
                    reopenTestData();
                    break;
                case "Get Impacted TestCases":
                    getImpactedTestCases(tdPanel);
                    break;
                case "Go To TestCase":
                    tdPanel.goToSelectedTestCase();
                    break;
            }
        }

    }

    private void changeSave(Boolean saved) {
        toolBar.setSave(!saved);
        popupMenu.setSave(!saved);
    }

    private void makeAsGlobalData(TestDataTablePanel tdPanel) {
        int[] columns = tdPanel.table.getSelectedColumns();
        if (columns != null && columns.length > 0) {
            GlobalDataModel gdModel = getCurrentEnviromentData().getGlobalData();
            Object[] data = addAndGetKeyForGlobalData(gdModel);
            int row = (int) data[1];
            String[] columnNames = new String[columns.length];
            for (int i = 0; i < columns.length; i++) {
                columnNames[i] = tdPanel.table.getColumnName(columns[i]);
            }
            for (String columnName : columnNames) {
                gdModel.addColumn(columnName);
                if (Objects.toString(gdModel.getValueAt(row,
                        gdModel.getColumnIndex(columnName)), "").isEmpty()) {

                    gdModel.setValueAt(
                            tdPanel.table.getValueAt(
                                    tdPanel.table.getSelectedRow(),
                                    tdPanel.std.getColumnIndex(columnName)),
                            row, gdModel.getColumnIndex(columnName));

                }
            }
            tdPanel.makeAsGlobalData(data[0].toString());
        }
    }

    private Object[] addAndGetKeyForGlobalData(GlobalDataModel gdModel) {
        Object[] objects = new Object[2];
        JComboBox jcb = new JComboBox(gdModel.getKeys().toArray());
        jcb.setEditable(true);
        JOptionPane.showMessageDialog(null, jcb, "Select or Enter a GlobalId", JOptionPane.QUESTION_MESSAGE);

        String key = Objects.toString(jcb.getSelectedItem(), "");

        if (key.trim().isEmpty()) {
            key = "#gd1";
        } else if (!key.startsWith("#")) {
            key = "#" + key;
        }
        objects[0] = key;

        if (gdModel.getRowCount() == 0
                || !gdModel.getKeys().contains(key)) {
            gdModel.addRecord();
            gdModel.setValueAt(key, gdModel.getRowCount() - 1, 0);
            objects[1] = gdModel.getRowCount() - 1;
        } else if (gdModel.getKeys().contains(key)) {
            objects[1] = gdModel.getRecordIndexByKey(key);
        }
        return objects;
    }

    Set<String> getListOfEnvironements() {
        return testDesign.getProject().getTestData().getEnvironments();
    }

    List<String> getListOfTestDatas(String env) {
        List<String> tdL = new ArrayList<>();
        for (AbstractDataModel std : testDesign.getProject().getTestData().getTestDataFor(env).getTestDataList()) {
            tdL.add(std.getName());
        }
        return tdL;
    }

    private void addNewEnvironment(TestData sTestData) {
        envTab.insertTab(sTestData.getEnviroment(), null, createNewTestDataTab(sTestData), null, envTab.getTabCount() - 1);
    }

    private void addInAllEnvironement() {
        TestDataTablePanel panel = getSelectedData();
        if (!panel.isGlobalData) {
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            testDesign.getProject().getTestData().duplicateSheetsInOtherEnv(envName, (TestDataModel) panel.std);
            reloadAllExcept(envTab.getSelectedIndex());
        }
    }

    private void reloadAllExcept(int index) {
        for (int i = 0; i < envTab.getTabCount() - 1; i++) {
            if (i != index) {
                reloadEnvironment(i);
            }
        }
    }

    private void reloadEnvironment(int index) {
        if (index != -1) {
            String envName = envTab.getTitleAt(index);
            envTab.removeTabAt(index);
            envTab.insertTab(envName, null,
                    createNewTestDataTab(testDesign.getProject().getTestData().getTestDataFor(envName)), null, index);
        }
    }

    private void reloadEnvironment(String envName) {
        for (int i = 0; i < envTab.getTabCount(); i++) {
            if (envTab.getTitleAt(i).equals(envName)) {
                reloadEnvironment(i);
                break;
            }
        }
    }

    Boolean addNewEnvironment(String envName, String duplicateDataFromEnv,
            List<String> duplicateSheets, Boolean globalDataAsWell) {
        if (testDesign.getProject().getTestData().getTestDataFor(envName) == null) {
            if (duplicateDataFromEnv == null) {
                testDesign.getProject().getTestData().
                        createNewEnvironment(envName);
            } else {
                testDesign.getProject().getTestData().
                        createNewEnvironment(envName, duplicateDataFromEnv,
                                duplicateSheets, globalDataAsWell);
            }
            addNewEnvironment(testDesign.getProject().getTestData().getTestDataFor(envName));
            return true;
        } else {
            Notification.show("An Environment with name '" + envName + "' is already present");
            return false;
        }
    }

    private void addColumnInOtherEnvironement(TestDataTablePanel tdPanel) {
        if (!tdPanel.isGlobalData) {
            List<String> colList = tdPanel.getSelectedColumns();
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            testDesign.getProject().getTestData().duplicateColumnInOtherEnv(envName, (TestDataModel) tdPanel.std, colList);
        }
    }

    private void addRowInOtherEnvironement(TestDataTablePanel tdPanel) {
        if (!tdPanel.isGlobalData) {
            int[] rows = tdPanel.table.getSelectedRows();
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            testDesign.getProject().getTestData().duplicateRowsInOtherEnv(envName, (TestDataModel) tdPanel.std, rows);
        }
    }

    public void switchEnvView() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                envTab.setShowTabsHeader(!envTab.isShowTabsHeader());
                for (int i = 0; i < envTab.getTabCount(); i++) {
                    if (envTab.getTitleAt(i).equals("Default")) {
                        envTab.setSelectedIndex(i);
                    }
                }
                envTab.revalidate();
                envTab.repaint();
            }
        });
    }

    private void renameTestDataTabs(String oldName, String newName) {
        JTabbedPane selectedTab = (JTabbedPane) envTab.getSelectedComponent();
        for (Object object : envTab.getComponents()) {
            if (!object.equals(selectedTab) && object instanceof JTabbedPane) {
                JTabbedPane env = (JTabbedPane) object;
                for (int i = 0; i < env.getTabCount(); i++) {
                    if (env.getTitleAt(i).equals(oldName)) {
                        env.setTitleAt(i, newName);
                    }
                }
            }
        }
    }

    private Boolean renameEnvironment(String newName) {
        String envName = envTab.getTitleAt(envTab.getSelectedIndex());
        if (!envName.equals("Default") && !envName.equals(newName.trim())) {
            return testDesign.getProject().getTestData().renameEnvironment(envName, newName);
        }
        return false;
    }

    private void deleteEnvironment() {
        String envName = envTab.getTitleAt(envTab.getSelectedIndex());
        if (!envName.equals("Default")) {
            int option = JOptionPane.showConfirmDialog(null,
                    "Are you sure want to delete Environment [" + envName + "]", "Delete Environent",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (option == JOptionPane.YES_OPTION) {
                envTab.removeTabAt(envTab.getSelectedIndex());
                testDesign.getProject().getTestData().deleteEnvironment(envName);
            }
        }
    }

    private void getImpactedTestCases(TestDataTablePanel tdPanel) {
        testDesign.getImpactUI().loadForTestData(testDesign.getProject()
                .getImpactedTestDataTestCases(tdPanel.std.getName()), tdPanel.std.getName());
    }

    public Boolean navigateToTestData(String sheetName, String columnName) {
        if (envTab.getSelectedComponent() instanceof JTabbedPane) {
            JTabbedPane tab = (JTabbedPane) envTab.getSelectedComponent();
            for (int i = 0; i < tab.getTabCount(); i++) {
                if (tab.getComponentAt(i) instanceof TestDataTablePanel) {
                    TestDataTablePanel tdPanel = (TestDataTablePanel) tab.getComponentAt(i);
                    if (tdPanel.std.getName().equals(sheetName)) {
                        int colIndex = tdPanel.std.getColumnIndex(columnName);
                        if (colIndex != -1) {
                            tab.setSelectedIndex(i);
                            tdPanel.table.selectColumn(colIndex);
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }

    public void importTestData(File file) {
        String name = org.apache.commons.io.FilenameUtils.getName(file.getName());
        TestDataModel model = getCurrentEnviromentData().getByName(name);
        if (model != null && model.getLocation().equals(file.getAbsolutePath())) {
            Notification.show("Datasheet already Present");
        } else if (model != null) {
            Notification.show("A sheet with name already present.Please rename and import");
        } else {
            model = getCurrentEnviromentData().importTestData(file);
            addToLastTab((JTabbedPane) envTab.getSelectedComponent(), model);
        }
    }

    class TestDataTablePanel extends JPanel {

        AbstractDataModel std;
        XTable table;

        private final Boolean isGlobalData;

        private int previousRowSelection;
        private int previousColumnSelection;

        TestDataAutoSuggest tDAutoSuggest;

        public TestDataTablePanel(AbstractDataModel std, Boolean isGlobalData) {
            this.std = std;
            this.isGlobalData = isGlobalData;
            init();
        }

        public TestDataTablePanel(AbstractDataModel std) {
            this(std, false);
        }

        private void init() {
            table = new XTable() {
                @Override
                public TableCellEditor getCellEditor(int row, int column) {
                    if (!isGlobalData) {
                        return tDAutoSuggest.getCellEditorFor(column, super.getCellEditor(row, column));
                    }
                    return super.getCellEditor(row, column);
                }

            };
            if (isGlobalData) {
                table.setColumnRename(onRenameAction(), 0);
                load();
            } else {
                table.setColumnRename(onRenameAction(), 0, 1, 2, 3);
            }
            tDAutoSuggest = new TestDataAutoSuggest(testDesign.getProject(), table);
            table.setDragEnabled(true);
            table.setTransferHandler(new TestDataDnD());
            table.setComponentPopupMenu(popupMenu);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent me) {
                    if (SwingUtilities.isLeftMouseButton(me) && me.isAltDown()) {
                        goToSelectedTestCase();
                    } else if (SwingUtilities.isLeftMouseButton(me)) {
                        addLastRow();
                    }
                }
            });
            setLayout(new BorderLayout());
            add(new JScrollPane(table));
            addTableProps();
            std.setSaveListener(saveListener);
        }

        private void load() {
            std.loadTableModel();
            table.setModel(std);
            changeSave(std.isSaved());
        }

        private Action onRenameAction() {
            return new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    assignThePreviouslySelected();
                    Boolean flag = testDesign.getProject().getTestData()
                            .renameTestDataColumn(std.getName(),
                                    getValue("oldvalue").toString(),
                                    getValue("newvalue").toString());
                    putValue("rename", flag);
                    if (flag) {
                        selectThePreviouslySelected();
                    }
                }

            };
        }

        private void makeAsGlobalData(String key) {
            std.getUndoManager().startGroupEdit();
            int[] columns = table.getSelectedColumns();
            for (int i = 0; i < columns.length; i++) {
                for (int row : table.getSelectedRows()) {
                    table.setValueAt(key, row, columns[i]);
                }
            }
            std.getUndoManager().stopGroupEdit();
        }

        private void encrypt() {
            int[] cols = table.getSelectedColumns();
            int[] rows = table.getSelectedRows();
            for (int row : rows) {
                for (int col : cols) {
                    if (row != -1 && col != -1) {
                        if (isGlobalData) {
                            if (col == 0) {
                                continue;
                            }
                        } else if (col < 4) {
                            continue;
                        }
                        String data = Objects.toString(table.getValueAt(row, col), "");
                        if (data != null && !data.isEmpty()) {
                            table.setValueAt(Utility.encrypt(data), row, col);
                        }
                    }
                }
            }

        }

        private void moveColumnLeft() {
            try {

                int sCol = table.getSelectedColumn();
                if (sCol>=5){
                    TableModel model = table.getModel();
                    table.moveColumn(sCol, sCol - 1);
                    TableModel customModel = createCustomTableModel(model, table.getColumnModel());
                    saveChanges(customModel);
                    std.setSaved(true);
                    }
                
            } catch (Exception e) {
                Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, e);
            }

        }

        private void moveColumnRight() {
            try {

                int sCol = table.getSelectedColumn();
                if (sCol>=4){
                    TableModel model = table.getModel();
                    TableModel customModel = createCustomTableModel(model, table.getColumnModel());
                    table.moveColumn(sCol, sCol + 1);
                    saveChanges(customModel);
                    std.setSaved(true);
                }

            } catch (Exception e) {
                Logger.getLogger(JtableUtils.class.getName()).log(Level.SEVERE, null, e);
            }

        }

        private void moveRowUp() {
            if (table.getSelectedRows().length > 0) {
                List<Integer> rows = Utils.getSorted(table.getSelectedRows());
                int from = rows.get(0);
                int to = rows.get(rows.size() - 1);
                if (std.moveRowsUp(from, to)) {
                    table.getSelectionModel().setSelectionInterval(from - 1, to - 1);
                }
            }
        }

        private void moveRowDown() {
            if (table.getSelectedRows().length > 0) {
                List<Integer> rows = Utils.getSorted(table.getSelectedRows());
                int from = rows.get(0);
                int to = rows.get(rows.size() - 1);
                if (std.moveRowsDown(from, to)) {
                    table.getSelectionModel().setSelectionInterval(from + 1, to + 1);
                }
            }
        }

        private void ccp(String type) {
            switch (type) {
                case "Cut":
                    table.cut();
                    break;
                case "Copy":
                    table.copy();
                    break;
                case "Paste":
                    table.paste();
                    break;
            }
        }

        private Boolean rename(String newName) {
            String oldName = std.getName();
            if (testDesign.getProject().getTestData().renameTestData(oldName, newName)) {
                renameTestDataTabs(oldName, newName);
                return true;
            } else {
                Notification.show("A TestData with name '" + newName + "' is present already");
            }
            return false;
        }

        private void save() {
            stopCellEditing();
            std.save();
        }

        private void reload() {
            stopCellEditing();
            std.load();
        }

        private void addLastRow() {
            int row = table.getSelectedRow();
            int column = table.getSelectedColumn();
            if (row == table.getRowCount() - 1
                    && column == table.getColumnCount() - 1) {
                addRow();
            }
        }

        private void addRow() {
            stopCellEditing();
            std.addRecord();
        }

        private void insertRowBelow() {
            stopCellEditing();
            if (table.getSelectedRow() != -1
                    && table.getSelectedRow() + 1 < table.getRowCount()) {
                std.addRecord(table.getSelectedRow() + 1);
            } else {
                std.addRecord();
            }
        }

        private void insertRow() {
            stopCellEditing();
            if (table.getSelectedRow() != -1) {
                std.addRecord(table.getSelectedRow());
            }
        }

        private void replicateRow() {
            stopCellEditing();
            if (table.getSelectedRow() != -1) {
                std.replicateRecord(table.getSelectedRow());
            }
        }

        private void addColumn() {
            assignThePreviouslySelected();
            stopCellEditing();
            std.addColumn();
            selectThePreviouslySelected();
        }

        private void clearValues() {
            stopCellEditing();
            if (table.getSelectedRowCount() > 0) {
                std.clearValues(
                        table.getSelectedRows(),
                        table.getSelectedColumns());
            }
        }

        private void deleteSelectedRows() {
            stopCellEditing();
            List<Integer> rowList = Utils.getReverseSorted(table.getSelectedRows());
            std.getUndoManager().startGroupEdit();
            for (Integer row : rowList) {
                std.removeRecord(row);
            }
            std.getUndoManager().stopGroupEdit();
        }

        private void deleteSelectedColumns() {
            stopCellEditing();
            List<Integer> colList = Utils.getReverseSorted(table.getSelectedColumns());
            if (!colList.isEmpty()) {
                if (isGlobalData) {
                    colList.remove(Integer.valueOf(0));
                } else {
                    colList.remove(Integer.valueOf(0));
                    colList.remove(Integer.valueOf(1));
                    colList.remove(Integer.valueOf(2));
                    colList.remove(Integer.valueOf(3));

                }
                std.removeColumn(colList);
            }
        }

        private List<String> getSelectedColumns() {
            List<String> colList = new ArrayList<>();
            for (int col : table.getSelectedColumns()) {
                colList.add(std.getColumnName(col));
            }
            return colList;
        }
        
         private TableModel createCustomTableModel(TableModel originalModel, TableColumnModel columnModel) {
            AbstractTableModel customModel = new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                return columnModel.getColumnCount();
            }

            @Override
            public int getRowCount() {
                return originalModel.getRowCount();
            }

            @Override
            public Object getValueAt(int row, int column) {
                int originalColumn = columnModel.getColumn(column).getModelIndex();
                return originalModel.getValueAt(row, originalColumn);
            }

            @Override
            public String getColumnName(int column) {
                int originalColumn = columnModel.getColumn(column).getModelIndex();
                return originalModel.getColumnName(originalColumn);
            }

            @Override
            public Class<?> getColumnClass(int column) {
                int originalColumn = columnModel.getColumn(column).getModelIndex();
                return originalModel.getColumnClass(originalColumn);
            }

            // Override other necessary methods based on your requirements
        };

        return customModel;
    }

        private void addTableProps() {

            table.setActionFor("MoveUp", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveRowUp();
                }
            });
            table.setActionFor("MoveDown", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveRowDown();
                }
            });
            table.setActionFor("MoveRight", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveColumnRight();
                }
            });
            table.setActionFor("MoveLeft", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moveColumnLeft();
                }
            });
            table.setActionFor("Insert", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    insertRow();
                }
            });
            table.setActionFor("Add", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addRow();
                }
            });
            table.setActionFor("Delete", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteSelectedRows();
                }
            });
            table.setActionFor("Clear", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    clearValues();
                }
            });
            table.setActionFor("Add Column", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addColumn();
                }
            });
            table.setActionFor("Delete Column", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteSelectedColumns();
                }
            });
            table.setActionFor("Replicate", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    replicateRow();
                }
            });
            table.setActionFor("Save", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    save();
                }
            });
            table.setActionFor("Reload", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    reload();
                }
            });
            table.setActionFor("Open", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openWithSystemEditor();
                }

            });
            table.setActionFor("Search", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toolBar.focusSearch();
                }
            });

            table.setActionFor("Copy Above", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    copyAbove();
                }
            });

        }

        private void copyAbove() {
            stopCellEditing();
            int row = table.getSelectedRow();
            if (row > 0) {
                for (int col : table.getSelectedColumns()) {
                    String value = Objects.toString(table.getValueAt(row - 1, col), "");
                    table.setValueAt(value, row, col);
                }
            }
        }

        private void openWithSystemEditor() {
            save();
            Utils.openWithSystemEditor(std.getLocation());
        }

        private void goToSelectedTestCase() {
            if (!isGlobalData) {
                if (table.getSelectedRow() != -1) {
                    Boolean invalid = false;
                    String scenVal = Objects.toString(
                            table.getValueAt(table.getSelectedRow(), 0), "");
                    String tcVal = Objects.toString(
                            table.getValueAt(table.getSelectedRow(), 1), "");
                    if (!scenVal.isEmpty() && !tcVal.isEmpty()) {
                        Scenario scenario = testDesign.getProject()
                                .getScenarioByName(scenVal);
                        if (scenario != null) {
                            TestCase testCase = scenario.getTestCaseByName(tcVal);
                            if (testCase != null) {
                                testDesign.loadTableModelForSelection(testCase);
                            } else {
                                invalid = true;
                            }
                        } else {
                            invalid = true;
                        }
                    } else {
                        invalid = true;
                    }
                    if (invalid) {
                        Notification.show("Testcase "
                                + "[" + scenVal + ":" + tcVal + "]"
                                + " not available in Project");
                    }
                }
            }
        }

        private void stopCellEditing() {
            if (table.getCellEditor() != null) {
                table.getCellEditor().stopCellEditing();
            }
        }

        private void assignThePreviouslySelected() {
            previousRowSelection = table.getSelectedRow();
            previousColumnSelection = table.getSelectedColumn();
        }

        private void selectThePreviouslySelected() {
            if (previousRowSelection != -1 && previousColumnSelection != -1) {
                table.setRowSelectionInterval(previousRowSelection, previousRowSelection);
                table.setColumnSelectionInterval(previousColumnSelection, previousColumnSelection);
            }
        }
        

        

        private void saveChanges(TableModel model) {
            System.out.println(std.getLocation());
            try (FileWriter writer = new FileWriter(std.getLocation())) {

                // Write the column headers
                for (int column = 0; column < model.getColumnCount(); column++) {
                    writer.append(model.getColumnName(column));
                    
                    if (column < model.getColumnCount() - 1) {
                        writer.append(',');
                    } else {
                        writer.append('\n');
                    }
                }

                // Write the data rows
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int column = 0; column < model.getColumnCount(); column++) {
                        Object value = model.getValueAt(row, column);
                        if (value != null) {
                            writer.append(value.toString());
                        }

                        if (column < model.getColumnCount() - 1) {
                            writer.append(',');
                        } else {
                            writer.append('\n');
                        }
                    }
                }

                writer.flush();
               
            } catch (IOException e) {
                System.err.println("Error exporting table data to CSV: " + e.getMessage());
            }

        }

    }

    class TestDataTabPopup extends JPopupMenu {

        JMenuItem addNew;
        JMenuItem addInAll;
        JMenuItem search;
        JMenuItem close;
        JMenuItem delete;
        JMenuItem reopen;
        JMenuItem impactAnalysis;

        public TestDataTabPopup() {
            init();
        }

        private void init() {
            addNew = new JMenuItem("Add New");
            addNew.setActionCommand("Add New TestData");
            addNew.setIcon(Canvas.EmptyIcon);

            addInAll = new JMenuItem("Add In All Env");
            addInAll.setActionCommand("Add In All Env");

            search = new JMenuItem("Search TestData");
            search.setActionCommand("Search TestData");

            close = new JMenuItem("Close");
            close.setActionCommand("Close TestData");
            delete = new JMenuItem("Delete");
            delete.setActionCommand("Delete TestData");

            reopen = new JMenuItem("Reopen Closed");
            reopen.setActionCommand("Reopen Closed");

            impactAnalysis = new JMenuItem("Get Impacted TestCases");

            addNew.addActionListener(TestDataComponent.this);
            addInAll.addActionListener(TestDataComponent.this);
            search.addActionListener(TestDataComponent.this);
            close.addActionListener(TestDataComponent.this);
            delete.addActionListener(TestDataComponent.this);
            reopen.addActionListener(TestDataComponent.this);
            impactAnalysis.addActionListener(TestDataComponent.this);

            add(addNew);
            add(addInAll);
            addSeparator();
            add(search);
            addSeparator();
            add(close);
            add(delete);
            addSeparator();
            add(impactAnalysis);
            addSeparator();

            add(reopen);
        }

    }

    class TestDataEnvPopup extends JPopupMenu implements ActionListener {

        JMenuItem addNew;
        JMenuItem close;
        JMenuItem delete;
        JMenuItem reopen;

        public TestDataEnvPopup() {
            init();
        }

        private void init() {
            addNew = new JMenuItem("Add New");
            addNew.setActionCommand("Add New Enivronment");

            addNew.setIcon(Canvas.EmptyIcon);

            close = new JMenuItem("Close");
            close.setActionCommand("Close Enivronment");
            delete = new JMenuItem("Delete");
            delete.setActionCommand("Delete Enivronment");

            reopen = new JMenuItem("Reopen Closed");
            reopen.setActionCommand("Reopen Closed Enivronment");

            addNew.addActionListener(this);
            close.addActionListener(this);
            delete.addActionListener(this);
            reopen.addActionListener(this);

            add(addNew);
            addSeparator();
            add(close);
            add(delete);

            addSeparator();
            add(reopen);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            switch (ae.getActionCommand()) {
                case "Add New Enivronment":
                    envTab.setSelectedIndex(envTab.getTabCount() - 1);
                    environmentPanel.selectTextBox();
                    break;
                case "Close Enivronment":
                    if (envTab.getSelectedIndex() != envTab.getTabCount() - 1) {
                        if (envTab.getTabCount() > 2) {
                            envTab.removeTabAt(envTab.getSelectedIndex());
                        }
                    }
                    break;
                case "Delete Enivronment":
                    deleteEnvironment();
                    break;
                case "Reopen Closed Enivronment":
                    load();
                    break;
            }

        }
    }

}
