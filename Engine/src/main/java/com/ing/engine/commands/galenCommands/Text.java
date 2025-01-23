package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.SpecValidation.SpecReader;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.galenframework.specs.SpecText;
import com.galenframework.specs.SpecText.Type;
import com.ing.engine.support.Status;
import java.util.Arrays;
import org.openqa.selenium.support.ui.Select;

public class Text extends General {

    public Text(CommandControl cc) {
        super(cc);
    }

    private void assertElementText(Type type) {
        checkElementTypeBeforeProcessing();
        SpecText spec = SpecReader.reader().getSpecText(type, Data);
        spec.setOriginalText(getMessage(type));
        validate(spec);
    }

    private void checkElementTypeBeforeProcessing() {
        if (Element != null) {
            if (Element.getTagName().equalsIgnoreCase("select")) {
                Select select = new Select(Element);
                Element = select.getFirstSelectedOption();
                System.out.println("As it is Select Element assserting "
                        + "the text of first selected Element");
            }
        }
    }

    private String getMessage(Type type) {
        return String.format("%s's Text %s %s ", ObjectName, type.toString(), Data);
    }

    @Action(object = ObjectType.APP,desc = "Assert if [<Object>]'s Text Equals [<Data>]",input = InputType.YES)
    public void assertElementTextEquals() {
        assertElementText(Type.IS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Text Contains [<Data>]", input = InputType.YES)
    public void assertElementTextContains() {
        assertElementText(Type.CONTAINS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Text StartsWith [<Data>]", input = InputType.YES)
    public void assertElementTextStartsWith() {
        assertElementText(Type.STARTS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Text EndsWith [<Data>]", input = InputType.YES)
    public void assertElementTextEndsWith() {
        assertElementText(Type.ENDS);
    }

    @Action(object = ObjectType.APP, desc = "Assert if [<Object>]'s Text Matches [<Data>]", input = InputType.YES)
    public void assertElementTextMatchesWith() {
        assertElementText(Type.MATCHES);
    }
}
