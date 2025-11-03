package com.ing.engine.commands.json;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonOperations extends General {

    public JsonOperations(CommandControl cc) {
        super(cc);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean compareJsonFiles(String filePath1, String filePath2, List<String> ignoredPaths) {
        try {
            // Read JSON files
            JsonNode json1 = objectMapper.readTree(new File(filePath1));
            JsonNode json2 = objectMapper.readTree(new File(filePath2));
            try {
                // Perform JSON comparison with detailed differences
                JsonAssertions.assertThatJson(json1.toString())
                        .whenIgnoringPaths(ignoredPaths.toArray(new String[0])) // Ignore specified paths
                        .isEqualTo(json2.toString());
                System.out.println("JSON files are equal (excluding ignored nodes).");
                return true;
            } catch (AssertionError e) {
                // If JSONs differ, print a detailed report
                System.err.println("JSON files differ! See details below:");
                System.err.println(e.getMessage());
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error reading JSON files: " + e.getMessage());
            return false;
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.JSON, desc = "Compare JSON files", input = InputType.YES, condition = InputType.OPTIONAL)
    public void compareJSONFiles() {

        try {
            String[] parts = Data.split(",");

            boolean areEqual = compareJsonFiles(parts[0], parts[1], ignoreJSONPaths.get(Thread.currentThread().toString()));

            System.out.println("Comparison result: " + areEqual);
            if (areEqual) {
                Report.updateTestLog(Action, "JSON Path(s) compared successfully", Status.DONE);
            } else {
                Report.updateTestLog(Action, "JSON Path(s) compared successfully but differences encountered", Status.FAIL);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during JSON files comparison", ex);
            Report.updateTestLog(Action, "Error in JSON files comparison: " + "\n" + ex.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.JSON, desc = "Ignore JSON Path(s)", input = InputType.YES, condition = InputType.NO)
    public void ignoreJSONPath() throws Exception {
        try {
            ignoreJSONPaths.computeIfAbsent(Thread.currentThread().toString(), k -> new ArrayList<>()).add(Data);
            Report.updateTestLog(Action, "JSON Path to ignore for comparison has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting JSON Path to ignore for comparison", ex);
            Report.updateTestLog(Action, "Error in setting JSON Path to ignore for comparison: " + "\n" + ex.getMessage(), Status.FAIL);
        }
    }

}
