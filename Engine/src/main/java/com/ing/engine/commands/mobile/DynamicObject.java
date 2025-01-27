package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.MobileObject;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.HashMap;
import java.util.Map;

public class DynamicObject extends Command {

    public DynamicObject(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.MOBILE, desc = "Set  all objects property to [<Data>] at runtime.", input = InputType.YES, condition = InputType.YES)
    public void setMobileglobalObjectProperty() {
        if (!Data.isEmpty()) {
            if (Condition.isEmpty()) {
                String[] groups = Data.split(",");
                for (String group : groups) {
                    String[] vals = group.split("=", 2);
                    MobileObject.globalDynamicValue.put(vals[0], vals[1]);
                }
            } else {
                MobileObject.globalDynamicValue.put(Condition, Data);
            }
            String text = String.format("Setting Global Object Property for %s with %s", Condition, Data);
            Report.updateTestLog(Action, text, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
        }
    }

    @Action(object = ObjectType.APP, desc = "Set object [<Object>] property  as [<Data>] at runtime", input = InputType.YES, condition = InputType.YES)
    public void setMobileObjectProperty() {
        if (!Data.isEmpty()) {
            if (Condition.isEmpty()) {
                String[] groups = Data.split(",");
                for (String group : groups) {
                    String[] vals = group.split("=", 2);
                    setProperty(vals[0], vals[1]);
                }
            } else {
                setProperty(Condition, Data);
            }
            String text = String.format("Setting Object Property for %s with %s for Object [%s - %s]",
                    Condition, Data, Reference, ObjectName);
            Report.updateTestLog(Action, text, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
        }
    }

    private void setProperty(String key, String value) {
        if (!MobileObject.dynamicValue.containsKey(Reference)) {
            Map<String, Map<String, String>> Object = new HashMap<>();
            Map<String, String> property = new HashMap<>();
            property.put(key, value);
            Object.put(ObjectName, property);
            MobileObject.dynamicValue.put(Reference, Object);
        } else if (!MobileObject.dynamicValue.get(Reference).containsKey(ObjectName)) {
            Map<String, String> property = new HashMap<>();
            property.put(key, value);
            MobileObject.dynamicValue.get(Reference).put(ObjectName, property);
        } else {
            MobileObject.dynamicValue.get(Reference).get(ObjectName).put(key, value);
        }
    }
}
