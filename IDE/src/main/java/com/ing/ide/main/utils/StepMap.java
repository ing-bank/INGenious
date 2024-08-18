
package com.ing.ide.main.utils;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.component.utils.CSVHParser;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.engine.support.methodInf.MethodInfoManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * 
 */
public class StepMap {

    private final List<String> HEADERS = Arrays.asList("Subject", "Test Name", "Step", "Description", "Expected Result");

    private final File STEP_MAP_FILE = new File("Configuration" + File.separator + "StepMap.csv");
    CSVHParser stepMapTemplate;

    public StepMap() {
        load();
    }

    private void load() {
        stepMapTemplate = FileUtils.getCSVHParser(STEP_MAP_FILE);
    }

    public void convertScenarios(File file, List<Scenario> scenarios) {
        if (scenarios != null && !scenarios.isEmpty()) {
            try (FileWriter out = new FileWriter(file);
                    CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
                printer.printRecord(HEADERS);
                for (Scenario scenario : scenarios) {
                    for (TestCase testCase : scenario.getTestCases()) {
                        convertTestCase(testCase, printer);
                        printer.println();
                    }
                    printer.println();
                }
            } catch (Exception ex) {
                Logger.getLogger(StepMap.class.getName()).log(Level.SEVERE, "Error while converting", ex);
            }
        }
    }

    public void convertTestCase(File file, List<TestCase> testCases) {
        if (testCases != null && !testCases.isEmpty()) {
            try (FileWriter out = new FileWriter(file);
                    CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
                printer.printRecord(HEADERS);
                for (TestCase testCase : testCases) {
                    convertTestCase(testCase, printer);
                    printer.println();
                }
            } catch (Exception ex) {
                Logger.getLogger(StepMap.class.getName()).log(Level.SEVERE, "Error while converting", ex);
            }
        }
    }

    
    public void convertTestCase(File file, TestCase testCase) {
        if (testCase != null) {
            try (FileWriter out = new FileWriter(file);
                    CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
                printer.printRecord(HEADERS);
                convertTestCase(testCase, printer);

            } catch (Exception ex) {
                Logger.getLogger(StepMap.class.getName()).log(Level.SEVERE, "Error while converting", ex);
            }
        }
    }

    public String convertTestCase(TestCase testCase) {
        if (testCase != null) {
            try (StringWriter out = new StringWriter();
                    CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
                printer.printRecord(HEADERS);
                convertTestCase(testCase, printer);
                return out.toString();
            } catch (Exception ex) {
                Logger.getLogger(StepMap.class.getName()).log(Level.SEVERE, "Error while converting", ex);
            }
        }
        return null;
    }

    private void convertTestCase(TestCase testCase, CSVPrinter printer) throws IOException {
        Boolean clearOnExit = testCase.getTestSteps().isEmpty();
        testCase.loadTableModel();
        for (TestStep testStep : testCase.getTestSteps()) {
            if (testStep.isReusableStep()) {
                TestCase rTestCase = testCase.getProject()
                        .getScenarioByName(testStep.getReusableData()[0])
                        .getTestCaseByName(testStep.getReusableData()[1]);
                convertTestCase(rTestCase, printer);
            } else if (!testStep.getAction().isEmpty()) {
                printer.print(testCase.getScenario().getName());
                printer.print(testCase.getName());
                printer.print(testStep.getAction());
                CSVRecord descRecord = getRecordByAction(testStep.getAction());
                if (descRecord != null) {
                    printer.print(resolve(testStep, descRecord.get("Description")));
                    printer.print(resolve(testStep, descRecord.get("Expected Result")));
                } else {
                    printer.print(resolve(testStep, MethodInfoManager.getDescriptionFor(testStep.getAction())));
                    printer.print("");
                }
                printer.println();
            }
        }
        if (clearOnExit) {
            testCase.getTestSteps().clear();
        }
    }

    private String resolve(TestStep testStep, String val) {
        if (val != null) {
            val = val.replaceAll("<Data>", Matcher.quoteReplacement(resolveTestData(testStep.getInput())));
            val = val.replaceAll("<Object>", Matcher.quoteReplacement(testStep.getObject()));
            val = val.replaceAll("<Object2>", Matcher.quoteReplacement(testStep.getCondition()));
        }
        return val;
    }

    private String resolveTestData(String data) {
        if (data == null) {
            return "";
        }
        if (data.startsWith("@")) {
            data = data.substring(1);
        } else if (data.startsWith("%") && data.endsWith("%")) {
            data = data.substring(0, data.length() - 1);
        }
        return data;
    }

    private CSVRecord getRecordByAction(String actionName) {
        if (!actionName.isEmpty()) {
            for (CSVRecord record : stepMapTemplate.getRecords()) {
                String action = record.get("Step");
                if (action.equalsIgnoreCase(actionName)) {
                    return record;
                }
            }
        }
        return null;
    }
}
