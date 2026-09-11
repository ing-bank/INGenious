package com.ing.datalib.component;

import static com.ing.datalib.component.utils.FileUtils.DIR_FILTER;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.io.ProjectMigrator;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.component.utils.NamingUtils;
import com.ing.datalib.component.utils.SortOrderStore;
import com.ing.datalib.exception.TestCaseConversionException;
import com.ing.datalib.model.DataItem;
import com.ing.datalib.model.Meta;
import com.ing.datalib.model.ProjectInfo;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebOR.ORScope;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.util.data.FileScanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 * Represents an automation project and acts as the central entry point for loading, managing,
 * and persisting project data from disk.
 * <p>
 * A {@code Project} encapsulates the project’s filesystem location and name, and maintains the
 * in-memory model of core assets such as scenarios (TestPlan), releases/test sets (TestLab),
 * environment test data, project settings, and the {@link ObjectRepository}. It supports loading
 * and reloading from disk, saving all managed components, and producing table models for UI
 * components via {@code getTableModelFor(...)}.
 * </p>
 *
 * <p>
 * The class also provides refactoring utilities that propagate renames across scenarios, releases,
 * and test data (e.g., scenario/test case renames, page/object reference updates, and test data
 * renames), including scope-aware refactoring for Object Repository references where applicable.
 * </p>
 */
public class Project {
    private static final Logger LOGGER = Logger.getLogger(Project.class.getName());

    public static final String TEST_PLAN_DIR = "TestPlan";

    public static final String REUSABLE_COMPONENTS_DIR = "ReusableComponents";

    public static final String SHARED_REUSABLE_COMPONENTS_DIR = "SharedReusableComponents";

    public static final String SHARED_TEST_DATA_DIR = "SharedTestData";

    private List<Scenario> scenarios = new ArrayList<>();

    private final List<Scenario> reusableScenarios = new ArrayList<>();

    private final List<Scenario> sharedReusableScenarios = new ArrayList<>();

    private final List<Release> releases = new ArrayList<>();

    private String testdataType;

    private EnvTestData testData;

    private EnvTestData sharedTestData;

    private String location;

    private String name;

    private ProjectSettings projectSettings;

    private ObjectRepository objectRepository;

    private ProjectInfo projectInfo;

    private int lastImpactedReusableReferenceUpdates = 0;

    /**
     * When true, skips auto-migrations on project load to ensure read-only validation.
     * Used by validation operations to prevent unintended file modifications.
     */
    private boolean readOnlyMode = false;

    /**
     * Returns whether this Project is in read-only mode.
     * When true, no migrations, transformations, or file saves should occur.
     *
     * @return true if in read-only mode, false otherwise
     */
    public boolean isReadOnlyMode() {
        return readOnlyMode;
    }

    /**
     * Constructs a new project with the specified name, location, and test data type.
     * @param name project name
     * @param projectLocation parent directory where the project will be located
     * @param testdataType type of test data (e.g., "csv")
     */
    public Project(String name, String projectLocation, String testdataType) {
        this.location = projectLocation + File.separator + name;
        this.testdataType = testdataType;
        this.name = name;
        load();
    }

    /**
     * Constructs a new project from an existing project location.
     * @param projectLocation absolute path to the project directory
     * @param testdataType type of test data (e.g., "csv")
     */
    public Project(String projectLocation, String testdataType) {
        this.name = new File(projectLocation).getName();
        this.location = projectLocation;
        this.testdataType = testdataType;
        load();
    }

    /**
     * Constructs a new project from an existing project location with default CSV test data type.
     * @param projectLocation absolute path to the project directory
     */
    public Project(String projectLocation) {
        this(projectLocation, "csv");
    }

    /**
     * Constructs a new project from an existing project location in read-only mode.
     * When readOnlyMode is true, auto-migrations (CSV to YAML, XML, legacy references)
     * are skipped to ensure the project structure is not modified during inspection.
     * This is used for validation operations where no file modifications should occur.
     * @param projectLocation absolute path to the project directory
     * @param readOnlyMode when true, skips all migration logic during load
     */
    public Project(String projectLocation, boolean readOnlyMode) {
        this.name = new File(projectLocation).getName();
        this.location = projectLocation;
        this.testdataType = "csv";
        this.readOnlyMode = readOnlyMode;
        load();
    }

    /**
     * Initiates the project loading process.
     */
    private void load() {
        loadProject();
    }

    /**
     * Creates a new project with default scenarios, test cases, releases, and test sets.
     * @return this project instance
     */
    public Project createProject() {
        addScenario("NewScenario").addTestCase("NewTestCase");
        addRelease("NewRelease").addTestSet("NewTestSet");
        loadTestDatas();
        projectInfo = loadProjectInfo(getProjectFile());
        return this;
    }

    /**
     * Loads all project components from disk including scenarios, test sets, test data, settings, and object repository.
     * Performs migration of legacy reusable component XML if present and auto-migrates CSV test cases to YAML if enabled.
     * When in read-only mode, all migrations are skipped to prevent file modifications.
     */
    private void loadProject() {
        // Load project info early to check migration flags
        projectInfo = loadProjectInfo(getProjectFile());

        // ════════════════════════════════════════════════════════════════════
        // Phase 1: Load all project components (read-only, always executed)
        // ════════════════════════════════════════════════════════════════════
        loadScenariosFromTestPlan();
        loadTestSets();
        loadScenariosFromTestPlan();
        loadScenariosFromReusableComponents();
        loadScenariosFromSharedReusableComponents();
        loadTestDatas();
        projectSettings = new ProjectSettings(this, readOnlyMode);
        objectRepository = new ObjectRepository(this, readOnlyMode);
        // Note: ObjectRepository constructor now receives readOnlyMode to skip XML->YAML migration

        // ════════════════════════════════════════════════════════════════════
        // Phase 1.5: Propagate read-only mode to components
        // ════════════════════════════════════════════════════════════════════
        if (readOnlyMode) {
            // Propagate read-only mode to prevent migrations during validation
            for (Scenario scenario : scenarios) {
                scenario.setReadOnlyMode(true);
            }
            for (Scenario scenario : reusableScenarios) {
                scenario.setReadOnlyMode(true);
            }
            for (Scenario scenario : sharedReusableScenarios) {
                scenario.setReadOnlyMode(true);
            }
            // Note: TestData readOnlyMode is propagated in EnvTestData.loadForEnv()
        }

        // ════════════════════════════════════════════════════════════════════
        // Phase 2: Apply migrations and reconciliation (skipped in read-only)
        // ════════════════════════════════════════════════════════════════════
        if (!readOnlyMode) {
            // Auto-migrate CSV test cases to YAML if enabled
            migrateTestsFromCsvToYaml();

            // Migrate reusable component XML if present
            migrateReusableComponentXmlIfPresent();

            // Migrate legacy Execute references
            migrateLegacyReusableExecuteReferencesOnLoad();

            // Reconcile shared reusable project tracking to clean stale entries
            try {
                reconcileSharedReusableProjectsItems();
            } catch (Exception ex) {
                Logger
                    .getLogger(Project.class.getName())
                    .log(Level.WARNING, "Failed to reconcile shared reusable projects items", ex);
            }

            // Reconcile shared test data project tracking to clean stale entries
            try {
                reconcileSharedTestDataProjectsItems();
            } catch (Exception ex) {
                Logger
                    .getLogger(Project.class.getName())
                    .log(Level.WARNING, "Failed to reconcile shared test data projects items", ex);
            }
            registerSharedTestDataUsage();
        }
    }

    /**
     * Migrates legacy unscoped Execute reusable references during project load.
     *
     * <p>This eagerly loads test cases once and applies TestCase-level migration
     * (including mandatory Project-first fallback for unscoped legacy references).</p>
     */
    private void migrateLegacyReusableExecuteReferencesOnLoad() {
        int testCasesScanned = 0;
        for (Scenario scenario : getAllScenarios()) {
            for (TestCase testCase : scenario.getTestCases()) {
                testCase.loadTestCaseTableModel();
                testCasesScanned++;
            }
        }

        LOGGER.log(
            Level.FINE,
            "Legacy Execute reference migration check completed for {0} test case(s)",
            testCasesScanned
        );
    }

    /**
     * Auto-migrates CSV test cases to YAML format if enabled via project configuration.
     *
     * <p>This method checks the {@code autoMigrateCsvToYaml} flag in projectinfo.json.
     * Migration is enabled by default (when flag is null or true). Set to {@code false}
     * to explicitly disable auto-migration.</p>
     *
     * <p>This invokes {@link ProjectMigrator#migrate(File, boolean, boolean)} to
     * convert all CSV test cases, reusable components, and test sets to YAML format.</p>
     *
     * <p>The {@code keepCsvBackupOnMigrate} flag controls whether original CSV files are
     * moved to {@code .migration-backup/} or deleted after successful migration.</p>
     *
     * <p>Migration is performed once during project load. If migration fails, an error is
     * logged but project loading continues to ensure backward compatibility.</p>
     */
    private void migrateTestsFromCsvToYaml() {
        if (projectInfo == null) {
            return; // No project info available, skip migration
        }

        // Check if auto-migration is explicitly disabled (defaults to true if not set)
        Boolean autoMigrateFlag = projectInfo.getAutoMigrateCsvToYaml();
        // Default value of autoMigrateFlag is true when not set
        boolean autoMigrate = autoMigrateFlag == null || autoMigrateFlag;
        if (!autoMigrate) {
            return; // Auto-migration explicitly disabled
        }

        // Determine whether to keep CSV backups (defaults to true for safety)
        Boolean keepBackupFlag = projectInfo.getKeepCsvBackupOnMigrate();
        // Default value of keepBackupFlag is true when not set
        boolean keepBackup = keepBackupFlag == null || keepBackupFlag;

        try {
            LOGGER.log(
                Level.INFO,
                "Auto-migrating CSV test cases to YAML (keepBackup={0})...",
                keepBackup
            );

            ProjectMigrator.Result result = ProjectMigrator.migrate(
                new File(location),
                false, // not a dry run
                keepBackup
            );

            // Log migration results
            if (result.hasChanges()) {
                LOGGER.log(
                    Level.INFO,
                    "CSV to YAML migration completed: {0} file(s) converted, {1} conflict(s), {2} error(s)",
                    new Object[] {
                        result.converted.size(),
                        result.conflicts.size(),
                        result.errors.size()
                    }
                );

                if (!result.errors.isEmpty()) {
                    for (String error : result.errors) {
                        LOGGER.log(Level.WARNING, "Migration error: {0}", error);
                    }
                }

                // Reload test cases in all scenarios to pick up newly created YAML files
                reloadScenarios();
            } else {
                LOGGER.log(
                    Level.FINE,
                    "No CSV files to migrate (already migrated or YAML-only project)"
                );
            }
        } catch (Exception ex) {
            LOGGER.log(
                Level.WARNING,
                "Auto-migration from CSV to YAML failed: " + ex.getMessage(),
                ex
            );
            // Don't throw - allow project to load even if migration fails
        }
    }

    /**
     * Reloads test cases for all scenarios (test plan, reusable, and shared reusable).
     * This ensures that Scenario objects pick up the latest test case data on disk,
     * such as after a CSV-to-YAML migration replaces the underlying files.
     */
    private void reloadScenarios() {
        int reloadedCount = 0;

        // Reload test plan scenarios
        for (Scenario scenario : scenarios) {
            scenario.reloadTestCases();
            reloadedCount++;
        }

        // Reload reusable component scenarios
        for (Scenario scenario : reusableScenarios) {
            scenario.reloadTestCases();
            reloadedCount++;
        }

        // Reload shared reusable scenarios
        for (Scenario scenario : sharedReusableScenarios) {
            scenario.reloadTestCases();
            reloadedCount++;
        }

        LOGGER.log(Level.INFO, "Reloaded {0} scenario(s) to reflect YAML migration", reloadedCount);
    }

    /**
     * Reconciles the shared reusable projects.items file by removing stale project entries.
     * Validates that all projects in the file still exist at their recorded paths.
     */
    private void reconcileSharedReusableProjectsItems() {
        try {
            File sharedRoot = new File(getSharedReusableComponentsPath());
            File projectsFile = new File(sharedRoot, "projects.items");

            if (!projectsFile.exists()) {
                return; // No projects.items file to reconcile
            }

            try {
                String content = FileScanner.readFile(projectsFile);
                if (content == null || content.isEmpty()) {
                    return; // Empty file, nothing to reconcile
                }

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<java.util.Map<String, String>> projects = mapper.readValue(
                    content,
                    mapper
                        .getTypeFactory()
                        .constructCollectionType(java.util.List.class, java.util.Map.class)
                );

                // Filter out stale entries - keep only projects that still exist on disk
                java.util.List<java.util.Map<String, String>> validProjects = new java.util.ArrayList<>();
                for (java.util.Map<String, String> proj : projects) {
                    String projectPath = proj.get("path");
                    if (projectPath != null && !projectPath.isEmpty()) {
                        File projectDir = new File(projectPath);
                        // Keep entry if the project directory exists
                        if (projectDir.exists() && projectDir.isDirectory()) {
                            validProjects.add(proj);
                        }
                    }
                }

                // Write reconciled list back only if changes were made
                if (validProjects.size() != projects.size()) {
                    String jsonOutput = mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(validProjects);

                    // Atomic write: write to temp and rename
                    File tmp = new File(projectsFile.getPath() + ".tmp");
                    FileScanner.writeFile(tmp, jsonOutput);
                    if (tmp.exists()) {
                        if (!tmp.renameTo(projectsFile)) {
                            // Fallback if rename fails
                            FileScanner.writeFile(projectsFile, jsonOutput);
                        }
                    }
                }
            } catch (Exception ex) {
                Logger
                    .getLogger(Project.class.getName())
                    .log(Level.WARNING, "Failed to read projects.items during reconciliation", ex);
            }
        } catch (Exception ex) {
            Logger
                .getLogger(Project.class.getName())
                .log(Level.WARNING, "Error reconciling shared reusable projects.items", ex);
        }
    }

    /**
     * Reconciles the shared test data projects.items file by removing stale project entries.
     * Validates that all projects in the file still exist at their recorded paths.
     */
    private void reconcileSharedTestDataProjectsItems() {
        try {
            File sharedRoot = new File(getSharedTestDataPath());
            File projectsFile = new File(sharedRoot, "projects.items");

            if (!projectsFile.exists()) {
                return; // No projects.items file to reconcile
            }

            try {
                String content = FileScanner.readFile(projectsFile);
                if (content == null || content.isEmpty()) {
                    return; // Empty file, nothing to reconcile
                }

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.List<java.util.Map<String, String>> projects = mapper.readValue(
                    content,
                    mapper
                        .getTypeFactory()
                        .constructCollectionType(java.util.List.class, java.util.Map.class)
                );

                // Filter out stale entries - keep only projects that still exist on disk
                java.util.List<java.util.Map<String, String>> validProjects = new java.util.ArrayList<>();
                for (java.util.Map<String, String> proj : projects) {
                    String projectPath = proj.get("path");
                    if (projectPath != null && !projectPath.isEmpty()) {
                        File projectDir = new File(projectPath);
                        // Keep entry if the project directory exists
                        if (projectDir.exists() && projectDir.isDirectory()) {
                            validProjects.add(proj);
                        }
                    }
                }

                // Write reconciled list back only if changes were made
                if (validProjects.size() != projects.size()) {
                    String jsonOutput = mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(validProjects);

                    // Atomic write: write to temp and rename
                    File tmp = new File(projectsFile.getPath() + ".tmp");
                    FileScanner.writeFile(tmp, jsonOutput);
                    if (tmp.exists()) {
                        if (!tmp.renameTo(projectsFile)) {
                            // Fallback if rename fails
                            FileScanner.writeFile(projectsFile, jsonOutput);
                        }
                    }
                }
            } catch (Exception ex) {
                Logger
                    .getLogger(Project.class.getName())
                    .log(Level.WARNING, "Failed to read projects.items during reconciliation", ex);
            }
        } catch (Exception ex) {
            Logger
                .getLogger(Project.class.getName())
                .log(Level.WARNING, "Error reconciling shared test data projects.items", ex);
        }
    }

    /**
     * True when any test step in this project (Test Plan, Project Reusables or Shared
     * Reusables) carries a {@code [Shared]}-scoped Test Data reference - i.e. the project
     * consumes the app-root Shared Test Data store.
     *
     * @return whether this project references Shared Test Data
     */
    public boolean usesSharedTestData() {
        for (Scenario scenario : getAllScenarios()) {
            for (TestCase testCase : scenario.getTestCases()) {
                testCase.loadTableModel();
                for (TestStep step : testCase.getTestSteps()) {
                    if (
                        (step.isTestDataStep() && "[Shared]".equals(step.getTestDataScopeTag())) ||
                        TestStep.containsSharedTestDataToken(step.getInput()) ||
                        TestStep.containsSharedTestDataToken(step.getCondition())
                    ) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adds {@code project} to {@code Shared/SharedTestData/projects.items} if not already
     * present (never removes). The lightweight per-save counterpart to
     * {@link #registerSharedTestDataUsage()} - called from {@link TestCase#save()} whenever a
     * saved test case carries a {@code [Shared]} Test Data reference, mirroring how Shared
     * Reusable consumers are tracked.
     *
     * @param project the project to record as a Shared Test Data consumer
     */
    public static void addSharedTestDataProjectEntry(Project project) {
        if (project == null || project.getName() == null) {
            return;
        }
        try {
            File sharedRoot = new File(getSharedTestDataPath());
            if (!sharedRoot.exists()) {
                sharedRoot.mkdirs();
            }
            File projectsFile = new File(sharedRoot, "projects.items");

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, String>> projects = new java.util.ArrayList<>();
            if (projectsFile.exists()) {
                String content = FileScanner.readFile(projectsFile);
                if (content != null && !content.isEmpty()) {
                    projects =
                        mapper.readValue(
                            content,
                            mapper
                                .getTypeFactory()
                                .constructCollectionType(java.util.List.class, java.util.Map.class)
                        );
                }
            }

            String path = project.getLocation();
            if (projects.stream().anyMatch(p -> path.equals(p.get("path")))) {
                return;
            }
            java.util.Map<String, String> entry = new java.util.LinkedHashMap<>();
            entry.put("name", project.getName());
            entry.put("path", path);
            projects.add(entry);

            String jsonOutput = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(projects);
            File tmp = new File(projectsFile.getPath() + ".tmp");
            FileScanner.writeFile(tmp, jsonOutput);
            if (!tmp.exists() || !tmp.renameTo(projectsFile)) {
                FileScanner.writeFile(projectsFile, jsonOutput);
            }
        } catch (Exception ex) {
            Logger
                .getLogger(Project.class.getName())
                .log(Level.WARNING, "Failed to add shared test data project entry", ex);
        }
    }

    /**
     * Keeps this project's entry in {@code Shared/SharedTestData/projects.items} in step with
     * whether it currently {@link #usesSharedTestData() references Shared Test Data} - adding
     * the entry when it does, removing it when it no longer does. Mirrors the Shared Reusable
     * Components / Shared Object Repository {@code projects.items} convention so the Shared
     * Test Data panel can warn which projects a rename/delete would impact.
     */
    public void registerSharedTestDataUsage() {
        try {
            boolean uses = usesSharedTestData();
            File sharedRoot = new File(getSharedTestDataPath());
            File projectsFile = new File(sharedRoot, "projects.items");
            if (!uses && !projectsFile.exists()) {
                return;
            }
            if (!sharedRoot.exists()) {
                if (!uses) {
                    return;
                }
                sharedRoot.mkdirs();
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.List<java.util.Map<String, String>> projects = new java.util.ArrayList<>();
            if (projectsFile.exists()) {
                String content = FileScanner.readFile(projectsFile);
                if (content != null && !content.isEmpty()) {
                    projects =
                        mapper.readValue(
                            content,
                            mapper
                                .getTypeFactory()
                                .constructCollectionType(java.util.List.class, java.util.Map.class)
                        );
                }
            }

            boolean present = projects.stream().anyMatch(p -> location.equals(p.get("path")));
            boolean changed = false;
            if (uses && !present) {
                java.util.Map<String, String> entry = new java.util.LinkedHashMap<>();
                entry.put("name", name);
                entry.put("path", location);
                projects.add(entry);
                changed = true;
            } else if (!uses && present) {
                projects.removeIf(p -> location.equals(p.get("path")));
                changed = true;
            }
            if (!changed) {
                return;
            }

            String jsonOutput = mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(projects);
            FileScanner.writeFile(projectsFile, jsonOutput);
        } catch (Exception ex) {
            Logger
                .getLogger(Project.class.getName())
                .log(Level.WARNING, "Failed to register shared test data usage", ex);
        }
    }

    /**
     * Projects other than this one recorded in {@code Shared/SharedTestData/projects.items} as
     * consumers of Shared Test Data. Each item is {@code "name | path"} (or just the name when
     * no path was recorded). Used to warn before a Shared Test Data rename / delete.
     *
     * @return referencing project labels, empty when none / file missing
     */
    public List<String> getOtherProjectsUsingSharedTestData() {
        List<String> result = new ArrayList<>();
        try {
            File projectsFile = new File(getSharedTestDataPath(), "projects.items");
            if (!projectsFile.exists()) {
                return result;
            }
            String content = FileScanner.readFile(projectsFile);
            if (content == null || content.trim().isEmpty()) {
                return result;
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, String>> projects = mapper.readValue(
                content,
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
            for (Map<String, String> proj : projects) {
                String projName = proj.get("name");
                String projPath = proj.get("path");
                if (projName == null || projName.isEmpty()) {
                    continue;
                }
                if (projName.equals(name) && projPath != null && projPath.equals(location)) {
                    continue; // exclude the current project
                }
                result.add(
                    projPath == null || projPath.isEmpty() ? projName : projName + " | " + projPath
                );
            }
        } catch (Exception ex) {
            Logger
                .getLogger(Project.class.getName())
                .log(Level.WARNING, "Failed to read shared test data projects.items", ex);
        }
        return result;
    }

    /**
     * Returns the project information metadata.
     * @return project information
     */
    public ProjectInfo getInfo() {
        return projectInfo;
    }

    /**
     * Returns the list of scenarios in the Test Plan.
     * @return list of Test Plan scenarios
     */
    public List<Scenario> getScenarios() {
        return scenarios;
    }

    /**
     * Returns the list of reusable scenarios.
     * @return list of Reusable Components scenarios
     */
    public List<Scenario> getReusableScenarios() {
        return reusableScenarios;
    }

    /**
     * Returns all scenarios from both Test Plan and Reusable Components.
     * @return combined list of all scenarios
     */
    public List<Scenario> getAllScenarios() {
        return Stream
            .concat(
                Stream.concat(scenarios.stream(), reusableScenarios.stream()),
                sharedReusableScenarios.stream()
            )
            .collect(toList());
    }

    /**
     * Returns all releases in the project.
     * @return list of releases
     */
    public List<Release> getReleases() {
        return releases;
    }

    /**
     * Finds a scenario by name in the Test Plan.
     * @param name scenario name to search for (case-insensitive)
     * @return the scenario if found, null otherwise
     */
    public Scenario getScenarioByName(String name) {
        for (Scenario scenario : getAllScenarios()) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                return scenario;
            }
        }
        return null;
    }

    /**
     * Finds a reusable scenario by name, excluding deleted scenarios.
     * @param name scenario name to search for (case-insensitive)
     * @return the reusable scenario if found and active, null otherwise
     */
    public Scenario getReusableScenarioByName(String name) {
        for (Scenario scenario : reusableScenarios) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                // Verify the scenario folder still exists on disk
                if (new File(scenario.getLocation()).exists()) {
                    return scenario;
                }
            }
        }
        return null;
    }

    /**
     * Finds a test plan scenario by name.
     * @param name scenario name to search for (case-insensitive)
     * @return the reusable scenario if found, null otherwise
     */
    public Scenario getTestPlanScenarioByName(String name) {
        for (Scenario scenario : scenarios) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                return scenario;
            }
        }
        return null;
    }

    /**
     * Returns all shared reusable scenarios (from app-level Shared folder).
     * @return list of shared reusable scenarios
     */
    public List<Scenario> getSharedScenarios() {
        return sharedReusableScenarios;
    }

    /**
     * Finds a shared reusable scenario by name, excluding deleted scenarios.
     * @param name scenario name to search for (case-insensitive)
     * @return the shared reusable scenario if found and active, null otherwise
     */
    public Scenario getSharedReusableScenarioByName(String name) {
        for (Scenario scenario : sharedReusableScenarios) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                // Verify the scenario folder still exists on disk
                if (new File(scenario.getLocation()).exists()) {
                    return scenario;
                }
            }
        }
        return null;
    }

    /**
     * Finds the index of a scenario by name in the Test Plan.
     * @param name scenario name to search for (case-insensitive)
     * @return the index if found, -1 otherwise
     */
    public int getIndexOfScenarioByName(String name) {
        for (int i = 0; i < scenarios.size(); i++) {
            if (scenarios.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds a release by name.
     * @param name release name to search for (case-insensitive)
     * @return the release if found, null otherwise
     */
    public Release getReleaseByName(String name) {
        for (Release release : releases) {
            if (release.getName().equalsIgnoreCase(name)) {
                return release;
            }
        }
        return null;
    }

    /**
     * Finds the index of a release by name.
     * @param name release name to search for (case-insensitive)
     * @return the index if found, -1 otherwise
     */
    public int getIndexOfReleaseByName(String name) {
        for (int i = 0; i < releases.size(); i++) {
            if (releases.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sets the list of Test Plan scenarios.
     * @param scenarios new list of scenarios
     */
    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }

    /**
     * Returns the project's filesystem location.
     * @return absolute path to the project directory
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns the absolute path to the TestPlan directory.
     * @return TestPlan directory path
     */
    public String getTestPlanPath() {
        return getLocation() + File.separator + TEST_PLAN_DIR;
    }

    /**
     * Returns the absolute path to the ReusableComponents directory.
     * @return ReusableComponents directory path
     */
    public String getReusableComponentsPath() {
        return getLocation() + File.separator + REUSABLE_COMPONENTS_DIR;
    }

    /**
     * Returns the absolute path to the shared Reusable Components directory at app root level.
     * This directory is global across all projects and shared at the application level.
     * @return Shared Reusable Components directory path
     */
    public static String getSharedReusableComponentsPath() {
        try {
            String appRoot = new File(System.getProperty("user.dir")).getCanonicalPath();
            return (
                appRoot +
                File.separator +
                "Shared" +
                File.separator +
                SHARED_REUSABLE_COMPONENTS_DIR
            );
        } catch (java.io.IOException ex) {
            // Fallback to non-canonical path
            return (
                System.getProperty("user.dir") +
                File.separator +
                "Shared" +
                File.separator +
                SHARED_REUSABLE_COMPONENTS_DIR
            );
        }
    }

    /**
     * Returns the absolute path to the Shared Test Data directory at app root level.
     * This directory is global across all projects and shared at the application level.
     * @return Shared Test Data directory path
     */
    public static String getSharedTestDataPath() {
        try {
            String appRoot = new File(System.getProperty("user.dir")).getCanonicalPath();
            return appRoot + File.separator + "Shared" + File.separator + SHARED_TEST_DATA_DIR;
        } catch (java.io.IOException ex) {
            // Fallback to non-canonical path
            return (
                System.getProperty("user.dir") +
                File.separator +
                "Shared" +
                File.separator +
                SHARED_TEST_DATA_DIR
            );
        }
    }

    /**
     * Returns the absolute path to a scenario directory based on its source.
     * @param source the scenario source (TEST_PLAN, REUSABLE_COMPONENTS, or SHARED_REUSABLE_COMPONENTS)
     * @param scenarioName name of the scenario
     * @return absolute path to the scenario directory
     */
    public String getScenarioPath(Scenario.Source source, String scenarioName) {
        String base;
        if (source == Scenario.Source.REUSABLE_COMPONENTS) {
            base = getReusableComponentsPath();
        } else if (source == Scenario.Source.SHARED_REUSABLE_COMPONENTS) {
            base = getSharedReusableComponentsPath();
        } else {
            base = getTestPlanPath();
        }
        return base + File.separator + scenarioName;
    }

    /**
     * Sets the project location.
     * @param location new project location
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Saves the project metadata file to disk.
     * @param project project info to save
     * @param file target file
     */
    private void saveProjectFile(ProjectInfo project, File file) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            updateProjectInfo(project, this);
            FileScanner.writeFile(file, project.toJson());
        } catch (JsonProcessingException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            FileScanner.writeFile(file, new String());
        }
    }

    /**
     * Updates project metadata by removing deleted scenarios and test cases.
     * @param project project info to update
     * @param sp source project
     */
    private void updateProjectInfo(ProjectInfo project, Project sp) {
        try {
            List<String> scns = sp
                .getAllScenarios()
                .stream()
                .map(Scenario::getName)
                .collect(toList());
            project
                .findScenarios()
                .filter(scn -> !scns.contains(scn.getName()))
                .collect(toList())
                .forEach(
                    scn -> {
                        project.getMeta().remove(scn);
                        project
                            .getData()
                            .removeAll(
                                project
                                    .getData()
                                    .stream()
                                    .filter(Objects::nonNull)
                                    .filter(di -> di.hasScenario(scn.getName()))
                                    .collect(toList())
                            );
                    }
                );
            project
                .getData()
                .removeAll(
                    project
                        .getData()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(di -> !sp.hasTestCaseInAnyScenario(di.getScenario(), di.getName()))
                        .collect(toList())
                );
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Checks if a test case exists in any scenario (Test Plan or Reusable Components).
     * @param scenarioName scenario name
     * @param testCaseName test case name
     * @return true if the test case exists, false otherwise
     */
    public boolean hasTestCaseInAnyScenario(String scenarioName, String testCaseName) {
        return (
            hasTestCaseInScenario(testCaseName, getScenarioByName(scenarioName)) ||
            hasTestCaseInScenario(testCaseName, getReusableScenarioByName(scenarioName))
        );
    }

    /**
     * Checks if a test case exists in a given scenario.
     * @param tc test case name
     * @param scnobj scenario object
     * @return true if the test case exists in the scenario, false otherwise
     */
    private boolean hasTestCaseInScenario(String tc, Scenario scnobj) {
        return scnobj != null && scnobj.getTestCaseByName(tc) != null;
    }

    /**
     * Returns the project metadata file.
     * @return project file (.project)
     */
    private File getProjectFile() {
        return new File(getLocation(), ".project");
    }

    /**
     * Returns the project name.
     * @return the project name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the project name.
     * @param name new project name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Renames the project and updates all related settings and references.
     * @param newName new project name
     * @return true if successful, false otherwise
     */
    public Boolean rename(String newName) {
        if (FileUtils.renameFile(getLocation(), newName)) {
            setName(newName);
            setLocation(new File(getLocation()).getParent() + File.separator + newName);
            getProjectSettings().resetLocation();
            for (Release release : releases) {
                for (TestSet testSet : release.getTestSets()) {
                    testSet.resetExecSettingsLocation();
                }
            }
            getObjectRepository().getWebOR().setName(newName);
            getObjectRepository().getWebSharedOR().setName(newName);
            getObjectRepository().getMobileOR().setName(newName);
            getObjectRepository().getMobileSharedOR().setName(newName);
            getObjectRepository().getStructuredDataOR().setName(newName);
            getObjectRepository().getStructuredDataSharedOR().setName(newName);
            getObjectRepository().getSapOR().setName(newName);
            getObjectRepository().getSapSharedOR().setName(newName);
            return true;
        }
        return false;
    }

    /**
     * Loads all scenarios from the TestPlan directory.
     * @return true if successful, false otherwise
     */
    private Boolean loadScenariosFromTestPlan() {
        scenarios.clear();
        File file = new File(location);
        if (file.exists() && file.isDirectory()) {
            File testPlan = new File(getTestPlanPath());
            if (testPlan.exists() && testPlan.list() != null) {
                List<String> names = new ArrayList<>(
                    java.util.Arrays.asList(testPlan.list(DIR_FILTER))
                );
                names = SortOrderStore.apply(testPlan, names);
                for (String scenario : names) {
                    scenarios.add(new Scenario(this, scenario, Scenario.Source.TEST_PLAN));
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Loads all scenarios from the ReusableComponents directory.
     * @return true if successful, false otherwise
     */
    private Boolean loadScenariosFromReusableComponents() {
        reusableScenarios.clear();
        File file = new File(location);
        if (file.exists() && file.isDirectory()) {
            File reusableRoot = new File(getReusableComponentsPath());
            if (reusableRoot.exists() && reusableRoot.list() != null) {
                List<String> names = new ArrayList<>(
                    java.util.Arrays.asList(reusableRoot.list(DIR_FILTER))
                );
                names = SortOrderStore.apply(reusableRoot, names);
                for (String scenario : names) {
                    reusableScenarios.add(
                        new Scenario(this, scenario, Scenario.Source.REUSABLE_COMPONENTS)
                    );
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Loads all scenarios from the Shared Reusable Components directory at app root level.
     * @return true if successful, false otherwise
     */
    private Boolean loadScenariosFromSharedReusableComponents() {
        sharedReusableScenarios.clear();
        File sharedRoot = new File(getSharedReusableComponentsPath());
        if (sharedRoot.exists() && sharedRoot.isDirectory() && sharedRoot.list() != null) {
            List<String> names = new ArrayList<>(
                java.util.Arrays.asList(sharedRoot.list(DIR_FILTER))
            );
            names = SortOrderStore.apply(sharedRoot, names);
            for (String scenario : names) {
                sharedReusableScenarios.add(
                    new Scenario(this, scenario, Scenario.Source.SHARED_REUSABLE_COMPONENTS)
                );
            }
            return true;
        }
        return false;
    }

    /**
     * Migrates reusable test cases from legacy XML format to directory-based format.
     * Moves test cases marked as reusable from TestPlan to ReusableComponents.
     */
    private void migrateReusableComponentXmlIfPresent() {
        File xmlFile = new File(getLocation(), "ReusableComponent.xml");
        if (!xmlFile.exists()) {
            return;
        }
        Reusable.parseAndSetReusable(this);
        int moved = 0;
        for (Scenario scenario : new ArrayList<>(scenarios)) {
            for (TestCase testCase : new ArrayList<>(scenario.getTestCases())) {
                if (testCase.getReusable() != null) {
                    try {
                        moveTestCaseFile(testCase, Scenario.Source.REUSABLE_COMPONENTS);
                        moved++;
                    } catch (TestCaseConversionException e) {
                        LOGGER.log(
                            Level.WARNING,
                            "Failed to migrate test case: " + e.getMessage(),
                            e
                        );
                    }
                }
            }
        }
        File backup = new File(xmlFile.getParentFile(), "ReusableComponent.xml.bak");
        if (!backup.exists()) {
            xmlFile.renameTo(backup);
        }

        if (moved > 0) {
            // moveTestCaseFile() only updates the source scenario's in-memory test case
            // list; it never adds the moved test case to the target ReusableComponents
            // scenario (that scenario may not even exist in memory yet if this is its
            // first test case). Re-scan from disk so reusableScenarios reflects the files
            // that were just moved - otherwise the UI tree stays stale for this session
            // even though the files are correctly migrated on disk.
            loadScenariosFromTestPlan();
            loadScenariosFromReusableComponents();
            loadScenariosFromSharedReusableComponents();
        }

        LOGGER.log(Level.INFO, "Migrated reusable testcases: {0}", moved);
    }

    /**
     * Moves a test case to Reusable Components.
     * @param testCase the test case to move
     * @throws TestCaseConversionException if the move operation fails
     */
    public void moveTestCaseToReusable(TestCase testCase) throws TestCaseConversionException {
        moveTestCaseFile(testCase, Scenario.Source.REUSABLE_COMPONENTS);
    }

    /**
     * Moves a test case to Test Plan.
     * @param testCase the test case to move
     * @throws TestCaseConversionException if the move operation fails
     */
    public void moveTestCaseToTestPlan(TestCase testCase) throws TestCaseConversionException {
        moveTestCaseFile(testCase, Scenario.Source.TEST_PLAN);
    }

    /**
     * Moves a test case file to the specified target source (Test Plan or Reusable Components).
     * @param testCase the test case to move
     * @param targetSource the destination source (TEST_PLAN or REUSABLE_COMPONENTS)
     * @throws TestCaseConversionException if the move operation fails
     */
    private void moveTestCaseFile(TestCase testCase, Scenario.Source targetSource)
        throws TestCaseConversionException {
        if (testCase == null || testCase.getScenario() == null) {
            throw new TestCaseConversionException("Invalid test case or scenario");
        }

        lastImpactedReusableReferenceUpdates = 0;

        Scenario sourceScenario = testCase.getScenario();
        String scenarioName = sourceScenario.getName();
        String testCaseName = testCase.getName();
        Scenario.Source sourceType = sourceScenario.getSource();
        String targetName = targetSource == Scenario.Source.REUSABLE_COMPONENTS
            ? "Reusable Components"
            : "Test Plan";

        File source = new File(testCase.getLocation());
        if (!source.exists()) {
            throw new TestCaseConversionException("Test Case file does not exist");
        }

        File targetDir = new File(getScenarioPath(targetSource, scenarioName));
        targetDir.mkdirs();

        File target = new File(targetDir, testCaseName + testCase.getFormat().extension());

        if (target.exists()) {
            throw new TestCaseConversionException(
                "Test case '" +
                testCaseName +
                "' already exists in scenario '" +
                scenarioName +
                "' in " +
                targetName
            );
        }

        try {
            Files.move(source.toPath(), target.toPath());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed moving test case file", ex);
            throw new TestCaseConversionException(
                "Failed to move test case: " + ex.getMessage(),
                ex
            );
        }

        // When moving TO Test Plan from a reusable source, remove the case from source and cleanup
        if (sourceType != Scenario.Source.TEST_PLAN && targetSource == Scenario.Source.TEST_PLAN) {
            sourceScenario.removeTestCase(testCase);
            if (sourceType == Scenario.Source.REUSABLE_COMPONENTS) {
                cleanupEmptyScenario(sourceScenario);
            }
        }

        // When moving FROM Test Plan TO Project Reusable, remove the case from the Test Plan
        // and cleanup the scenario if it becomes empty so the scenario name can be reused.
        if (
            sourceType == Scenario.Source.TEST_PLAN &&
            targetSource == Scenario.Source.REUSABLE_COMPONENTS
        ) {
            sourceScenario.removeTestCase(testCase);
            cleanupEmptyScenario(sourceScenario);
        }

        lastImpactedReusableReferenceUpdates =
            refactorReusableReferencesAcrossProject(
                scenarioName,
                testCaseName,
                sourceType,
                scenarioName,
                testCaseName,
                targetSource,
                testCase
            );

        // Explicitly set Scope to the new location as part of this conversion - this is intentional
        // and must not be confused with (or blocked by) the reload-time "preserve existing Scope" logic.
        testData.updateScope(
            scenarioName,
            testCaseName,
            scopeToken(sourceType),
            scopeToken(targetSource)
        );
    }

    /**
     * Maps a scenario source to the raw Scope token stored against Test Data entries:
     * "" for Test Plan, "[Project]" for Project Reusables, "[Shared]" for Shared Reusables.
     */
    private String scopeToken(Scenario.Source source) {
        if (source == Scenario.Source.REUSABLE_COMPONENTS) {
            return "[Project]";
        }
        if (source == Scenario.Source.SHARED_REUSABLE_COMPONENTS) {
            return "[Shared]";
        }
        return "";
    }

    /**
     * Loads all test sets from the TestLab directory.
     * @return true if successful, false otherwise
     */
    private Boolean loadTestSets() {
        releases.clear();
        File file = new File(location);
        if (file.exists() && file.isDirectory()) {
            File testLabList = new File(location + File.separator + "TestLab");
            if (testLabList.exists() && testLabList.list() != null) {
                for (String release : testLabList.list(DIR_FILTER)) {
                    releases.add(new Release(this, release));
                }
            }
            return true;
        }
        return false;
    }

    public Release addRelease(String releaseName) {
        if (getReleaseByName(releaseName) == null) {
            Release rls = new Release(this, releaseName);
            releases.add(rls);
            return rls;
        }
        return null;
    }

    public void removeRelease(Release release) {
        int index = releases.indexOf(release);
        if (releases.remove(release)) {}
    }

    /**
     * Adds a new scenario to the Test Plan.
     * @param scenarioName name of the scenario to add
     * @return the created scenario, or null if a scenario with the same name already exists in any scope
     */
    public Scenario addScenario(String scenarioName) {
        if (getTestPlanScenarioByName(scenarioName) == null) {
            Scenario scn = new Scenario(this, scenarioName, Scenario.Source.TEST_PLAN);
            scenarios.add(scn);
            return scn;
        }
        return null;
    }

    /**
     * Adds a new scenario to Reusable Components.
     * @param scenarioName name of the scenario to add
     * @return the created scenario, or null if a scenario with the same name already exists in the Project Reusable scope
     */
    public Scenario addReusableScenario(String scenarioName) {
        if (getReusableScenarioByName(scenarioName) == null) {
            Scenario scn = new Scenario(this, scenarioName, Scenario.Source.REUSABLE_COMPONENTS);
            reusableScenarios.add(scn);
            return scn;
        }
        return null;
    }

    /**
     * Adds a new shared reusable scenario to the project.
     * @param scenarioName name of the scenario to add
     * @return the newly created shared reusable scenario, or null if already exists in the Shared Reusable scope
     */
    public Scenario addSharedReusableScenario(String scenarioName) {
        if (getSharedReusableScenarioByName(scenarioName) == null) {
            Scenario scn = new Scenario(
                this,
                scenarioName,
                Scenario.Source.SHARED_REUSABLE_COMPONENTS
            );
            sharedReusableScenarios.add(scn);
            return scn;
        }
        return null;
    }

    /**
     * Removes a scenario from the project (either Test Plan or Reusable Components).
     * @param scenario scenario to remove
     */
    public void removeScenario(Scenario scenario) {
        if (scenario == null) {
            return;
        }
        if (scenario.isReusableScenario()) {
            reusableScenarios.remove(scenario);
        } else if (scenario.isSharedReusableScenario()) {
            sharedReusableScenarios.remove(scenario);
        } else {
            scenarios.remove(scenario);
        }
    }

    /**
     * Checks if a scenario exists in reusable scopes only (project/shared reusable).
     */
    private boolean scenarioExistsInReusableScopes(String scenarioName) {
        return (
            getReusableScenarioByName(scenarioName) != null ||
            getSharedReusableScenarioByName(scenarioName) != null
        );
    }

    /**
     * Generates a unique reusable-scope scenario name by appending "_n" only when duplicates exist.
     * Test Plan scenarios do not influence this naming.
     * @param baseName base scenario name
     * @param isCopy true if this is a copy operation, false if move
     * @return unique name or baseName if not in use
     */
    private String makeScenarioNameUnique(String baseName, boolean isCopy) {
        if (!isCopy) {
            return baseName;
        }
        return NamingUtils.generateUniqueName(baseName, this::scenarioExistsInReusableScopes);
    }

    /**
     * Copies a project reusable test case to shared reusables.
     * If destination scenario doesn't exist, it is created.
     */
    public TestCase copyTestCaseToSharedReusable(TestCase testCase)
        throws TestCaseConversionException {
        return transferReusableBetweenScopes(
            testCase,
            Scenario.Source.SHARED_REUSABLE_COMPONENTS,
            false
        );
    }

    /**
     * Moves a project reusable test case to shared reusables.
     * Source scenario is removed when no test cases remain.
     */
    public TestCase moveTestCaseToSharedReusable(TestCase testCase)
        throws TestCaseConversionException {
        return transferReusableBetweenScopes(
            testCase,
            Scenario.Source.SHARED_REUSABLE_COMPONENTS,
            true
        );
    }

    /**
     * Copies a shared reusable test case to project reusables.
     * If destination scenario doesn't exist, it is created.
     */
    public TestCase copyTestCaseToReusable(TestCase testCase) throws TestCaseConversionException {
        return transferReusableBetweenScopes(testCase, Scenario.Source.REUSABLE_COMPONENTS, false);
    }

    /**
     * Moves a shared reusable test case to project reusables.
     * Source scenario is removed when no test cases remain.
     */
    public TestCase moveSharedReusableToReusable(TestCase testCase)
        throws TestCaseConversionException {
        return transferReusableBetweenScopes(testCase, Scenario.Source.REUSABLE_COMPONENTS, true);
    }

    private TestCase transferReusableBetweenScopes(
        TestCase testCase,
        Scenario.Source targetSource,
        boolean move
    )
        throws TestCaseConversionException {
        if (testCase == null || testCase.getScenario() == null) {
            throw new TestCaseConversionException("Invalid test case or scenario");
        }

        lastImpactedReusableReferenceUpdates = 0;

        Scenario sourceScenario = testCase.getScenario();
        Scenario.Source sourceType = sourceScenario.getSource();

        if (
            !(
                targetSource == Scenario.Source.REUSABLE_COMPONENTS ||
                targetSource == Scenario.Source.SHARED_REUSABLE_COMPONENTS
            )
        ) {
            throw new TestCaseConversionException(
                "Target scope must be reusable or shared reusable"
            );
        }
        if (sourceType == targetSource) {
            throw new TestCaseConversionException("Source and destination scopes are the same");
        }

        String scenarioName = sourceScenario.getName();
        String testCaseName = testCase.getName();

        // For move operations only: validate that same scenario + testcase doesn't exist in target
        if (move) {
            Scenario existingTargetScenario = getScenarioInScope(targetSource, scenarioName);
            if (
                existingTargetScenario != null &&
                existingTargetScenario.getTestCaseByName(testCaseName) != null
            ) {
                throw new TestCaseConversionException(
                    "Cannot move: Scenario '" +
                    scenarioName +
                    "' with test case '" +
                    testCaseName +
                    "' already exists in the target scope. Same scenario and test case names are not allowed."
                );
            }
        }
        // For copy operations, naming is resolved with collision-only suffixing.

        Scenario targetScenario = getOrCreateScenarioForScope(targetSource, scenarioName, !move);
        if (targetScenario == null) {
            throw new TestCaseConversionException(
                "Failed to create or resolve target scenario '" +
                scenarioName +
                "' in destination scope"
            );
        }
        String targetTestCaseName = uniqueNameInScenario(targetScenario, testCase.getName(), !move);

        File sourceFile = new File(testCase.getLocation());
        if (!sourceFile.exists()) {
            throw new TestCaseConversionException("Test Case file does not exist");
        }

        File targetDir = new File(getScenarioPath(targetSource, targetScenario.getName()));
        targetDir.mkdirs();
        File targetFile = new File(
            targetDir,
            targetTestCaseName + testCase.getFormat().extension()
        );
        try {
            if (move) {
                Files.move(
                    sourceFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
            } else {
                Files.copy(sourceFile.toPath(), targetFile.toPath());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed transfering test case file", ex);
            throw new TestCaseConversionException(
                "Failed to transfer test case: " + ex.getMessage(),
                ex
            );
        }

        TestCase targetTestCase = new TestCase(targetScenario, targetTestCaseName);
        targetScenario.getTestCases().add(targetTestCase);

        if (move) {
            sourceScenario.removeTestCase(testCase);
            lastImpactedReusableReferenceUpdates =
                refactorReusableReferencesAcrossProject(
                    scenarioName,
                    testCaseName,
                    sourceType,
                    targetScenario.getName(),
                    targetTestCaseName,
                    targetSource,
                    null
                );
            cleanupEmptyScenario(sourceScenario);

            // Explicitly set Scope to the new location as part of this conversion. Only applies to
            // moves: a copy leaves the source test case (and its Test Data Scope) exactly where it was.
            // Move never renames scenario/testcase (see uniqueNameInScenario), so old names still match.
            testData.updateScope(
                scenarioName,
                testCaseName,
                scopeToken(sourceType),
                scopeToken(targetSource)
            );
        }

        return targetTestCase;
    }

    /**
     * Returns and resets the number of impacted test cases updated by the last reusable move operation.
     */
    public int getAndResetLastImpactedReusableReferenceUpdates() {
        int count = lastImpactedReusableReferenceUpdates;
        lastImpactedReusableReferenceUpdates = 0;
        return count;
    }

    private int refactorReusableReferencesAcrossProject(
        String oldScenarioName,
        String oldTestCaseName,
        Scenario.Source oldSource,
        String newScenarioName,
        String newTestCaseName,
        Scenario.Source newSource,
        TestCase excluded
    ) {
        int impacted = 0;
        for (Scenario scenario : getAllScenarios()) {
            for (TestCase candidate : scenario.getTestCases()) {
                if (candidate == excluded) {
                    continue;
                }
                if (
                    candidate.refactorReusableReferenceAcrossScope(
                        oldScenarioName,
                        oldTestCaseName,
                        oldSource,
                        newScenarioName,
                        newTestCaseName,
                        newSource
                    )
                ) {
                    impacted++;
                }
            }
        }
        return impacted;
    }

    /**
     * Gets a scenario in a specific scope (Reusable or Shared Reusable).
     * @param scope the scope to search in
     * @param scenarioName name of the scenario
     * @return the scenario if found, null otherwise
     */
    private Scenario getScenarioInScope(Scenario.Source scope, String scenarioName) {
        if (scope == Scenario.Source.REUSABLE_COMPONENTS) {
            return getReusableScenarioByName(scenarioName);
        } else if (scope == Scenario.Source.SHARED_REUSABLE_COMPONENTS) {
            return getSharedReusableScenarioByName(scenarioName);
        }
        return null;
    }

    private Scenario getOrCreateScenarioForScope(
        Scenario.Source scope,
        String scenarioName,
        boolean isCopy
    ) {
        if (!isCopy) {
            Scenario scenario = getScenarioInScope(scope, scenarioName);
            if (scenario != null) {
                return scenario;
            }
            return addScenarioInScope(scope, scenarioName);
        }

        String uniqueName = makeScenarioNameUnique(scenarioName, true);
        if (scope == Scenario.Source.REUSABLE_COMPONENTS) {
            Scenario scenario = getReusableScenarioByName(uniqueName);
            return scenario != null ? scenario : addScenarioInScope(scope, uniqueName);
        }
        Scenario scenario = getSharedReusableScenarioByName(uniqueName);
        return scenario != null ? scenario : addScenarioInScope(scope, uniqueName);
    }

    private Scenario addScenarioInScope(Scenario.Source scope, String scenarioName) {
        if (scope == Scenario.Source.REUSABLE_COMPONENTS) {
            Scenario existing = getReusableScenarioByName(scenarioName);
            if (existing != null) {
                return existing;
            }
            Scenario created = new Scenario(
                this,
                scenarioName,
                Scenario.Source.REUSABLE_COMPONENTS
            );
            reusableScenarios.add(created);
            return created;
        }
        if (scope == Scenario.Source.SHARED_REUSABLE_COMPONENTS) {
            Scenario existing = getSharedReusableScenarioByName(scenarioName);
            if (existing != null) {
                return existing;
            }
            Scenario created = new Scenario(
                this,
                scenarioName,
                Scenario.Source.SHARED_REUSABLE_COMPONENTS
            );
            sharedReusableScenarios.add(created);
            return created;
        }
        return null;
    }

    private String uniqueNameInScenario(Scenario scenario, String baseName, boolean isCopy) {
        if (!isCopy) {
            // For move operations, keep original name
            return baseName;
        }
        return NamingUtils.generateUniqueName(
            baseName,
            name -> scenario.getTestCaseByName(name) != null
        );
    }

    private void cleanupEmptyScenario(Scenario scenario) {
        if (scenario == null || !scenario.getTestCases().isEmpty()) {
            return;
        }
        FileUtils.deleteFile(scenario.getLocation());
        removeScenario(scenario);
    }

    /**
     * Returns the environment test data.
     * @return environment test data
     */
    public EnvTestData getTestData() {
        return testData;
    }

    /**
     * Returns the Shared Test Data, loaded from the app-root Shared/SharedTestData location.
     * @return shared environment test data
     */
    public EnvTestData getSharedTestData() {
        return sharedTestData;
    }

    /**
     * Loads test data from disk.
     */
    private void loadTestDatas() {
        testData = new EnvTestData(this);
        sharedTestData = new EnvTestData(this, true);
    }

    /**
     * Copies a project test data sheet to Shared Test Data.
     * If a sheet with the same file name already exists in the shared location, it is overwritten.
     * @param sheet the sheet to copy
     * @param environment the environment the sheet belongs to
     * @throws IOException if the file could not be copied
     */
    public void copyTestDataSheetToShared(TestDataModel sheet, String environment)
        throws IOException {
        transferTestDataSheet(sheet, environment, getSharedTestDataPath(), false);
    }

    /**
     * Moves a project test data sheet to Shared Test Data.
     * @param sheet the sheet to move
     * @param environment the environment the sheet belongs to
     * @throws IOException if the file could not be moved
     */
    public void moveTestDataSheetToShared(TestDataModel sheet, String environment)
        throws IOException {
        transferTestDataSheet(sheet, environment, getSharedTestDataPath(), true);
    }

    /**
     * Copies a Shared Test Data sheet into this project.
     * @param sheet the sheet to copy
     * @param environment the environment the sheet belongs to
     * @throws IOException if the file could not be copied
     */
    public void copyTestDataSheetToProject(TestDataModel sheet, String environment)
        throws IOException {
        transferTestDataSheet(
            sheet,
            environment,
            getLocation() + File.separator + "TestData",
            false
        );
    }

    /**
     * Moves a Shared Test Data sheet into this project.
     * @param sheet the sheet to move
     * @param environment the environment the sheet belongs to
     * @throws IOException if the file could not be moved
     */
    public void moveTestDataSheetToProject(TestDataModel sheet, String environment)
        throws IOException {
        transferTestDataSheet(
            sheet,
            environment,
            getLocation() + File.separator + "TestData",
            true
        );
    }

    private void transferTestDataSheet(
        TestDataModel sheet,
        String environment,
        String targetRoot,
        boolean move
    )
        throws IOException {
        File sourceFile = new File(sheet.getLocation());
        if (!sourceFile.exists()) {
            throw new FileNotFoundException("Test data sheet file does not exist: " + sourceFile);
        }

        String envSuffix = "Default".equals(environment) ? "" : File.separator + environment;
        File targetDir = new File(targetRoot + envSuffix);
        targetDir.mkdirs();
        File targetFile = new File(targetDir, sourceFile.getName());

        if (move) {
            Files.move(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
        } else {
            Files.copy(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );
        }

        // Reload so both the project and shared TestData reflect the transferred sheet
        loadTestDatas();
    }

    // ─── "Make As Shared TestData" ────────────────────────────────────────────

    /**
     * Outcome of a "Make As Shared TestData" operation, for user feedback.
     */
    public static final class MakeSharedTestDataResult {
        /** original sheet name -&gt; final name in the Shared store (suffixed on collision). */
        public final Map<String, String> movedSheets = new LinkedHashMap<>();

        /** whole-input Test Data references rewritten to {@code [Shared] ...}. */
        public int referenceUpdates = 0;

        /**
         * Moved sheet name -&gt; the project environments that still contain a sheet of that
         * name, so references were <em>not</em> retagged (doing so would break those
         * environments). Populated only for the environment-scoped move.
         */
        public final Map<String, List<String>> partiallyMovedSheets = new LinkedHashMap<>();

        /**
         * The Shared Reusable test cases created by promoting referencing Test Plan /
         * Project-Reusable cases (the moved copies, not the originals). Empty when the user
         * declined promotion. The IDE feeds these to the "move referenced project objects to
         * Shared OR too?" prompt.
         */
        public final List<TestCase> promoted = new ArrayList<>();

        /** reusable-reference (Execute step) updates caused by those promotions. */
        public int promotedReferenceUpdates = 0;
    }

    /**
     * Test cases across <em>every</em> scope (Test Plan, Project Reusables, Shared Reusables)
     * whose whole-input Test Data reference points at {@code testDataName}. Unlike
     * {@link #getImpactedTestDataTestCases(String)} (Test Plan only) this is scope-wide, so it
     * can drive the "also convert to Shared Reusable" prompt.
     *
     * @param testDataName datasheet name
     * @return impacted test cases, in scenario iteration order
     */
    public List<TestCase> getImpactedTestDataTestCasesAllScopes(String testDataName) {
        List<TestCase> impacted = new ArrayList<>();
        for (Scenario scenario : getAllScenarios()) {
            impacted.addAll(scenario.getImpactedTestDataTestCases(testDataName));
        }
        return impacted;
    }

    /**
     * Test cases a datasheet holds data rows for - the distinct (Scenario, Flow) pairs in its
     * rows - that live in the Test Plan or Project Reusables (per each row's {@code Scope}
     * column, falling back to a name lookup). These are the cases another user of the Shared
     * data could not run if the sheet moves to Shared Test Data without them.
     *
     * @param sheetName datasheet name
     * @return the served Test Plan / Project-Reusable test cases, de-duplicated
     */
    public List<TestCase> getTestCasesServedByDataSheet(String sheetName) {
        java.util.LinkedHashMap<String, TestCase> found = new LinkedHashMap<>();
        for (TestData env : testData.getAllEnvironments()) {
            TestDataModel model = env.getByNameIgnoreCase(sheetName);
            if (model == null) {
                continue;
            }
            model.loadTableModel();
            for (Record record : model.getRecords()) {
                String scen = Objects.toString(record.getScenario(), "").trim();
                String tc = Objects.toString(record.getTestcase(), "").trim();
                String scope = Objects.toString(record.getScope(), "").trim();
                if (scen.isEmpty() || tc.isEmpty() || "[Shared]".equals(scope)) {
                    continue;
                }
                Scenario scenario = "[Project]".equals(scope)
                    ? getReusableScenarioByName(scen)
                    : getTestPlanScenarioByName(scen);
                if (scenario == null) {
                    scenario = getTestPlanScenarioByName(scen);
                }
                if (scenario == null) {
                    scenario = getReusableScenarioByName(scen);
                }
                if (scenario == null || scenario.isSharedReusableScenario()) {
                    continue;
                }
                TestCase testCase = scenario.getTestCaseByName(tc);
                if (testCase != null) {
                    found.putIfAbsent(scenario.getName() + " " + tc, testCase);
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /**
     * Every Test Plan / Project-Reusable test case that would be "left behind" if
     * {@code sheetName} moves to Shared Test Data: the union of the cases the sheet holds data
     * for ({@link #getTestCasesServedByDataSheet(String)}) and the cases whose steps reference
     * it ({@link #getImpactedTestDataTestCasesAllScopes(String)}). Drives the "also convert
     * these test cases to Shared Reusables?" prompt.
     *
     * @param sheetName datasheet name
     * @return de-duplicated promotable test cases
     */
    public List<TestCase> getPromotableTestCasesForSheet(String sheetName) {
        java.util.LinkedHashMap<String, TestCase> byId = new LinkedHashMap<>();
        for (TestCase tc : getTestCasesServedByDataSheet(sheetName)) {
            byId.putIfAbsent(promotableId(tc), tc);
        }
        for (TestCase tc : getImpactedTestDataTestCasesAllScopes(sheetName)) {
            if (tc.getScenario() != null && !tc.getScenario().isSharedReusableScenario()) {
                byId.putIfAbsent(promotableId(tc), tc);
            }
        }
        return new ArrayList<>(byId.values());
    }

    private static String promotableId(TestCase tc) {
        return (tc.getScenario() == null ? "" : tc.getScenario().getName()) + " " + tc.getName();
    }

    /**
     * Rewrites every whole-input Test Data reference to {@code originalName} (untagged,
     * {@code [Project]} or {@code [Shared]}) across all scenarios to
     * {@code [Shared] finalName:Column}, saving each changed test case.
     *
     * @param originalName the datasheet name references currently point at
     * @param finalName the datasheet name in the Shared store (may differ on collision)
     * @return number of test steps changed
     */
    public int retagTestDataReferencesToShared(String originalName, String finalName) {
        int count = 0;
        for (Scenario scenario : getAllScenarios()) {
            count += scenario.retagTestDataReferencesToShared(originalName, finalName);
        }
        return count;
    }

    /**
     * Moves one project datasheet from environment {@code env} into Shared Test Data and, when
     * that was the only project environment holding a sheet of that name, rewrites every
     * reference to it (Test Plan, Project Reusables, Shared Reusables) to {@code [Shared] ...}.
     * If the same sheet name still exists in another project environment, the references are
     * left untouched and the sheet is recorded in
     * {@link MakeSharedTestDataResult#partiallyMovedSheets}.
     *
     * @param env environment the datasheet belongs to
     * @param sheetName datasheet to move
     * @param testCasesToPromote referencing Test Plan / Project-Reusable test cases the user
     *     opted to also convert to Shared Reusables (may be {@code null} / empty)
     * @return summary of what changed
     */
    public MakeSharedTestDataResult makeTestDataSheetShared(
        String env,
        String sheetName,
        List<TestCase> testCasesToPromote
    )
        throws IOException {
        save();
        MakeSharedTestDataResult result = new MakeSharedTestDataResult();
        promoteReferencingTestCases(testCasesToPromote, result);
        moveSheetToSharedInEnv(env, sheetName, result);
        save();
        registerSharedTestDataUsage();
        return result;
    }

    /**
     * Moves a whole project Test Data environment into Shared Test Data: <em>only that
     * environment's</em> datasheets and Global Data (merged by {@code GlobalDataID} into the
     * Shared environment's single Global Data). The project environment is then removed
     * entirely - folder, {@code GlobalData.csv} and all - except {@code Default}, which cannot
     * be removed and is instead left empty. Other environments are untouched.
     *
     * @param env environment to move
     * @param testCasesToPromote see {@link #makeTestDataSheetShared(String, String, List)}
     * @return summary of what changed
     */
    public MakeSharedTestDataResult makeEnvironmentTestDataShared(
        String env,
        List<TestCase> testCasesToPromote
    )
        throws IOException {
        save();
        MakeSharedTestDataResult result = new MakeSharedTestDataResult();
        promoteReferencingTestCases(testCasesToPromote, result);

        TestData projEnv = testData.getTestDataFor(env);
        if (projEnv != null) {
            String envFolder = projEnv.getLocation();

            List<String> sheetNames = new ArrayList<>();
            for (TestDataModel model : projEnv.getTestDataList()) {
                sheetNames.add(model.getName());
            }
            for (String name : sheetNames) {
                moveSheetToSharedInEnv(env, name, result);
            }

            if (sharedTestData.getTestDataFor(env) == null) {
                sharedTestData.createNewEnvironment(env);
            }
            mergeGlobalData(
                projEnv.getGlobalData(),
                sharedTestData.getTestDataFor(env).getGlobalData()
            );

            if ("Default".equals(env)) {
                // Default cannot be removed; clear its Global Data and drop the stale file.
                clearAllRecords(projEnv.getGlobalData());
                new File(projEnv.getGlobalData().getLocation()).delete();
                projEnv.getGlobalData().setSaved(true);
            } else {
                testData.deleteEnvironment(env);
                // deleteEnvironment leaves the folder and GlobalData.csv on disk - remove them.
                FileUtils.deleteFile(envFolder);
            }
        }

        save();
        registerSharedTestDataUsage();
        return result;
    }

    private void promoteReferencingTestCases(
        List<TestCase> testCases,
        MakeSharedTestDataResult result
    ) {
        if (testCases == null) {
            return;
        }
        for (TestCase testCase : testCases) {
            if (
                testCase.getScenario() == null || testCase.getScenario().isSharedReusableScenario()
            ) {
                continue;
            }
            try {
                TestCase moved = moveTestCaseToSharedReusable(testCase);
                if (moved != null) {
                    result.promoted.add(moved);
                }
                result.promotedReferenceUpdates +=
                    getAndResetLastImpactedReusableReferenceUpdates();
            } catch (TestCaseConversionException ex) {
                LOGGER.log(
                    Level.WARNING,
                    "Could not promote test case '" + testCase.getName() + "' to Shared Reusable",
                    ex
                );
            }
        }
    }

    /**
     * Moves {@code env}'s copy of {@code sheetName} into {@code Shared/<env>} (suffixing the
     * name on collision <em>within that Shared environment</em>). References are retagged to
     * {@code [Shared] finalName} only when no project environment still holds a sheet of that
     * name; otherwise the sheet is recorded in {@code partiallyMovedSheets} and references are
     * left as-is so the other environments keep working.
     */
    private void moveSheetToSharedInEnv(
        String env,
        String sheetName,
        MakeSharedTestDataResult result
    ) {
        TestData projEnv = testData.getTestDataFor(env);
        TestDataModel source = projEnv == null ? null : projEnv.getByName(sheetName);
        if (source == null) {
            return;
        }
        if (sharedTestData.getTestDataFor(env) == null) {
            sharedTestData.createNewEnvironment(env);
        }
        TestData sharedEnv = sharedTestData.getTestDataFor(env);
        if (sharedEnv == null) {
            return;
        }
        String finalName = uniqueSharedSheetNameInEnv(env, sheetName);
        TestDataModel target = sharedEnv.addTestData(sharedEnv.getNewTestData(finalName));
        source.cloneAs(target);
        target.save();
        projEnv.deleteTestData(sheetName);

        result.movedSheets.put(sheetName, finalName);

        List<String> stillInProject = testData.findEnvironmentsWithDatasheet(sheetName);
        if (stillInProject.isEmpty()) {
            result.referenceUpdates += retagTestDataReferencesToShared(sheetName, finalName);
        } else {
            result.partiallyMovedSheets.put(sheetName, stillInProject);
        }
    }

    private String uniqueSharedSheetNameInEnv(String env, String base) {
        TestData sharedEnv = sharedTestData.getTestDataFor(env);
        if (sharedEnv == null || sharedEnv.getByNameIgnoreCase(base) == null) {
            return base;
        }
        int i = 1;
        while (sharedEnv.getByNameIgnoreCase(base + "_" + i) != null) {
            i++;
        }
        return base + "_" + i;
    }

    /**
     * Upserts every {@code GlobalDataID}-keyed row of {@code src} into {@code dst}, adding any
     * missing columns and overwriting a {@code dst} cell only with a non-empty {@code src}
     * value. There is a single Global Data per environment, so this merges rather than
     * replaces.
     */
    private static void mergeGlobalData(GlobalDataModel src, GlobalDataModel dst) {
        if (src == null || dst == null) {
            return;
        }
        src.loadTableModel();
        dst.loadTableModel();
        for (String col : src.getColumns()) {
            if (!dst.hasColumn(col)) {
                dst.addColumn(col);
            }
        }
        int srcKey = src.getColumnIndex("GlobalDataID");
        int dstKey = dst.getColumnIndex("GlobalDataID");
        for (int r = 0; r < src.getRowCount(); r++) {
            String key = srcKey < 0 ? "" : Objects.toString(src.getValueAt(r, srcKey), "").trim();
            if (key.isEmpty()) {
                continue;
            }
            int dstRow = dst.getRecordIndexByKey(key);
            if (dstRow < 0) {
                dst.addRecord();
                dstRow = dst.getRowCount() - 1;
                dst.setValueAt(key, dstRow, dstKey);
            }
            for (String col : src.getColumns()) {
                if ("GlobalDataID".equals(col)) {
                    continue;
                }
                int dc = dst.getColumnIndex(col);
                String val = Objects.toString(src.getValueAt(r, src.getColumnIndex(col)), "");
                if (dc >= 0 && !val.isEmpty()) {
                    dst.setValueAt(val, dstRow, dc);
                }
            }
        }
        dst.setSaved(false);
    }

    private static void clearAllRecords(AbstractDataModel<?> model) {
        if (model == null) {
            return;
        }
        model.loadTableModel();
        for (int r = model.getRowCount() - 1; r >= 0; r--) {
            model.removeRecord(r);
        }
        model.setSaved(false);
    }

    /**
     * Returns the test data type.
     * @return test data type (default "csv")
     */
    public String getTestdataType() {
        if (testdataType == null) {
            testdataType = "csv";
        }
        return testdataType;
    }

    /**
     * Sets the test data type.
     * @param testdataType new test data type
     */
    public void setTestdataType(String testdataType) {
        this.testdataType = testdataType;
    }

    /**
     * Saves all project components including scenarios, reusable scenarios, test data, releases, object repository, and settings.
     */
    public void save() {
        saveProjectFile(projectInfo, getProjectFile());
        for (Scenario scenario : scenarios) {
            scenario.save();
        }
        for (Scenario scenario : reusableScenarios) {
            scenario.save();
        }
        testData.save();
        if (sharedTestData != null) {
            // Persists the app-root Shared Test Data, including its environment.properties, so
            // environments added/renamed/deleted in the Shared Test Data tab survive a reload -
            // mirroring how testData.save() persists the project's own environments.
            sharedTestData.save();
        }
        for (Release release : releases) {
            release.save();
        }
        objectRepository.save();
        projectSettings.save();
    }

    /**
     * Reloads the project from disk, refreshing all scenarios, test data, and settings.
     */
    public void reload() {
        loadProject();
    }

    /**
     * Returns a table model for the given object (typically a scenario or test case).
     * @param selectedNode object to get table model for
     * @return table model for the object, or empty model if not applicable
     */
    public TableModel getTableModelFor(Object selectedNode) {
        if (selectedNode instanceof DataModel) {
            DataModel scenario = (DataModel) selectedNode;
            scenario.loadTableModel();
            return scenario;
        }
        return new DefaultTableModel();
    }

    /**
     * Returns string representation of the project (project name).
     * @return project name
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Returns detailed string representation of the project including location and scenarios.
     * @return detailed project information
     */
    public String printString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("Project - ")
            .append(name)
            .append("\n")
            .append("Location - ")
            .append(location)
            .append("\n")
            .append("Scenarios - ")
            .append(scenarios.size())
            .append("\n");

        for (Scenario scenario : scenarios) {
            builder.append("\n").append(scenario.toString());
        }
        return builder.toString();
    }

    /**
     * Refactors (renames) a scenario across the entire project including all releases, test sets, and test data.
     * @param oldScenarioName old scenario name
     * @param newScenarioName new scenario name
     */
    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        LOGGER.log(
            Level.INFO,
            "Refactoring started for Scenario [{0}] to [{1}]",
            new Object[] { oldScenarioName, newScenarioName }
        );
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorScenario(oldScenarioName, newScenarioName);
        }
        for (Release release : releases) {
            release.refactorScenario(oldScenarioName, newScenarioName);
        }
        testData.refactorScenario(oldScenarioName, newScenarioName);
        LOGGER.log(
            Level.INFO,
            "Refactoring done for Scenario [{0}] to [{1}]",
            new Object[] { oldScenarioName, newScenarioName }
        );
        getInfo().findScenario(oldScenarioName).ifPresent(scn -> scn.setName(newScenarioName));
        getInfo()
            .getData()
            .stream()
            .filter(Objects::nonNull)
            .filter(di -> di.hasScenario(oldScenarioName))
            .forEach(
                di -> {
                    di
                        .getAttributes()
                        .find(Meta.Attributes.scenario.name())
                        .ifPresent(scn -> scn.setName(newScenarioName));
                }
            );
    }

    /**
     * Refactors (renames) a test case across the entire project including all releases, test sets, and test data.
     * @param scenarioName scenario containing the test case
     * @param oldTestCaseName old test case name
     * @param newTestCaseName new test case name
     */
    public void refactorTestCase(
        String scenarioName,
        String oldTestCaseName,
        String newTestCaseName
    ) {
        LOGGER.log(
            Level.INFO,
            "Refactoring started for TestCase [{0}] to [{1}]",
            new Object[] { oldTestCaseName, newTestCaseName }
        );
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
        for (Release release : releases) {
            release.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
        testData.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        LOGGER.log(
            Level.INFO,
            "Refactoring done for TestCase [{0}] to [{1}]",
            new Object[] { oldTestCaseName, newTestCaseName }
        );
        getInfo()
            .getData()
            .stream()
            .filter(Objects::nonNull)
            .filter(di -> di.hasScenario(scenarioName) && di.getName().equals(oldTestCaseName))
            .forEach(di -> di.setName(newTestCaseName));
    }

    /**
     * Refactors (moves) a test case from one scenario to another across the entire project.
     * @param testCaseName test case name
     * @param oldScenarioName old scenario name
     * @param newScenarioName new scenario name
     */
    public void refactorTestCaseScenario(
        String testCaseName,
        String oldScenarioName,
        String newScenarioName
    ) {
        LOGGER.log(
            Level.INFO,
            "Refactoring started TestCase [{0}] from Scenario [{1}] to [{2}]",
            new Object[] { testCaseName, oldScenarioName, newScenarioName }
        );
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
        for (Release release : releases) {
            release.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
        testData.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        LOGGER.log(
            Level.INFO,
            "Refactoring done TestCase [{0}] from Scenario [{1}] to [{2}]",
            new Object[] { testCaseName, oldScenarioName, newScenarioName }
        );
        getInfo()
            .getData()
            .stream()
            .filter(Objects::nonNull)
            .filter(di -> di.hasScenario(oldScenarioName) && di.getName().equals(testCaseName))
            .forEach(
                di -> {
                    di
                        .getAttributes()
                        .find(Meta.Attributes.scenario.name())
                        .ifPresent(scn -> scn.setName(newScenarioName));
                }
            );
    }

    /**
     * Refactors (renames) an object reference across all scenarios in the project.
     * @param pageName page name containing the object
     * @param oldName old object name
     * @param newName new object name
     */
    public void refactorObjectName(String pageName, String oldName, String newName) {
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorObjectName(pageName, oldName, newName);
        }
    }

    /**
     * Refactors (renames) an object and its page across all scenarios in the project.
     * @param oldpageName old page name
     * @param oldObjName old object name
     * @param newPageName new page name
     * @param newObjName new object name
     */
    public void refactorObjectName(
        String oldpageName,
        String oldObjName,
        String newPageName,
        String newObjName
    ) {
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorObjectName(oldpageName, oldObjName, newPageName, newObjName);
        }
    }

    /**
     * Renames an object reference on the given page for the specified OR scope across the project,
     * by delegating to all scenarios.
     *
     * @param scope    OR scope to match (e.g., shared vs project)
     * @param pageName page (screen) name containing the object reference
     * @param oldName  existing object name to replace
     * @param newName  new object name to apply
     */
    public void refactorObjectName(ORScope scope, String pageName, String oldName, String newName) {
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorObjectName(scope, pageName, oldName, newName);
        }
    }

    /**
     * Refactors Mobile OR object references in TestSteps.
     * Mobile scope is mapped to Web scope because Scenarios/TestSteps
     * are tool-agnostic and only care about PROJECT vs SHARED.
     */
    public void refactorMobileObjectName(
        MobileOR.ORScope scope,
        String pageName,
        String oldName,
        String newName
    ) {
        for (Scenario scenario : getAllScenarios()) {
            WebOR.ORScope webScope = (scope == MobileOR.ORScope.SHARED)
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;
            scenario.refactorObjectName(webScope, pageName, oldName, newName);
        }
    }

    /**
     * Refactors Structured Data OR object references in TestSteps.
     * Mobile scope is mapped to Web scope because Scenarios/TestSteps
     * are tool-agnostic and only care about PROJECT vs SHARED.
     */
    public void refactorStructuredDataObjectName(
        StructuredDataOR.ORScope scope,
        String pageName,
        String oldName,
        String newName
    ) {
        for (Scenario scenario : getAllScenarios()) {
            WebOR.ORScope webScope = (scope == StructuredDataOR.ORScope.SHARED)
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;
            scenario.refactorObjectName(webScope, pageName, oldName, newName);
        }
    }

    /**
     * Refactors SAP OR object references in TestSteps.
     * SAP scope is mapped to Web scope because Scenarios/TestSteps
     * are tool-agnostic and only care about PROJECT vs SHARED.
     */
    public void refactorSapObjectName(
        SapOR.ORScope scope,
        String pageName,
        String oldName,
        String newName
    ) {
        for (Scenario scenario : getAllScenarios()) {
            WebOR.ORScope webScope = (scope == SapOR.ORScope.SHARED)
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;
            scenario.refactorObjectName(webScope, pageName, oldName, newName);
        }
    }

    /**
     * Refactors (renames) a page across all scenarios in the project.
     * @param oldPageName old page name
     * @param newPageName new page name
     */
    public void refactorPageName(String oldPageName, String newPageName) {
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorPageName(oldPageName, newPageName);
        }
    }

    /**
     * Refactors (renames) a page reference across the project for a given Object Repository scope.
     * <p>
     * In addition to delegating the rename for the raw page names, this method also renames
     * scope-qualified page names using the convention:
     * <ul>
     *   <li>{@code "[Shared] " + pageName} when scope is {@code ORScope.SHARED}</li>
     *   <li>{@code "[Project] " + pageName} otherwise</li>
     * </ul>
     * For each {@link Scenario}, it applies both:
     * {@code scenario.refactorPageName(oldPageName, newPageName)} and
     * {@code scenario.refactorPageName(oldScoped, newScoped)}.
     * </p>
     *
     * @param scope       the Object Repository scope used to derive the scoped page name prefix
     * @param oldPageName the original page name to be replaced
     * @param newPageName the new page name to apply
     *
     * @implNote This method performs two refactors per scenario: one for the plain page name and one
     *           for the derived scoped form (e.g., {@code "[Shared] Login"}).
     */
    public void refactorPageName(ORScope scope, String oldPageName, String newPageName) {
        String oldScoped = scope == ORScope.SHARED
            ? "[Shared] " + oldPageName
            : "[Project] " + oldPageName;
        String newScoped = scope == ORScope.SHARED
            ? "[Shared] " + newPageName
            : "[Project] " + newPageName;
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorPageName(oldPageName, newPageName);
            scenario.refactorPageName(oldScoped, newScoped);
        }
    }

    /**
     * Refactors (renames) a test data reference across all scenarios in the project.
     * @param oldTDName old test data name
     * @param newTDName new test data name
     */
    public void refactorTestData(String oldTDName, String newTDName) {
        refactorTestData(oldTDName, newTDName, null);
    }

    /**
     * Refactors (renames) a test data reference across all scenarios in the project, limited to
     * references in the given scope.
     * @param oldTDName old test data name
     * @param newTDName new test data name
     * @param scopeToken "[Shared]" to rewrite only Shared-tagged references, "[Project]" to
     *     rewrite only untagged / Project-tagged references, or {@code null} to rewrite any
     */
    public void refactorTestData(String oldTDName, String newTDName, String scopeToken) {
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorTestData(oldTDName, newTDName, scopeToken);
        }
    }

    /**
     * Refactors (renames) a test data column reference across all scenarios in the project.
     * @param testDataName test data name
     * @param oldColumnName old column name
     * @param newColumnName new column name
     */
    public void refactorTestDataColumn(
        String testDataName,
        String oldColumnName,
        String newColumnName
    ) {
        refactorTestDataColumn(testDataName, oldColumnName, newColumnName, null);
    }

    /**
     * Refactors (renames) a test data column reference across all scenarios in the project,
     * limited to references in the given scope.
     * @param testDataName test data name
     * @param oldColumnName old column name
     * @param newColumnName new column name
     * @param scopeToken "[Shared]" / "[Project]" / {@code null} - see
     *     {@link #refactorTestData(String, String, String)}
     */
    public void refactorTestDataColumn(
        String testDataName,
        String oldColumnName,
        String newColumnName,
        String scopeToken
    ) {
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorTestDataColumn(testDataName, oldColumnName, newColumnName, scopeToken);
        }
    }

    /**
     * Returns test cases that reference the specified object.
     * @param pageName page name
     * @param objectName object name
     * @return list of impacted test cases
     */
    public List<TestCase> getImpactedObjectTestCases(String pageName, String objectName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(scenario.getImpactedObjectTestCases(pageName, objectName));
        }
        return impactedTestCases;
    }

    /**
     * Returns test cases that reference the specified object with OR scope support.
     * Searches for both plain and scoped page names (e.g., "[Shared] PageName").
     * @param scope Object Repository scope (SHARED or PROJECT)
     * @param pageName page name
     * @param objectName object name
     * @return list of impacted test cases
     */
    public List getImpactedObjectTestCases(ORScope scope, String pageName, String objectName) {
        Set impacted = new LinkedHashSet<>();
        String scopedPageName = null;
        if (scope != null) {
            scopedPageName =
                (scope == ORScope.SHARED) ? "[Shared] " + pageName : "[Project] " + pageName;
        }
        // Search in TestPlan scenarios
        for (Scenario scenario : scenarios) {
            impacted.addAll(scenario.getImpactedObjectTestCases(pageName, objectName));
            if (scopedPageName != null) {
                impacted.addAll(scenario.getImpactedObjectTestCases(scopedPageName, objectName));
            }
        }
        // Search in ReusableComponents scenarios
        for (Scenario scenario : reusableScenarios) {
            impacted.addAll(scenario.getImpactedObjectTestCases(pageName, objectName));
            if (scopedPageName != null) {
                impacted.addAll(scenario.getImpactedObjectTestCases(scopedPageName, objectName));
            }
        }
        // Sort by type (Test Plan first), then by scenario name, then by test case name
        List<TestCase> sortedList = new ArrayList<>(impacted);
        sortedList.sort(
            (tc1, tc2) -> {
                // First compare by source type (TEST_PLAN comes before REUSABLE_COMPONENTS)
                int sourceCompare = tc1
                    .getScenario()
                    .getSource()
                    .compareTo(tc2.getScenario().getSource());
                if (sourceCompare != 0) {
                    return sourceCompare;
                }
                // Then compare by scenario name
                int scenarioCompare = tc1
                    .getScenario()
                    .getName()
                    .compareToIgnoreCase(tc2.getScenario().getName());
                if (scenarioCompare != 0) {
                    return scenarioCompare;
                }
                // Finally compare by test case name
                return tc1.getName().compareToIgnoreCase(tc2.getName());
            }
        );
        return sortedList;
    }

    /**
     * Returns test cases that reference the specified test case.
     * Searches across Test Plan, Project Reusable Components, and Shared Reusable Components.
     * @param scenarioName scenario name
     * @param testCaseName test case name
     * @return list of impacted test cases
     */
    public List<TestCase> getImpactedTestCaseTestCases(String scenarioName, String testCaseName) {
        List<TestCase> impactedTestCases = new ArrayList<>();

        // Search in Test Plan scenarios
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(
                scenario.getImpactedTestCaseTestCases(scenarioName, testCaseName)
            );
        }

        // Search in Project Reusable Components scenarios
        for (Scenario scenario : reusableScenarios) {
            impactedTestCases.addAll(
                scenario.getImpactedTestCaseTestCases(scenarioName, testCaseName)
            );
        }

        // Search in Shared Reusable Components scenarios
        for (Scenario scenario : sharedReusableScenarios) {
            impactedTestCases.addAll(
                scenario.getImpactedTestCaseTestCases(scenarioName, testCaseName)
            );
        }

        return impactedTestCases;
    }

    /**
     * Returns test cases that reference the specified test data.
     * @param testDataName test data name
     * @return list of impacted test cases
     */
    public List<TestCase> getImpactedTestDataTestCases(String testDataName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(scenario.getImpactedTestDataTestCases(testDataName));
        }
        return impactedTestCases;
    }

    /**
     * Returns the project settings.
     * @return project settings
     */
    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    /**
     * Returns the object repository for this project.
     * @return object repository
     */
    public ObjectRepository getObjectRepository() {
        return objectRepository;
    }

    /**
     * Loads project information from the .project file.
     * @param f project file
     * @return loaded or newly created project info
     */
    private ProjectInfo loadProjectInfo(File f) {
        try {
            if (f.exists() && !FileScanner.readFile(f).isEmpty()) {
                return checkData(new ObjectMapper().readValue(f, ProjectInfo.class));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return updateData(ProjectInfo.create(name));
    }

    /**
     * Checks and updates project data if it's empty.
     * @param project project info to check
     * @return updated project info
     */
    private ProjectInfo checkData(ProjectInfo project) {
        if (project.getData().isEmpty()) {
            updateData(project);
        }
        return project;
    }

    /**
     * Updates project data with current scenario and test case metadata.
     * @param project project info to update
     * @return updated project info
     */
    private ProjectInfo updateData(ProjectInfo project) {
        getAllScenarios().stream().map(To::Meta).forEach(project::addMeta);
        getAllScenarios().stream().flatMap(To::TC).map(To.DI::fromTC).forEach(project::addData);
        releases.stream().flatMap(To::TS).map(To.DI::fromTS).forEach(project::addData);
        return project;
    }

    /**
     * Utility class for converting project entities to metadata and data items.
     */
    static class To {

        /**
         * Extracts test cases from a scenario as a stream.
         * @param scn scenario
         * @return stream of test cases
         */
        private static Stream<TestCase> TC(Scenario scn) {
            return scn.getTestCases().stream();
        }

        /**
         * Extracts test sets from a release as a stream.
         * @param scn release
         * @return stream of test sets
         */
        private static Stream<TestSet> TS(Release scn) {
            return scn.getTestSets().stream();
        }

        /**
         * Creates metadata object from a scenario.
         * @param scn scenario
         * @return meta object for scenario
         */
        private static Meta Meta(Scenario scn) {
            return Meta.createScenario(scn.getName());
        }

        /**
         * Utility class for creating DataItem objects from project entities.
         */
        static class DI {

            /**
             * Creates a data item with specified attributes.
             * @param id data item ID
             * @param name data item name
             * @param t type attribute value
             * @return created data item
             */
            private static DataItem create(String id, String name, Object t) {
                DataItem data = new DataItem();
                data.setId(id);
                data.setName(name);
                data.getAttributes().add(Meta.Attributes.type, t);
                return data;
            }

            /**
             * Creates a data item from a test case.
             * @param tc test case
             * @return data item representing the test case
             */
            private static DataItem fromTC(TestCase tc) {
                DataItem data = create(
                    tc.getKey(),
                    tc.getName(),
                    tc.isReusable() ? Meta.Attributes.reusable : Meta.Attributes.testcase
                );
                data.getAttributes().add(Meta.Attributes.scenario, tc.getScenario().getName());
                return data;
            }

            /**
             * Creates a data item from a test set.
             * @param ts test set
             * @return data item representing the test set
             */
            private static DataItem fromTS(TestSet ts) {
                DataItem data = create(ts.getName(), ts.getName(), Meta.Attributes.testset);
                data.getAttributes().add(Meta.Attributes.release, ts.getRelease().getName());
                return data;
            }
        }
    }
}
