package com.ing.ingenious.api.contract.ui;

/**
 * The test case a recording belongs to, named rather than referenced.
 *
 * <p>A target is just three values: the scenario, the test case inside it, and whether that
 * scenario is a reusable one. Names rather than object references, so a plugin can express a
 * target for a test case that does not exist yet — Studio creates whatever is missing before
 * recording into it, exactly as it does when a user types a new name in the recorder's own
 * target chooser.
 *
 * <p>Instances are immutable and carry no state beyond those three values, so a plugin may
 * build one per call without keeping anything alive between recordings.
 *
 * @see RecordingTargetApi
 */
public final class RecordingTarget {

    private final String scenarioName;
    private final String testCaseName;
    private final boolean reusableScenario;
    private final String startUrl;

    /**
     * A target under an ordinary test scenario.
     *
     * @param scenarioName the scenario name, neither {@code null} nor blank
     * @param testCaseName the test case name, neither {@code null} nor blank
     * @throws IllegalArgumentException when either name is {@code null} or blank
     */
    public RecordingTarget(String scenarioName, String testCaseName) {
        this(scenarioName, testCaseName, false);
    }

    /**
     * A target under either an ordinary or a reusable scenario.
     *
     * @param scenarioName the scenario name, neither {@code null} nor blank
     * @param testCaseName the test case name, neither {@code null} nor blank
     * @param reusableScenario {@code true} to place the test case under a reusable scenario
     * @throws IllegalArgumentException when either name is {@code null} or blank
     */
    public RecordingTarget(String scenarioName, String testCaseName, boolean reusableScenario) {
        this(scenarioName, testCaseName, reusableScenario, null);
    }

    /**
     * A target that also says which page the recording should start on.
     *
     * <p>Use this when the plugin knows more about the context than the project does — a test
     * case that belongs to a different module or entry page than the project's usual one. Pass
     * {@code null} to express no opinion, which leaves the project's own recorder setting in
     * charge.
     *
     * @param scenarioName the scenario name, neither {@code null} nor blank
     * @param testCaseName the test case name, neither {@code null} nor blank
     * @param reusableScenario {@code true} to place the test case under a reusable scenario
     * @param startUrl the page to open, or {@code null} for no opinion
     * @throws IllegalArgumentException when either name is {@code null} or blank
     */
    public RecordingTarget(
        String scenarioName,
        String testCaseName,
        boolean reusableScenario,
        String startUrl
    ) {
        this.scenarioName = require(scenarioName, "scenarioName");
        this.testCaseName = require(testCaseName, "testCaseName");
        this.reusableScenario = reusableScenario;
        this.startUrl = startUrl == null || startUrl.trim().isEmpty() ? null : startUrl.trim();
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be null or blank");
        }
        return value.trim();
    }

    /**
     * @return the scenario holding the test case, never blank
     */
    public String getScenarioName() {
        return scenarioName;
    }

    /**
     * @return the test case to record into, never blank
     */
    public String getTestCaseName() {
        return testCaseName;
    }

    /**
     * @return {@code true} when the scenario is a reusable scenario
     */
    public boolean isReusableScenario() {
        return reusableScenario;
    }

    /**
     * The page the recording should start on.
     *
     * @return the URL, or {@code null} when this target has no opinion and the project's own
     *     recorder setting decides
     */
    public String getStartUrl() {
        return startUrl;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecordingTarget)) {
            return false;
        }
        RecordingTarget that = (RecordingTarget) other;
        return (
            reusableScenario == that.reusableScenario &&
            scenarioName.equals(that.scenarioName) &&
            testCaseName.equals(that.testCaseName) &&
            (startUrl == null ? that.startUrl == null : startUrl.equals(that.startUrl))
        );
    }

    @Override
    public int hashCode() {
        int result = scenarioName.hashCode();
        result = 31 * result + testCaseName.hashCode();
        result = 31 * result + (reusableScenario ? 1 : 0);
        return 31 * result + (startUrl == null ? 0 : startUrl.hashCode());
    }

    @Override
    public String toString() {
        return (
            (reusableScenario ? "[Reusable] " : "") +
            scenarioName +
            " / " +
            testCaseName +
            (startUrl == null ? "" : " @ " + startUrl)
        );
    }
}
