package com.ing.engine.commands.file;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.execution.data.TestDataToken;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileOperations extends General {

    public FileOperations(CommandControl cc) {
        super(cc);
    }

    @Action(
        object = ObjectType.FILE,
        desc = "Populate Data and Saving File",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    ) // MyFiles/
    @Args(
        input = ArgType.TEXT,
        inputExample = "Hello {name}\nYour id is {id}",
        inputHelp = "file content or template text to populate before saving; can also be a path to an existing template file",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required"
    )
    public void populateData() {
        try {
            String fileName = getVar("%fileName%");
            String fileLocation = getVar("%fileLocation%");

            if (!fileLocation.endsWith("/")) {
                fileLocation += "/";
            }
            try (PrintWriter out = new PrintWriter(fileLocation + fileName)) {
                out.println(handleFileContent(Data));
                Report.updateTestLog(
                    Action,
                    "File [" + fileName + "] is saved successfully in  " + fileLocation,
                    Status.DONE
                );
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileOperations.class.getName()).log(Level.SEVERE, null, ex);
                Report.updateTestLog(
                    Action,
                    "Error Saving file in the directory :" + "\n" + ex.getMessage(),
                    Status.DEBUG
                );
            }
        } catch (Exception ex) {
            Logger.getLogger(FileOperations.class.getName()).log(Level.SEVERE, null, ex);
            Report.updateTestLog(
                Action,
                "Something went wrong in populating data and saving the file :" +
                "\n" +
                ex.getMessage(),
                Status.DEBUG
            );
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
        // Resolves {Sheet:Column} / {[Project] Sheet:Column} / {[Shared] Sheet:Column} tokens;
        // unknown tokens are left literal.
        return TestDataToken.resolveEmbeddedTokens(fileContent, userData);
    }

    private String handleuserDefinedVariables(String fileContent) {
        Collection<Object> valuelist = Control
            .getCurrentProject()
            .getProjectSettings()
            .getUserDefinedSettings()
            .values();
        for (Object prop : valuelist) {
            if (fileContent.contains("{" + prop + "}")) {
                fileContent = fileContent.replace("{" + prop + "}", prop.toString());
            }
        }
        return fileContent;
    }
}
