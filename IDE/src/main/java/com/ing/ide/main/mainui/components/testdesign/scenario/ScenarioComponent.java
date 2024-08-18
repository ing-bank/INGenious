
package com.ing.ide.main.mainui.components.testdesign.scenario;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Notification;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 * 
 */
public class ScenarioComponent extends JPanel implements ActionListener {

    private final TestDesign testDesign;

    private final ScenarioToolBar toolBar;
    private final ScenarioPopupMenu popupMenu;

    private final XTable scenarioTable;

    private ScenarioAutoSuggest scAutoSuggest;

    public ScenarioComponent(TestDesign testDesign) {
        this.testDesign = testDesign;
        scenarioTable = new XTable();
        toolBar = new ScenarioToolBar(this);
        popupMenu = new ScenarioPopupMenu(this);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(scenarioTable), BorderLayout.CENTER);
        scenarioTable.setComponentPopupMenu(popupMenu);
        initTableListeners();
    }

    public JTable getScenarioTable() {
        return scenarioTable;
    }

    public void loadTableModelForSelection(Object obj) {
        Scenario scenario = (Scenario) obj;
        getScenarioTable().setModel(new DefaultTableModel());
        getScenarioTable().setModel(testDesign.getProject().getTableModelFor(scenario));
        refreshTitle();
        checkForSave();
    }

    private void checkForSave() {
        toolBar.changeSave(false);
        for (TestCase testCase : getCurrentScenario().getTestcasesAlone()) {
            if (!testCase.isSaved()) {
//                toolBar.changeSave(true);
                break;
            }
        }
    }

    public void refreshTitle() {
        if (getCurrentScenario() != null) {
            toolBar.setPlaceHolderText(getCurrentScenario().getName());
        }
    }

    public void load() {
        scAutoSuggest = new ScenarioAutoSuggest(testDesign.getProject(), scenarioTable);
    }

    private void initTableListeners() {
        scenarioTable.setActionFor("Insert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertComp();
            }
        });
        scenarioTable.setActionFor("Add", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addComp();
            }
        });
        scenarioTable.setActionFor("Delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeComp();
            }
        });
        scenarioTable.setActionFor("Save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        scenarioTable.setActionFor("Reload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });
     /*   scenarioTable.setActionFor("Search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toolBar.focusSearch();
            }
        });*/
        scenarioTable.setTransferHandler(new ScenarioDnD());
        scenarioTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isLeftMouseButton(me) && me.isAltDown()) {
                    goToSelectedReusable();
                }
            }

        });

    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Component":
                addComp();
                break;
            case "Remove Component":
                removeComp();
                break;
            case "Save":
                save();
                break;
            case "Reload":
                reload();
                break;
          /*  case "Search":
                scenarioTable.searchFor(((JTextField) ae.getSource()).getText());
                break;*/
            case "GoToNextSearch":
                scenarioTable.goToNextSearch();
                break;
            case "GoToPrevoiusSearch":
                scenarioTable.goToPrevoiusSearch();
                break;
            case "Cut":
            case "Copy":
            case "Paste":
                ccp(ae.getActionCommand());
                break;
            case "Create Reusable":
                createAsReusable();
                break;
            default:
                throw new UnsupportedOperationException(ae.getActionCommand());
        }
    }

    private void goToSelectedReusable() {
        if (scenarioTable.getSelectedRow() != -1) {
            int selCol = scenarioTable.getSelectedColumn();
            TestCase testCase = getSelectedTestCase();
            if (selCol > 0) {
                TestStep tStep = testCase.getTestSteps().get(scenarioTable.getSelectedColumn() - 1);
                String[] reusableData = tStep.getReusableData();
                if (reusableData != null) {
                    TestCase rtestCase = testDesign.getProject().getScenarioByName(reusableData[0]).getTestCaseByName(reusableData[1]);
                    testDesign.loadTableModelForSelection(rtestCase);
                } else {
                    testDesign.loadTableModelForSelection(testCase);
                    testDesign.getTestCaseComp().getTestCaseTable().changeSelection(scenarioTable.getSelectedColumn() - 1, 3, false, false);
                }
            } else {
                testDesign.loadTableModelForSelection(testCase);
            }
        }
    }

    private void createAsReusable() {
        if (scenarioTable.getSelectedColumnCount() > 0) {
            List<Integer> cols = Utils.getSorted(scenarioTable.getSelectedColumns());
            cols.remove(new Integer(0));
            if (!cols.isEmpty()) {
                TestCase testCase = getSelectedTestCase();
                int from = cols.get(0) - 1;
                int to = cols.get(cols.size() - 1) - 1;
                if (to >= testCase.getRowCount()) {
                    to = testCase.getRowCount() - 1;
                }
                String name = JOptionPane.showInputDialog("Enter the Reusable Name");
                if (name != null && !name.trim().isEmpty()) {
                    TestCase reusable = testCase.createAsReusable(name, from, to);
                    if (reusable != null) {
                        testDesign.getReusableTree().getTreeModel().addTestCase(reusable);
                        getCurrentScenario().fireTableStructureChanged();
                    } else {
                        Notification.show("Couldn't Create Reusable - " + name);
                    }
                }
            }
        }
    }

    private TestCase getSelectedTestCase() {
        if (scenarioTable.getSelectedRow() != -1) {
            return getCurrentScenario().
                    getTestCaseByName(scenarioTable.getValueAt(
                            scenarioTable.getSelectedRow(), 0).toString());
        }
        return null;
    }

    Scenario getCurrentScenario() {
        if (scenarioTable.getModel() instanceof Scenario) {
            return (Scenario) scenarioTable.getModel();
        }
        return null;
    }

    private void insertComp() {
        int col = scenarioTable.getSelectedColumn() - 1;
        if (col != -1) {
            TestCase testCase = getSelectedTestCase();
            if (testCase != null) {
                testCase.addNewStepAt(col);
                int row = scenarioTable.getSelectedRow();
                getCurrentScenario().fireTableStructureChanged();
                scenarioTable.changeSelection(row, col + 1, false, false);
                toolBar.changeSave(true);
            }
        }
    }

    private void addComp() {
        TestCase testCase = getSelectedTestCase();
        if (testCase != null) {
            testCase.addRow();
            int row = scenarioTable.getSelectedRow();
            getCurrentScenario().fireTableStructureChanged();
            scenarioTable.changeSelection(row, testCase.getRowCount(), false, false);
            toolBar.changeSave(true);
        }
    }

    private void removeComp() {
        TestCase testCase = getSelectedTestCase();
        if (testCase != null) {
            testCase.addRow();
            int row = scenarioTable.getSelectedColumn() - 1;
            if (row != -1) {
                testCase.removeRow(row);
                getCurrentScenario().fireTableStructureChanged();
                toolBar.changeSave(true);
            }
        }
    }

    private void save() {
        getCurrentScenario().save();
        getCurrentScenario().fireTableStructureChanged();
        toolBar.changeSave(false);
    }

    private void reload() {
        for (TestCase testCase : getCurrentScenario().getTestcasesAlone()) {
            testCase.reload();
        }
        testDesign.getTestCaseComp().reload();
        getCurrentScenario().fireTableStructureChanged();
        toolBar.changeSave(false);
    }

    private void ccp(String operation) {
        switch (operation) {
            case "Cut":
                scenarioTable.cut();
                break;
            case "Copy":
                scenarioTable.copy();
                break;
            case "Paste":
                scenarioTable.paste();
                break;
        }
    }

}
