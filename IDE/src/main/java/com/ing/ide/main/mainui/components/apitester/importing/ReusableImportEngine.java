package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.APIRequest;
import com.ing.datalib.api.AuthConfig;
import com.ing.datalib.api.KeyValuePair;
import com.ing.datalib.api.RequestBody;
import com.ing.datalib.api.importer.ImportOptions;
import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportUtils;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.NormalizedEnvironment;
import com.ing.datalib.api.importer.NormalizedRequest;
import com.ing.datalib.api.importer.NormalizedVariable;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ide.main.mainui.components.apitester.APITester;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Glues a {@link NormalizedCollection} into INGenious as reusable scenarios + reusable
 * test cases via {@link APITester#convertRequestToReusable}.
 * <p>
 * When {@code ImportOptions.importAsDatasheet} is enabled, also:
 * <ul>
 *   <li>Creates a datasheet named after the collection</li>
 *   <li>Creates data environments for each Postman environment</li>
 *   <li>Populates environment values into the datasheet columns</li>
 *   <li>Converts variable placeholders to INGenious datasheet syntax</li>
 * </ul>
 */
public class ReusableImportEngine {
    private static final Logger LOG = Logger.getLogger(ReusableImportEngine.class.getName());

    private final APITester apiTester;
    private final Project project;

    public ReusableImportEngine(APITester apiTester, Project project) {
        this.apiTester = apiTester;
        this.project = project;
    }

    public ImportResult importAsReusables(NormalizedCollection nc, ImportOptions opts) {
        ImportResult result = new ImportResult();
        result.setRequestsRead(nc.getRequests().size());
        result.getWarnings().addAll(safe(nc));

        final boolean toReusable = opts.getTargetType() != ImportOptions.TargetType.TEST_CASE;

        // Collect all unique variable names from environments for datasheet creation
        Set<String> allVariableNames = new LinkedHashSet<>();
        if (opts.isImportEnvironments() && !nc.getEnvironments().isEmpty()) {
            for (NormalizedEnvironment env : nc.getEnvironments()) {
                for (NormalizedVariable v : env.getVariables()) {
                    allVariableNames.add(v.getKey());
                }
            }
        }

        // Determine datasheet name (using naming convention)
        String datasheetName = ImportUtils.applyNamingConventionAndSanitize(
            nc.getName(),
            opts.getNamingConvention()
        );

        // Create datasheet and data environments if enabled
        if (opts.isImportEnvironments() && !allVariableNames.isEmpty()) {
            createDatasheetAndEnvironments(nc, datasheetName, allVariableNames, result);
        } else if (opts.isImportEnvironments() && nc.getEnvironments().isEmpty()) {
            result
                .getWarnings()
                .add(
                    ImportWarning.warn(
                        "environments",
                        "Import Environment enabled but no environment files found — continuing without datasheet creation."
                    )
                );
        }

        // Track created test cases for datasheet row creation
        List<String[]> createdTestCases = new ArrayList<>(); // [scenarioName, testCaseName]

        for (NormalizedRequest nr : nc.getRequests()) {
            try {
                String scenarioName = resolveScenarioName(nc, nr, opts);
                Scenario scn = toReusable
                    ? project.getReusableScenarioByName(scenarioName)
                    : project.getScenarioByName(scenarioName);
                if (scn == null) {
                    scn =
                        toReusable
                            ? project.addReusableScenario(scenarioName)
                            : project.addScenario(scenarioName);
                    if (scn != null) {
                        result.getCreatedScenarios().add(scenarioName);
                    }
                }
                if (scn == null) {
                    result
                        .getWarnings()
                        .add(
                            ImportWarning.error(
                                loc(nr),
                                "Could not create scenario '" + scenarioName + "'"
                            )
                        );
                    result.incReusablesSkipped();
                    continue;
                }

                String tcName = resolveTestCaseName(scn, nc, nr, opts);
                if (tcName == null) {
                    result.incReusablesSkipped();
                    continue;
                }

                // OVERWRITE policy: drop existing target first
                if (opts.getConflictPolicy() == ImportOptions.ConflictPolicy.OVERWRITE) {
                    com.ing.datalib.component.TestCase existing = scn.getTestCaseByName(tcName);
                    if (existing != null) {
                        scn.removeTestCase(existing);
                    }
                }

                APIRequest req = nr.getRequest();

                // Convert variables to datasheet syntax if datasheet import is enabled
                if (opts.isImportEnvironments() && !allVariableNames.isEmpty()) {
                    convertRequestVariables(req, datasheetName, allVariableNames);
                }

                req.setName(tcName);
                com.ing.datalib.component.TestCase created = toReusable
                    ? apiTester.convertRequestToReusable(req, scn, tcName)
                    : apiTester.convertRequestToTestCase(req, scn, tcName);
                if (created != null) {
                    result.incReusablesCreated();
                    result.getCreatedReusables().add(scenarioName + " / " + tcName);
                    createdTestCases.add(new String[] { scenarioName, tcName });
                } else {
                    result.incReusablesSkipped();
                    result
                        .getWarnings()
                        .add(
                            ImportWarning.warn(
                                loc(nr),
                                "Failed to create " +
                                (toReusable ? "reusable" : "test case") +
                                " '" +
                                tcName +
                                "'"
                            )
                        );
                }
            } catch (Exception ex) {
                LOG.warning("Failed to import request " + loc(nr) + ": " + ex.getMessage());
                result.incReusablesSkipped();
                result.getWarnings().add(ImportWarning.error(loc(nr), ex.getMessage()));
            }
        }

        // Create datasheet rows for all imported test cases/reusables when environments are imported
        if (opts.isImportEnvironments() && !allVariableNames.isEmpty()) {
            createDatasheetRows(datasheetName, createdTestCases, nc.getEnvironments(), result);
        }

        return result;
    }

    /**
     * Creates data environments and datasheets with columns from environment variables.
     * Only creates the structure (environments and columns) - rows are populated later
     * in createDatasheetRows after test cases are generated.
     */
    private void createDatasheetAndEnvironments(
        NormalizedCollection nc,
        String datasheetName,
        Set<String> variableNames,
        ImportResult result
    ) {
        result.setDatasheetName(datasheetName);

        // Process each Postman environment as a data environment
        for (NormalizedEnvironment env : nc.getEnvironments()) {
            String envName = env.getName();
            if (envName == null || envName.trim().isEmpty()) {
                envName = "Imported_Environment";
            }

            try {
                // Create data environment if it doesn't exist
                if (!project.getTestData().getEnvironments().contains(envName)) {
                    project.getTestData().createNewEnvironment(envName);
                    result.incDataEnvironmentsCreated();
                    result.getCreatedDataEnvironments().add(envName);
                }

                // Get the TestData for this environment
                TestData envTestData = project.getTestData().getTestDataFor(envName);
                if (envTestData == null) {
                    result
                        .getWarnings()
                        .add(
                            ImportWarning.warn(
                                "dataenv/" + envName,
                                "Failed to access data environment — skipping."
                            )
                        );
                    continue;
                }

                // Create or get the datasheet in this environment
                TestDataModel datasheet = envTestData.getByName(datasheetName);
                if (datasheet == null) {
                    datasheet = envTestData.addTestData(envTestData.getNewTestData(datasheetName));
                    if (datasheet != null) {
                        // Remove default Data1 and Data2 columns that are auto-created
                        removeDefaultColumns(datasheet);
                        result.incDatasheetsCreated();
                    }
                }

                if (datasheet == null) {
                    result
                        .getWarnings()
                        .add(
                            ImportWarning.warn(
                                "datasheet/" + envName + "/" + datasheetName,
                                "Failed to create datasheet in environment '" + envName + "'."
                            )
                        );
                    continue;
                }

                // Add columns for each variable (skip duplicates)
                Set<String> existingColumns = new HashSet<>(datasheet.getColumns());
                int columnsAdded = 0;
                for (String varName : variableNames) {
                    if (!existingColumns.contains(varName)) {
                        datasheet.addColumn(varName);
                        columnsAdded++;
                    }
                }

                if (envName.equals(nc.getEnvironments().get(0).getName())) {
                    // Only count columns once (for first environment)
                    result.setDatasheetColumnsCreated(columnsAdded);
                }

                // Save the datasheet structure (rows will be added later in createDatasheetRows)
                datasheet.save();
            } catch (Exception ex) {
                LOG.warning(
                    "Failed to create data environment " + envName + ": " + ex.getMessage()
                );
                result
                    .getWarnings()
                    .add(
                        ImportWarning.error(
                            "dataenv/" + envName,
                            "Failed to create data environment: " + ex.getMessage()
                        )
                    );
            }
        }

        // Save environment configuration to persist the new environments
        try {
            project.getTestData().save();
        } catch (Exception ex) {
            LOG.warning("Failed to save environment configuration: " + ex.getMessage());
        }
    }

    /**
     * Removes the default Data1 and Data2 columns that are auto-created by the framework.
     * These columns should not exist in imported datasheets - only environment variables should be columns.
     */
    private void removeDefaultColumns(TestDataModel datasheet) {
        if (datasheet.getColumns().contains("Data1")) {
            datasheet.removeColumn("Data1");
        }
        if (datasheet.getColumns().contains("Data2")) {
            datasheet.removeColumn("Data2");
        }
    }

    /**
     * Creates rows in the datasheet for each imported test case.
     * Each row contains: Scenario Name, Test Case Name, Iteration=1, SubIteration=1, Scope=Project,
     * plus all environment variable values populated from the corresponding environment.
     *
     * Note: Datasheets are ONLY created inside the imported environment folders,
     * not in the default/global location.
     */
    private void createDatasheetRows(
        String datasheetName,
        List<String[]> testCases,
        List<NormalizedEnvironment> environments,
        ImportResult result
    ) {
        // Create rows ONLY in the imported data environments where the datasheet exists
        // Do NOT create datasheets in the default environment
        for (NormalizedEnvironment env : environments) {
            String envName = env.getName();
            if (envName == null || envName.trim().isEmpty()) {
                envName = "Imported_Environment";
            }

            TestData envData = project.getTestData().getTestDataFor(envName);
            if (envData == null) {
                continue;
            }

            TestDataModel datasheet = envData.getByName(datasheetName);
            if (datasheet == null) {
                continue;
            }

            datasheet.loadTableModel();

            // Create one row per test case, with environment values populated in each row
            for (String[] tc : testCases) {
                String scenarioName = tc[0];
                String testCaseName = tc[1];

                // Create a row with scenario and test case populated
                Record record = (Record) datasheet.addRecord();
                record.setScenario(scenarioName);
                record.setTestcase(testCaseName);
                record.setIteration("1");
                record.setSubIteration("1");
                record.setScope("Project");

                // Populate environment variable values in this row
                int rowIndex = datasheet.getRowCount() - 1;
                for (NormalizedVariable v : env.getVariables()) {
                    int colIndex = datasheet.getColumnIndex(v.getKey());
                    if (colIndex >= 0) {
                        String value = v.isSecret() ? "" : v.getValue();
                        datasheet.setValueAt(value, rowIndex, colIndex);
                    }
                }

                // Only count rows for first environment to avoid double counting
                if (envName.equals(environments.get(0).getName())) {
                    result.incDatasheetRowsCreated();
                }
            }

            datasheet.save();
        }
    }

    /**
     * Converts all variable placeholders in an APIRequest from %var% or {{var}}
     * to INGenious datasheet syntax {datasheet:var}.
     */
    private void convertRequestVariables(
        APIRequest req,
        String datasheetName,
        Set<String> knownVariables
    ) {
        // Convert URL
        req.setUrl(
            ImportUtils.convertToDatasheetSyntax(req.getUrl(), datasheetName, knownVariables)
        );

        // Convert query parameters
        for (KeyValuePair kv : req.getQueryParams()) {
            kv.setKey(
                ImportUtils.convertToDatasheetSyntax(kv.getKey(), datasheetName, knownVariables)
            );
            kv.setValue(
                ImportUtils.convertToDatasheetSyntax(kv.getValue(), datasheetName, knownVariables)
            );
        }

        // Convert headers
        for (KeyValuePair kv : req.getHeaders()) {
            kv.setKey(
                ImportUtils.convertToDatasheetSyntax(kv.getKey(), datasheetName, knownVariables)
            );
            kv.setValue(
                ImportUtils.convertToDatasheetSyntax(kv.getValue(), datasheetName, knownVariables)
            );
        }

        // Convert path variables
        for (KeyValuePair kv : req.getPathVariables()) {
            kv.setKey(
                ImportUtils.convertToDatasheetSyntax(kv.getKey(), datasheetName, knownVariables)
            );
            kv.setValue(
                ImportUtils.convertToDatasheetSyntax(kv.getValue(), datasheetName, knownVariables)
            );
        }

        // Convert body
        RequestBody body = req.getBody();
        if (body != null) {
            body.setRawContent(
                ImportUtils.convertToDatasheetSyntax(
                    body.getRawContent(),
                    datasheetName,
                    knownVariables
                )
            );
            body.setGraphqlQuery(
                ImportUtils.convertToDatasheetSyntax(
                    body.getGraphqlQuery(),
                    datasheetName,
                    knownVariables
                )
            );
            body.setGraphqlVariables(
                ImportUtils.convertToDatasheetSyntax(
                    body.getGraphqlVariables(),
                    datasheetName,
                    knownVariables
                )
            );

            if (body.getFormData() != null) {
                for (KeyValuePair kv : body.getFormData()) {
                    kv.setKey(
                        ImportUtils.convertToDatasheetSyntax(
                            kv.getKey(),
                            datasheetName,
                            knownVariables
                        )
                    );
                    kv.setValue(
                        ImportUtils.convertToDatasheetSyntax(
                            kv.getValue(),
                            datasheetName,
                            knownVariables
                        )
                    );
                }
            }

            if (body.getUrlEncodedData() != null) {
                for (KeyValuePair kv : body.getUrlEncodedData()) {
                    kv.setKey(
                        ImportUtils.convertToDatasheetSyntax(
                            kv.getKey(),
                            datasheetName,
                            knownVariables
                        )
                    );
                    kv.setValue(
                        ImportUtils.convertToDatasheetSyntax(
                            kv.getValue(),
                            datasheetName,
                            knownVariables
                        )
                    );
                }
            }
        }

        // Convert auth config
        AuthConfig auth = req.getAuth();
        if (auth != null) {
            auth.setBasicUsername(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getBasicUsername(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setBasicPassword(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getBasicPassword(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setBearerToken(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getBearerToken(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setApiKeyName(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getApiKeyName(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setApiKeyValue(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getApiKeyValue(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setOauth2AccessToken(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getOauth2AccessToken(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setOauth2TokenUrl(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getOauth2TokenUrl(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setOauth2ClientId(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getOauth2ClientId(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setOauth2ClientSecret(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getOauth2ClientSecret(),
                    datasheetName,
                    knownVariables
                )
            );
            auth.setOauth2Scope(
                ImportUtils.convertToDatasheetSyntax(
                    auth.getOauth2Scope(),
                    datasheetName,
                    knownVariables
                )
            );
        }
    }

    private static String loc(NormalizedRequest nr) {
        if (nr == null || nr.getRequest() == null) return "?";
        List<String> p = nr.getFolderPath();
        return (
            ((p == null || p.isEmpty()) ? "" : String.join("/", p) + "/") +
            nr.getRequest().getName()
        );
    }

    private static List<ImportWarning> safe(NormalizedCollection nc) {
        return nc == null ? java.util.Collections.emptyList() : java.util.Collections.emptyList();
    }

    /** Returns the target reusable scenario name based on the chosen strategy. */
    private String resolveScenarioName(
        NormalizedCollection nc,
        NormalizedRequest nr,
        ImportOptions opts
    ) {
        if (opts.getTargetScenarioName() != null && !opts.getTargetScenarioName().isEmpty()) {
            return ImportUtils.applyNamingConventionAndSanitize(
                opts.getTargetScenarioName(),
                opts.getNamingConvention()
            );
        }
        // Preserve user-entered prefix exactly as provided (no normalization)
        String prefix = opts.getScenarioPrefix() == null ? "" : opts.getScenarioPrefix();
        String base;
        switch (opts.getHierarchyStrategy()) {
            case SCENARIO_PER_TOP_FOLDER:
                {
                    List<String> p = nr.getFolderPath();
                    if (p == null || p.isEmpty()) {
                        base = nc.getName();
                    } else {
                        base = p.get(0);
                    }
                    break;
                }
            case FLATTEN:
            default:
                base = nc.getName();
                break;
        }
        // Apply naming convention only to base name, then prepend prefix as-is
        String convertedBase = ImportUtils.applyNamingConventionAndSanitize(
            base,
            opts.getNamingConvention()
        );
        return prefix + convertedBase;
    }

    /** Builds the reusable test case name including any folder-flattening + conflict policy. */
    private String resolveTestCaseName(
        Scenario scn,
        NormalizedCollection nc,
        NormalizedRequest nr,
        ImportOptions opts
    ) {
        List<String> p = nr.getFolderPath();
        StringBuilder sb = new StringBuilder();
        if (p != null && !p.isEmpty()) {
            int start = opts.getHierarchyStrategy() ==
                ImportOptions.HierarchyStrategy.SCENARIO_PER_TOP_FOLDER
                ? 1
                : 0;
            for (int i = start; i < p.size(); i++) {
                if (sb.length() > 0) sb.append('_');
                sb.append(p.get(i));
            }
        }
        if (sb.length() > 0) sb.append('_');
        sb.append(nr.getRequest().getName());
        String candidate = ImportUtils.applyNamingConventionAndSanitize(
            sb.toString(),
            opts.getNamingConvention()
        );

        if (
            project.hasTestCaseInAnyScenario(scn.getName(), candidate) ||
            scn.getTestCaseByName(candidate) != null
        ) {
            switch (opts.getConflictPolicy()) {
                case SKIP:
                    return null;
                case OVERWRITE:
                    return candidate;
                case RENAME_SUFFIX:
                default:
                    int n = 2;
                    String renamed;
                    do {
                        renamed = candidate + "_" + n;
                        n++;
                    } while (
                        project.hasTestCaseInAnyScenario(scn.getName(), renamed) ||
                        scn.getTestCaseByName(renamed) != null
                    );
                    return renamed;
            }
        }
        return candidate;
    }
}
