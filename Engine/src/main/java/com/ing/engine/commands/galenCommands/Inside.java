
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.Location;
import com.galenframework.specs.SpecInside;
import java.util.List;

public class Inside extends General {

    public Inside(CommandControl cc) {
        super(cc);
    }
/*
    private void assertElement(Boolean isPartly) {
        SpecInside spec = SpecReader.reader().getSpecInside(Condition, Data, isPartly);
        spec.setOriginalText(getMessage(isPartly, spec.getLocations()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is inside [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL,
    		condition = InputType.YES)
    public void assertElementInside() {
        assertElement(false);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is partly inside [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL,
    		condition = InputType.YES)
    public void assertElementInsidePartly() {
        assertElement(true);
    }

    private String getMessage(Boolean isPartly, List<Location> locations) {
        String partly = isPartly ? " partly " : "";
        String message = String.format("%s is %sinside %s", ObjectName, partly, Condition);
        if (!locations.isEmpty()) {
            message += " over location" + Data;
        }
        return message;
    }
*/
}
