
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecHeight;
import com.galenframework.specs.SpecWidth;

/**
 *
 * 
 */
public class WidthAndHeight extends General {

    public WidthAndHeight(CommandControl cc) {
        super(cc);
    }
/*
    private void assertWidth(RelativeElement rElement) {
        SpecWidth spec = SpecReader.reader().getSpecWidth(rElement, Data, Condition);
        spec.setOriginalText(getMessage("width", rElement));
        validate(spec, rElement);
    }

    private void assertHeight(RelativeElement rElement) {
        SpecHeight spec = SpecReader.reader().getSpecHeight(rElement, Data, Condition);
        spec.setOriginalText(getMessage("height", rElement));
        validate(spec, rElement);
    }

    
    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>]'s width is [<Data>] ", 
    		input =InputType.YES,
    		condition = InputType.OPTIONAL)
    public void assertElementWidth() {
        assertWidth(RelativeElement.None);
    }

    @Action(object = ObjectType.APP, 
    		desc ="Assert if [<Object>] 's width is [<Data>] of [<Object2>]", 
    		input =InputType.YES, 
    		condition = InputType.YES
    		)
    public void assertElementWidthElement() {
        assertWidth(RelativeElement.WebElement);
    }

    @Action(object = ObjectType.APP, desc ="Assert if [<Object>]'s height is [<Data>] ", input =InputType.YES, condition = InputType.OPTIONAL)
    public void assertElementHeight() {
        assertHeight(RelativeElement.None);
    }
    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] 's height is [<Data>] of [<Object2>]", input =InputType.YES, condition = InputType.YES)
    public void assertElementHeightElement() {
        assertHeight(RelativeElement.WebElement);
    }

    private String getMessage(String type, RelativeElement rElement) {
        String message = String.format("%s's %s is %s", ObjectName, type, Data);
        if (rElement.equals(RelativeElement.WebElement)) {
            message += " of " + Condition;
        }
        return message;
    }
*/
}
