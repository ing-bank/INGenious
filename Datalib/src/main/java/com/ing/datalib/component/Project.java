
package com.ing.datalib.component;

import com.ing.datalib.component.utils.FileUtils;
import static com.ing.datalib.component.utils.FileUtils.DIR_FILTER;
import com.ing.datalib.model.DataItem;
import com.ing.datalib.model.Meta;
import com.ing.datalib.model.ProjectInfo;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.util.data.FileScanner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 *
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

    public Project(String name, String projectLocation, String testdataType) {
        this.location = projectLocation + File.separator + name;
        this.testdataType = testdataType;
        this.name = name;
        load();
    }

    public Project(String projectLocation, String testdataType) {
        this.name = new File(projectLocation).getName();
        this.location = projectLocation;
        this.testdataType = testdataType;
        load();
    }

    public Project(String projectLocation) {
        this(projectLocation, "csv");
    }

    private void load() {
        loadProject();
    }

    public Project createProject() {
        addScenario("NewScenario").addTestCase("NewTestCase");
        addRelease("NewRelease").addTestSet("NewTestSet");
        loadTestDatas();
        projectInfo = loadProjectInfo(getProjectFile());
        return this;
    }

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

    public ProjectInfo getInfo() {
        return projectInfo;
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public List<Scenario> getReusableScenarios() {
        return reusableScenarios;
    }

    public List<Scenario> getAllScenarios() {
        return Stream.concat(scenarios.stream(), reusableScenarios.stream())
                .collect(toList());
    }

    public List<Release> getReleases() {
        return releases;
    }

    public Scenario getScenarioByName(String name) {
        for (Scenario scenario : scenarios) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                return scenario;
            }
        }
        return null;
    }

    public Scenario getReusableScenarioByName(String name) {
        for (Scenario scenario : reusableScenarios) {
            if (scenario.getName().equalsIgnoreCase(name)) {
                return scenario;
            }
        }
        return null;
    }

    public int getIndexOfScenarioByName(String name) {
        for (int i = 0; i < scenarios.size(); i++) {
            if (scenarios.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public Release getReleaseByName(String name) {
        for (Release release : releases) {
            if (release.getName().equalsIgnoreCase(name)) {
                return release;
            }
        }
        return null;
    }

    public int getIndexOfReleaseByName(String name) {
        for (int i = 0; i < releases.size(); i++) {
            if (releases.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }

    public String getLocation() {
        return location;
    }

    public String getTestPlanPath() {
        return getLocation() + File.separator + TEST_PLAN_DIR;
    }

    public String getReusableComponentsPath() {
        return getLocation() + File.separator + REUSABLE_COMPONENTS_DIR;
    }

    public String getScenarioPath(Scenario.Source source, String scenarioName) {
        String base = source == Scenario.Source.REUSABLE_COMPONENTS
                ? getReusableComponentsPath()
                : getTestPlanPath();
        return base + File.separator + scenarioName;
    }

    public void setLocation(String location) {
        this.location = location;
    }

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

    public boolean hasTestCaseInAnyScenario(String scenarioName, String testCaseName) {
        return hasTestCaseInScenario(testCaseName, getScenarioByName(scenarioName))
                || hasTestCaseInScenario(testCaseName, getReusableScenarioByName(scenarioName));
    }

    private boolean hasTestCaseInScenario(String tc, Scenario scnobj) {
        return scnobj != null && scnobj.getTestCaseByName(tc) != null;
    }

    private File getProjectFile() {
        return new File(getLocation(), ".project");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
            getObjectRepository().getMobileOR().setName(newName);
            return true;
        }
        return false;
    }

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
                    if (moveTestCaseFile(testCase, Scenario.Source.REUSABLE_COMPONENTS)) {
                        moved++;
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

    public boolean moveTestCaseToReusable(TestCase testCase) {
        return moveTestCaseFile(testCase, Scenario.Source.REUSABLE_COMPONENTS);
    }

    public boolean moveTestCaseToTestPlan(TestCase testCase) {
        return moveTestCaseFile(testCase, Scenario.Source.TEST_PLAN);
    }

    private boolean moveTestCaseFile(TestCase testCase, Scenario.Source targetSource) {
        if (testCase == null || testCase.getScenario() == null) {
            return false;
        }
        File source = new File(testCase.getLocation());
        if (!source.exists()) {
            return false;
        }
        File targetDir = new File(getScenarioPath(targetSource, testCase.getScenario().getName()));
        targetDir.mkdirs();
        File target = new File(targetDir, testCase.getName() + ".csv");
        if (target.exists()) {
            return false;
        }
        try {
            Files.move(source.toPath(), target.toPath());
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed moving test case file", ex);
            return false;
        }
    }

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

    public Scenario addScenario(String scenarioName) {
        if (getScenarioByName(scenarioName) == null) {
            Scenario scn = new Scenario(this, scenarioName, Scenario.Source.TEST_PLAN);
            scenarios.add(scn);
            return scn;
        }
        return null;
    }

    public Scenario addReusableScenario(String scenarioName) {
        if (getReusableScenarioByName(scenarioName) == null) {
            Scenario scn = new Scenario(this, scenarioName, Scenario.Source.REUSABLE_COMPONENTS);
            reusableScenarios.add(scn);
            return scn;
        }
        return null;
    }

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

    public EnvTestData getTestData() {
        return testData;
    }

    private void loadTestDatas() {
        testData = new EnvTestData(this);
    }

    public String getTestdataType() {
        if (testdataType == null) {
            testdataType = "csv";
        }
        return testdataType;
    }

    public void setTestdataType(String testdataType) {
        this.testdataType = testdataType;
    }

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

    public void reload() {
        loadProject();
    }

    public TableModel getTableModelFor(Object selectedNode) {
        if (selectedNode instanceof DataModel) {
            DataModel scenario = (DataModel) selectedNode;
            scenario.loadTableModel();
            return scenario;
        }
        return new DefaultTableModel();
    }

    @Override
    public String toString() {
        return name;
    }

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

    public void refactorObjectName(String pageName, String oldName, String newName) {
        for (Scenario scenario : scenarios) {
            scenario.refactorObjectName(pageName, oldName, newName);
        }
    }

    public void refactorObjectName(String oldpageName, String oldObjName, String newPageName, String newObjName) {
        for (Scenario scenario : scenarios) {
            scenario.refactorObjectName(oldpageName, oldObjName, newPageName, newObjName);
        }
    }

    public void refactorPageName(String oldPageName, String newPageName) {
        for (Scenario scenario : scenarios) {
            scenario.refactorPageName(oldPageName, newPageName);
        }
    }

    public void refactorTestData(String oldTDName, String newTDName) {
        for (Scenario scenario : scenarios) {
            scenario.refactorTestData(oldTDName, newTDName);
        }
    }

    public void refactorTestDataColumn(String testDataName, String oldColumnName, String newColumnName) {
        for (Scenario scenario : scenarios) {
            scenario.refactorTestDataColumn(testDataName, oldColumnName, newColumnName);
        }
    }

    public List<TestCase> getImpactedObjectTestCases(String pageName, String objectName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(scenario.getImpactedObjectTestCases(pageName, objectName));
        }
        return impactedTestCases;
    }

    public List<TestCase> getImpactedTestCaseTestCases(String scenarioName, String testCaseName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(scenario.getImpactedTestCaseTestCases(scenarioName, testCaseName));
        }
        return impactedTestCases;
    }

    public List<TestCase> getImpactedTestDataTestCases(String testDataName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (Scenario scenario : scenarios) {
            impactedTestCases.addAll(scenario.getImpactedTestDataTestCases(testDataName));
        }
        return impactedTestCases;
    }

    public ProjectSettings getProjectSettings() {
        return projectSettings;
    }

    public ObjectRepository getObjectRepository() {
        return objectRepository;
    }

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

    private ProjectInfo checkData(ProjectInfo project) {
        if (project.getData().isEmpty()) {
            updateData(project);
        }
        return project;
    }

    private ProjectInfo updateData(ProjectInfo project) {
        getAllScenarios().stream().map(To::Meta).forEach(project::addMeta);
        getAllScenarios().stream().flatMap(To::TC).map(To.DI::fromTC).forEach(project::addData);
        releases.stream().flatMap(To::TS).map(To.DI::fromTS).forEach(project::addData);
        return project;
    }

    static class To {

        private static Stream<TestCase> TC(Scenario scn) {
            return scn.getTestCases().stream();
        }

        private static Stream<TestSet> TS(Release scn) {
            return scn.getTestSets().stream();
        }

        private static Meta Meta(Scenario scn) {
            return Meta.createScenario(scn.getName());
        }

        static class DI {

            private static DataItem create(String id, String name, Object t) {
                DataItem data = new DataItem();
                data.setId(id);
                data.setName(name);
                data.getAttributes().add(Meta.Attributes.type, t);
                return data;
            }

            private static DataItem fromTC(TestCase tc) {
                DataItem data = create(tc.getKey(), tc.getName(), tc.isReusable() ? Meta.Attributes.reusable : Meta.Attributes.testcase);
                data.getAttributes().add(Meta.Attributes.scenario, tc.getScenario().getName());
                return data;
            }

            private static DataItem fromTS(TestSet ts) {
                DataItem data = create(ts.getName(), ts.getName(), Meta.Attributes.testset);
                data.getAttributes().add(Meta.Attributes.release, ts.getRelease().getName());
                return data;
            }
        }
    }

}
