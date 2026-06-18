package com.ing.testdata.csv;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.DataProvider;
import com.ing.datalib.testdata.model.Record;
import java.io.File;
import java.util.Objects;

@DataProvider(type = "csv")
public class CsvDataProvider extends TestData {

    public CsvDataProvider(Project sProject, String enviroment) {
        super(sProject, enviroment);
    }

    @Override
    public void load() {
        File file = new File(getLocation());
        if (file.exists()) {
            for (File tData : file.listFiles(FileUtils.CSV_FILTER)) {
                if (!tData.getName().equals("GlobalData.csv")) {
                    CsvTestData csvData = new CsvTestData(tData.getAbsolutePath());
                    // ensure model is loaded so we can migrate records if the file lacks the Scope column
                    csvData.loadTableModel();
                    // perform migration: ensure Scope column exists and populate based on prefixes or project reusables
                    try {
                        boolean modified = false;
                        for (com.ing.datalib.testdata.model.Record record : csvData.getRecords()) {
                            // If the CSV was using the older 4-column format, values for Iteration/SubIteration
                            // will currently be at indices 2 and 3 while index 4 is empty. Detect this pattern
                            // (index2 and index3 numeric, index4 empty) and shift them right so index2 can
                            // be used for the new Scope column.
                            try {
                                String idx2 = java.util.Objects.toString(record.get(2), "");
                                String idx3 = java.util.Objects.toString(record.get(3), "");
                                String idx4 = java.util.Objects.toString(record.get(4), "");
                                if (
                                    (idx4 == null || idx4.isEmpty()) &&
                                    idx2.matches("\\d+") &&
                                    idx3.matches("\\d+")
                                ) {
                                    record.set(4, idx3);
                                    record.set(3, idx2);
                                    record.set(2, "");
                                }
                            } catch (Exception ex) {
                                // ignore index errors
                            }
                            // ensure record has slots for all headers (Record constructor does this for new records)
                            // get current scenario value
                            String scenario = "";
                            try {
                                scenario = java.util.Objects.toString(record.get(0), "").trim();
                            } catch (Exception ex) {
                                // ignore
                            }
                            String scope = "";
                            String normalized = scenario;
                            if (normalized.startsWith("[Project] ")) {
                                scope = "[Project]";
                                normalized = normalized.substring("[Project] ".length());
                            } else if (normalized.startsWith("[Shared] ")) {
                                scope = "[Shared]";
                                normalized = normalized.substring("[Shared] ".length());
                            } else if (normalized.startsWith("[TestPlan] ")) {
                                // Test plan scenario - scope should remain empty
                                scope = "";
                                normalized = normalized.substring("[TestPlan] ".length());
                            } else {
                                // try to match against project test plan or reusables if available
                                try {
                                    com.ing.datalib.component.Project proj = getsProject();
                                    if (proj != null) {
                                        // If it exists in the test plan scenarios, treat as TestPlan (scope empty)
                                        if (proj.getScenarioByName(normalized) != null) {
                                            scope = "";
                                        } else if (
                                            proj.getReusableScenarioByName(normalized) != null
                                        ) {
                                            scope = "[Project]";
                                        } else if (
                                            proj.getSharedReusableScenarioByName(normalized) != null
                                        ) {
                                            scope = "[Shared]";
                                        }
                                    }
                                } catch (Exception ex) {
                                    // project may not be fully initialized; ignore
                                }
                            }
                            // if scope determined or slot exists but empty, update record
                            try {
                                String existingScope = java.util.Objects.toString(
                                    record.get(2),
                                    ""
                                );
                                // If the scenario belongs to Test Plan, ensure Scope is empty and normalize
                                if ((scope == null || scope.isEmpty())) {
                                    // if existingScope is non-empty, clear it to reflect test plan ownership
                                    if (existingScope != null && !existingScope.isEmpty()) {
                                        record.set(2, "");
                                        record.set(0, normalized);
                                        modified = true;
                                    } else if (
                                        !Objects.toString(record.get(0), "").equals(normalized)
                                    ) {
                                        // normalization changed the scenario text
                                        record.set(0, normalized);
                                        modified = true;
                                    }
                                } else {
                                    // For Project/Shared, set scope if different or missing
                                    if (!scope.equals(existingScope)) {
                                        record.set(2, scope);
                                        record.set(0, normalized);
                                        modified = true;
                                    }
                                }
                            } catch (Exception ex) {
                                // ignore out-of-bounds or other issues
                            }
                        }
                        if (modified) {
                            csvData.saveChanges();
                        }
                    } catch (Exception ex) {
                        // ignore migration errors and continue loading
                    }
                    addTestData(csvData);
                }
            }
        }
        loadGlobalData();
    }

    private void loadGlobalData() {
        File file = new File(getLocation() + File.separator + "GlobalData.csv");
        setGlobalData(new CsvGlobalData(file.getAbsolutePath()));
    }

    @Override
    public CsvTestData getNewTestData(String name) {
        CsvTestData csvData = new CsvTestData(getLocation() + File.separator + name + ".csv");
        csvData.setColumns(Record.HEADERS);
        csvData.addColumn("Data1");
        csvData.addColumn("Data2");
        return csvData;
    }

    @Override
    public CsvTestData importTestData(File file) {
        CsvTestData csvData = new CsvTestData(file.getAbsolutePath());
        csvData.loadTableModel();
        csvData.setLocation(getLocation() + File.separator + file.getName());
        csvData.saveChanges();
        return csvData;
    }
}
