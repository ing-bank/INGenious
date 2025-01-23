
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.galenWrapper.SpecValidation.SpecTitle;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecText;
import java.util.Arrays;

/**
 *
 * 
 */
public class Title extends General {

    public Title(CommandControl cc) {
        super(cc);
    }
/*
    private void assertTitle(SpecText.Type type) {
        SpecTitle spec = SpecReader.reader().getSpecTitle(type, Data);
        spec.setOriginalText(getMessage(type));
        validate(spec, RelativeElement.None);
    }

    private void assertTitleI(SpecText.Type type) {
        SpecTitle spec = SpecReader.reader().getSpecTitle(type, Data.toLowerCase());
        spec.setOperations(Arrays.asList(new String[]{"lowercase"}));
        spec.setOriginalText(getMessage(type));
        validate(spec, RelativeElement.None);
    }

    @Action(object = ObjectType.MOBILE, desc ="Assert if Browser's Title Equals [<Data>]", input =InputType.YES)
    public void assertTitleEquals() {
        assertTitle(SpecText.Type.IS);
    }

    @Action(object = ObjectType.MOBILE, desc ="Assert if Browser's Title Contains [<Data>]", input =InputType.YES)
    public void assertTitleContains() {
        assertTitle(SpecText.Type.CONTAINS);
    }

    @Action(object = ObjectType.MOBILE, desc ="Assert if Browser's Title StartsWith [<Data>]", input =InputType.YES)
    public void assertTitleStartsWith() {
        assertTitle(SpecText.Type.STARTS);
    }

    @Action(object = ObjectType.MOBILE, desc ="Assert if Browser's Title EndsWith [<Data>]", input =InputType.YES)
    public void assertTitleEndsWith() {
        assertTitle(SpecText.Type.ENDS);
    }

    @Action(object = ObjectType.MOBILE,desc ="Assert if Browser's Title Matches [<Data>]",input =InputType.YES)
    public void assertTitleMatches() {
        assertTitle(SpecText.Type.MATCHES);
    }

    @Action(object = ObjectType.MOBILE,desc ="Assert if Browser's Title Equals [Ignorecase] [<Data>]",input =InputType.YES)
    public void assertTitleIEquals() {
        assertTitleI(SpecText.Type.IS);
    }

    @Action(object = ObjectType.MOBILE,desc ="Assert if Browser's Title Contains [Ignorecase] [<Data>]",input =InputType.YES)
    public void assertTitleIContains() {
        assertTitleI(SpecText.Type.CONTAINS);
    }

    @Action(object = ObjectType.MOBILE,desc ="Assert if Browser's Title StartsWith [Ignorecase] [<Data>]",input =InputType.YES)
    public void assertTitleIStartsWith() {
        assertTitleI(SpecText.Type.STARTS);
    }

    @Action(object = ObjectType.MOBILE, desc ="Assert if Browser's Title EndsWith [Ignorecase] [<Data>]",input =InputType.YES)
    public void assertTitleIEndsWith() {
        assertTitleI(SpecText.Type.ENDS);
    }

   
    private String getMessage(SpecText.Type type) {
        return String.format("%s's Title %s %s ", ObjectName, type.toString(), Data);
    }
*/
}
