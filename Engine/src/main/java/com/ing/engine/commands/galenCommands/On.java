
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.Location;
import com.galenframework.specs.Side;
import com.galenframework.specs.SpecOn;
import java.util.List;

public class On extends General {

    public On(CommandControl cc) {
        super(cc);
    }
/*
    private void asssertElementOn(Side horizontal, Side vertical) {
        SpecOn spec = SpecReader.reader().getSpecOn(Condition, horizontal, vertical, Data);
        spec.setOriginalText(getMessage(horizontal, vertical, spec.getLocations()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is  on top left of [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL, 
    		condition = InputType.YES)
    public void assertElementOnTopLeft() {
        asssertElementOn(Side.TOP, Side.LEFT);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is  on top right of [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL,
    		condition = InputType.YES)
    public void assertElementOnTopRight() {
        asssertElementOn(Side.TOP, Side.RIGHT);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is  on bottom left of [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL,
    		condition = InputType.YES)
    public void assertElementOnBottomLeft() {
        asssertElementOn(Side.BOTTOM, Side.LEFT);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] is  on bottom right of [<Object2>] [<Data>]", 
    		input =InputType.OPTIONAL, 
    		condition = InputType.YES)
    public void assertElementOnBottomRight() {
        asssertElementOn(Side.BOTTOM, Side.RIGHT);
    }

    private String getMessage(Side horizontal, Side vertical, List<Location> locations) {
        String message = String.format("%s is On %s-%s of %s", ObjectName, horizontal.toString(), vertical.toString(), Condition);
        if (!locations.isEmpty()) {
            message += " over location" + Data;
        }
        return message;
    }
*/
}
