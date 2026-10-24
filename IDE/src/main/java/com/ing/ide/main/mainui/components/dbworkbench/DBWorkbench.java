package com.ing.ide.main.mainui.components.dbworkbench;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.dbworkbench.DBQuery;
import com.ing.datalib.dbworkbench.DBQueryCollection;
import com.ing.datalib.dbworkbench.DBValidation;
import com.ing.datalib.settings.DBProperties;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.SlideShow;
import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import com.ing.ide.main.mainui.components.dbworkbench.util.SqlScript;
import com.ing.util.encryption.Encryption;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for the Database Workbench. Owns the live JDBC connections, resolves
 * connection details from the project's {@link DBProperties} store, and converts a
 * {@link DBQuery} into an INGenious Test Case or reusable User Intent using the
 * existing {@code Database} engine actions.
 * <p>
 * Mirrors {@code APITester}.
 */
public class DBWorkbench implements SlideShow.SlideChangeListener {
    private static final Logger LOG = Logger.getLogger(DBWorkbench.class.getName());
    private static final String DB_OBJECT = "Database";
    private static final String ENC_SUFFIX = " Enc";

    private final AppMainFrame mainFrame;
    private final DBWorkbenchUI ui;
    private final JdbcExecutor executor;
    private final ObjectMapper objectMapper;
    private final List<DBQueryCollection> collections = new ArrayList<>();
    private static final String DEFAULT_COLLECTION = "My Queries";

    public DBWorkbench(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.executor = new JdbcExecutor();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.ui = new DBWorkbenchUI(this);
    }

    public AppMainFrame getMainFrame() {
        return mainFrame;
    }

    public DBWorkbenchUI getDBWorkbenchUI() {
        return ui;
    }

    public JdbcExecutor getExecutor() {
        return executor;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Connections
    // ═══════════════════════════════════════════════════════════════════

    public DBProperties getDatabaseSettings() {
        Project project = mainFrame.getProject();
        if (project == null) return null;
        return project.getProjectSettings().getDatabaseSettings();
    }

    public List<String> getConnectionAliases() {
        DBProperties dbp = getDatabaseSettings();
        return dbp != null ? new ArrayList<>(dbp.getDbList()) : new ArrayList<>();
    }

    /**
     * Returns runtime connection properties for the alias with the password
     * decrypted to plaintext, ready for a JDBC {@code getConnection}.
     */
    public Properties resolveConnectionProps(String alias) {
        DBProperties dbp = getDatabaseSettings();
        if (dbp == null) return null;
        Properties stored = dbp.getDBPropertiesFor(alias);
        if (stored == null) return null;
        Properties runtime = new Properties();
        for (String key : stored.stringPropertyNames()) {
            runtime.setProperty(key, stored.getProperty(key));
        }
        String pass = runtime.getProperty(JdbcExecutor.PASSWORD, "");
        if (pass.endsWith(ENC_SUFFIX)) {
            String cipher = pass.substring(0, pass.length() - ENC_SUFFIX.length());
            String plain = Encryption.getInstance().decrypt(cipher);
            runtime.setProperty(JdbcExecutor.PASSWORD, plain != null ? plain : "");
        }
        return runtime;
    }

    /** Called after connections are added/edited/deleted to refresh the UI. */
    public void onConnectionsChanged() {
        ui.refresh();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Scenario helpers (mirror APITester)
    // ═══════════════════════════════════════════════════════════════════

    public List<Scenario> getAvailableScenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        Project project = mainFrame.getProject();
        if (project != null) {
            scenarios.addAll(project.getScenarios());
        }
        return scenarios;
    }

    public List<Scenario> getAvailableReusableScenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        Project project = mainFrame.getProject();
        if (project != null) {
            scenarios.addAll(project.getReusableScenarios());
        }
        return scenarios;
    }

    /**
     * Returns the named scenario, creating it when it does not exist yet, so the
     * Automation dialog can accept a brand-new scenario name.
     *
     * @param name the scenario name typed or picked by the user
     * @param reusable true for a User Intent (reusable) scenario
     * @return the existing or newly created scenario, or {@code null} on failure
     */
    public Scenario findOrCreateScenario(String name, boolean reusable) {
        Project project = mainFrame.getProject();
        if (project == null || name == null || name.trim().isEmpty()) return null;
        String scenarioName = name.trim();
        Scenario existing = reusable
            ? project.getReusableScenarioByName(scenarioName)
            : project.getTestPlanScenarioByName(scenarioName);
        if (existing != null) return existing;

        Scenario created = reusable
            ? project.addReusableScenario(scenarioName)
            : project.addScenario(scenarioName);
        if (created != null) {
            reloadTestDesignTrees();
            LOG.info("Created scenario '" + scenarioName + "' from the Database Workbench");
        }
        return created;
    }

    /**
     * @param scenario the target scenario
     * @param testCaseName the name to check
     * @return true when the scenario already holds a test case with that name
     */
    public boolean testCaseExists(Scenario scenario, String testCaseName) {
        return scenario != null && scenario.getTestCaseByName(testCaseName) != null;
    }

    public void navigateToTestCase(TestCase testCase) {
        if (testCase == null) return;
        javax.swing.SwingUtilities.invokeLater(
            () -> {
                mainFrame.showTestDesign();
                if (mainFrame.getTestDesign() != null) {
                    mainFrame
                        .getTestDesign()
                        .getTestCaseComp()
                        .loadTableModelForSelection(testCase);
                }
            }
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // Automation conversion
    // ═══════════════════════════════════════════════════════════════════

    /** What to do when the target test case already exists. */
    public enum ExistingCasePolicy {
        /** The name is free; create a new test case. */
        CREATE,
        /** Replace every step of the existing test case. */
        OVERWRITE,
        /** Keep the existing steps and add the new ones at the end. */
        APPEND
    }

    /**
     * Converts a workbench query into a Test Case or a User Intent (reusable).
     *
     * @param query the query, its validations and its connection alias
     * @param scenario the target scenario
     * @param testCaseName the name of the test case / user intent
     * @param reusable true to create a User Intent instead of a Test Case
     * @param policy what to do when {@code testCaseName} already exists
     * @return the created or updated test case, or {@code null} on failure
     */
    public TestCase convertQueryToAutomation(
        DBQuery query,
        Scenario scenario,
        String testCaseName,
        boolean reusable,
        ExistingCasePolicy policy
    ) {
        if (query == null || scenario == null || testCaseName == null) return null;
        if (reusable && !scenario.isReusableScenario()) {
            LOG.warning(
                "convertQueryToAutomation called with non-reusable scenario: " + scenario.getName()
            );
            return null;
        }

        TestCase existing = scenario.getTestCaseByName(testCaseName);
        boolean created = existing == null;
        TestCase testCase = created ? scenario.addTestCase(testCaseName) : existing;
        if (testCase == null) {
            LOG.warning("Test case '" + testCaseName + "' could not be created");
            return null;
        }
        if (!created && policy == ExistingCasePolicy.OVERWRITE) {
            testCase.getTestSteps().clear();
        }

        try {
            ensureConnectionPersisted(query.getConnectionAlias());
            buildStepsForQuery(testCase, query);
            testCase.save();
            addToTestDesignTree(testCase, reusable, created);
            LOG.info(
                "Converted DB query '" +
                query.getName() +
                "' to " +
                (reusable ? "user intent '" : "test case '") +
                testCaseName +
                "'"
            );
            return testCase;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to convert DB query to automation", e);
            if (created) {
                scenario.removeTestCase(testCase);
            }
            return null;
        }
    }

    private void addToTestDesignTree(TestCase testCase, boolean reusable, boolean created) {
        if (!created || mainFrame.getTestDesign() == null) return;
        if (reusable) {
            if (mainFrame.getTestDesign().getReusableTree() != null) {
                mainFrame.getTestDesign().getReusableTree().getTreeModel().addTestCase(testCase);
            }
        } else if (mainFrame.getTestDesign().getProjectTree() != null) {
            mainFrame.getTestDesign().getProjectTree().getTreeModel().addTestCase(testCase);
        }
    }

    private void reloadTestDesignTrees() {
        if (mainFrame.getTestDesign() == null) return;
        if (mainFrame.getTestDesign().getProjectTree() != null) {
            mainFrame.getTestDesign().getProjectTree().load();
        }
        if (mainFrame.getTestDesign().getReusableTree() != null) {
            mainFrame.getTestDesign().getReusableTree().load();
        }
    }

    /**
     * Makes sure the alias referenced by the generated steps is on disk under
     * Database Configurations before the test case is saved, so the generated
     * test can resolve it without the IDE being restarted.
     */
    private void ensureConnectionPersisted(String alias) {
        DBProperties dbp = getDatabaseSettings();
        if (dbp == null || alias == null || alias.isEmpty()) return;
        if (dbp.getDBPropertiesFor(alias) != null) {
            dbp.save(alias);
        }
    }

    /**
     * Emits {@code Database} steps: open connection, one run step per SQL
     * statement in the script, one step per validation, then close the
     * connection. The engine executes a step's Input as a single statement, so a
     * multi-statement script has to be split here.
     */
    private void buildStepsForQuery(TestCase testCase, DBQuery query) {
        // 1. Open connection
        TestStep open = testCase.addNewStep();
        open.setObject(DB_OBJECT);
        open.setDescription("Initiate the DB transaction");
        open.setAction("initDBConnection");
        open.setInput("#" + safe(query.getConnectionAlias()));

        // 2. Run the query, one step per statement
        for (String statement : statementsOf(query)) {
            boolean write = SqlScript.classify(statement).isWrite();
            TestStep run = testCase.addNewStep();
            run.setObject(DB_OBJECT);
            run.setDescription(write ? "Execute DML query" : "Execute Select query");
            run.setAction(write ? "executeDMLQuery" : "executeSelectQuery");
            run.setInput("@" + statement);
        }

        // 3. Validations / stores
        if (query.getValidations() != null) {
            for (DBValidation v : query.getValidations()) {
                if (!v.isEnabled()) continue;
                addValidationStep(testCase, v);
            }
        }

        // 4. Close connection
        TestStep close = testCase.addNewStep();
        close.setObject(DB_OBJECT);
        close.setDescription("Close the DB Connection");
        close.setAction("closeDBConnection");
    }

    private List<String> statementsOf(DBQuery query) {
        List<String> statements = SqlScript.split(safe(query.getSql()));
        if (statements.isEmpty()) {
            statements.add(safe(query.getSql()));
        }
        return statements;
    }

    private void addValidationStep(TestCase testCase, DBValidation v) {
        TestStep step = testCase.addNewStep();
        step.setObject(DB_OBJECT);
        String column = safe(v.getColumn());
        String columnWithRow = v.getRow() > 1 ? column + "," + v.getRow() : column;

        switch (v.getOperator()) {
            case EQUALS:
            case CONTAINS:
            case EXISTS:
                // assertDBResult scans the whole column for the value; it does not take a row.
                step.setDescription("Assert value exists in column " + column);
                step.setAction("assertDBResult");
                step.setCondition(column);
                step.setInput("@" + safe(v.getExpectedValue()));
                break;
            case CELL_EQUALS:
                step.setDescription("Assert value equals at column " + column);
                step.setAction("assertColumnValueEquals");
                step.setCondition(columnWithRow);
                step.setInput("@" + safe(v.getExpectedValue()));
                break;
            case IS_NULL:
                step.setDescription("Assert value is NULL at column " + column);
                step.setAction("assertColumnValueIsNull");
                step.setCondition(columnWithRow);
                break;
            case IS_NOT_NULL:
                step.setDescription("Assert value is NOT NULL at column " + column);
                step.setAction("assertColumnValueIsNotNull");
                step.setCondition(columnWithRow);
                break;
            case ROW_COUNT:
                step.setDescription("Assert row count");
                step.setAction("assertRowCount");
                step.setInput("@" + safe(v.getExpectedValue()));
                break;
            case STORE_VAR:
                step.setDescription("Store DB value in variable");
                step.setAction("storeValueInVariable");
                step.setCondition(columnWithRow);
                step.setInput(safe(v.getExpectedValue()));
                break;
            case STORE_GLOBAL:
                step.setDescription("Store DB value in global variable");
                step.setAction("storeValueInGlobalVariable");
                step.setCondition(columnWithRow);
                step.setInput(safe(v.getExpectedValue()));
                break;
            case STORE_SHEET:
                step.setDescription("Store DB value in Test Data sheet");
                step.setAction("storeDBValueinDataSheet");
                step.setCondition(column);
                step.setInput(safe(v.getExpectedValue()));
                break;
            default:
                testCase.getTestSteps().remove(step);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Lifecycle (mirror APITester hooks called from AppMainFrame)
    // ═══════════════════════════════════════════════════════════════════

    public void loadData() {
        loadCollections();
        ui.refresh();
    }

    public void saveData() {
        saveCollections();
    }

    /** All saved queries across every collection (for the Open dialog). */
    public List<DBQuery> getAllSavedQueries() {
        List<DBQuery> all = new ArrayList<>();
        for (DBQueryCollection col : collections) {
            if (col.getQueries() != null) all.addAll(col.getQueries());
        }
        return all;
    }

    /** Saves (upserts by name) a query into the default collection and persists it. */
    public void saveQuery(DBQuery query) {
        DBQueryCollection target = collections.isEmpty() ? null : collections.get(0);
        if (target == null) {
            target = new DBQueryCollection(DEFAULT_COLLECTION);
            collections.add(target);
        }
        target
            .getQueries()
            .removeIf(q -> q.getName() != null && q.getName().equals(query.getName()));
        target.getQueries().add(query);
        saveCollections();
    }

    private Path getDataPath() {
        Project project = mainFrame.getProject();
        if (project == null) return null;
        return Path.of(project.getLocation(), "db-workbench", "collections");
    }

    private void loadCollections() {
        collections.clear();
        Path dir = getDataPath();
        if (dir == null) return;
        if (!Files.exists(dir)) return;
        try {
            Files
                .list(dir)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(
                    p -> {
                        try {
                            collections.add(
                                objectMapper.readValue(p.toFile(), DBQueryCollection.class)
                            );
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to load DB query collection: " + p, e);
                        }
                    }
                );
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list DB query collections", e);
        }
    }

    private void saveCollections() {
        Path dir = getDataPath();
        if (dir == null) return;
        try {
            Files.createDirectories(dir);
            for (DBQueryCollection col : collections) {
                String fileName = col.getName() == null
                    ? "collection"
                    : col.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
                objectMapper.writeValue(dir.resolve(fileName + ".json").toFile(), col);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to save DB query collections", e);
        }
    }

    public void registerSlideChangeListener() {
        SlideShow slideShow = mainFrame.getSlideShow();
        if (slideShow != null) {
            slideShow.addSlideChangeListener(this);
        }
    }

    @Override
    public void onSlideLeaving(String slideName) {
        if ("DBWorkbench".equals(slideName)) {
            saveData();
        }
    }

    /** Closes every live JDBC connection (project switch / shutdown). */
    public void closeAllConnections() {
        executor.closeAll();
    }
}
