package com.ing.ide.e2e.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Method;
import java.nio.file.Path;

/**
 * Scenario and test case codes and descriptions of one test method. Falls back to the class and
 * method names when {@link Scenario} or {@link TestCase} is missing.
 */
public final class TestIdentity {
    @JsonProperty
    private final String scenarioCode;

    @JsonProperty
    private final String scenarioDescription;

    @JsonProperty
    private final String testCaseCode;

    @JsonProperty
    private final String testCaseDescription;

    /**
     * @param scenarioCode scenario code
     * @param scenarioDescription scenario description
     * @param testCaseCode test case code
     * @param testCaseDescription test case description
     */
    @JsonCreator
    public TestIdentity(
        @JsonProperty("scenarioCode") final String scenarioCode,
        @JsonProperty("scenarioDescription") final String scenarioDescription,
        @JsonProperty("testCaseCode") final String testCaseCode,
        @JsonProperty("testCaseDescription") final String testCaseDescription
    ) {
        this.scenarioCode = scenarioCode;
        this.scenarioDescription = scenarioDescription;
        this.testCaseCode = testCaseCode;
        this.testCaseDescription = testCaseDescription;
    }

    /**
     * Reads the identity from the annotations of a test method.
     *
     * @param testClass the test class
     * @param method the test method
     * @return the identity
     */
    public static TestIdentity of(final Class<?> testClass, final Method method) {
        final Scenario scenario = testClass.getAnnotation(Scenario.class);
        final TestCase testCase = method.getAnnotation(TestCase.class);
        return new TestIdentity(
            scenario != null ? scenario.code() : testClass.getSimpleName(),
            scenario != null ? scenario.description() : "",
            testCase != null ? testCase.code() : method.getName(),
            testCase != null ? testCase.description() : ""
        );
    }

    /**
     * @return scenario code
     */
    public String scenarioCode() {
        return scenarioCode;
    }

    /**
     * @return scenario description
     */
    public String scenarioDescription() {
        return scenarioDescription;
    }

    /**
     * @return test case code
     */
    public String testCaseCode() {
        return testCaseCode;
    }

    /**
     * @return test case description
     */
    public String testCaseDescription() {
        return testCaseDescription;
    }

    /**
     * @param runDir the run output folder
     * @return the folder that holds this test case's evidence: {@code <run>/<scenario>/<test case>}
     */
    public Path evidenceDir(final Path runDir) {
        return runDir.resolve(safe(scenarioCode)).resolve(safe(testCaseCode));
    }

    /**
     * @return a file-name-safe key that is unique per test case
     */
    public String fileKey() {
        return safe(scenarioCode) + "__" + safe(testCaseCode);
    }

    private static String safe(final String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
