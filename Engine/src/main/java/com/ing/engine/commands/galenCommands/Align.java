
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecHorizontally;
import com.galenframework.specs.SpecVertically;

/**
 *
 * 
 */
public class Align extends General {

    public Align(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is aligned horizontally [<Data>] with [<Object2>]", input =InputType.YES, condition = InputType.YES)
    public void assertElementAlignedHoriz() {
        SpecHorizontally spec = SpecReader.reader().getSpecHorizontally(Condition, Data);
        spec.setOriginalText(getMessage("Horizontally", spec.getErrorRate()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is aligned vertically [<Data>] with [<Object2>]", input =InputType.YES, condition = InputType.YES)
    public void assertElementAlignedVert() {
        SpecVertically spec = SpecReader.reader().getSpecVertically(Condition, Data);
        spec.setOriginalText(getMessage("Vertically", spec.getErrorRate()));
        validate(spec);
    }

    private String getMessage(String align, int errorRate) {
        String message = String.format("%s is aligned %s with %s", ObjectName, align, Condition);
        if (errorRate != 0) {
            message += " With Error rate " + errorRate;
        }
        return message;
    }
*/
}
