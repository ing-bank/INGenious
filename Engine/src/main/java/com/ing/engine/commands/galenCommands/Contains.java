
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecContains;

public class Contains extends General {

    public Contains(CommandControl cc) {
        super(cc);
    }

    public void assertElementContains(Boolean isPartly) {
        SpecContains spec = SpecReader.reader().getSpecContains(getElementsList(), isPartly);
        spec.setOriginalText(getMessage(isPartly));
        validate(spec, RelativeElement.WebElementList);
    }

    
    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] contains <Object2> ", 
    	
    		condition = InputType.YES)
    public void assertElementContains() {
        assertElementContains(false);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] partly contains  <Object2> ", 
    		input =InputType.NO, 
    		condition = InputType.YES)
    public void assertElementContainsPartly() {
        assertElementContains(true);
    }

    private String getMessage(Boolean isPartly) {
        String partly = isPartly ? " partly " : "";
        return String.format("%s %scontains %s", ObjectName, partly, Condition);
    }

}
