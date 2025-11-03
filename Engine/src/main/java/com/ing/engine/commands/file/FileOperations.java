package com.ing.engine.commands.file;

import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.PrintWriter;

import org.xmlunit.diff.*;

import java.io.*;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FileOperations extends General {

    public FileOperations(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.FILE, desc = "Populate Data and Saving File", input = InputType.YES, condition = InputType.OPTIONAL)// MyFiles/
    public void populateData() {
        try {
            String fileName = getVar("%fileName%");
            String fileLocation = getVar("%fileLocation%");

            if (!fileLocation.endsWith("/")) {
                fileLocation += "/";
            }
            try (PrintWriter out = new PrintWriter(fileLocation + fileName))
            { out.println(handleFileContent(Data));
                Report.updateTestLog(Action,"File [" + fileName + "] is saved successfully in  " + fileLocation, Status.DONE);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileOperations.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(Action, "Error Saving file in the directory :" + "\n" + ex.getMessage(),
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(FileOperations.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Something went wrong in populating data and saving the file :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }

    @Action(object = ObjectType.FILE, desc = "Replace substring in File", input = InputType.YES, condition = InputType.OPTIONAL)// MyFiles/
    public void replace() {
        try {
            String fileName = getVar("%fileName%");
            String fileLocation = getVar("%fileLocation%");

            if (!fileLocation.endsWith("/")) {
                fileLocation += "/";
            }

            try {
                Path filePath = Paths.get(fileLocation + fileName);
                String content = new String(Files.readAllBytes(filePath));
                String[] parts = Data.split(",");

                String result = content.replace(parts[0], parts[1]);

                Files.write(filePath, result.getBytes());

                Report.updateTestLog(Action,"Replaced: " + parts[0] + " with " + parts[1] + " in [" + fileName + "] successfully in  " + fileLocation, Status.DONE);
            } catch (IOException ex) {
                Logger.getLogger(FileOperations.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(Action, "Error Saving file in the directory :" + "\n" + ex.getMessage(),
                        Status.DEBUG);
                throw new RuntimeException(ex);
            }
    } catch (Exception ex) {
            Logger.getLogger(FileOperations.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(Action, "Something went wrong in populating data and saving the file :" + "\n" + ex.getMessage(),
                    Status.DEBUG);
        }
    }



    private Boolean compareFiles(String file1Path, String file2Path) {

        boolean areEqual = true;

        try {
            BufferedReader reader1 = new BufferedReader(new FileReader(file1Path));
            BufferedReader reader2 = new BufferedReader(new FileReader(file2Path));

            String line1, line2;

            int lineNum = 1;

            while ((line1 = reader1.readLine()) != null | (line2 = reader2.readLine()) != null) {
                if (line1 == null || line2 == null || !line1.equals(line2)) {
                    areEqual = false;
                    System.out.println("Difference at line " + lineNum + ":");
                    System.out.println("File1: " + (line1 != null ? line1 : "EOF"));
                    System.out.println("File2: " + (line2 != null ? line2 : "EOF"));
                }
                lineNum++;
            }
            if (areEqual) {
                System.out.println("The files are identical.");
            }
            return areEqual;
        } catch (IOException e) {
                System.err.println("Error reading files: " + e.getMessage());
        }

        return areEqual;
    }


    @com.ing.engine.support.methodInf.Action(object = ObjectType.FILE, desc = "Compare Text files", input = InputType.YES, condition = InputType.NO)
    public void compareTextFiles() {
        try {
            String[] parts = Data.split(",");
            Boolean areEqual = compareFiles(parts[0],parts[1]);
            if (!areEqual) {
                Report.updateTestLog(Action, "Differences detected in Text files comparison, consult log for details", Status.DEBUG);
            } else {
                Report.updateTestLog(Action, "Text files compared successfully", Status.DONE);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Text files comparison", ex);
            Report.updateTestLog(Action, "Error in Text files comparison: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.FILE, desc = "Set File", input = InputType.YES, condition = InputType.OPTIONAL)
    public void setFile() {
        if (shouldExecute(Condition)) {
            try {
                setFile.put(Thread.currentThread().toString(), Data);
                Report.updateTestLog(Action, "File has been set successfully", Status.DONE);
            } catch (NumberFormatException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting File", ex);
                Report.updateTestLog(Action, "Error in setting File: " + "\n" + ex.getMessage(), Status.DEBUG);
            }
        } else {
            Report.updateTestLog(Action, "No Intermediary host port required, skipping step.", Status.DONE);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.FILE, desc = "Save values in List from File by Regex", input = InputType.YES, condition = InputType.NO)
    public void saveValuesFromFile() {
        try {

            Pattern pattern = Pattern.compile(Data);

            String filePath = setFile.get(Thread.currentThread().toString());

            List<String> matches = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        matches.add(matcher.group(1));
                        regexMatches.put(Thread.currentThread().toString(), matches);
                    }
                }

            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }

            if (!regexMatches.get(Thread.currentThread().toString()).isEmpty()) {
                Report.updateTestLog(Action, "Values retrieved successfully from file", Status.DONE);
                for (String value : regexMatches.get(Thread.currentThread().toString())) {
                    System.out.println("Value saved: " + value);
                }
            } else {
                Report.updateTestLog(Action, "No values matched for given regex", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during retrieving values from file", ex);
            Report.updateTestLog(Action, "Error in retrieving values from file: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    @com.ing.engine.support.methodInf.Action(object = ObjectType.FILE, desc = "Get Value from List by Index", input = InputType.YES, condition = InputType.NO)
    public void getValueFromListByIndex() {
        try {

            if (!regexMatches.get(Thread.currentThread().toString()).isEmpty()) {
                String value = regexMatches.get(Thread.currentThread().toString()).get(Integer.valueOf(Data));
                addVar(Condition, value);
                Report.updateTestLog(Action, "Value: " + value + " taken successfully from List", Status.DONE);

            } else {
                Report.updateTestLog(Action, "List of values is empty", Status.DEBUG);
            }

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during retrieving values from file", ex);
            Report.updateTestLog(Action, "Error in retrieving values from file: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    @com.ing.engine.support.methodInf.Action(object = ObjectType.FILE, desc = "Empty Values Retrieved From file", input = InputType.NO, condition = InputType.NO)
    public void emptyRetrievedValuesFromFile() {
        try {

            String filePath = setFile.get(Thread.currentThread().toString());

            if (!regexMatches.get(Thread.currentThread().toString()).isEmpty()) {
                regexMatches.get(Thread.currentThread().toString()).clear();
                Report.updateTestLog(Action, "List of values emptied", Status.DONE);
            } else {
                Report.updateTestLog(Action, "List of values was already empty", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during emptying list of values", ex);
            Report.updateTestLog(Action, "Error in emptying list of values: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    private String handleFileContent(String data) throws FileNotFoundException {
        String fileContent = data;
        File file = new File(Data);
        if (file.isFile()) {
            Scanner sc = new Scanner(file);
            fileContent = "";
            while (sc.hasNext()) {
                fileContent += sc.nextLine() + "\n";
            }
            sc.close();
        }
        fileContent = handleDataSheetVariablesImproved(fileContent);
        fileContent = handleuserDefinedVariables(fileContent);
        return fileContent;
    }

    private String handleDataSheetVariablesImproved(String fileContent) {

        String iteration = userData.getIteration();

        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();

        Pattern pattern = Pattern.compile("\\{(.*?):(.*?)}(?:\\[(\\d+)])?");
        StringBuilder result = new StringBuilder();

        for (String line : fileContent.split("\n")) {
            Matcher matcher = pattern.matcher(line);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String sheet = matcher.group(1);
                String column = matcher.group(2);
                String subIteration = matcher.group(3);
                int index = (subIteration != null) ? Integer.parseInt(subIteration) : 0;


                /* switch to default when sheet is not found */
                if (!sheetlist.contains(sheet)) {
                    System.out.println("Data sheet: "+ sheet + " not found for selected environment, using default environment for sheet selection");
                    sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.getCurrentProject().getTestData().defEnv())
                                .getTestDataNames();
                }

                int sheetIndex = sheetlist.indexOf(sheet);

                if (sheetIndex == -1) {
                    throw new IllegalArgumentException("Sheet '" + sheet + "' not found.");
                }

                TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheet);
                List<String> columns = tdModel.getColumns();
                int columnIndex = columns.indexOf(column);

                String replacement = "";
                if (index > 0) {
                    replacement = userData.getData(sheetlist.get(sheetIndex), columns.get(columnIndex), iteration, subIteration);
                } else {
                    replacement = userData.getData(sheetlist.get(sheetIndex), columns.get(columnIndex));
                }

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);

            result.append(sb);
        }
        return result.toString();
    }

    private String handleDataSheetVariables(String fileContent) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (fileContent.contains("{" + sheetlist.get(sheet) + ":")) {
                TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();

                for (int col = 0; col < columns.size(); col++) {
                    if (fileContent.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                        fileContent = fileContent.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col)));
                    }
                }

            }
        }
        return fileContent;
    }

    private String replace(String fileContent, List<String> sheetlist, int sheet, List<String> columns) {
        for (int col = 0; col < columns.size(); col++) {
            if (fileContent.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                fileContent = fileContent.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                        userData.getData(sheetlist.get(sheet), columns.get(col)));
            }
        }
        return fileContent;
    }

    private String replaceBySubIteration(String fileContent, List<String> sheetlist, int sheet, List<String> columns, String subIteration) {
        String iteration = userData.getIteration();
        for (int col = 0; col < columns.size(); col++) {
            if (fileContent.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}" + "[" + subIteration + "}")) {
                fileContent = fileContent.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}" + "[" + subIteration + "}",
                        userData.getData(sheetlist.get(sheet), columns.get(col), iteration, subIteration));
            }
        }
        return fileContent;
    }

    private String handleuserDefinedVariables(String fileContent) {
        Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                .values();
        for (Object prop : valuelist) {
            if (fileContent.contains("{" + prop + "}")) {
                fileContent = fileContent.replace("{" + prop + "}", prop.toString());
            }
        }
        return fileContent;
    }

}
