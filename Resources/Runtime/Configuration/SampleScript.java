


import com.ing.engine.commands.General;
import com.ing.engine.core.CommandControl;

import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;

public class SampleScript extends General {

    public SampleScript(CommandControl cc) {
        super(cc);
    }

    @Action(desc = "Description of the method", input = InputType.NO)
    public void yourCustomMethod() {
        //To Do. Put your code here
        Report.updateTestLog(Action, Description, Status.DONE);
    }

    @Action(object = ObjectType.SELENIUM)
    public void youCustomMethod2() {
        if (Element.getText().equals("Something")) {
            Report.updateTestLog(Action, "Element text matched with Something", Status.PASS);
        } else {
            Report.updateTestLog(Action, "Error in action", Status.FAIL);
        }
    }
}
