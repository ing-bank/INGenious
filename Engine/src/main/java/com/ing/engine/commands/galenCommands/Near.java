
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.Location;
import com.galenframework.specs.SpecNear;
import java.util.List;

public class Near extends General {

    public Near(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is near [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL, 
    		condition = InputType.YES)
    public void assertElementNear() {
        SpecNear spec = SpecReader.reader().getSpecNear(Condition, Data);
        spec.setOriginalText(getMessage(spec.getLocations()));
        validate(spec);
    }

    private String getMessage(List<Location> locations) {
        return String.format("%s is Near %s over location %s", ObjectName, Condition, Data);
    }
*/
}
