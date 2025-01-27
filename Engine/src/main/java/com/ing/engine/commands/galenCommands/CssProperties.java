
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecCss;
import com.galenframework.specs.SpecText.Type;

public class CssProperties extends General {

    public CssProperties(CommandControl cc) {
        super(cc);
    }
/*
    private void assertElementCss(Type type) {
        SpecCss spec = SpecReader.reader().getSpecCSS(type, Data);
        spec.setOriginalText(getMessage(spec));
        validate(spec);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Css Property Equals [<Data>]", input = InputType.YES)
    public void assertElementCssPropEquals() {
        assertElementCss(Type.IS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Css Property Contains [<Data>]", input = InputType.YES)
    public void assertElementCssPropContains() {
        assertElementCss(Type.CONTAINS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Css Property StartsWith [<Data>]", input = InputType.YES)
    public void assertElementCssPropStartsWith() {
        assertElementCss(Type.STARTS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Css Property EndsWith [<Data>]", input = InputType.YES)
    public void assertElementCssPropEndsWith() {
        assertElementCss(Type.ENDS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Css Property Matches [<Data>]", input = InputType.YES)
    public void assertElementCssPropMatches() {
        assertElementCss(Type.MATCHES);
    }
    private String getMessage(SpecCss spec) {
        return String.format("%s's CssProperty %s %s %s ", ObjectName, spec.getCssPropertyName(), spec.getType().toString(), spec.getText());
    }
*/
}
