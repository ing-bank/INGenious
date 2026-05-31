package com.ing.datalib.component.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.ExecutionStep.HEADERS;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** YAML implementation of {@link TestSetStore}. */
public class YamlTestSetStore implements TestSetStore {

    private final ObjectMapper mapper;

    public YamlTestSetStore() {
        this.mapper = YamlTestCaseStore.buildMapper();
    }

    @Override
    public TestCaseFormat format() {
        return TestCaseFormat.YAML;
    }

    @Override
    public List<List<String>> load(File file) throws IOException {
        if (file == null || !file.exists()) {
            return new ArrayList<>();
        }
        TestSetYaml yaml = mapper.readValue(file, TestSetYaml.class);
        if (yaml == null || yaml.getExecutions() == null) {
            return new ArrayList<>();
        }
        List<List<String>> rows = new ArrayList<>(yaml.getExecutions().size());
        for (TestSetYaml.ExecutionYaml ex : yaml.getExecutions()) {
            rows.add(toRow(ex));
        }
        return rows;
    }

    @Override
    public void save(File file,
                     String testSetName,
                     String releaseName,
                     List<List<String>> rows) throws IOException {
        CsvTestCaseStore.ensureParent(file);

        TestSetYaml yaml = new TestSetYaml();
        yaml.setName(testSetName);
        yaml.setRelease(releaseName);

        List<TestSetYaml.ExecutionYaml> execs = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            execs.add(toExecutionYaml(row));
        }
        yaml.setExecutions(execs);

        mapper.writeValue(file, yaml);
    }

    private static List<String> toRow(TestSetYaml.ExecutionYaml ex) {
        int size = HEADERS.size();
        List<String> row = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            row.add("");
        }
        row.set(HEADERS.Execute.getIndex(), ex.getExecute() == null ? "true" : String.valueOf(ex.getExecute()));
        row.set(HEADERS.TestScenario.getIndex(), nullToEmpty(ex.getTestScenario()));
        row.set(HEADERS.TestCase.getIndex(), nullToEmpty(ex.getTestCase()));
        row.set(HEADERS.Iteration.getIndex(), defaultIfEmpty(ex.getIteration(), "Single"));
        row.set(HEADERS.Status.getIndex(), defaultIfEmpty(ex.getStatus(), "NoRun"));
        row.set(HEADERS.Browser.getIndex(), nullToEmpty(ex.getBrowser()));
        row.set(HEADERS.BrowserVersion.getIndex(), defaultIfEmpty(ex.getBrowserVersion(), "Default"));
        row.set(HEADERS.Platform.getIndex(), defaultIfEmpty(ex.getPlatform(), "Any"));
        return row;
    }

    private static TestSetYaml.ExecutionYaml toExecutionYaml(List<String> row) {
        TestSetYaml.ExecutionYaml ex = new TestSetYaml.ExecutionYaml();
        ex.setExecute(parseBool(safeGet(row, HEADERS.Execute.getIndex())));
        ex.setTestScenario(emptyToNull(safeGet(row, HEADERS.TestScenario.getIndex())));
        ex.setTestCase(emptyToNull(safeGet(row, HEADERS.TestCase.getIndex())));
        ex.setIteration(emptyToNull(safeGet(row, HEADERS.Iteration.getIndex())));
        ex.setStatus(emptyToNull(safeGet(row, HEADERS.Status.getIndex())));
        ex.setBrowser(emptyToNull(safeGet(row, HEADERS.Browser.getIndex())));
        ex.setBrowserVersion(emptyToNull(safeGet(row, HEADERS.BrowserVersion.getIndex())));
        ex.setPlatform(emptyToNull(safeGet(row, HEADERS.Platform.getIndex())));
        return ex;
    }

    private static Boolean parseBool(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Boolean.valueOf(value);
    }

    private static String safeGet(List<String> row, int idx) {
        return idx < row.size() && row.get(idx) != null ? row.get(idx) : "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String defaultIfEmpty(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
