
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecImage;

public class Image extends General {

    public Image(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.APP, desc ="Assert if [<Object>]'s image has [<Data>]", input =InputType.YES)
    public void assertElementImage() {
        SpecImage spec = SpecReader.reader().getSpecImage(Reference, ObjectName, Data);
        spec.setOriginalText(getMessage(spec));
        validate(spec);
    }

    private String getMessage(SpecImage spec) {
        return String.format("%s's image matches with given image with attibutes %s ", ObjectName, Data);
    }
*/
}
