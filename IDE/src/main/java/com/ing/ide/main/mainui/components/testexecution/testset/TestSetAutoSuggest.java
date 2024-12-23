
package com.ing.ide.main.mainui.components.testexecution.testset;

import static com.ing.datalib.component.ExecutionStep.HEADERS.Browser;
import static com.ing.datalib.component.ExecutionStep.HEADERS.Iteration;
import static com.ing.datalib.component.ExecutionStep.HEADERS.Platform;
import static com.ing.datalib.component.ExecutionStep.HEADERS.TestCase;
import static com.ing.datalib.component.ExecutionStep.HEADERS.TestScenario;
import com.ing.datalib.component.Project;
import com.ing.engine.drivers.PlaywrightDriverFactory;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggestCellEditor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JTable;

public class TestSetAutoSuggest {

    private final Project sProject;

    private final JTable table;

    private AutoSuggest scenarioAutoSuggest;

    private AutoSuggest testCaseAutoSuggest;

    private AutoSuggest iterationAutoSuggest;

    private AutoSuggest browserAutoSuggest;

    private AutoSuggest platformAutoSuggest;

    public TestSetAutoSuggest(Project sProject, JTable table) {
        this.sProject = sProject;
        this.table = table;
        initAutoSuggest();
    }

    private void initAutoSuggest() {
        scenarioAutoSuggest = new AutoSuggest() {

            @Override
            public void beforeSearch(String text) {
                setSearchList(Utils.asStringList(sProject.getScenarios()));
            }
        };
        testCaseAutoSuggest = new AutoSuggest() {
            @Override
            public void beforeSearch(String text) {
                clearSearchList();
                String scenario = Objects.toString(table.getValueAt(table.getSelectedRow(), 1), "");
                if (!scenario.trim().isEmpty()
                        && sProject.getScenarioByName(scenario) != null) {
                    setSearchList(Utils.asStringList(sProject.getScenarioByName(scenario).getTestCases()));
                }
            }

        };
        iterationAutoSuggest = new AutoSuggest().withSearchList(getIterationList());
        browserAutoSuggest = new AutoSuggest();
        platformAutoSuggest = new AutoSuggest().withSearchList(getPlatformList());
    }

    public void installForTestSet() {
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(TestScenario.getIndex()).setCellEditor(new AutoSuggestCellEditor(scenarioAutoSuggest));
        table.getColumnModel().getColumn(TestCase.getIndex()).setCellEditor(new AutoSuggestCellEditor(testCaseAutoSuggest));
        table.getColumnModel().getColumn(Iteration.getIndex()).setCellEditor(new AutoSuggestCellEditor(iterationAutoSuggest));
        table.getColumnModel().getColumn(Browser.getIndex()).setCellEditor(new AutoSuggestCellEditor(browserAutoSuggest));
        table.getColumnModel().getColumn(Platform.getIndex()).setCellEditor(new AutoSuggestCellEditor(platformAutoSuggest));
    }

    private List<String> getIterationList() {
        List<String> iterationList = new ArrayList<>();
        iterationList.add("All");
        iterationList.add("Single");
        iterationList.add("n");
        iterationList.add("n:n");
        return iterationList;
    }

    private List<String> getPlatformList() {
        List<String> platformList = new ArrayList<>();
        platformList.add(System.getProperty("os.name"));
        return platformList;
    }
   

    void loadBrowsers() {
        List<String> browsers = PlaywrightDriverFactory.Browser.getValuesAsList();
        browsers.addAll(sProject.getProjectSettings().getEmulators().getEmulatorNames());
        browserAutoSuggest.setSearchList(browsers);
    }
}
