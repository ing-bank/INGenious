package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.commands.galenCommands.Text;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ByLabel extends Command {
    CommandControl cc; //Commander

    public ByLabel(CommandControl cc) {
        super(cc);
        this.cc = cc; //Commander
    }

    @Action(
        object = ObjectType.APP,
        desc = "Set the data [<Data>] to an input element that is adjacent to the provided label element [<Object>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void setInputByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Basic(cc).Set();
    }

    @Action(
        object = ObjectType.APP,
        desc = "Tap on an element whose label is provided in the [<Object>]"
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void TapInputByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Basic(cc).Tap();
    }

    @Action(
        object = ObjectType.MOBILE,
        desc = "Tap on the element whose label is provided in the [<Input>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void TapInputByText() {
        cc.Element = findInputElementByLabelTextByXpath(Data); //Another variant
        new Basic(cc).Tap();
    }

    @Action(
        object = ObjectType.APP,
        desc = "Submit input element adjacent to the provided label element [<Object>]"
    )
    @Args(
        inputHelp = "no input required (e.g. leave empty)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void submitInputByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Basic(cc).Submit();
    }

    @Action(
        object = ObjectType.APP,
        desc = "Assert if [<Object>]'s Text adjacent to provided label element Equals [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementTextByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Text(cc).assertElementTextEquals(); //Create object for the necessary Class[Text as it has the assertElementTextEquals etc and call you desired method[assertElementTextEquals]
    }

    @Action(
        object = ObjectType.APP,
        desc = "Assert if [<Object>]'s Text adjacent to provided label element Contains [<Data>]",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "@value",
        inputHelp = "input value (e.g. @value)",
        condition = ConditionKind.NONE,
        conditionHelp = "no condition required (e.g. leave empty)"
    )
    public void assertElementTextContainsByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Text(cc).assertElementTextContains();
    }

    private WebElement findInputElementByLabelTextByXpath() {
        return findInputElementByLabelTextByXpath(Element.getText());
    }

    private WebElement findInputElementByLabelTextByXpath(String text) {
        return mDriver.findElement(By.xpath("//*[text()='" + text + "']/following::input[1]"));
    }
}
