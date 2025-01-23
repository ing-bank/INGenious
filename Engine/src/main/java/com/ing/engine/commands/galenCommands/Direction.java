
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.Range;
import com.galenframework.specs.SpecAbove;
import com.galenframework.specs.SpecBelow;
import com.galenframework.specs.SpecLeftOf;
import com.galenframework.specs.SpecRightOf;

public class Direction extends General {

    public Direction(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is  above [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementAbove() {
        SpecAbove spec = SpecReader.reader().getSpecAbove(Condition, Data);
        spec.setOriginalText(getMessage("above", spec.getRange()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is  below [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL, 
    		condition = InputType.YES)
    public void assertElementBelow() {
        SpecBelow spec = SpecReader.reader().getSpecBelow(Condition, Data);
        spec.setOriginalText(getMessage("below", spec.getRange()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is leftof [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL,
    		condition = InputType.YES)
    public void assertElementLeftOf() {
        SpecLeftOf spec = SpecReader.reader().getSpecLeftOf(Condition, Data);
        spec.setOriginalText(getMessage("left of", spec.getRange()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is rightof [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL,condition = InputType.YES)
    public void assertElementRightOf() {
        SpecRightOf spec = SpecReader.reader().getSpecRightOf(Condition, Data);
        spec.setOriginalText(getMessage("right of", spec.getRange()));
        validate(spec);
    }

    private String getMessage(String direction, Range errorRate) {
        String message = String.format("%s is %s %s ", ObjectName, direction, Condition);
        if (errorRate != null && !errorRate.holds(0)) {
            message += " With Range " + errorRate.toString();
        }
        return message;
    }
*/
}
