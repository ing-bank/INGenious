package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.TabTitleEditListener;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.FrozenColumnScrollPane;
import com.ing.ide.main.utils.table.JTableUtils;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.main.utils.table.XTableUtils;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Utility;
import com.ing.ide.util.Validator;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
    private static final javax.swing.Icon ADD_NEW_TAB_ICON = INGIcons.swingColored("icon.add", 16);
    private static final String TAB_ORDER_SEPARATOR = "\u001F";
    private static final String ENV_TAB_ORDER_KEY = "ui.testdata.env.order";
    private static final String TESTDATA_TAB_ORDER_PREFIX = "ui.testdata.env.tabs.";

    private final TestDesign testDesign;

    private final XJTabbedPane envTab;

    private final TestDataToolBar toolBar;

    private final TestDataPopupMenu popupMenu;

    private final TestDataTabPopup testDataTabPopup;

    private final TestDataEnvPopup testDataEnvPopup;

    private SaveListener saveListener;

    private final StylizedEnvironment environmentPanel;

    public TestDataComponent(TestDesign sProxy) {
        this.testDesign = sProxy;
        envTab = new XJTabbedPane();
        toolBar = new TestDataToolBar(this);
        popupMenu = new TestDataPopupMenu(this);
        environmentPanel = new StylizedEnvironment(this);
        testDataTabPopup = new TestDataTabPopup();
        testDataEnvPopup = new TestDataEnvPopup();
        init();
    }

    private void init() {
        envTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        envTab.setComponentPopupMenu(testDataEnvPopup);
        envTab.setBackground(UIManager.getColor("Panel.background"));
        TabReorderSupport.install(
            envTab,
            index -> index < envTab.getTabCount() - 1,
            this::persistEnvironmentTabOrder
        );

        TabTitleEditListener l = new TabTitleEditListener(envTab, onTestDataEnvRenameAction());
        envTab.addChangeListener(l);
        envTab.addMouseListener(l);

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));
        add(toolBar, BorderLayout.NORTH);
        add(envTab, BorderLayout.CENTER);

        saveListener =
            new SaveListener() {

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
            for (TestData sTestData : getEnvironmentsInSavedOrder()) {
                envTab.addTab(sTestData.getEnviroment(), createNewTestDataTab(sTestData));
            }
            addAddNewTab();
        }
    }

    /**
     * Returns environments ordered by the saved UI tab order.
     *
     * <p>Environments not present in the saved order are appended in their current natural
     * iteration order, preserving behavior when environments are added later.</p>
     *
     * @return ordered environment test data list
     */
    private List<TestData> getEnvironmentsInSavedOrder() {
        List<TestData> allEnvironments = new ArrayList<>(
            testDesign.getProject().getTestData().getAllEnvironments()
        );
        List<String> savedOrder = getSavedOrder(ENV_TAB_ORDER_KEY);
        if (savedOrder.isEmpty()) {
            return allEnvironments;
        }

        Map<String, TestData> byName = new LinkedHashMap<>();
        for (TestData env : allEnvironments) {
            byName.put(env.getEnviroment(), env);
        }

        List<TestData> ordered = new ArrayList<>();
        for (String envName : savedOrder) {
            TestData td = byName.remove(envName);
            if (td != null) {
                ordered.add(td);
            }
        }
        ordered.addAll(byName.values());
        return ordered;
    }

    private JTabbedPane createNewTestDataTab(TestData sTestData) {
        JTabbedPane testdataTab = new JTabbedPane();
        testdataTab.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        testdataTab.setTabPlacement(JTabbedPane.BOTTOM);
        testdataTab.setBackground(UIManager.getColor("Panel.background"));

        List<AbstractDataModel> orderedModels = getTestDataModelsInSavedOrder(sTestData);
        addToTab(testdataTab, sTestData.getGlobalData(), true);
        for (AbstractDataModel std : orderedModels) {
            addToTab(testdataTab, std, false);
        }

        JLabel label = new JLabel("Click + to Add New TestData");
        label.setBackground(UIManager.getColor("Panel.background"));
        label.setOpaque(true);
        testdataTab.addTab("", ADD_NEW_TAB_ICON, label);
        label.setHorizontalAlignment(JLabel.CENTER);
        TabReorderSupport.install(
            testdataTab,
            index -> index > 0 && index < testdataTab.getTabCount() - 1,
            () -> persistTestDataTabOrder(getEnvironmentNameFor(testdataTab), testdataTab)
        );
        TabTitleEditListener l = new TabTitleEditListener(testdataTab, onTestDataRenameAction(), 0);
        l.setOnMiddleClickAction(onCloseAction());
        testdataTab.addChangeListener(l);
        testdataTab.addMouseListener(l);
        testdataTab.addChangeListener(this);
        testdataTab.addMouseListener(onAddNewTDTab());
        testdataTab.setComponentPopupMenu(testDataTabPopup);
        return testdataTab;
    }

    /**
     * Returns test data models for an environment ordered by the saved tab order.
     *
     * <p>Saved names that no longer exist are ignored. Models not present in saved order are
     * appended at the end in current iteration order.</p>
     *
     * @param sTestData environment that owns test data models
     * @return ordered test data models
     */
    private List<AbstractDataModel> getTestDataModelsInSavedOrder(TestData sTestData) {
        List<AbstractDataModel> models = new ArrayList<>(sTestData.getTestDataList());
        List<String> savedOrder = getSavedOrder(getTestDataTabOrderKey(sTestData.getEnviroment()));
        if (savedOrder.isEmpty()) {
            return models;
        }

        Map<String, AbstractDataModel> byName = new LinkedHashMap<>();
        for (AbstractDataModel model : models) {
            byName.put(model.getName(), model);
        }

        List<AbstractDataModel> ordered = new ArrayList<>();
        for (String name : savedOrder) {
            AbstractDataModel model = byName.remove(name);
            if (model != null) {
                ordered.add(model);
            }
        }
        ordered.addAll(byName.values());
        return ordered;
    }

    private String getEnvironmentNameFor(JTabbedPane testdataTab) {
        for (int i = 0; i < envTab.getTabCount(); i++) {
            if (envTab.getComponentAt(i) == testdataTab) {
                return envTab.getTitleAt(i);
            }
        }
        return null;
    }

    private void persistEnvironmentTabOrder() {
        List<String> order = new ArrayList<>();
        for (int i = 0; i < envTab.getTabCount() - 1; i++) {
            order.add(envTab.getTitleAt(i));
        }
        saveOrder(ENV_TAB_ORDER_KEY, order);
    }

    private void persistTestDataTabOrder(String envName, JTabbedPane testdataTab) {
        if (envName == null || testdataTab == null) {
            return;
        }
        List<String> order = new ArrayList<>();
        for (int i = 0; i < testdataTab.getTabCount() - 1; i++) {
            // Skip the global data tab from persisted order.
            if (i == 0) {
                continue;
            }
            order.add(testdataTab.getTitleAt(i));
        }
        saveOrder(getTestDataTabOrderKey(envName), order);
    }

    private String getTestDataTabOrderKey(String envName) {
        return TESTDATA_TAB_ORDER_PREFIX + envName;
    }

    /**
     * Persists tab order to user-defined project settings.
     *
     * <p>Order values are stored as a separator-joined string under the provided key and saved
     * immediately.</p>
     *
     * @param key settings key
     * @param order ordered tab names to persist
     */
    private void saveOrder(String key, List<String> order) {
        if (
            testDesign.getProject() == null || testDesign.getProject().getProjectSettings() == null
        ) {
            return;
        }
        String value = String.join(TAB_ORDER_SEPARATOR, order);
        testDesign
            .getProject()
            .getProjectSettings()
            .getUserDefinedSettings()
            .setProperty(key, value);
        testDesign.getProject().getProjectSettings().getUserDefinedSettings().save();
    }

    private List<String> getSavedOrder(String key) {
        if (
            testDesign.getProject() == null || testDesign.getProject().getProjectSettings() == null
        ) {
            return new ArrayList<>();
        }
        String value = testDesign
            .getProject()
            .getProjectSettings()
            .getUserDefinedSettings()
            .getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(TAB_ORDER_SEPARATOR)));
    }

    private MouseAdapter onAddNewTDTab() {
        return new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {
                JTabbedPane tabbedPane = (JTabbedPane) me.getSource();
                if (tabbedPane.getSelectedIndex() != -1 && getSelectedData() == null) {
                    Rectangle rect = tabbedPane
                        .getUI()
                        .getTabBounds(tabbedPane, tabbedPane.getSelectedIndex());
                    if (rect.contains(me.getPoint())) {
                        tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex() - 1);
                        addNewTestData(tabbedPane);
                    }
                }
            }
        };
    }

    private void addAddNewTab() {
        JScrollPane scrollPane = new JScrollPane(environmentPanel);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UIManager.getColor("Panel.background"));
        scrollPane.getViewport().setBackground(UIManager.getColor("Panel.background"));
        envTab.addTab("", ADD_NEW_TAB_ICON, scrollPane);
    }

    private TestDataTablePanel addToTab(
        JTabbedPane testdataTab,
        AbstractDataModel std,
        Boolean isGlobalData
    ) {
        TestDataTablePanel tdPanel = new TestDataTablePanel(std, isGlobalData);
        testdataTab.addTab(std.getName(), tdPanel);
        return tdPanel;
    }

    private TestDataTablePanel addToLastTab(JTabbedPane testdataTab, AbstractDataModel std) {
        TestDataTablePanel tdPanel = new TestDataTablePanel(std);
        testdataTab.insertTab(std.getName(), null, tdPanel, null, testdataTab.getTabCount() - 1);
        testdataTab.setSelectedIndex(testdataTab.getTabCount() - 2);
        persistTestDataTabOrder(getEnvironmentNameFor(testdataTab), testdataTab);
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
        TestDataModel model = testDesign
            .getProject()
            .getTestData()
            .getTestDataFor(envTab.getTitleAt(envTab.getSelectedIndex()))
            .addTestData();
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
        int option = JOptionPane.showConfirmDialog(
            null,
            combo,
            "Go To TestData",
            JOptionPane.DEFAULT_OPTION
        );
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
            persistTestDataTabOrder(getEnvironmentNameFor(tab), tab);
        }
    }

    private void deleteTestData(Object source) {
        JTabbedPane tab = (JTabbedPane) source;
        TestDataTablePanel panel = getSelectedData();
        if (!panel.isGlobalData) {
            int index = tab.getSelectedIndex();
            String name = tab.getTitleAt(index);
            int option = JOptionPane.showConfirmDialog(
                null,
                "Are you sure want to delete the TestData [" + name + "]",
                "Delete TestData",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                Boolean flag = testDesign
                    .getProject()
                    .getTestData()
                    .getTestDataFor(envTab.getTitleAt(envTab.getSelectedIndex()))
                    .deleteTestData(name);
                if (flag) {
                    tab.setSelectedIndex(index - 1);
                    tab.removeTabAt(index);
                    persistTestDataTabOrder(getEnvironmentNameFor(tab), tab);
                } else {
                    Notification.show("Couldn't Delete Testdata - '" + name + "'");
                }
            }
        }
    }

    private void renameTestDataFromMenu() {
        TestDataTablePanel panel = getSelectedData();
        if (panel != null && !panel.isGlobalData) {
            JTabbedPane tab = (JTabbedPane) envTab.getSelectedComponent();
            int index = tab.getSelectedIndex();
            String oldName = tab.getTitleAt(index);

            String newName = JOptionPane.showInputDialog(
                this,
                "Enter new name for TestData:",
                oldName
            );

            if (newName != null && !newName.trim().isEmpty() && Validator.isValidName(newName)) {
                panel.rename(newName);
            }
        }
    }

    private void reopenTestData() {
        int index = envTab.getSelectedIndex();
        String envName = envTab.getTitleAt(index);
        envTab.removeTabAt(index);
        TestData sTestData = testDesign.getProject().getTestData().getTestDataFor(envName);
        envTab.insertTab(
            sTestData.getEnviroment(),
            null,
            createNewTestDataTab(sTestData),
            null,
            index
        );
        envTab.setSelectedIndex(index);
    }

    private TestDataTablePanel getSelectedData() {
        if (envTab.getSelectedComponent() instanceof JTabbedPane) {
            JTabbedPane tab = (JTabbedPane) envTab.getSelectedComponent();
            if (
                tab.getTabCount() > 0 &&
                tab.getSelectedComponent() != null &&
                tab.getSelectedComponent() instanceof TestDataTablePanel
            ) {
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
                case "Add New TestData":
                    addNewTestData(envTab.getSelectedComponent());
                    break;
                case "Add In All Env":
                    addInAllEnvironement();
                    break;
                case "Rename TestData":
                    renameTestDataFromMenu();
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
        int[] rows = tdPanel.table.getSelectedRows();
        if (columns != null && columns.length > 0 && rows != null && rows.length > 0) {
            GlobalDataModel gdModel = getCurrentEnviromentData().getGlobalData();
            Object[] data = addAndGetKeyForGlobalData(gdModel);
            int globalRow = (int) data[1];

            // Copy values from the first selected row in test data to global data
            int sourceRow = rows[0];
            for (int viewCol : columns) {
                String columnName = tdPanel.table.getColumnName(viewCol);
                gdModel.addColumn(columnName);
                // Get value using VIEW column index (table.getValueAt uses view indices)
                Object value = tdPanel.table.getValueAt(sourceRow, viewCol);
                // Copy value from test data to global data, overwriting if necessary
                gdModel.setValueAt(value, globalRow, gdModel.getColumnIndex(columnName));
            }
            // Replace all selected cells with global data reference
            tdPanel.makeAsGlobalData(data[0].toString());
        }
    }

    private Object[] addAndGetKeyForGlobalData(GlobalDataModel gdModel) {
        Object[] objects = new Object[2];
        JComboBox jcb = new JComboBox(gdModel.getKeys().toArray());
        jcb.setEditable(true);
        JOptionPane.showMessageDialog(
            null,
            jcb,
            "Select or Enter a GlobalId",
            JOptionPane.QUESTION_MESSAGE
        );

        String key = Objects.toString(jcb.getSelectedItem(), "");

        if (key.trim().isEmpty()) {
            key = "#gd1";
        } else if (!key.startsWith("#")) {
            key = "#" + key;
        }
        objects[0] = key;

        if (gdModel.getRowCount() == 0 || !gdModel.getKeys().contains(key)) {
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
        for (AbstractDataModel std : testDesign
            .getProject()
            .getTestData()
            .getTestDataFor(env)
            .getTestDataList()) {
            tdL.add(std.getName());
        }
        return tdL;
    }

    private void addNewEnvironment(TestData sTestData) {
        envTab.insertTab(
            sTestData.getEnviroment(),
            null,
            createNewTestDataTab(sTestData),
            null,
            envTab.getTabCount() - 1
        );
    }

    private void addInAllEnvironement() {
        TestDataTablePanel panel = getSelectedData();
        if (!panel.isGlobalData) {
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            testDesign
                .getProject()
                .getTestData()
                .duplicateSheetsInOtherEnv(envName, (TestDataModel) panel.std);
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
            envTab.insertTab(
                envName,
                null,
                createNewTestDataTab(testDesign.getProject().getTestData().getTestDataFor(envName)),
                null,
                index
            );
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

    Boolean addNewEnvironment(
        String envName,
        String duplicateDataFromEnv,
        List<String> duplicateSheets,
        Boolean globalDataAsWell
    ) {
        if (testDesign.getProject().getTestData().getTestDataFor(envName) == null) {
            if (duplicateDataFromEnv == null) {
                testDesign.getProject().getTestData().createNewEnvironment(envName);
            } else {
                testDesign
                    .getProject()
                    .getTestData()
                    .createNewEnvironment(
                        envName,
                        duplicateDataFromEnv,
                        duplicateSheets,
                        globalDataAsWell
                    );
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
            testDesign
                .getProject()
                .getTestData()
                .duplicateColumnInOtherEnv(envName, (TestDataModel) tdPanel.std, colList);
        }
    }

    private void addRowInOtherEnvironement(TestDataTablePanel tdPanel) {
        if (!tdPanel.isGlobalData) {
            int[] rows = tdPanel.table.getSelectedRows();
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());
            testDesign
                .getProject()
                .getTestData()
                .duplicateRowsInOtherEnv(envName, (TestDataModel) tdPanel.std, rows);
        }
    }

    public void switchEnvView() {
        SwingUtilities.invokeLater(
            new Runnable() {

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
            }
        );
    }

    private void renameTestDataTabs(String oldName, String newName) {
        JTabbedPane selectedTab = (JTabbedPane) envTab.getSelectedComponent();
        // Only rename tabs in the selected environment, not in other environments
        for (int i = 0; i < selectedTab.getTabCount(); i++) {
            if (selectedTab.getTitleAt(i).equals(oldName)) {
                selectedTab.setTitleAt(i, newName);
            }
        }
    }

    /**
     * Rename datasheet tabs across multiple specified environments.
     *
     * @param oldName the old datasheet name
     * @param newName the new datasheet name
     * @param environments list of environment names to update tabs in
     */
    private void renameTestDataTabsInEnvironments(
        String oldName,
        String newName,
        List<String> environments
    ) {
        for (int envIndex = 0; envIndex < envTab.getTabCount(); envIndex++) {
            String envName = envTab.getTitleAt(envIndex);
            if (environments.contains(envName)) {
                JTabbedPane testdataTab = (JTabbedPane) envTab.getComponentAt(envIndex);
                for (int tabIndex = 0; tabIndex < testdataTab.getTabCount(); tabIndex++) {
                    if (testdataTab.getTitleAt(tabIndex).equals(oldName)) {
                        testdataTab.setTitleAt(tabIndex, newName);
                    }
                }
            }
        }
    }

    private Boolean renameEnvironment(String newName) {
        String envName = envTab.getTitleAt(envTab.getSelectedIndex());
        if (!envName.equals("Default") && !envName.equals(newName.trim())) {
            boolean renamed = testDesign
                .getProject()
                .getTestData()
                .renameEnvironment(envName, newName);
            if (renamed) {
                String oldKey = getTestDataTabOrderKey(envName);
                String newKey = getTestDataTabOrderKey(newName);
                String oldValue = testDesign
                    .getProject()
                    .getProjectSettings()
                    .getUserDefinedSettings()
                    .getProperty(oldKey);
                if (oldValue != null) {
                    testDesign
                        .getProject()
                        .getProjectSettings()
                        .getUserDefinedSettings()
                        .setProperty(newKey, oldValue);
                    testDesign
                        .getProject()
                        .getProjectSettings()
                        .getUserDefinedSettings()
                        .remove(oldKey);
                    testDesign.getProject().getProjectSettings().getUserDefinedSettings().save();
                }
                persistEnvironmentTabOrder();
            }
            return renamed;
        }
        return false;
    }

    private void deleteEnvironment() {
        String envName = envTab.getTitleAt(envTab.getSelectedIndex());
        if (!envName.equals("Default")) {
            int option = JOptionPane.showConfirmDialog(
                null,
                "Are you sure want to delete Environment [" + envName + "]",
                "Delete Environent",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (option == JOptionPane.YES_OPTION) {
                envTab.removeTabAt(envTab.getSelectedIndex());
                testDesign.getProject().getTestData().deleteEnvironment(envName);
                testDesign
                    .getProject()
                    .getProjectSettings()
                    .getUserDefinedSettings()
                    .remove(getTestDataTabOrderKey(envName));
                testDesign.getProject().getProjectSettings().getUserDefinedSettings().save();
                persistEnvironmentTabOrder();
            }
        }
    }

    private void getImpactedTestCases(TestDataTablePanel tdPanel) {
        testDesign
            .getImpactUI()
            .loadForTestData(
                testDesign.getProject().getImpactedTestDataTestCases(tdPanel.std.getName()),
                tdPanel.std.getName()
            );
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
        TestDataModel model = getCurrentEnviromentData().getByNameIgnoreCase(name);
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
        // Number of frozen (non-scrollable) columns on the left.
        private static final int frozenColumnCount = 5;

        AbstractDataModel std;
        XTable table;
        FrozenColumnScrollPane frozenScrollPane;

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
            table =
                new XTable() {

                    @Override
                    public TableCellEditor getCellEditor(int row, int column) {
                        if (!isGlobalData) {
                            // When using FrozenColumnScrollPane, columns 0-4 are removed from view
                            // So view column 0 is model column 5 - need to offset by frozenColumnCount
                            int modelColumn = column + frozenColumnCount;
                            return tDAutoSuggest.getCellEditorFor(
                                modelColumn,
                                super.getCellEditor(row, column)
                            );
                        }
                        return super.getCellEditor(row, column);
                    }

                    @Override
                    public boolean isCellEditable(int row, int column) {
                        if (!isGlobalData) {
                            // This table only shows dynamic columns (model index >= frozenColumnCount),
                            // so the raw view column must never be compared to model-column constants
                            // like the Scope column (model index 2) directly.
                            int modelColumn = column + frozenColumnCount;
                            if (modelColumn == 2) {
                                return false;
                            }
                        }
                        return super.isCellEditable(row, column);
                    }
                };
            if (isGlobalData) {
                table.setColumnRename(onRenameAction(), 0);
                load();
            } else {
                // For non-global data, enable column renaming for all scrollable columns
                // (fixed columns 0-4 are in a separate table managed by FrozenColumnScrollPane)
                // Note: After FrozenColumnScrollPane setup, view column 0 = model column 4
                table.setColumnRename(onRenameAction());
            }
            tDAutoSuggest = new TestDataAutoSuggest(testDesign.getProject(), table);
            table.setDragEnabled(true);
            table.setTransferHandler(new TestDataDnD());
            table.setComponentPopupMenu(popupMenu);

            table.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseClicked(MouseEvent me) {
                        if (SwingUtilities.isLeftMouseButton(me) && me.isAltDown()) {
                            goToSelectedTestCase();
                        } else if (SwingUtilities.isLeftMouseButton(me)) {
                            addLastRow();
                        }
                    }
                }
            );
            setLayout(new BorderLayout());
            setBackground(UIManager.getColor("Panel.background"));

            if (!isGlobalData) {
                // Use frozen column scroll pane for test data (but not global data)
                frozenScrollPane = new FrozenColumnScrollPane(table, frozenColumnCount);
                frozenScrollPane.setBackground(UIManager.getColor("Panel.background"));
                frozenScrollPane
                    .getViewport()
                    .setBackground(UIManager.getColor("Panel.background"));

                // Apply popup menu to fixed table as well
                frozenScrollPane.getFixedTable().setComponentPopupMenu(popupMenu);

                // Keep Delete behavior consistent between main and fixed tables.
                frozenScrollPane
                    .getFixedTable()
                    .getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Clear");
                frozenScrollPane
                    .getFixedTable()
                    .getActionMap()
                    .put(
                        "Clear",
                        new AbstractAction() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                clearValuesFromFixedTable();
                            }
                        }
                    );

                // Set cell editor provider for fixed columns (columns 0-4: Scenario, Flow, Scope, Iteration, SubIteration)

                frozenScrollPane.setCellEditorProvider(
                    (row, column, defaultEditor) ->
                        tDAutoSuggest.getCellEditorFor(column, defaultEditor)
                );

                configureFrozenInsertRowPrompt();

                add(frozenScrollPane);
            } else {
                JScrollPane scrollPane = new JScrollPane(table);
                scrollPane.setBackground(UIManager.getColor("Panel.background"));
                scrollPane.getViewport().setBackground(UIManager.getColor("Panel.background"));
                add(scrollPane);
            }
            addTableProps();
            configureInsertColumnPrompt();
            std.setSaveListener(saveListener);
        }

        private void load() {
            std.loadTableModel();
            table.setModel(std);
            changeSave(std.isSaved());
            // Update frozen scroll pane after model change
            if (!isGlobalData && frozenScrollPane != null) {
                frozenScrollPane.updateModel();
            }
        }

        private void configureFrozenInsertRowPrompt() {
            table.setInsertRowPromptEnabled(false);

            frozenScrollPane.setFixedInsertRowHandler(this::insertRowFromFrozenPrompt);
            frozenScrollPane.setFixedInsertRowPromptEnabled(true);
        }

        private void insertRowFromFrozenPrompt(int insertIndex) {
            stopCellEditing();

            int rowCount = table.getRowCount();
            int safeInsertIndex = Math.max(0, Math.min(insertIndex, rowCount));

            if (safeInsertIndex >= rowCount) {
                std.addRecord();
            } else {
                std.addRecord(safeInsertIndex);
            }

            selectInsertedRowAcrossFrozenTables(safeInsertIndex);
        }

        private void selectInsertedRowAcrossFrozenTables(int insertedRowIndex) {
            SwingUtilities.invokeLater(
                () -> {
                    int rowCount = table.getRowCount();

                    if (rowCount == 0) {
                        table.clearSelection();

                        if (frozenScrollPane != null && frozenScrollPane.getFixedTable() != null) {
                            frozenScrollPane.getFixedTable().clearSelection();
                        }

                        return;
                    }

                    int safeRow = Math.max(0, Math.min(insertedRowIndex, rowCount - 1));

                    table.setRowSelectionInterval(safeRow, safeRow);

                    if (table.getColumnCount() > 0) {
                        table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
                    }

                    if (frozenScrollPane != null && frozenScrollPane.getFixedTable() != null) {
                        JTable fixedTable = frozenScrollPane.getFixedTable();

                        fixedTable.setRowSelectionInterval(safeRow, safeRow);

                        if (fixedTable.getColumnCount() > 0) {
                            fixedTable.setColumnSelectionInterval(
                                0,
                                fixedTable.getColumnCount() - 1
                            );
                        }

                        fixedTable.repaint();
                    }

                    table.repaint();
                }
            );
        }

        private Action onRenameAction() {
            return new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    assignThePreviouslySelected();
                    Boolean flag = testDesign
                        .getProject()
                        .getTestData()
                        .renameTestDataColumn(
                            std.getName(),
                            getValue("oldvalue").toString(),
                            getValue("newvalue").toString()
                        );
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
                        } else if (col < frozenColumnCount) {
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
            String envName = envTab.getTitleAt(envTab.getSelectedIndex());

            // Check for duplicates in other environments
            List<String> otherEnvsWithSameName = testDesign
                .getProject()
                .getTestData()
                .findOtherEnvironmentsWithDatasheet(oldName, envName);

            if (!otherEnvsWithSameName.isEmpty()) {
                // Show confirmation dialog for cross-environment rename
                CrossEnvironmentRenameDialog dialog = CrossEnvironmentRenameDialog.showDialog(
                    (Frame) SwingUtilities.getWindowAncestor(TestDataComponent.this),
                    oldName,
                    envName,
                    otherEnvsWithSameName
                );

                if (!dialog.isConfirmed()) {
                    // User cancelled
                    return false;
                }

                List<String> selectedEnvs = dialog.getSelectedEnvironmentsForRename();

                if (selectedEnvs == null) {
                    // User chose to rename current environment only
                    if (
                        testDesign
                            .getProject()
                            .getTestData()
                            .renameTestData(oldName, newName, envName)
                    ) {
                        renameTestDataTabs(oldName, newName);
                        return true;
                    } else {
                        Notification.show(
                            "A TestData with name '" + newName + "' is present already"
                        );
                        return false;
                    }
                } else {
                    // User chose to rename across selected environments
                    // Add current environment to the list
                    List<String> allEnvs = new ArrayList<>(selectedEnvs);
                    allEnvs.add(envName);

                    if (
                        testDesign
                            .getProject()
                            .getTestData()
                            .renameTestDataAcrossEnvironments(oldName, newName, allEnvs)
                    ) {
                        // Update tabs in all affected environments
                        renameTestDataTabsInEnvironments(oldName, newName, allEnvs);
                        return true;
                    } else {
                        Notification.show(
                            "A TestData with name '" +
                            newName +
                            "' is present already in one or more environments"
                        );
                        return false;
                    }
                }
            } else {
                // No duplicates in other environments, proceed with normal rename
                if (
                    testDesign.getProject().getTestData().renameTestData(oldName, newName, envName)
                ) {
                    renameTestDataTabs(oldName, newName);
                    return true;
                } else {
                    Notification.show("A TestData with name '" + newName + "' is present already");
                }
                return false;
            }
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
            if (row == table.getRowCount() - 1 && column == table.getColumnCount() - 1) {
                addRow();
            }
        }

        private void addRow() {
            stopCellEditing();
            std.addRecord();
        }

        private void insertRowBelow() {
            stopCellEditing();
            if (table.getSelectedRow() != -1 && table.getSelectedRow() + 1 < table.getRowCount()) {
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
            int[] selectedRows = table.getSelectedRows();
            int lastIndex = selectedRows[selectedRows.length - 1];
            int added = 0;
            for (int row : selectedRows) {
                std.replicateRecord(row, lastIndex + 1 + added);
                added++;
            }
        }

        private void addColumn() {
            assignThePreviouslySelected();
            stopCellEditing();

            if (!isGlobalData && frozenScrollPane != null) {
                // Check if focus is on fixed table - don't allow adding column there
                JTable fixedTable = frozenScrollPane.getFixedTable();
                int fixedSelectedCol = fixedTable.getSelectedColumn();
                if (fixedSelectedCol >= 0) {
                    // Fixed column selected - do nothing, user cannot add column here
                    Notification.show(
                        "Cannot add columns in the fixed area. Select a column in the scrollable area or add at the end."
                    );
                    return;
                }

                // Check if a column is selected in the main (scrollable) table
                int mainSelectedCol = table.getSelectedColumn();
                if (mainSelectedCol >= 0) {
                    // Main table view column needs offset: model = view + frozenColumnCount
                    int insertIndex = mainSelectedCol + frozenColumnCount + 1;
                    std.addColumnAt(insertIndex);
                } else {
                    // No column selected - add at the end
                    std.addColumn();
                }
            } else {
                // Global data or no frozen pane
                int selectedCol = table.getSelectedColumn();
                if (selectedCol >= 0 && !isGlobalData) {
                    // For global data, column 0 is protected, so insert after selection
                    std.addColumnAt(selectedCol + 1);
                } else {
                    std.addColumn();
                }
            }

            selectThePreviouslySelected();
        }

        private void clearValues() {
            stopCellEditing();
            if (table.getSelectedRowCount() > 0) {
                std.clearValues(table.getSelectedRows(), getSelectedModelColumns());
            }
        }

        private void clearValuesFromFixedTable() {
            stopCellEditing();

            if (
                isGlobalData || frozenScrollPane == null || frozenScrollPane.getFixedTable() == null
            ) {
                clearValues();
                return;
            }

            JTable fixedTable = frozenScrollPane.getFixedTable();
            if (fixedTable.getSelectedRowCount() > 0) {
                std.clearValues(table.getSelectedRows(), getSelectedModelColumnsFromFixedTable());
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

            if (!isGlobalData && frozenScrollPane != null) {
                // Get selected columns from the scrollable table
                // View column indices need to be converted to model indices (add frozenColumnCount)
                int[] viewCols = table.getSelectedColumns();
                if (viewCols.length == 0) {
                    return;
                }

                List<Integer> modelColList = new ArrayList<>();
                for (int viewCol : viewCols) {
                    int modelCol = viewCol + frozenColumnCount; // offset by frozenColumnCount
                    modelColList.add(modelCol);
                }

                // Sort in reverse order for safe removal
                modelColList.sort((a, b) -> b - a);
                std.removeColumn(modelColList);
            } else {
                // Global data or no frozen pane - original logic
                List<Integer> colList = Utils.getReverseSorted(table.getSelectedColumns());
                if (!colList.isEmpty()) {
                    if (isGlobalData) {
                        colList.remove(Integer.valueOf(0));
                    } else {
                        // Remove protected (frozen) columns from deletion list.
                        for (int colIndex = 0; colIndex < frozenColumnCount; colIndex++) {
                            colList.remove(Integer.valueOf(colIndex));
                        }
                    }
                    std.removeColumn(colList);
                }
            }
            load();
        }

        private List<String> getSelectedColumns() {
            List<String> colList = new ArrayList<>();
            for (int col : getSelectedModelColumns()) {
                colList.add(std.getColumnName(col));
            }
            return colList;
        }

        private int[] getSelectedModelColumns() {
            int[] selectedColumns = table.getSelectedColumns();
            int[] modelColumns = new int[selectedColumns.length];

            for (int i = 0; i < selectedColumns.length; i++) {
                modelColumns[i] = table.convertColumnIndexToModel(selectedColumns[i]);
            }

            return modelColumns;
        }

        private int[] getSelectedModelColumnsFromFixedTable() {
            JTable fixedTable = frozenScrollPane.getFixedTable();
            int[] selectedColumns = fixedTable.getSelectedColumns();
            int[] modelColumns = new int[selectedColumns.length];

            for (int i = 0; i < selectedColumns.length; i++) {
                modelColumns[i] = fixedTable.convertColumnIndexToModel(selectedColumns[i]);
            }

            return modelColumns;
        }

        private TableModel createCustomTableModel(
            TableModel originalModel,
            TableColumnModel columnModel
        ) {
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

        private void configureInsertColumnPrompt() {
            table.setInsertColumnHandler(
                insertColumnIndex -> insertColumnFromHeaderPrompt(insertColumnIndex)
            );

            if (isGlobalData) {
                // Do not show the plus button before GlobalDataID.
                table.setMinimumInsertColumn(1);
            } else {
                // Normal TestData can still insert before the first visible scrollable column.
                table.setMinimumInsertColumn(0);
            }

            table.setInsertColumnPromptEnabled(true);
        }

        private void insertColumnFromHeaderPrompt(int viewInsertIndex) {
            assignThePreviouslySelected();
            stopCellEditing();

            int modelColumnCount = std.getColumnCount();

            if (isGlobalData) {
                /*
                 * GlobalData column 0 is protected: GlobalDataID.
                 * Do not allow inserting before it.
                 *
                 * Header prompt boundary 0 means "before GlobalDataID", so clamp it to 1.
                 */
                int modelInsertIndex = Math.max(1, Math.min(viewInsertIndex, modelColumnCount));

                if (modelInsertIndex >= modelColumnCount) {
                    std.addColumn();
                } else {
                    std.addColumnAt(modelInsertIndex);
                }

                selectThePreviouslySelected();
                return;
            }

            /*
             * For non-global TestData, FrozenColumnScrollPane removes model columns 0-4
             * from the main scrollable table.
             *
             * Therefore:
             * main table view column 0 == model column 5
             *
             * A prompt insert boundary at view index N maps to model index N + 5.
             */
            if (frozenScrollPane != null) {
                JTable fixedTable = frozenScrollPane.getFixedTable();

                if (fixedTable != null) {
                    fixedTable.clearSelection();
                }

                int modelInsertIndex = Math.max(
                    frozenColumnCount,
                    Math.min(viewInsertIndex + frozenColumnCount, modelColumnCount)
                );

                if (modelInsertIndex >= modelColumnCount) {
                    std.addColumn();
                } else {
                    std.addColumnAt(modelInsertIndex);
                }

                selectThePreviouslySelected();
                return;
            }

            /*
             * Fallback for non-global data without FrozenColumnScrollPane.
             */
            int modelInsertIndex = Math.max(0, Math.min(viewInsertIndex, modelColumnCount));

            if (modelInsertIndex >= modelColumnCount) {
                std.addColumn();
            } else {
                std.addColumnAt(modelInsertIndex);
            }

            selectThePreviouslySelected();
        }

        private void addTableProps() {
            table.setActionFor(
                "MoveUp",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        moveRowUp();
                    }
                }
            );
            table.setActionFor(
                "MoveDown",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        moveRowDown();
                    }
                }
            );
            table.setActionFor(
                "Insert",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        insertRow();
                    }
                }
            );
            table.setActionFor(
                "Add",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        insertRowBelow();
                    }
                }
            );
            table.setActionFor(
                "Delete",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        deleteSelectedRows();
                    }
                }
            );
            table.setActionFor(
                "Clear",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        clearValues();
                    }
                }
            );
            table.setActionFor(
                "Add Column",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        addColumn();
                    }
                }
            );
            table.setActionFor(
                "Delete Column",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        deleteSelectedColumns();
                    }
                }
            );
            table.setActionFor(
                "Replicate",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        replicateRow();
                    }
                }
            );
            table.setActionFor(
                "Save",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        save();
                    }
                }
            );
            table.setActionFor(
                "Reload",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        reload();
                    }
                }
            );
            table.setActionFor(
                "Open",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        openWithSystemEditor();
                    }
                }
            );
            table.setActionFor(
                "Search",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        toolBar.focusSearch();
                    }
                }
            );

            table.setActionFor(
                "Copy Above",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        copyAbove();
                    }
                }
            );
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
                    // For test data with FrozenColumnScrollPane, columns 0-4 are in the fixed table
                    // We need to read Scenario (column 0) and TestCase (column 1) from the fixed table
                    int selectedRow = table.getSelectedRow();
                    JTable sourceTable = frozenScrollPane.getFixedTable();
                    String scenVal = Objects.toString(sourceTable.getValueAt(selectedRow, 0), "");
                    String tcVal = Objects.toString(sourceTable.getValueAt(selectedRow, 1), "");
                    if (!scenVal.isEmpty() && !tcVal.isEmpty()) {
                        Scenario scenario = testDesign.getProject().getScenarioByName(scenVal);
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
                        Notification.show(
                            "Testcase " +
                            "[" +
                            scenVal +
                            ":" +
                            tcVal +
                            "]" +
                            " not available in Project"
                        );
                    }
                }
            }
        }

        private void stopCellEditing() {
            if (table.getCellEditor() != null) {
                table.getCellEditor().stopCellEditing();
            }
            if (
                frozenScrollPane != null && frozenScrollPane.getFixedTable().getCellEditor() != null
            ) {
                frozenScrollPane.getFixedTable().getCellEditor().stopCellEditing();
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
        JMenuItem rename;
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

            rename = new JMenuItem("Rename");
            rename.setActionCommand("Rename TestData");

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
            rename.addActionListener(TestDataComponent.this);
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
            add(rename);
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
