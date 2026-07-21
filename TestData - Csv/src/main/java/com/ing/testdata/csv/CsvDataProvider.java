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
        // Skip all migrations and saves if in read-only mode (e.g., during validation)
        if (isReadOnlyMode()) {
            loadGlobalData();
            return;
        }

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
                            // If the CSV was using the older 4-column format (Scenario, Flow, Iteration, SubIteration),
                            // the Scope column didn't exist. After loading, values will be at wrong indices:
                            // Old: idx0=Scenario, idx1=Flow, idx2=Iteration, idx3=SubIteration, idx4+=DataColumns
                            // New: idx0=Scenario, idx1=Flow, idx2=Scope, idx3=Iteration, idx4=SubIteration, idx5+=DataColumns
                            // Detect old format by checking if index 2 is numeric (would be Iteration in old format)
                            // AND index 3 is also numeric (would be SubIteration).
                            // If index 2 has a Scope value like "[Project]" or "[Shared]", it's already new format.
                            try {
                                String idx2 = java.util.Objects.toString(record.get(2), "").trim();
                                String idx3 = java.util.Objects.toString(record.get(3), "").trim();

                                // Check if index 2 is numeric and index 3 is numeric - this indicates old 4-column format
                                // Also verify idx2 is NOT a Scope marker like "[Project]" or "[Shared]"
                                boolean idx2IsNumeric = idx2.matches("\\d+");
                                boolean idx3IsNumeric = idx3.matches("\\d+");
                                boolean idx2IsScopeMarker =
                                    idx2.startsWith("[") && idx2.endsWith("]");

                                if (idx2IsNumeric && idx3IsNumeric && !idx2IsScopeMarker) {
                                    // Old format detected (Scenario, Flow, Iteration, SubIteration, Data...)
                                    // Insert an empty Scope slot at index 2 which shifts Iteration/SubIteration/Data columns
                                    // to their correct new positions: idx2=Scope, idx3=Iteration, idx4=SubIteration, idx5+=Data
                                    try {
                                        record.add(2, "");
                                        modified = true;
                                    } catch (Exception e) {
                                        // if insertion fails for any reason, fall back to best-effort manual shift
                                        try {
                                            record.set(4, idx3);
                                            record.set(3, idx2);
                                            record.set(2, "");
                                            modified = true;
                                        } catch (Exception ex) {
                                            // ignore - will be handled later
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                // ignore index errors - record may not have enough elements
                            }
                            // ensure record has slots for all headers (Record constructor does this for new records)
                            // get current scenario and testcase values
                            String scenario = "";
                            String testcase = "";
                            try {
                                scenario = java.util.Objects.toString(record.get(0), "").trim();
                                testcase = java.util.Objects.toString(record.get(1), "").trim();
                            } catch (Exception ex) {
                                // ignore
                            }
                            String scope = "";
                            String normalized = scenario;

                            // First, check scenario for explicit scope markers
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

                            // If scope is still not determined, check the test case to infer scope
                            if (scope == null || scope.isEmpty()) {
                                try {
                                    com.ing.datalib.component.Project proj = getsProject();
                                    if (proj != null && !testcase.isEmpty()) {
                                        // Check if the test case exists in project reusable scenarios
                                        boolean foundInReusables = false;
                                        for (com.ing.datalib.component.Scenario reusableScenario : proj.getReusableScenarios()) {
                                            if (
                                                reusableScenario.getTestCaseByName(testcase) != null
                                            ) {
                                                foundInReusables = true;
                                                break;
                                            }
                                        }
                                        if (foundInReusables) {
                                            // Found in project reusables - set scope to [Project]
                                            scope = "[Project]";
                                        }
                                        // If not found in project reusables, it's from test plan - scope remains empty
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
