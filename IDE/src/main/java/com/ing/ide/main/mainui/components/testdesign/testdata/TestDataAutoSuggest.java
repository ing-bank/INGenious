
package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.engine.util.data.fx.FParser;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggestCellEditor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 *
 * 
 */
public class TestDataAutoSuggest {

    private final Project sProject;
    private final JTable table;

    private AutoSuggest scenarioSugg;
    private AutoSuggest testCaseSugg;
    private AutoSuggest functionSugg;

    public TestDataAutoSuggest(Project sProject, JTable table) {
        this.sProject = sProject;
        this.table = table;
        init();
    }

    private void init() {
        functionSugg = new AutoSuggest() {
            @Override
            public void beforeSearch(String text) {
                if (text.startsWith("=")) {
                    setSearchList(getFunctionList());
                } else {
                    clearSearchList();
                }
            }

            private List<String> getFunctionList() {
                List<String> functions = new ArrayList<>();
                for (String func : FParser.getFuncList()) {
                    functions.add("=".concat(func));
                }
                return functions;
            }

        }.withOnHide(stopEditingOnFocusLost());
        scenarioSugg = new AutoSuggest().withOnHide(stopEditingOnFocusLost());
        testCaseSugg = new AutoSuggest().withOnHide(stopEditingOnFocusLost());
    }

    private Action stopEditingOnFocusLost() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
            }
        };
    }

    private void updateScenarios() {
        Set allScenarios = new LinkedHashSet<>();
        
        // Add Test Scenarios to the autosuggest list
        List<String> scenarioList = Utils.asStringList(sProject.getScenarios());
        Collections.sort(scenarioList, String.CASE_INSENSITIVE_ORDER);
        allScenarios.addAll(scenarioList);
        
        // Add Reusable Component Test Scenarios to the autosuggest list
        List<String> reusableScenarioList = Utils.asStringList(sProject.getReusableScenarios());
        Collections.sort(reusableScenarioList, String.CASE_INSENSITIVE_ORDER);
        allScenarios.addAll(reusableScenarioList);
        
        scenarioSugg.setSearchList(new ArrayList<>(allScenarios));
    }

    private void updateTestCases() {
        testCaseSugg.clearSearchList();
        if (table.getSelectedRow() != -1) {
            // Use model directly to get scenario value (column 0 in model)
            // This works regardless of whether we're in the fixed table or main table
            String scenario = Objects.toString(
                    table.getModel().getValueAt(table.getSelectedRow(), 0), "");
            if (!scenario.trim().isEmpty()){
                Set allTestCases = new LinkedHashSet<>();
                
                // Add all test cases that match the scenario name
                if (sProject.getScenarioByName(scenario) != null) {
                    List<String> testCaseList = Utils.asStringList(
                            sProject.getScenarioByName(scenario).getTestCases());
                    Collections.sort(testCaseList, String.CASE_INSENSITIVE_ORDER);
                    allTestCases.addAll(testCaseList);
                }
                
                // Add all reusable component test cases that match the scenario name
                if (sProject.getReusableScenarioByName(scenario) != null) {
                    List<String> reusableTestCaseList = Utils.asStringList(
                            sProject.getReusableScenarioByName(scenario).getTestCases());
                    Collections.sort(reusableTestCaseList, String.CASE_INSENSITIVE_ORDER);
                    allTestCases.addAll(reusableTestCaseList);
                }
                
                testCaseSugg.setSearchList(new ArrayList<>(allTestCases));
            }
        }
    }

    public TableCellEditor getCellEditorFor(int column, TableCellEditor cellEditor) {
        switch (column) {
            case 0:
                updateScenarios();
                return new AutoSuggestCellEditor(scenarioSugg);
            case 1:
                updateTestCases();
                return new AutoSuggestCellEditor(testCaseSugg);
            case 2:
            case 3:
                return cellEditor;
            default:
                return new AutoSuggestCellEditor(functionSugg);
        }
    }
}
