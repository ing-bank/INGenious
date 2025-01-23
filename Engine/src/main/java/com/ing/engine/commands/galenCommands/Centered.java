
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecCentered;

public class Centered extends General {

    public Centered(CommandControl cc) {
        super(cc);
    }
/*
    private void assertElementCentered(SpecCentered.Alignment alignment, SpecCentered.Location location) {
        SpecCentered spec = SpecReader.reader().getSpecCentered(Condition, Data, location, alignment);
        spec.setOriginalText(getMessage(alignment, location, spec.getErrorRate()));
        validate(spec);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is centeredAllOn [<Object2>] [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementCenteredAOn() {
        assertElementCentered(SpecCentered.Alignment.ALL, SpecCentered.Location.ON);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is centeredAllInside [<Object2>] [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementCenteredAInside() {
        assertElementCentered(SpecCentered.Alignment.ALL, SpecCentered.Location.INSIDE);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is centeredHorizontallyOn [<Object2>] [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementCenteredHOn() {
        assertElementCentered(SpecCentered.Alignment.HORIZONTALLY, SpecCentered.Location.ON);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is centeredHorizontallyInside [<Object2>] [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementCenteredHInside() {
        assertElementCentered(SpecCentered.Alignment.HORIZONTALLY, SpecCentered.Location.INSIDE);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is centeredVerticallyOn [<Object2>] [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementCenteredVOn() {
        assertElementCentered(SpecCentered.Alignment.VERTICALLY, SpecCentered.Location.ON);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] is centeredVerticallyInside [<Object2>] [<Data>]", input =InputType.OPTIONAL, condition = InputType.YES)
    public void assertElementCenteredVInside() {
        assertElementCentered(SpecCentered.Alignment.VERTICALLY, SpecCentered.Location.INSIDE);
    }

    private String getMessage(SpecCentered.Alignment alignment, SpecCentered.Location location, int errorRate) {
        String message = String.format("%s is centered %s %s %s", ObjectName, alignment.toString(), location.toString(), Condition);
        if (Data != null && !Data.trim().isEmpty()) {
            message += " With Error rate " + errorRate;
        }
        return message;
    }
*/
}
