@Action(object = ObjectType.BROWSER, 
        desc = "Open URL [<Data>] with optional timeout [<Condition>]", 
        input = InputType.YES, 
        condition = InputType.OPTIONAL)
public void Open() {
    try {
        Page.NavigateOptions options = new Page.NavigateOptions();
        
        // Optional timeout from Condition field
        if (Condition != null && !Condition.isEmpty() && Condition.matches("[0-9]+")) {
            options.setTimeout(Double.parseDouble(Condition) * 1000);
        }
        
        Page.navigate(Data, options);
        Report.updateTestLog(Action, "Opened " + Data, Status.DONE);
        
    } catch (TimeoutError e) {
        if (Condition != null && !Condition.isEmpty()) {
            Report.updateTestLog(Action, 
                "Opened URL but cancelled after " + Condition + " seconds", 
                Status.DONE);
        } else {
            Report.updateTestLog(Action, "Page load timed out", Status.FAIL);
        }
    }
}
