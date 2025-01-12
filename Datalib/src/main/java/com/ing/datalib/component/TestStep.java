
package com.ing.datalib.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * 
 */
public class TestStep {

    private static String BREAKPOINT = "*";
    private static String COMMENT = "//";

    public enum HEADERS {

        Step(0), ObjectName(1), Description(2), Action(3), Input(4), Condition(5), Reference(6);

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

    private final TestCase testcase;

    List<String> stepDetails = Collections.synchronizedList(new ArrayList<String>(HEADERS.values().length) {
        @Override
        public String set(int index, String element) {
            String val = super.set(index, element);
            if (testcase != null && testcase.getTestSteps().contains(TestStep.this)) {
                testcase.fireTableCellUpdated(testcase.getTestSteps().indexOf(TestStep.this),
                        index);
            }
            return val;
        }
    });

    public TestStep(TestCase testcase, CSVRecord record) {
        this.testcase = testcase;
        loadStep(record);
    }

    public TestStep(TestCase testcase) {
        this.testcase = testcase;
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
        return testcase.getProject();
    }

    public TestCase getTestcase() {
        return testcase;
    }

    private void loadStep(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            stepDetails.add(record.get(i));
        }
        while (stepDetails.size() != HEADERS.values().length) {
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
        builder.append("TestStep - ")
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
        clearValues();
        setAction(scenario + ":" + reusable);
        setObject("Execute");
        return this;
    }

    public TestStep asObjectStep(String objectName, String pageName) {
        setObject(objectName);
        setReference(pageName);
        return this;
    }

    public void copyValuesTo(TestStep step) {
        for (int i = 0; i < size(); i++) {
            step.putValueAt(i, getValueAt(i));
        }
    }

    public Boolean isPageObjectStep() {
        return !getObject().equals("Browser")
                && !getObject().isEmpty()
                && !getReference().isEmpty();
    }

    public Boolean isReusableStep() {
        return getObject().equals("Execute") && getAction().matches(".+:.+");
    }

    public Boolean isTestDataStep() {
       if (getInput().startsWith("<") || getInput().startsWith("{") || getInput().startsWith("["))
            return false;
       else if (getInput().matches("(?!(@|=|%)).+:.+"))
	// return getInput().matches("(?!(@|=|%)).+:.+");
            return true; 
       else
            return false;
        
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

    public String[] getReusableData() {
        if (isReusableStep()) {
            return getAction().split(":");
        }
        return null;
    }

    public String[] getTestDataFromInput() {
        if (isTestDataStep() ) {
            return getInput().split(":");
        }
        return null;
    }

    public String[] getPageObject() {
        if (isPageObjectStep()) {
            return new String[]{getObject(), getReference()};
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
        return (getObject().equals("Queue") && getAction().contains("setText")) || (getObject().equals("Kafka") && getAction().contains("produceMessage"));
    }

    public Boolean isWebserviceRequestStep() {
        String requests[] = new String[]{"get", "delete", "post", "put", "patch"};
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
}
