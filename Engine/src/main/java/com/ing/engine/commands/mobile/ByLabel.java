package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.commands.galenCommands.Text;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ByLabel extends Command {

    CommandControl cc;//Commander

    public ByLabel(CommandControl cc) {
        super(cc);
        this.cc = cc;//Commander
    }

    @Action(object = ObjectType.APP,
            desc = "Set the data [<Data>] to an input element that is adjacent to the provided label element [<Object>]",
            input = InputType.YES)
    public void setInputByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Basic(cc).Set();
    }

    @Action(object = ObjectType.APP,
            desc = "Tap on an element whose label is provided in the [<Object>]"
    )
    public void TapInputByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Basic(cc).Tap();
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Tap on the element whose label is provided in the [<Input>]",
            input = InputType.YES)
    public void TapInputByText() {
        cc.Element = findInputElementByLabelTextByXpath(Data);//Another variant
        new Basic(cc).Tap();
    }

    @Action(object = ObjectType.APP, desc = "Submit input element adjacent to the provided label element [<Object>]")
    public void submitInputByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Basic(cc).Submit();
    }

    @Action(object = ObjectType.APP,
            desc = "Assert if [<Object>]'s Text adjacent to provided label element Equals [<Data>]",
            input = InputType.YES)
    public void assertElementTextByLabel() {
        cc.Element = findInputElementByLabelTextByXpath();
        new Text(cc).assertElementTextEquals();//Create object for the necessary Class[Text as it has the assertElementTextEquals etc and call you desired method[assertElementTextEquals]
    }

    @Action(object = ObjectType.APP,
            desc = "Assert if [<Object>]'s Text adjacent to provided label element Contains [<Data>]",
            input = InputType.YES)
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
