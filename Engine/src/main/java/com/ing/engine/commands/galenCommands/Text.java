
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

/**
 *
 *
 */
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

    private void assertElementTextI(Type type) {
        checkElementTypeBeforeProcessing();
        SpecText spec = SpecReader.reader().getSpecText(type, Data.toLowerCase());
        spec.setOperations(Arrays.asList(new String[]{"lowercase"}));
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

    @Action(object = ObjectType.MOBILE,
            desc = "Assert if [<Object>]'s Text Equals [<Data>]",
            input = InputType.YES)
    public void assertElementTextEquals() {
       if(Element != null)
       {
          if(Element.getText().equals(Data)){
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] Equals to Expected Text ["+Data+"]", Status.PASS);  
          }
          else{
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] is not Equals to Expected Text ["+Data+"]", Status.FAIL); 
          }
             
       }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Assert if [<Object>]'s Text Contains [<Data>]",
            input = InputType.YES)
    public void assertElementTextContains() {
       if(Element != null)
       {
          if(Element.getText().contains(Data)){
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] Contains Expected Text ["+Data+"]", Status.PASS);  
          }
          else{
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] is not Contains Expected Text ["+Data+"]", Status.FAIL); 
          }
             
       }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Assert if [<Object>]'s Text StartsWith [<Data>]",
            input = InputType.YES)
    public void assertElementTextStartsWith() {
        if(Element != null)
       {
          if(Element.getText().startsWith(Data)){
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] Starts with Expected Text ["+Data+"]", Status.PASS);  
          }
          else{
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] is not Starts with Expected Text ["+Data+"]", Status.FAIL); 
          }
             
       }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Assert if [<Object>]'s Text EndsWith [<Data>]",
            input = InputType.YES)
    public void assertElementTextEndsWith() {
        if(Element != null)
       {
          if(Element.getText().endsWith(Data)){
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] Ends with Expected Text ["+Data+"]", Status.PASS);  
          }
          else{
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] is not Ends with Expected Text ["+Data+"]", Status.FAIL); 
          }
             
       }
    }

    @Action(object = ObjectType.MOBILE,
            desc = "Assert if [<Object>]'s Text Matches [<Data>]",
            input = InputType.YES)
    public void assertMElementTextMatches() {
         if(Element != null)
       {
          if(Element.getText().equalsIgnoreCase(Data)){
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] Matches Expected Text ["+Data+"]", Status.PASS);  
          }
          else{
             Report.updateTestLog(Action, " Element Text [" + Element.getText()+"] is not Matches with Expected Text ["+Data+"]", Status.FAIL); 
          }
             
       }
    }

    private String getMessage(Type type) {
        return String.format("%s's Text %s %s ", ObjectName, type.toString(), Data);
    }
}
