
package com.ing.datalib.component;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.utils.FileUtils;
import static com.ing.datalib.component.utils.FileUtils.DIR_FILTER;
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
import com.ing.datalib.util.data.FileScanner;


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

    private List<Scenario> scenarios = new ArrayList<>();

    private final List<Scenario> reusableScenarios = new ArrayList<>();

    private final List<Release> releases = new ArrayList<>();

    private String testdataType;

    private EnvTestData testData;

    private String location;

    private String name;

    private ProjectSettings projectSettings;

    private ObjectRepository objectRepository;

    private ProjectInfo projectInfo;

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
     * Performs migration of legacy reusable component XML if present.
     */
    private void loadProject() {
        loadScenariosFromTestPlan();
        loadTestSets();
        migrateReusableComponentXmlIfPresent();
        loadScenariosFromTestPlan();
        loadScenariosFromReusableComponents();
        loadTestDatas();
        projectSettings = new ProjectSettings(this);
        objectRepository = new ObjectRepository(this);
        projectInfo = loadProjectInfo(getProjectFile());
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
        return Stream.concat(scenarios.stream(), reusableScenarios.stream())
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
     * Finds a reusable scenario by name.
     * @param name scenario name to search for (case-insensitive)
     * @return the reusable scenario if found, null otherwise
     */
    public Scenario getReusableScenarioByName(String name) {
        for (Scenario scenario : reusableScenarios) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                return scenario;
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
     * Returns the absolute path to a scenario directory based on its source.
     * @param source the scenario source (TEST_PLAN or REUSABLE_COMPONENTS)
     * @param scenarioName name of the scenario
     * @return absolute path to the scenario directory
     */
    public String getScenarioPath(Scenario.Source source, String scenarioName) {
        String base = source == Scenario.Source.REUSABLE_COMPONENTS
                ? getReusableComponentsPath()
                : getTestPlanPath();
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
            List<String> scns = sp.getAllScenarios().stream().map(Scenario::getName).collect(toList());
            project.findScenarios().filter(scn -> !scns.contains(scn.getName()))
                    .collect(toList())
                    .forEach(scn -> {
                        project.getMeta().remove(scn);
                        project.getData().removeAll(project.getData().stream().filter(Objects::nonNull)
                                .filter(di -> di.hasScenario(scn.getName()))
                                .collect(toList()));
                    });
            project.getData().removeAll(project.getData().stream().filter(Objects::nonNull)
                    .filter(di -> !sp.hasTestCaseInAnyScenario(di.getScenario(), di.getName()))
                    .collect(toList()));
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
        return hasTestCaseInScenario(testCaseName, getScenarioByName(scenarioName))
                || hasTestCaseInScenario(testCaseName, getReusableScenarioByName(scenarioName));
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
                for (String scenario : testPlan.list(DIR_FILTER)) {
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
                for (String scenario : reusableRoot.list(DIR_FILTER)) {
                    reusableScenarios.add(new Scenario(this, scenario, Scenario.Source.REUSABLE_COMPONENTS));
                }
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
                        LOGGER.log(Level.WARNING, "Failed to migrate test case: " + e.getMessage(), e);
                    }
                }
            }
        }
        File backup = new File(xmlFile.getParentFile(), "ReusableComponent.xml.bak");
        if (!backup.exists()) {
            xmlFile.renameTo(backup);
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
    private void moveTestCaseFile(TestCase testCase, Scenario.Source targetSource) throws TestCaseConversionException {
        if (testCase == null || testCase.getScenario() == null) {
            throw new TestCaseConversionException("Invalid test case or scenario");
        }
        
        String scenarioName = testCase.getScenario().getName();
        String testCaseName = testCase.getName();
        String targetName = targetSource == Scenario.Source.REUSABLE_COMPONENTS ? "Reusable Components" : "Test Plan";
        
        File source = new File(testCase.getLocation());
        if (!source.exists()) {
            throw new TestCaseConversionException("Test Case file does not exist");
        }
        
        File targetDir = new File(getScenarioPath(targetSource, scenarioName));
        targetDir.mkdirs();
        File target = new File(targetDir, testCaseName + ".csv");
        
        if (target.exists()) {
            throw new TestCaseConversionException("Test case '" + testCaseName + "' already exists in scenario '" + scenarioName + "' in " + targetName);
        }
        
        try {
            Files.move(source.toPath(), target.toPath());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed moving test case file", ex);
            throw new TestCaseConversionException("Failed to move test case: " + ex.getMessage(), ex);
        }
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
        if (releases.remove(release)) {
        }
    }

    /**
     * Adds a new scenario to the Test Plan.
     * @param scenarioName name of the scenario to add
     * @return the created scenario, or null if a scenario with the same name already exists
     */
    public Scenario addScenario(String scenarioName) {
        if (getScenarioByName(scenarioName) == null) {
            Scenario scn = new Scenario(this, scenarioName, Scenario.Source.TEST_PLAN);
            scenarios.add(scn);
            return scn;
        }
        return null;
    }

    /**
     * Adds a new scenario to Reusable Components.
     * @param scenarioName name of the scenario to add
     * @return the created scenario, or null if a scenario with the same name already exists
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
     * Removes a scenario from the project (either Test Plan or Reusable Components).
     * @param scenario scenario to remove
     */
    public void removeScenario(Scenario scenario) {
        if (scenario == null) {
            return;
        }
        if (scenario.isReusableScenario()) {
            reusableScenarios.remove(scenario);
        } else {
            scenarios.remove(scenario);
        }
    }

    /**
     * Returns the environment test data.
     * @return environment test data
     */
    public EnvTestData getTestData() {
        return testData;
    }

    /**
     * Loads test data from disk.
     */
    private void loadTestDatas() {
        testData = new EnvTestData(this);
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
        LOGGER.log(Level.INFO, "Refactoring started for Scenario [{0}] to [{1}]", new Object[]{oldScenarioName, newScenarioName});
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorScenario(oldScenarioName, newScenarioName);
        }
        for (Release release : releases) {
            release.refactorScenario(oldScenarioName, newScenarioName);
        }
        testData.refactorScenario(oldScenarioName, newScenarioName);
        LOGGER.log(Level.INFO, "Refactoring done for Scenario [{0}] to [{1}]", new Object[]{oldScenarioName, newScenarioName});
        getInfo().findScenario(oldScenarioName).ifPresent(scn -> scn.setName(newScenarioName));
        getInfo().getData().stream().filter(Objects::nonNull).filter(di -> di.hasScenario(oldScenarioName)).forEach(di -> {
            di.getAttributes().find(Meta.Attributes.scenario.name())
                    .ifPresent(scn -> scn.setName(newScenarioName));
        });
    }

    /**
     * Refactors (renames) a test case across the entire project including all releases, test sets, and test data.
     * @param scenarioName scenario containing the test case
     * @param oldTestCaseName old test case name
     * @param newTestCaseName new test case name
     */
    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        LOGGER.log(Level.INFO, "Refactoring started for TestCase [{0}] to [{1}]", new Object[]{oldTestCaseName, newTestCaseName});
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
        for (Release release : releases) {
            release.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
        testData.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        LOGGER.log(Level.INFO, "Refactoring done for TestCase [{0}] to [{1}]", new Object[]{oldTestCaseName, newTestCaseName});
        getInfo().getData().stream().filter(Objects::nonNull)
                .filter(di -> di.hasScenario(scenarioName) && di.getName().equals(oldTestCaseName))
                .forEach(di -> di.setName(newTestCaseName));
    }

    /**
     * Refactors (moves) a test case from one scenario to another across the entire project.
     * @param testCaseName test case name
     * @param oldScenarioName old scenario name
     * @param newScenarioName new scenario name
     */
    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        LOGGER.log(Level.INFO, "Refactoring started TestCase [{0}] from Scenario [{1}] to [{2}]", new Object[]{testCaseName, oldScenarioName, newScenarioName});
        for (Scenario scenario : getAllScenarios()) {
            scenario.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
        for (Release release : releases) {
            release.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
        testData.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        LOGGER.log(Level.INFO, "Refactoring done TestCase [{0}] from Scenario [{1}] to [{2}]", new Object[]{testCaseName, oldScenarioName, newScenarioName});
        getInfo().getData().stream().filter(Objects::nonNull)
                .filter(di -> di.hasScenario(oldScenarioName) && di.getName().equals(testCaseName))
                .forEach(di -> {
                    di.getAttributes().find(Meta.Attributes.scenario.name())
                            .ifPresent(scn -> scn.setName(newScenarioName));
                });
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
    public void refactorObjectName(String oldpageName, String oldObjName, String newPageName, String newObjName) {
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
    public void refactorMobileObjectName(MobileOR.ORScope scope, String pageName, String oldName, String newName) {
        for (Scenario scenario : getAllScenarios()) {
            WebOR.ORScope webScope =
            (scope == MobileOR.ORScope.SHARED)
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;
            scenario.refactorObjectName(
                webScope,
                pageName,
                oldName,
                newName
            );
        }
    }

     /**
     * Refactors Structured Data OR object references in TestSteps.
     * Mobile scope is mapped to Web scope because Scenarios/TestSteps
     * are tool-agnostic and only care about PROJECT vs SHARED.
     */
    public void refactorStructuredDataObjectName(StructuredDataOR.ORScope scope, String pageName, String oldName, String newName) {
        for (Scenario scenario : getAllScenarios()) {
            WebOR.ORScope webScope =
            (scope == StructuredDataOR.ORScope.SHARED)
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;
            scenario.refactorObjectName(
                webScope,
                pageName,
                oldName,
                newName
            );
        }
    }

    /**
     * Refactors SAP OR object references in TestSteps.
     * SAP scope is mapped to Web scope because Scenarios/TestSteps
     * are tool-agnostic and only care about PROJECT vs SHARED.
     */
    public void refactorSapObjectName(SapOR.ORScope scope, String pageName, String oldName, String newName) {
        for (Scenario scenario : getAllScenarios()) {
            WebOR.ORScope webScope =
            (scope == SapOR.ORScope.SHARED)
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;
            scenario.refactorObjectName(
                webScope,
                pageName,
                oldName,
                newName
            );
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
        String oldScoped = scope == ORScope.SHARED ? "[Shared] " + oldPageName : "[Project] " + oldPageName;
        String newScoped = scope == ORScope.SHARED ? "[Shared] " + newPageName : "[Project] " + newPageName;
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
        for (Scenario scenario : scenarios) {
            scenario.refactorTestData(oldTDName, newTDName);
        }
    }

    /**
     * Refactors (renames) a test data column reference across all scenarios in the project.
     * @param testDataName test data name
     * @param oldColumnName old column name
     * @param newColumnName new column name
     */
    public void refactorTestDataColumn(String testDataName, String oldColumnName, String newColumnName) {
        for (Scenario scenario : scenarios) {
            scenario.refactorTestDataColumn(testDataName, oldColumnName, newColumnName);
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
            scopedPageName = (scope == ORScope.SHARED)
                    ? "[Shared] " + pageName
                    : "[Project] " + pageName;
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
        sortedList.sort((tc1, tc2) -> {
            // First compare by source type (TEST_PLAN comes before REUSABLE_COMPONENTS)
            int sourceCompare = tc1.getScenario().getSource().compareTo(tc2.getScenario().getSource());
            if (sourceCompare != 0) {
                return sourceCompare;
            }
            // Then compare by scenario name
            int scenarioCompare = tc1.getScenario().getName().compareToIgnoreCase(tc2.getScenario().getName());
            if (scenarioCompare != 0) {
                return scenarioCompare;
            }
            // Finally compare by test case name
            return tc1.getName().compareToIgnoreCase(tc2.getName());
        });
        return sortedList;
    }

    /**
     * Returns test cases that reference the specified test case.
     * @param scenarioName scenario name
     * @param testCaseName test case name
     * @return list of impacted test cases
     */
    public List<TestCase> getImpactedTestCaseTestCases(String scenarioName, String testCaseName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(scenario.getImpactedTestCaseTestCases(scenarioName, testCaseName));
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
                DataItem data = create(tc.getKey(), tc.getName(), tc.isReusable() ? Meta.Attributes.reusable : Meta.Attributes.testcase);
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