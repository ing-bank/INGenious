
package com.ing.engine.execution.run;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;

/**
 *
 * 
 */
public class Annotation extends General {

    public Annotation(CommandControl cc) {
        super(cc);
    }

    public void beforeStepExecution() {
    }

    public void afterStepExecution() {
        Report.getCurrentStatus();//To get the status of current executed step
        if (Report.isStepPassed()) {
            //do something
        } else {

        }
    }

}
