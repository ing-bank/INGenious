package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.APIEnvironment;
import com.ing.datalib.api.APIRequest;
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
import com.ing.ide.main.mainui.components.apitester.APITester;

import java.util.List;
import java.util.logging.Logger;

/**
 * Glues a {@link NormalizedCollection} into INGenious as reusable scenarios + reusable
 * test cases via {@link APITester#convertRequestToReusable}.
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

        for (NormalizedRequest nr : nc.getRequests()) {
            try {
                String scenarioName = resolveScenarioName(nc, nr, opts);
                Scenario scn = toReusable
                        ? project.getReusableScenarioByName(scenarioName)
                        : project.getScenarioByName(scenarioName);
                if (scn == null) {
                    scn = toReusable
                            ? project.addReusableScenario(scenarioName)
                            : project.addScenario(scenarioName);
                    if (scn != null) {
                        result.getCreatedScenarios().add(scenarioName);
                    }
                }
                if (scn == null) {
                    result.getWarnings().add(ImportWarning.error(loc(nr),
                            "Could not create scenario '" + scenarioName + "'"));
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
                req.setName(tcName);
                com.ing.datalib.component.TestCase created = toReusable
                        ? apiTester.convertRequestToReusable(req, scn, tcName)
                        : apiTester.convertRequestToTestCase(req, scn, tcName);
                if (created != null) {
                    result.incReusablesCreated();
                    result.getCreatedReusables().add(scenarioName + " / " + tcName);
                } else {
                    result.incReusablesSkipped();
                    result.getWarnings().add(ImportWarning.warn(loc(nr),
                            "Failed to create " + (toReusable ? "reusable" : "test case")
                                    + " '" + tcName + "'"));
                }
            } catch (Exception ex) {
                LOG.warning("Failed to import request " + loc(nr) + ": " + ex.getMessage());
                result.incReusablesSkipped();
                result.getWarnings().add(ImportWarning.error(loc(nr), ex.getMessage()));
            }
        }

        if (opts.isImportEnvironments()) {
            for (NormalizedEnvironment env : nc.getEnvironments()) {
                try {
                    APIEnvironment ae = new APIEnvironment(env.getName());
                    for (NormalizedVariable v : env.getVariables()) {
                        ae.setVariable(v.getKey(), v.isSecret() ? "" : v.getValue());
                        if (v.isSecret()) {
                            result.getWarnings().add(ImportWarning.warn(
                                    "env/" + env.getName() + "/" + v.getKey(),
                                    "Secret variable imported with empty value — set it manually."));
                        }
                    }
                    apiTester.addEnvironment(ae);
                    result.incEnvironmentsCreated();
                } catch (Exception ex) {
                    result.getWarnings().add(ImportWarning.warn("env/" + env.getName(),
                            "Failed to import environment: " + ex.getMessage()));
                }
            }
        }

        return result;
    }

    private static String loc(NormalizedRequest nr) {
        if (nr == null || nr.getRequest() == null) return "?";
        List<String> p = nr.getFolderPath();
        return ((p == null || p.isEmpty()) ? "" : String.join("/", p) + "/") + nr.getRequest().getName();
    }

    private static List<ImportWarning> safe(NormalizedCollection nc) {
        return nc == null ? java.util.Collections.emptyList() : java.util.Collections.emptyList();
    }

    /** Returns the target reusable scenario name based on the chosen strategy. */
    private String resolveScenarioName(NormalizedCollection nc, NormalizedRequest nr, ImportOptions opts) {
        if (opts.getTargetScenarioName() != null && !opts.getTargetScenarioName().isEmpty()) {
            return ImportUtils.sanitizeFileName(opts.getTargetScenarioName());
        }
        String prefix = opts.getScenarioPrefix() == null ? "" : opts.getScenarioPrefix();
        String base;
        switch (opts.getHierarchyStrategy()) {
            case SCENARIO_PER_TOP_FOLDER: {
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
        return ImportUtils.sanitizeFileName(prefix + base);
    }

    /** Builds the reusable test case name including any folder-flattening + conflict policy. */
    private String resolveTestCaseName(Scenario scn, NormalizedCollection nc,
                                       NormalizedRequest nr, ImportOptions opts) {
        List<String> p = nr.getFolderPath();
        StringBuilder sb = new StringBuilder();
        if (p != null && !p.isEmpty()) {
            int start = opts.getHierarchyStrategy() == ImportOptions.HierarchyStrategy.SCENARIO_PER_TOP_FOLDER
                    ? 1 : 0;
            for (int i = start; i < p.size(); i++) {
                if (sb.length() > 0) sb.append('_');
                sb.append(p.get(i));
            }
        }
        if (sb.length() > 0) sb.append('_');
        sb.append(nr.getRequest().getName());
        String candidate = ImportUtils.sanitizeFileName(sb.toString());

        if (project.hasTestCaseInAnyScenario(scn.getName(), candidate)
                || scn.getTestCaseByName(candidate) != null) {
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
                    } while (project.hasTestCaseInAnyScenario(scn.getName(), renamed)
                            || scn.getTestCaseByName(renamed) != null);
                    return renamed;
            }
        }
        return candidate;
    }
}
