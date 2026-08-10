package com.ing.datalib.component;

import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.csv.CSVRecord;

/**
 * Represents a single step within a {@link TestCase}, storing action, object,
 * input and related fields. Integrates with the test case table model, supports
 * reusable/page‑object step detection, and provides utilities such as comment
 * and breakpoint toggling.
 */
public class TestStep {
    private static String BREAKPOINT = "*";
    private static String COMMENT = "//";
    private static String HARD_ASSERTION = "~";
    private static final Set<String> NON_PAGE_OBJECTS = Set.of(
        "execute",
        "app",
        "browser",
        "mobile",
        "database",
        "webservice",
        "kafka",
        "synthetic data",
        "queue",
        "file",
        "general",
        "string operations"
    );

    public enum HEADERS {
        Step(0),
        ObjectName(1),
        Description(2),
        Action(3),
        Input(4),
        Condition(5),
        Reference(6);

        private final int index;

        private HEADERS(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static List<String> getValues() {
            List<String> list = new ArrayList<>();
            for (HEADERS header : HEADERS.values()) {
                list.add(header.name());
            }
            return list;
        }

        public static int size() {
            return HEADERS.values().length;
        }
    }

    private final TestCase testCase;

    /**
     * Transient, in-memory only flag marking a step that was just added by the live
     * Playwright recorder. Used by the editor to highlight freshly recorded steps; it is
     * never persisted to disk and is cleared when the user explicitly saves the test case.
     */
    private transient boolean newlyRecorded = false;

    List<String> stepDetails = Collections.synchronizedList(
        new ArrayList<String>(HEADERS.values().length) {

            @Override
            public String set(int index, String element) {
                String val = super.set(index, element);
                if (testCase != null && testCase.getTestSteps().contains(TestStep.this)) {
                    testCase.fireTableCellUpdated(
                        testCase.getTestSteps().indexOf(TestStep.this),
                        index
                    );
                }
                return val;
            }
        }
    );

    public List<String> getStepDetails() {
        return this.stepDetails;
    }

    public TestStep(TestCase testcase, CSVRecord record) {
        this.testCase = testcase;
        loadStep(record);
    }

    /**
     * Constructs a step from a pre-parsed row of values (format-agnostic).
     * Used by the YAML loader; the row is expected to be aligned with
     * {@link HEADERS} and is padded/truncated to the header count.
     */
    public TestStep(TestCase testcase, List<String> row) {
        this.testCase = testcase;
        loadStep(row);
    }

    public TestStep(TestCase testcase) {
        this.testCase = testcase;
        loadEmptyStep();
    }

    public String getTag() {
        return Objects.toString(stepDetails.get(HEADERS.Step.getIndex()), "");
    }

    public void setTag(String step) {
        stepDetails.set(HEADERS.Step.getIndex(), step);
    }

    public String getObject() {
        return Objects.toString(stepDetails.get(HEADERS.ObjectName.getIndex()), "");
    }

    public TestStep setObject(String object) {
        stepDetails.set(HEADERS.ObjectName.getIndex(), object);
        return this;
    }

    public String getReference() {
        return Objects.toString(stepDetails.get(HEADERS.Reference.getIndex()), "");
    }

    public TestStep setReference(String reference) {
        stepDetails.set(HEADERS.Reference.getIndex(), reference);
        return this;
    }

    public String getAction() {
        return Objects.toString(stepDetails.get(HEADERS.Action.getIndex()), "");
    }

    public TestStep setAction(String action) {
        stepDetails.set(HEADERS.Action.getIndex(), action);
        return this;
    }

    public String getInput() {
        return Objects.toString(stepDetails.get(HEADERS.Input.getIndex()), "");
    }

    public TestStep setInput(String input) {
        stepDetails.set(HEADERS.Input.getIndex(), input);
        return this;
    }

    public String getCondition() {
        return Objects.toString(stepDetails.get(HEADERS.Condition.getIndex()), "");
    }

    public TestStep setCondition(String condition) {
        stepDetails.set(HEADERS.Condition.getIndex(), condition);
        return this;
    }

    public String getDescription() {
        return Objects.toString(stepDetails.get(HEADERS.Description.getIndex()), "");
    }

    public TestStep setDescription(String description) {
        stepDetails.set(HEADERS.Description.getIndex(), description);
        return this;
    }

    public Project getProject() {
        return testCase.getProject();
    }

    public TestCase getTestCase() {
        return testCase;
    }

    private void loadStep(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            stepDetails.add(record.get(i));
        }
        while (stepDetails.size() < HEADERS.values().length) {
            stepDetails.add("");
        }
    }

    private void loadStep(List<String> row) {
        int target = HEADERS.values().length;
        for (int i = 0; i < Math.min(row.size(), target); i++) {
            String value = row.get(i);
            stepDetails.add(value == null ? "" : value);
        }
        while (stepDetails.size() < target) {
            stepDetails.add("");
        }
    }

    private void loadEmptyStep() {
        for (HEADERS value : HEADERS.values()) {
            stepDetails.add("");
        }
    }

    public String getValueAt(int index) {
        return stepDetails.get(index);
    }

    public String getValueBy(String header) {
        return stepDetails.get(HEADERS.valueOf(header).getIndex());
    }

    public void putValueAt(int index, String value) {
        stepDetails.set(index, value);
    }

    public int size() {
        return stepDetails.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder
            .append("TestStep - ")
            .append(getAction())
            .append(" | ")
            .append(getObject())
            .append(" | ")
            .append(getInput());
        return builder.toString();
    }

    public void clearValues() {
        for (int i = 0; i < size(); i++) {
            putValueAt(i, "");
        }
    }

    public TestStep asReusableStep(String scenario, String reusable) {
        setAction(scenario + ":" + reusable);
        setObject("Execute");
        return this;
    }

    public TestStep asObjectStep(String objectName, String pageName) {
        setObject(objectName);
        setReference(pageName);
        return this;
    }

    public TestStep asObjectStep(ResolvedWebObject rwo) {
        setObject(rwo.getObjectName());
        setReference(new ResolvedWebObject.PageRef(rwo.getPageName(), rwo.getScope()).qualified());
        return this;
    }

    public TestStep asObjectStep(ResolvedMobileObject rmo) {
        setObject(rmo.getObjectName());
        setReference(
            new ResolvedMobileObject.PageRef(rmo.getPageName(), rmo.getScope()).qualified()
        );
        return this;
    }

    public TestStep asObjectStep(ResolvedStructuredDataObject rsdo) {
        setObject(rsdo.getObjectName());
        setReference(
            new ResolvedStructuredDataObject.PageRef(rsdo.getPageName(), rsdo.getScope())
            .qualified()
        );
        return this;
    }

    public void copyValuesTo(TestStep step) {
        for (int i = 0; i < size(); i++) {
            step.putValueAt(i, getValueAt(i));
        }
    }

    public Boolean isPageObjectStep() {
        String objectName = Objects.toString(getObject(), "").trim();
        String reference = Objects.toString(getReference(), "").trim();
        return (
            !objectName.isEmpty() &&
            !reference.isEmpty() &&
            !NON_PAGE_OBJECTS.contains(objectName.toLowerCase(Locale.ROOT))
        );
    }

    public Boolean isReusableStep() {
        return getObject().equals("Execute") && getAction().matches(".+:.+");
    }

    public Boolean isTestDataStep() {
        if (
            getInput().startsWith("<") || getInput().startsWith("{") || getInput().startsWith("[")
        ) return false; else if (
            getInput().matches("(?!(@|=|%)).+:.+")
        ) return true; else return false; // return getInput().matches("(?!(@|=|%)).+:.+");
    }

    public Boolean isEmpty() {
        for (String val : stepDetails) {
            if (!toString(val).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String toString(Object val) {
        return Objects.isNull(val) ? "" : val.toString();
    }

    public void toggleComment() {
        if (getTag().contains(COMMENT)) {
            setTag(getTag().replace(COMMENT, ""));
        } else {
            setTag(COMMENT.concat(getTag()));
        }
    }

    public void toggleBreakPoint() {
        if (getTag().contains(BREAKPOINT)) {
            setTag(getTag().replace(BREAKPOINT, ""));
        } else if (getTag().startsWith(COMMENT)) {
            setTag(COMMENT.concat(BREAKPOINT).concat(getTag().replace(COMMENT, "")));
        } else {
            setTag(BREAKPOINT.concat(getTag()));
        }
    }

    public Boolean hasBreakPoint() {
        return getTag().startsWith(BREAKPOINT);
    }

    public Boolean isCommented() {
        return getTag().startsWith(COMMENT);
    }

    /**
     * @return {@code true} when this step's action is an assertion (its action
     *         name starts with {@code assert}, case-insensitive).
     */
    public Boolean isAssertStep() {
        return getAction().toLowerCase().startsWith("assert");
    }

    /**
     * @return {@code true} when this assertion step is marked as a hard
     *         assertion. All assertions are soft by default; a hard assertion
     *         stops the current data iteration on failure.
     */
    public Boolean isHardAssertion() {
        return getTag().contains(HARD_ASSERTION);
    }

    /**
     * Marks (or unmarks) this step as a hard assertion. The flag is stored as a
     * marker in the step tag, mirroring how comment and breakpoint markers are
     * persisted, so it survives save/load without a schema change.
     *
     * @param hard {@code true} for hard assertion, {@code false} for soft
     *             (the default).
     */
    public void setHardAssertion(boolean hard) {
        boolean current = getTag().contains(HARD_ASSERTION);
        if (hard && !current) {
            setTag(getTag().concat(HARD_ASSERTION));
        } else if (!hard && current) {
            setTag(getTag().replace(HARD_ASSERTION, ""));
        }
    }

    /**
     * @return {@code true} when this step was just added by the live recorder and has not yet
     *         been saved by the user. Highlighted in the editor while {@code true}.
     */
    public boolean isNewlyRecorded() {
        return newlyRecorded;
    }

    public void setNewlyRecorded(boolean newlyRecorded) {
        this.newlyRecorded = newlyRecorded;
    }

    public String[] getReusableData() {
        if (isReusableStep()) {
            try {
                ReusableRef ref = getEffectiveReusableRef();
                return new String[] { ref.getScenarioName(), ref.getTestCaseName() };
            } catch (IllegalArgumentException ex) {
                String[] parts = getAction().split(":", 2);
                if (parts.length == 2) {
                    return parts;
                }
            }
        }
        return null;
    }

    /**
     * Returns reusable reference parsed from action and enriched with scope from Reference column
     * when action is unscoped legacy format.
     */
    public ReusableRef getEffectiveReusableRef() {
        if (!isReusableStep()) {
            return null;
        }

        ReusableRef parsed = ReusableRef.parse(getAction());
        if (parsed.getScope() != ReusableRef.Scope.UNSCOPED) {
            return parsed;
        }

        ReusableRef.Scope scopedFromReference = parseReusableScopeFromReference(getReference());
        if (scopedFromReference == ReusableRef.Scope.UNSCOPED) {
            return parsed;
        }
        return new ReusableRef(
            scopedFromReference,
            parsed.getScenarioName(),
            parsed.getTestCaseName()
        );
    }

    private ReusableRef.Scope parseReusableScopeFromReference(String reference) {
        String ref = Objects.toString(reference, "").trim();
        if (ref.startsWith("[Project]")) {
            return ReusableRef.Scope.PROJECT;
        }
        if (ref.startsWith("[Shared]")) {
            return ReusableRef.Scope.SHARED;
        }
        return ReusableRef.Scope.UNSCOPED;
    }

    public String[] getTestDataFromInput() {
        if (isTestDataStep()) {
            return getInput().split(":");
        }
        return null;
    }

    public String[] getPageObject() {
        if (isPageObjectStep()) {
            return new String[] { getObject(), getReference() };
        }
        return null;
    }

    public Boolean isDuplicate(TestStep step) {
        for (int i = 1; i < stepDetails.size(); i++) {
            if (!Objects.equals(getValueAt(i), step.getValueAt(i))) {
                return false;
            }
        }
        return true;
    }

    public Boolean isDatabaseStep() {
        return getObject().equals("Database");
    }

    public Boolean isWebserviceStep() {
        return getObject().equals("Webservice");
    }

    public Boolean isBrowserStep() {
        return getObject().equals("Browser");
    }

    public Boolean isFileStep() {
        return getObject().equals("File");
    }

    public Boolean isMessageStep() {
        return getObject().equals("Queue") || getObject().equals("Kafka");
    }

    public Boolean isSetTextStep() {
        return (
            (getObject().equals("Queue") && getAction().contains("setText")) ||
            (getObject().equals("Kafka") && getAction().contains("produceMessage"))
        );
    }

    public Boolean isWebserviceRequestStep() {
        String requests[] = new String[] { "get", "delete", "post", "put", "patch" };
        boolean isWebserviceRequestStep = false;
        if (getObject().equals("Webservice")) {
            for (String request : requests) {
                if (getAction().contains(request)) {
                    isWebserviceRequestStep = true;
                    break;
                }
            }
        }
        return isWebserviceRequestStep;
    }

    public Boolean isWebserviceStartStep() {
        return (getObject().equals("Webservice") && getAction().contains("setEndPoint"));
    }

    public Boolean isWebserviceStopStep() {
        return (getObject().equals("Webservice") && getAction().contains("closeConnection"));
    }

    public Boolean isStringOperationsStep() {
        return getObject().equals("String Operations");
    }
}
