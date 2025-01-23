
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecColorScheme;

/**
 *
 * 
 */
public class ColorScheme extends General {

    public ColorScheme(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.APP, desc ="Assert if [<Object>] color scheme matches [<Data>] ", input =InputType.YES)
    public void assertElementColorScheme() {
        SpecColorScheme spec = SpecReader.reader().getSpecColorScheme(Data);
        spec.setOriginalText(getMessage());
        validate(spec, RelativeElement.None);
    }

    private String getMessage() {
        return String.format("%s's color scheme matches with %s", ObjectName, Data);
    }
*/
}
