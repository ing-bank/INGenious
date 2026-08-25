package com.ing.plugin.general;

import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.annotation.Action;

/**
 * General purpose plugin for text assertions and utility operations
 * 
 * @author Your Name
 */
public class TextAsserts {
    
    CommandPluginApi gen;
    public String Data;
    public String Action;
    public String Input;
    public TestCaseReportApi Report;

    public TextAsserts(CommandPluginApi gen) {
        System.out.println("TextAsserts Plugin initialized with CommandPluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Report = gen.getReport();
    }

    /**
     * Assert text is in lowercase
     */
    @Action(object = "Text Assertions", desc = "Assert if input is in lower case", input = InputType.YES, condition = InputType.NO)
    public void assertTextInLowerCase() {
        System.out.println("Hello World! This is the assertTextInLowerCase action");
        String var = gen.getVar(Input);
        System.out.println("Input is " + var);
        if (var.equals(var.toLowerCase())) {
            Report.updateTestLog(Action, "The input " + Data + " is in lower case.", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "The input " + Data + " is not in lower case.", Status.FAILNS);
        }
    }

    /**
     * Assert text is in uppercase
     */
    @Action(object = "Text Assertions", desc = "Assert if input is in upper case", input = InputType.YES, condition = InputType.NO)
    public void assertTextInUpperCase() {
        System.out.println("Hello World! This is the assertTextInUpperCase action");
        String var = gen.getVar(Input);
        System.out.println("Input is " + var);
        if (var.equals(var.toUpperCase())) {
            Report.updateTestLog(Action, "The input " + Data + " is in upper case.", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "The input " + Data + " is not in upper case.", Status.FAILNS);
        }
    }
    
    /**
     * General action demonstrating ObjectType enum usage
     */
    @Action(object = ObjectType.GENERAL, desc = "General purpose action", input = InputType.YES, condition = InputType.NO)
    public void testObjectypeUsingEnum() {
        String var = gen.getVar(Input);
        System.out.println("This is stored in the variable: " + var);
        Report.updateTestLog(Action, "Status code is : " + Data, Status.DONE);
    }
}
