package com.ing.datalib.component;

import com.ing.datalib.component.io.TestCaseFormat;
import com.ing.datalib.component.io.TestCaseStore;
import com.ing.datalib.component.io.TestCaseStoreFactory;
import com.ing.datalib.component.io.YamlTestCaseStore;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebOR.ORScope;
import com.ing.datalib.util.data.FileScanner;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.TableModelEvent;

/**
 * Represents a test case composed of ordered {@link TestStep} entries and implements a table model
 * suitable for direct editing in UI components.
 * <p>
 * A {@code TestCase} belongs to a {@link Scenario}, loads and persists its steps from/to a CSV file,
 * and supports common editing operations such as inserting, removing, moving, replicating steps,
 * clearing values, toggling comments/breakpoints, and bulk removal. Save state is tracked and propagated
 * via a {@link SaveListener}.
 * </p>
 *
 * <p>
 * The class also supports creating and managing reusable test cases (represented as “Execute” steps),
 * provides utilities for refactoring references (scenario/test case reuse links, object/page names,
 * test data and columns—including scope-aware OR references), and can report impact when a given object,
 * reusable, or test data reference is used.
 * </p>
 */
public class TestCase extends DataModel {
    private Scenario scenario;

    private final List<TestStep> testSteps = Collections.synchronizedList(
        new ArrayList<TestStep>()
    );

    private String name;

    private Boolean saved = true;

    private SaveListener saveListener;

    private Reusable reusable = null;

    private TestCase parentTestCase = null;

    private Boolean exitParamLoop = false;

    private Integer dynamicMaxInt = null;

    private int migratedReferencesCount = 0;
    private int migratedReusableActionCount = 0;
    private int migratedResolvedReusableActionCount = 0;

    private boolean migrationChecked = false;

    /**
     * When true, skips migrations on test case load to ensure read-only validation.
     * Set by the parent Scenario when loaded in read-only mode.
     */
    private boolean readOnlyMode = false;

    public TestCase(Scenario scenario, String name) {
        this.scenario = scenario;
        this.name = TestCaseFormat.stripExtension(name);
    }

    public Project getProject() {
        return scenario.getProject();
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public List<TestStep> getTestSteps() {
        return testSteps;
    }

    /**
     * Returns the number of references that were migrated to explicit scope prefixes during load.
     * This count is reset after retrieval to avoid duplicate notifications.
     *
     * @return The number of migrated references
     */
    public int getMigratedReferencesCount() {
        int count = migratedReferencesCount;
        migratedReferencesCount = 0; // Reset after retrieval
        return count;
    }

    @Override
    public void removeRow(int row) {
        if (row < testSteps.size()) {
            rowDeleted(row);
            testSteps.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    @Override
    public void insertRow(int row, Object[] values) {
        addNewStepAt(row);
        for (int i = 0; i < values.length; i++) {
            setValueAt(values[i], row, i);
        }
    }

    @Override
    public Boolean addRow() {
        addNewStep();
        return true;
    }

    public TestStep addNewStep() {
        TestStep step = new TestStep(this);
        testSteps.add(step);
        rowAdded(testSteps.size() - 1);
        fireTableRowsInserted(testSteps.size() - 1, testSteps.size() - 1);
        return step;
    }

    public TestStep addNewStepAt(int index) {
        TestStep step = new TestStep(this);
        testSteps.add(index, step);
        rowAdded(index);
        fireTableRowsInserted(index, index);
        return step;
    }

    public void replicateStepAt(int index) {
        TestStep step = testSteps.get(index);
        TestStep newStep = new TestStep(this);
        step.copyValuesTo(newStep);
        testSteps.add(index, newStep);
        rowAdded(index);
        fireTableRowsInserted(index, index);
    }

    public Boolean moveRowsUp(int from, int to) {
        if (from - 1 < 0) {
            return false;
        }
        to = to + 1;
        Collections.rotate(testSteps.subList(from - 1, to), -1);
        setSaved(false);
        return true;
    }

    public Boolean moveRowsDown(int from, int to) {
        if (to + 1 > testSteps.size() - 1) {
            return false;
        }
        to += 1;
        Collections.rotate(testSteps.subList(from, to + 1), 1);
        setSaved(false);
        return true;
    }

    public void clearSteps() {
        startGroupEdit();
        testSteps.clear();
        fireTableDataChanged();
        stopGroupEdit();
    }

    public void removeSteps(List<Integer> indices) {
        if (!indices.isEmpty()) {
            startGroupEdit();
            for (int index : indices) {
                rowDeleted(index);
                testSteps.remove(index);
            }
            stopGroupEdit();
            fireTableRowsDeleted(indices.get(indices.size() - 1), indices.get(0));
        }
    }

    public void removeSteps(int[] indices) {
        if (indices != null && indices.length > 0) {
            startGroupEdit();
            for (int index : indices) {
                if (index < testSteps.size()) {
                    rowDeleted(index);
                    testSteps.remove(index);
                    fireTableRowsDeleted(index, index);
                }
            }
            stopGroupEdit();
        }
    }

    public void toggleComment(int[] indices) {
        startGroupEdit();
        for (int index : indices) {
            testSteps.get(index).toggleComment();
        }
        stopGroupEdit();
    }

    public void toggleBreakPoint(int[] indices) {
        startGroupEdit();
        for (int index : indices) {
            testSteps.get(index).toggleBreakPoint();
        }
        stopGroupEdit();
    }

    /**
     * Marks the assertion steps at the given row indices as hard or soft
     * assertions. Non-assertion steps are ignored.
     *
     * @param indices the selected row indices
     * @param hard    {@code true} for hard assertion, {@code false} for soft
     */
    public void setHardAssertion(int[] indices, boolean hard) {
        startGroupEdit();
        for (int index : indices) {
            TestStep step = testSteps.get(index);
            if (step.isAssertStep()) {
                step.setHardAssertion(hard);
            }
        }
        stopGroupEdit();
    }

    public void addReusableStep(String reusable) {
        TestStep step = new TestStep(this);
        step.setObject("Execute");
        step.setAction(reusable);
        testSteps.add(step);
        rowAdded(testSteps.size() - 1);
        fireTableRowsInserted(testSteps.size() - 1, testSteps.size() - 1);
    }

    public void addReusableStep(int index, String reusable) {
        TestStep step = new TestStep(this);
        step.setObject("Execute");
        step.setAction(reusable);
        addStep(index, step);
    }

    public void addObjectStep(int index, String objectName, String pageName) {
        TestStep step = new TestStep(this).asObjectStep(objectName, pageName);
        addStep(index, step);
    }

    public void addObjectStep(int index, ResolvedWebObject rwo) {
        TestStep step = new TestStep(this).asObjectStep(rwo);
        addStep(index, step);
    }

    private void addStep(int index, TestStep step) {
        if (testSteps.size() > index) {
            testSteps.add(index, step);
        } else {
            testSteps.add(step);
        }
        rowAdded(index);
        fireTableRowsInserted(index, index);
    }

    public TestCase createAsReusable(String reusableName, int fromStep, int toStep) {
        return createAsReusable(getScenario(), reusableName, fromStep, toStep);
    }

    /**
     * Extracts the selected steps into a new reusable test case under the given
     * target scenario, replacing them in this test case with a single step that
     * invokes the newly created reusable.
     *
     * @param targetScenario scenario (typically under Reusable Components) that
     *                       will hold the new reusable test case
     * @param reusableName   name of the reusable test case to create
     * @param fromStep       index of the first selected step (inclusive)
     * @param toStep         index of the last selected step (inclusive)
     * @return the created reusable test case, or {@code null} if it could not be
     *         created
     */
    public TestCase createAsReusable(
        Scenario targetScenario,
        String reusableName,
        int fromStep,
        int toStep
    ) {
        if (targetScenario == null) {
            return null;
        }
        TestCase newTestcase = targetScenario.addTestCase(reusableName);
        if (newTestcase != null) {
            for (int i = fromStep; i <= toStep; i++) {
                testSteps.get(i).copyValuesTo(newTestcase.addNewStep());
            }
            newTestcase.save();
            startGroupEdit();

            // Create Execute step with appropriate scope prefix
            TestStep reusableStep = new TestStep(this);
            reusableStep.setObject("Execute");
            reusableStep.setAction(targetScenario.getName() + ":" + reusableName);

            // Auto-populate Reference column based on target scenario source
            if (targetScenario.isSharedReusableScenario()) {
                reusableStep.setReference("[Shared]");
            } else if (targetScenario.isReusableScenario()) {
                reusableStep.setReference("[Project]");
            }

            addStep(fromStep, reusableStep);

            for (int i = toStep + 1; i >= fromStep + 1; i--) {
                rowDeleted(i);
                testSteps.remove(i);
            }
            stopGroupEdit();
            fireTableRowsDeleted(fromStep + 1, toStep);
            return newTestcase;
        }
        return null;
    }

    public void toggleAsReusable() {
        if (reusable == null) {
            reusable = new Reusable();
        } else {
            reusable = null;
        }
    }

    public void copyValuesTo(TestCase testCase) {
        for (TestStep testStep : testSteps) {
            testStep.copyValuesTo(testCase.addNewStep());
        }
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        File dir = new File(scenario.getLocation());
        TestCaseFormat fmt = TestCaseStoreFactory.resolveFormat(dir, name, projectDefaultFormat());
        return dir.getPath() + File.separator + name + fmt.extension();
    }

    /**
     * Returns the on-disk format currently used by this test case
     * (resolved from existing files; falls back to the project default).
     */
    public TestCaseFormat getFormat() {
        File dir = new File(scenario.getLocation());
        return TestCaseStoreFactory.resolveFormat(dir, name, projectDefaultFormat());
    }

    private TestCaseFormat projectDefaultFormat() {
        try {
            if (getProject() != null && getProject().getInfo() != null) {
                return TestCaseFormat.parse(
                    getProject().getInfo().getTestCaseFormat(),
                    TestCaseFormat.YAML
                );
            }
        } catch (Exception ignored) {
            // Project info may be unavailable in unit-test contexts; fall through.
        }
        return TestCaseFormat.YAML;
    }

    public void loadTestCaseTableModel() {
        if (testSteps.isEmpty()) {
            loadSteps();
        } else if (!migrationChecked) {
            // Check for migration even if steps are already loaded
            // Skip if in read-only mode (e.g., during validation)
            if (!readOnlyMode) {
                checkAndMigrateReferences();
            }
            migrationChecked = true;
        }
    }

    @Override
    public synchronized void loadTableModel() {
        loadTestCaseTableModel();
    }

    public void reload() {
        testSteps.clear();
        loadSteps();
        fireTableStructureChanged();
        setSaved(true);
    }

    private void loadSteps() {
        File file = new File(getLocation());
        TestCaseFormat fmt = TestCaseFormat.fromFile(file);
        if (fmt == null) {
            fmt = projectDefaultFormat();
        }
        TestCaseStore store = TestCaseStoreFactory.testCaseStore(fmt);
        List<List<String>> rows;
        try {
            rows = store.load(file);
        } catch (Exception ex) {
            Logger
                .getLogger(TestCase.class.getName())
                .log(Level.SEVERE, "Error loading test case from " + file.getPath(), ex);
            rows = new ArrayList<>();
        }
        syncTagsFromStore(store, file);
        migratedReferencesCount = 0;
        migratedReusableActionCount = 0;
        migratedResolvedReusableActionCount = 0;

        if (!rows.isEmpty()) {
            for (List<String> row : rows) {
                TestStep step = new TestStep(this, row);

                // Auto-migrate unprefixed references to explicit [Project]/[Shared] format
                // Skip migrations if in read-only mode (e.g., during validation)
                if (!readOnlyMode) {
                    String ref = step.getReference();
                    if (
                        ref != null &&
                        !ref.trim().isEmpty() &&
                        !ref.startsWith("[Project] ") &&
                        !ref.startsWith("[Shared] ") &&
                        step.isPageObjectStep()
                    ) {
                        String migratedRef = resolveAndAddPrefix(step);
                        if (migratedRef != null && !migratedRef.equals(ref)) {
                            step.setReference(migratedRef);
                            migratedReferencesCount++;
                        }
                    }

                    // Auto-migrate unscoped Execute reusable references.
                    // Current model: keep Action unscoped and store scope in Reference column.
                    if (step.isReusableStep()) {
                        ScopedReusableMigrationResult result = migrateReusableScopeToReference(
                            step
                        );
                        if (result != null && result.changed) {
                            migratedReusableActionCount++;
                            if (result.successfullyResolved) {
                                migratedResolvedReusableActionCount++;
                            }
                        }
                    }
                }

                testSteps.add(step);
            }
            setSaved(true);

            // Save only if migrations occurred and NOT in read-only mode
            boolean hasReusableScopeMigration = migratedReusableActionCount > 0;
            if (!readOnlyMode && (migratedReferencesCount > 0 || hasReusableScopeMigration)) {
                setSaved(false);
                save();
            }

            if (migratedReferencesCount > 0) {
                Logger
                    .getLogger(TestCase.class.getName())
                    .log(
                        Level.INFO,
                        "Migrated {0} object reference(s) to explicit scope prefixes in: {1}",
                        new Object[] { migratedReferencesCount, getName() }
                    );
            }

            if (migratedReusableActionCount > 0) {
                Logger
                    .getLogger(TestCase.class.getName())
                    .log(
                        Level.INFO,
                        "Migrated {0} Execute reusable reference(s) to Reference scope column in: {1} (resolved={2})",
                        new Object[] {
                            migratedReusableActionCount,
                            getName(),
                            migratedResolvedReusableActionCount
                        }
                    );
            }
        } else {
            testSteps.add(new TestStep(this));
        }
        migrationChecked = true;
        super.clearUndoRedo();
    }

    /**
     * Checks and migrates unprefixed references for test steps already loaded in memory.
     * This is called when test steps are already loaded but migration hasn't been checked yet.
     */
    /**
     * Sets the read-only mode for this TestCase.
     * When true, prevents test case migrations during load.
     *
     * @param readOnly true to enable read-only mode
     */
    public void setReadOnlyMode(boolean readOnly) {
        this.readOnlyMode = readOnly;
    }

    private void checkAndMigrateReferences() {
        migratedReferencesCount = 0;
        migratedReusableActionCount = 0;
        migratedResolvedReusableActionCount = 0;

        for (TestStep step : testSteps) {
            String ref = step.getReference();
            if (
                ref != null &&
                !ref.trim().isEmpty() &&
                !ref.startsWith("[Project] ") &&
                !ref.startsWith("[Shared] ") &&
                step.isPageObjectStep()
            ) {
                String migratedRef = resolveAndAddPrefix(step);
                if (migratedRef != null && !migratedRef.equals(ref)) {
                    step.setReference(migratedRef);
                    migratedReferencesCount++;
                }
            }

            if (step.isReusableStep()) {
                ScopedReusableMigrationResult result = migrateReusableScopeToReference(step);
                if (result != null && result.changed) {
                    migratedReusableActionCount++;
                    if (result.successfullyResolved) {
                        migratedResolvedReusableActionCount++;
                    }
                }
            }
        }

        boolean hasReusableScopeMigration = migratedReusableActionCount > 0;
        if (migratedReferencesCount > 0 || hasReusableScopeMigration) {
            setSaved(false);
            save();
        }

        if (migratedReferencesCount > 0) {
            Logger
                .getLogger(TestCase.class.getName())
                .log(
                    Level.INFO,
                    "Migrated {0} object reference(s) to explicit scope prefixes in: {1}",
                    new Object[] { migratedReferencesCount, getName() }
                );
        }

        if (migratedReusableActionCount > 0) {
            Logger
                .getLogger(TestCase.class.getName())
                .log(
                    Level.INFO,
                    "Migrated {0} Execute reusable reference(s) to Reference scope column in: {1} (resolved={2})",
                    new Object[] {
                        migratedReusableActionCount,
                        getName(),
                        migratedResolvedReusableActionCount
                    }
                );
        }
    }

    private ScopedReusableMigrationResult migrateReusableScopeToReference(TestStep step) {
        String action = step.getAction();
        if (action == null || action.trim().isEmpty()) {
            return null;
        }

        try {
            ReusableRef ref = ReusableRef.parse(action);

            ReusableRef.Scope resolvedScope = ref.getScope();
            boolean resolved = true;

            if (resolvedScope == ReusableRef.Scope.UNSCOPED) {
                resolvedScope = scopeFromReference(step.getReference());
                if (resolvedScope == ReusableRef.Scope.UNSCOPED) {
                    Scenario projectReusable = getProject()
                        .getReusableScenarioByName(ref.getScenarioName());
                    if (
                        projectReusable != null &&
                        projectReusable.getTestCaseByName(ref.getTestCaseName()) != null
                    ) {
                        resolvedScope = ReusableRef.Scope.PROJECT;
                    } else {
                        Scenario sharedReusable = getProject()
                            .getSharedReusableScenarioByName(ref.getScenarioName());
                        if (
                            sharedReusable != null &&
                            sharedReusable.getTestCaseByName(ref.getTestCaseName()) != null
                        ) {
                            resolvedScope = ReusableRef.Scope.SHARED;
                        } else {
                            // Mandatory fallback for legacy refs without explicit scope.
                            resolvedScope = ReusableRef.Scope.PROJECT;
                            resolved = false;
                        }
                    }
                }
            }

            String normalizedAction = new ReusableRef(
                ReusableRef.Scope.UNSCOPED,
                ref.getScenarioName(),
                ref.getTestCaseName()
            )
            .format();
            String scopeRef = resolvedScope == ReusableRef.Scope.SHARED ? "[Shared]" : "[Project]";

            boolean changed = false;
            if (!normalizedAction.equals(action)) {
                step.setAction(normalizedAction);
                changed = true;
            }
            if (!scopeRef.equals(step.getReference())) {
                step.setReference(scopeRef);
                changed = true;
            }

            return new ScopedReusableMigrationResult(changed, resolved);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ReusableRef.Scope scopeFromReference(String reference) {
        String ref = Objects.toString(reference, "").trim();
        if (ref.startsWith("[Project]")) {
            return ReusableRef.Scope.PROJECT;
        }
        if (ref.startsWith("[Shared]")) {
            return ReusableRef.Scope.SHARED;
        }
        return ReusableRef.Scope.UNSCOPED;
    }

    private static final class ScopedReusableMigrationResult {
        private final boolean changed;
        private final boolean successfullyResolved;

        private ScopedReusableMigrationResult(boolean changed, boolean successfullyResolved) {
            this.changed = changed;
            this.successfullyResolved = successfullyResolved;
        }
    }

    /**
     * Resolves an unprefixed reference and adds the appropriate [Project] or [Shared] prefix.
     * Uses Project-first resolution priority matching runtime behavior.
     *
     * @param step The test step containing the reference to resolve
     * @return The reference with explicit scope prefix, or null if unresolvable
     */
    private String resolveAndAddPrefix(TestStep step) {
        try {
            var repo = getProject().getObjectRepository();
            if (repo == null) {
                return null;
            }

            String ref = step.getReference();
            String objectName = step.getObject();

            // Try resolving as web object (Project scope first, then Shared)
            var wref = ResolvedWebObject.PageRef.parse(ref);
            var wres = repo.resolveWebObject(wref, objectName);

            if (wres != null) {
                if (wres.isFromShared()) {
                    return "[Shared] " + wres.getPageName();
                } else if (wres.isFromProject()) {
                    return "[Project] " + wres.getPageName();
                }
            }

            // Try resolving as mobile object (Project scope first, then Shared)
            var mref = ResolvedMobileObject.PageRef.parse(ref);
            var mres = repo.resolveMobileObject(mref, objectName);

            if (mres != null) {
                if (mres.isFromShared()) {
                    return "[Shared] " + mres.getPageName();
                } else if (mres.isFromProject()) {
                    return "[Project] " + mres.getPageName();
                }
            }

            // Try resolving as structured data object (Project scope first, then Shared)
            var sdref = ResolvedStructuredDataObject.PageRef.parse(ref);
            var sdres = repo.resolveStructuredDataObject(sdref, objectName);

            if (sdres != null) {
                if (sdres.isFromShared()) {
                    return "[Shared] " + sdres.getPageName();
                } else if (sdres.isFromProject()) {
                    return "[Project] " + sdres.getPageName();
                }
            }

            // Try resolving as SAP object (Project scope first, then Shared)
            var sapdref = ResolvedSapObject.PageRef.parse(ref);
            var sapdres = repo.resolveSapObject(sapdref, objectName);

            if (sapdres != null) {
                if (sapdres.isFromShared()) {
                    return "[Shared] " + sapdres.getPageName();
                } else if (sapdres.isFromProject()) {
                    return "[Project] " + sapdres.getPageName();
                }
            }

            // Unable to resolve - leave reference as-is (may be invalid or dynamic)
            return null;
        } catch (Exception e) {
            // If resolution fails, return null to leave reference unchanged
            return null;
        }
    }

    public void save() {
        if (!isSaved()) {
            createIfNotExists();
            File file = new File(getLocation());
            TestCaseFormat fmt = TestCaseFormat.fromFile(file);
            if (fmt == null) {
                fmt = projectDefaultFormat();
            }
            TestCaseStore store = TestCaseStoreFactory.testCaseStore(fmt);
            try {
                removeEmptySteps();
                autoNumber();
                List<List<String>> rows = new ArrayList<>(testSteps.size());
                for (TestStep testStep : testSteps) {
                    rows.add(new ArrayList<>(testStep.stepDetails));
                }
                store.save(
                    file,
                    name,
                    scenario == null ? null : scenario.getName(),
                    reusable != null,
                    collectTags(),
                    rows
                );
                setSaved(true);
                // After successful save, register any shared reusable references from this test case
                try {
                    updateSharedReusableProjectsItems();
                } catch (Exception ex) {
                    Logger
                        .getLogger(TestCase.class.getName())
                        .log(Level.FINE, "Failed to update shared reusable projects.items", ex);
                }
            } catch (Exception ex) {
                Logger
                    .getLogger(TestCase.class.getName())
                    .log(Level.SEVERE, "Error while saving", ex);
            }
        }
    }

    /**
     * Scans this test case for Execute/reusable steps that reference shared reusables
     * and records the current project name and path into SharedReusableComponents/projects.items
     * as a JSON array so the IDE can later look up which projects reference shared reusables.
     */
    private void updateSharedReusableProjectsItems() {
        // Check whether this test case actually references any SHARED reusables
        java.util.Set<String> sharedReusableNames = new java.util.HashSet<>();
        for (TestStep step : testSteps) {
            if (!step.isReusableStep()) continue;
            ReusableRef ref;
            try {
                ref = step.getEffectiveReusableRef();
            } catch (Exception e) {
                continue;
            }
            if (ref != null && ref.getScope() == ReusableRef.Scope.SHARED) {
                sharedReusableNames.add(ref.getScenarioName());
            }
        }

        if (!sharedReusableNames.isEmpty()) {
            updateSharedReusableProjectsItems(getProject(), sharedReusableNames);
        }
    }

    /**
     * Static helper method to update projects.items with project information.
     * Called when a test case or test data is saved with shared reusable references.
     */
    public static void updateSharedReusableProjectsItems(
        Project project,
        java.util.Set<String> sharedReusableNames
    ) {
        try {
            if (
                project == null ||
                project.getName() == null ||
                sharedReusableNames == null ||
                sharedReusableNames.isEmpty()
            ) return;
            String projectName = project.getName();
            String projectPath = project.getLocation();

            // Ensure SharedReusableComponents directory exists
            File sharedRoot = new File(Project.getSharedReusableComponentsPath());
            if (!sharedRoot.exists()) sharedRoot.mkdirs();
            File projectsFile = new File(sharedRoot, "projects.items");

            // Read existing entries as JSON array
            java.util.List<java.util.Map<String, String>> projects = new java.util.ArrayList<>();
            if (projectsFile.exists()) {
                try {
                    String content = FileScanner.readFile(projectsFile);
                    if (content != null && !content.isEmpty()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        projects =
                            mapper.readValue(
                                content,
                                mapper
                                    .getTypeFactory()
                                    .constructCollectionType(
                                        java.util.List.class,
                                        java.util.Map.class
                                    )
                            );
                    }
                } catch (Exception ex) {
                    // best effort - ignore read problems, start fresh
                    projects = new java.util.ArrayList<>();
                }
            }

            // Build entry map for other projects only (exclude the current project being saved)
            java.util.Map<String, String> entry = new java.util.LinkedHashMap<>();
            entry.put("name", projectName);
            entry.put("path", projectPath);

            // Check if entry already exists (by name and path)
            boolean found = false;
            for (java.util.Map<String, String> proj : projects) {
                if (projectName.equals(proj.get("name")) && projectPath.equals(proj.get("path"))) {
                    found = true;
                    break;
                }
            }

            // Add current project if not already in the list
            if (!found) {
                projects.add(entry);
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String jsonOutput = mapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(projects);

                    // Atomic write: write to temp and rename
                    File tmp = new File(projectsFile.getPath() + ".tmp");
                    FileScanner.writeFile(tmp, jsonOutput);
                    if (tmp.exists()) {
                        if (!tmp.renameTo(projectsFile)) {
                            // Fallback if rename fails
                            FileScanner.writeFile(projectsFile, jsonOutput);
                        }
                    } else {
                        // If tmp wasn't created, attempt direct write
                        FileScanner.writeFile(projectsFile, jsonOutput);
                    }
                } catch (Exception ex) {
                    Logger
                        .getLogger(TestCase.class.getName())
                        .log(Level.SEVERE, "Error serializing projects.items JSON", ex);
                }
            }
        } catch (Exception ex) {
            Logger
                .getLogger(TestCase.class.getName())
                .log(Level.SEVERE, "Error updating shared reusable projects.items", ex);
        }
    }

    private void createIfNotExists() {
        File file = new File(getLocation()).getParentFile();
        file.mkdirs();
    }

    /**
     * Re-writes this test case's YAML file to capture metadata-only changes
     * (such as tags added/removed via the tag editor) without requiring the
     * user to edit a step first. Loads steps from disk if not already loaded,
     * then clears them again so the in-memory model is unaffected.
     */
    public void saveMetadata() {
        boolean wasEmpty = testSteps.isEmpty();
        if (wasEmpty) {
            loadSteps();
        }
        setSaved(false);
        save();
        if (wasEmpty) {
            testSteps.clear();
        }
    }

    /**
     * Mirrors tags found in the on-disk YAML into the project's
     * {@code DataItem} so the YAML remains the source of truth when present.
     */
    private void syncTagsFromStore(TestCaseStore store, File file) {
        if (!(store instanceof YamlTestCaseStore) || scenario == null) {
            return;
        }
        try {
            if (
                getProject() == null ||
                getProject().getInfo() == null ||
                getProject().getInfo().getData() == null
            ) {
                return;
            }
            List<String> diskTags = ((YamlTestCaseStore) store).loadTags(file);
            if (diskTags == null || diskTags.isEmpty()) {
                return;
            }
            com.ing.datalib.model.DataItem di = getProject()
                .getInfo()
                .getData()
                .findOrCreate(name, scenario.getName());
            for (String t : diskTags) {
                if (t == null || t.isEmpty()) continue;
                boolean exists = di
                    .getTags()
                    .stream()
                    .anyMatch(x -> x != null && t.equals(x.getValue()));
                if (!exists) {
                    di.getTags().add(com.ing.datalib.model.Tag.create(t));
                }
            }
        } catch (Exception ignored) {
            // Tag sync is best-effort and must not break step loading.
        }
    }

    /**
     * Returns the tag values associated with this test case in {@code ProjectInfo.data},
     * or an empty list when no tags / no project context are available.
     */
    private List<String> collectTags() {
        try {
            if (
                scenario == null ||
                getProject() == null ||
                getProject().getInfo() == null ||
                getProject().getInfo().getData() == null
            ) {
                return Collections.emptyList();
            }
            return getProject()
                .getInfo()
                .getData()
                .find(name, scenario.getName())
                .map(
                    di ->
                        di.getTags() == null
                            ? Collections.<String>emptyList()
                            : di
                                .getTags()
                                .stream()
                                .filter(Objects::nonNull)
                                .map(t -> t.getValue())
                                .filter(v -> v != null && !v.isEmpty())
                                .collect(java.util.stream.Collectors.toList())
                )
                .orElse(Collections.emptyList());
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private void removeEmptySteps() {
        for (int i = testSteps.size() - 1; i >= 0; i--) {
            if (testSteps.get(i).isEmpty()) {
                testSteps.remove(i);
                fireTableRowsDeleted(i, i);
            }
        }
    }

    private void autoNumber() {
        for (int i = 0; i < testSteps.size(); i++) {
            String val = testSteps.get(i).getTag().replaceAll("[0-9]+", "").trim();
            testSteps.get(i).setTag(val + (i + 1));
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return testSteps.size();
    }

    @Override
    public int getColumnCount() {
        return TestStep.HEADERS.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return TestStep.HEADERS.values()[columnIndex].name();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (testSteps.size() > rowIndex) {
            return testSteps.get(rowIndex).getValueAt(columnIndex);
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!getValueAt(rowIndex, columnIndex).equals(aValue)) {
            if (testSteps.size() <= rowIndex) {
                testSteps.add(new TestStep(this));
                setValueAt(aValue, rowIndex, columnIndex);
            }
            super.setValueAt(aValue, rowIndex, columnIndex);
            testSteps.get(rowIndex).putValueAt(columnIndex, Objects.toString(aValue, ""));
        }
    }

    public String printString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("\t\t")
            .append("TestCase - ")
            .append(name)
            .append("\n")
            .append("\t\t")
            .append("TestSteps - ")
            .append(testSteps.size())
            .append("\n");
        return builder.toString();
    }

    @Override
    public String toString() {
        return name;
    }

    public Boolean isSaved() {
        return saved;
    }

    public void setSaved(Boolean saved) {
        this.saved = saved;
        if (saveListener != null) {
            saveListener.onSave(saved);
        }
    }

    public void setSaveListener(SaveListener saveListener) {
        this.saveListener = saveListener;
    }

    @Override
    public void fireTableChanged(TableModelEvent tme) {
        setSaved(false);
        super.fireTableChanged(tme);
    }

    @Override
    public void fireTableCellUpdated(int i, int i1) {
        setSaved(false);
        super.fireTableCellUpdated(i, i1);
    }

    @Override
    public void fireTableRowsDeleted(int i, int i1) {
        setSaved(false);
        super.fireTableRowsDeleted(i, i1);
    }

    @Override
    public void fireTableRowsUpdated(int i, int i1) {
        setSaved(false);
        super.fireTableRowsUpdated(i, i1);
    }

    @Override
    public void fireTableRowsInserted(int i, int i1) {
        setSaved(false);
        super.fireTableRowsInserted(i, i1);
    }

    @Override
    public void fireTableStructureChanged() {
        setSaved(false);
        super.fireTableStructureChanged();
    }

    @Override
    public void fireTableDataChanged() {
        setSaved(false);
        super.fireTableDataChanged();
    }

    public boolean isReusable() {
        return getReusable() != null || (scenario != null && scenario.isReusableScenario());
    }

    public Reusable getReusable() {
        return reusable;
    }

    public void setReusable(Reusable reusable) {
        this.reusable = reusable;
    }

    public void setParentTestCase(TestCase parentTestCase) {
        this.parentTestCase = parentTestCase;
    }

    public TestCase getParentTestCase() {
        return parentTestCase;
    }

    public void setExitParamLoop(boolean exitParamLoop) {
        this.exitParamLoop = exitParamLoop;
    }

    public boolean getExitParamLoopFlag() {
        return exitParamLoop;
    }

    public Integer getDynamicMaxIter() {
        return dynamicMaxInt;
    }

    public void setDynamicMaxIter(Integer dynamicMaxInt) {
        this.dynamicMaxInt = dynamicMaxInt;
    }

    public String getKey() {
        return getScenario().getName() + "#" + getName();
    }

    @Override
    public Boolean delete() {
        if (FileUtils.deleteFile(getLocation())) {
            getScenario().removeTestCase(this);
            return true;
        }
        return false;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(oldScenarioName)) {
                    testStep.asReusableStep(newScenarioName, values[1]);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestCase(
        String scenarioName,
        String oldTestCaseName,
        String newTestCaseName
    ) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(scenarioName) && values[1].equals(oldTestCaseName)) {
                    testStep.asReusableStep(scenarioName, newTestCaseName);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestCaseScenario(
        String testCaseName,
        String oldScenarioName,
        String newScenarioName
    ) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(oldScenarioName) && values[1].equals(testCaseName)) {
                    testStep.asReusableStep(newScenarioName, testCaseName);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    /**
     * Refactors Execute reusable references across scope transitions and returns whether any step changed.
     * Supports scoped tokens ([Project]/[Shared]) and legacy unscoped tokens.
     */
    public boolean refactorReusableReferenceAcrossScope(
        String oldScenarioName,
        String oldTestCaseName,
        Scenario.Source oldSource,
        String newScenarioName,
        String newTestCaseName,
        Scenario.Source newSource
    ) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();

        boolean changesMade = false;
        for (TestStep testStep : testSteps) {
            if (!testStep.isReusableStep()) {
                continue;
            }

            String action = Objects.toString(testStep.getAction(), "");
            if (action.isEmpty()) {
                continue;
            }

            ReusableRef parsed;
            try {
                parsed = testStep.getEffectiveReusableRef();
            } catch (IllegalArgumentException ex) {
                continue;
            }
            if (parsed == null) {
                continue;
            }

            if (
                !parsed.getScenarioName().equalsIgnoreCase(oldScenarioName) ||
                !parsed.getTestCaseName().equalsIgnoreCase(oldTestCaseName) ||
                !matchesReusableSource(parsed.getScope(), oldSource)
            ) {
                continue;
            }

            ReusableRef.Scope targetScope = toReusableRefScope(newSource);
            String updatedAction = new ReusableRef(
                ReusableRef.Scope.UNSCOPED,
                newScenarioName,
                newTestCaseName
            )
            .format();
            if (!updatedAction.equals(action)) {
                testStep.setAction(updatedAction);
                changesMade = true;
            }
            String newReferenceScope = toReferenceScopeToken(targetScope);
            if (!Objects.equals(newReferenceScope, testStep.getReference())) {
                testStep.setReference(newReferenceScope);
                changesMade = true;
            }
        }

        if (changesMade) {
            save();
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return changesMade;
    }

    private boolean matchesReusableSource(ReusableRef.Scope scope, Scenario.Source source) {
        if (scope == ReusableRef.Scope.PROJECT) {
            return source == Scenario.Source.REUSABLE_COMPONENTS;
        }
        if (scope == ReusableRef.Scope.SHARED) {
            return source == Scenario.Source.SHARED_REUSABLE_COMPONENTS;
        }
        return (
            scope == ReusableRef.Scope.UNSCOPED &&
            source != Scenario.Source.SHARED_REUSABLE_COMPONENTS
        );
    }

    private ReusableRef.Scope toReusableRefScope(Scenario.Source source) {
        if (source == Scenario.Source.REUSABLE_COMPONENTS) {
            return ReusableRef.Scope.PROJECT;
        }
        if (source == Scenario.Source.SHARED_REUSABLE_COMPONENTS) {
            return ReusableRef.Scope.SHARED;
        }
        return ReusableRef.Scope.UNSCOPED;
    }

    private String toReferenceScopeToken(ReusableRef.Scope scope) {
        if (scope == ReusableRef.Scope.PROJECT) {
            return "[Project]";
        }
        if (scope == ReusableRef.Scope.SHARED) {
            return "[Shared]";
        }
        return "";
    }

    public void refactorTestData(String oldTDName, String newTDName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getTestDataFromInput();
            if (values != null) {
                if (values[0].equals(oldTDName)) {
                    testStep.setInput(newTDName + ":" + values[1]);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestDataColumn(
        String testDataName,
        String oldColumnName,
        String newColumnName
    ) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getTestDataFromInput();
            if (values != null) {
                if (values[0].equals(testDataName) && values[1].equals(oldColumnName)) {
                    testStep.setInput(testDataName + ":" + newColumnName);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorObjectName(String pageName, String oldName, String newName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String ref = Objects.toString(testStep.getReference(), "");
            String obj = Objects.toString(testStep.getObject(), "");
            String normalizedRef = normalizePageRef(ref);

            if (normalizedRef.equals(pageName) && obj.equals(oldName)) {
                testStep.setObject(newName);
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorObjectName(
        String oldpageName,
        String oldObjName,
        String newPageName,
        String newObjName
    ) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        boolean changesMade = false;

        for (TestStep testStep : testSteps) {
            String ref = normalizePageRef(Objects.toString(testStep.getReference(), ""));
            String obj = Objects.toString(testStep.getObject(), "");
            if (ref.equals(oldpageName) && obj.equals(oldObjName)) {
                testStep.setObject(newObjName);
                testStep.setReference(newPageName);
                changesMade = true;
            }
        }

        if (changesMade) {
            save();
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
    }

    /**
     * Renames an object reference on the given page within this test case, restricted to the specified OR scope.
     * A step matches when its reference has the expected scope prefix and its normalized page name equals {@code pageName}.
     *
     * @param scope    OR scope to match (e.g., {@code PROJECT} or {@code SHARED})
     * @param pageName page (screen) name (without scope prefix) to match
     * @param oldName  existing object name to replace
     * @param newName  new object name to apply
     */
    public void refactorObjectName(ORScope scope, String pageName, String oldName, String newName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        boolean changesMade = false;
        for (TestStep testStep : testSteps) {
            String refRaw = Objects.toString(testStep.getReference(), "");
            String obj = Objects.toString(testStep.getObject(), "");
            boolean scopedMatch =
                matchesScope(refRaw, scope) && normalizePageRef(refRaw).equals(pageName);
            if (scopedMatch && obj.equals(oldName)) {
                testStep.setObject(newName);
                changesMade = true;
            }
        }
        if (changesMade) {
            save();
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
    }

    /**
     * Checks whether a reference string is explicitly scoped for the given OR scope.
     * Returns {@code true} only when {@code ref} starts with the expected scope prefix
     * (e.g., {@code "[Project] "} or {@code "[Shared] "}); otherwise returns {@code false}.
     *
     * @param ref   raw reference value (may be {@code null})
     * @param scope scope to match against
     * @return {@code true} if {@code ref} begins with the prefix for {@code scope}; {@code false} otherwise
     */
    private boolean matchesScope(String ref, ORScope scope) {
        if (ref == null) return false;
        ref = ref.trim();
        if (scope == ORScope.PROJECT) return ref.startsWith("[Project] ");
        if (scope == ORScope.SHARED) return ref.startsWith("[Shared] ");
        return false;
    }

    /**
     * Normalizes a page reference by removing known scope prefixes.
     * Trims the input and strips {@code "[Project] "} or {@code "[Shared] "} when present;
     * otherwise returns the trimmed reference. Returns an empty string when {@code ref} is {@code null}.
     *
     * @param ref raw reference value (may be {@code null})
     * @return normalized page name without scope prefix (never {@code null})
     */
    private String normalizePageRef(String ref) {
        if (ref == null) return "";
        ref = ref.trim();
        if (ref.startsWith("[Project] ")) return ref.substring("[Project] ".length()).trim();
        if (ref.startsWith("[Shared] ")) return ref.substring("[Shared] ".length()).trim();
        return ref;
    }

    public void refactorPageName(String oldPageName, String newPageName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            if (testStep.getReference().equals(oldPageName)) {
                testStep.setReference(newPageName);
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public TestCase getImpactedObjectTestCases(String pageName, String objectName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        Boolean impacted = false;
        for (TestStep testStep : testSteps) {
            if (
                testStep.getReference().equals(pageName) && testStep.getObject().equals(objectName)
            ) {
                impacted = true;
                break;
            }
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return impacted ? this : null;
    }

    public TestCase getImpactedTestCaseTestCases(String scenarioName, String testCaseName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        Boolean impacted = false;
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(scenarioName) && values[1].equals(testCaseName)) {
                    impacted = true;
                    break;
                }
            }
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return impacted ? this : null;
    }

    public TestCase getImpactedTestDataTestCases(String testDataName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        Boolean impacted = false;
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getTestDataFromInput();
            if (values != null) {
                if (values[0].equals(testDataName)) {
                    impacted = true;
                    break;
                }
            }
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return impacted ? this : null;
    }

    @Override
    public Boolean rename(String newName) {
        TestCase existing = getScenario().getTestCaseByName(newName);
        if (existing == null || existing == this) {
            if (FileUtils.renameFile(getLocation(), newName + getFormat().extension())) {
                getProject().refactorTestCase(getScenario().getName(), name, newName);
                name = newName;
                return true;
            }
        }
        return false;
    }

    public Boolean renameReusable(String newName) {
        TestCase existing = getScenario().getTestCaseByName(newName);
        if (existing == null || existing == this) {
            if (FileUtils.renameFile(getLocation(), newName + getFormat().extension())) {
                getProject().refactorTestCase(getScenario().getName(), name, newName);
                name = newName;
                return true;
            }
        }
        return false;
    }

    /**
     * Renames this shared reusable test case.
     * @param newName new test case name
     * @return true if successful, false if a test case with the new name already exists
     */
    public Boolean renameSharedReusable(String newName) {
        TestCase existing = getScenario().getTestCaseByName(newName);
        if (existing == null || existing == this) {
            if (FileUtils.renameFile(getLocation(), newName + getFormat().extension())) {
                getProject().refactorTestCase(getScenario().getName(), name, newName);
                name = newName;
                return true;
            }
        }
        return false;
    }
}
