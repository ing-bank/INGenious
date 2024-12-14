package com.ing.engine.commands.file;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.io.File;
import java.io.PrintWriter;


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
        fileContent = handleDataSheetVariables(fileContent);
        fileContent = handleuserDefinedVariables(fileContent);
        return fileContent;
    }

    private String handleDataSheetVariables(String fileContent) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (fileContent.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
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
