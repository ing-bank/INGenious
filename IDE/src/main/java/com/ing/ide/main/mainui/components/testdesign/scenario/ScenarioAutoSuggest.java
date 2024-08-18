
package com.ing.ide.main.mainui.components.testdesign.scenario;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggestCellEditor;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;

/**
 *
 * 
 */
public class ScenarioAutoSuggest {

    private final Project sProject;

    private final JTable table;
    private AutoSuggest reusableAutoSuggest;

    public ScenarioAutoSuggest(Project sProject, JTable table) {
        this.sProject = sProject;
        this.table = table;
        initAutoSuggest();
        install();
    }

    private void initAutoSuggest() {
        reusableAutoSuggest = new AutoSuggest() {
            @Override
            public void beforeSearch(String text) {
                setSearchList(getReusables());
            }

        };
    }

    private List<String> getReusables() {
        List<String> reusableList = new ArrayList<>();
        for (Scenario scenario : sProject.getScenarios()) {
            int rcount = scenario.getReusableCount();
            for (int i = 0; i < rcount; i++) {
                reusableList.add(scenario.getName() + ":" + scenario.getReusableAt(i).getName());
            }
        }
        return reusableList;
    }

    private void stopEditing() {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
            table.getSelectionModel().setSelectionInterval(row, row);
            table.getColumnModel().getSelectionModel().setSelectionInterval(column, column);
        }
    }

    private void install() {
        table.setDefaultEditor(Object.class, new AutoSuggestCellEditor(reusableAutoSuggest));
    }
}
