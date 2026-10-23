@Action(object = ObjectType.APP, desc = "Tap on [<Object>]")
public void Tap() {
    if (gen.elementEnabled()) {
        Element.click();
        Report.updateTestLog(Action, 
            "Tapped on " + ObjectName, 
            Status.DONE);
    } else {
        throw new ElementException(
            ExceptionType.Element_Not_Enabled, 
            ObjectName);
    }
}
