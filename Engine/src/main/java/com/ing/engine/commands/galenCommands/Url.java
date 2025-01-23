
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.galenWrapper.SpecValidation.SpecUrl;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecText;
import java.util.Arrays;

/**
 *
 *
 */
public class Url extends General {

    public Url(CommandControl cc) {
        super(cc);
    }
/*
    private void assertUrl(SpecText.Type type) {
        SpecUrl spec = SpecReader.reader().getSpecUrl(type, Data);
        spec.setOriginalText(getMessage(type));
        validate(spec, RelativeElement.None);
    }

    private void assertUrlI(SpecText.Type type) {
        SpecUrl spec = SpecReader.reader().getSpecUrl(type, Data.toLowerCase());
        spec.setOperations(Arrays.asList(new String[]{"lowercase"}));
        spec.setOriginalText(getMessage(type));
        validate(spec, RelativeElement.None);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url Equals [<Data>]",input = InputType.YES)
    public void assertUrlEquals() {
        assertUrl(SpecText.Type.IS);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url Contains [<Data>]",input = InputType.YES)
    public void assertUrlContains() {
        assertUrl(SpecText.Type.CONTAINS);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url StartsWith [<Data>]",input = InputType.YES)
    public void assertUrlStartsWith() {
        assertUrl(SpecText.Type.STARTS);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url EndsWith [<Data>]",input = InputType.YES)
    public void assertUrlEndsWith() {
        assertUrl(SpecText.Type.ENDS);
    }

    @Action(object = ObjectType.MOBILE, desc = "Assert if Browser's Url Matches [<Data>]", input = InputType.YES)
    public void assertUrlMatches() {
        assertUrl(SpecText.Type.MATCHES);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url Equals [Ignorecase] [<Data>]",input = InputType.YES)
    public void assertUrlIEquals() {
        assertUrlI(SpecText.Type.IS);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url Contains [Ignorecase] [<Data>]",input = InputType.YES)
    public void assertUrlIContains() {
        assertUrlI(SpecText.Type.CONTAINS);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url StartsWith [Ignorecase] [<Data>]",input = InputType.YES)
    public void assertUrlIStartsWith() {
        assertUrlI(SpecText.Type.STARTS);
    }

    @Action(object = ObjectType.MOBILE,desc = "Assert if Browser's Url EndsWith [Ignorecase] [<Data>]",input = InputType.YES)
    public void assertUrlIEndsWith() {
        assertUrlI(SpecText.Type.ENDS);
    }

    private String getMessage(SpecText.Type type) {
        return String.format("%s's Url %s %s ", ObjectName, type.toString(), Data);
    }
*/
}
