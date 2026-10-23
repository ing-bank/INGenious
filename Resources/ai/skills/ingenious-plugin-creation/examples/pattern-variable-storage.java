@Action(object = ObjectType.PLAYWRIGHT, 
        desc = "Store [<Object>] text in variable [<Data>]", 
        input = InputType.YES)
public void storeElementTextinVariable() {
    try {
        String text = Locator.textContent();
        String variableName = Data;
        
        if (!variableName.matches("%.*%")) {
            Report.updateTestLog(Action, 
                "Variable format incorrect. Expected: %variableName%", 
                Status.FAIL);
            return;
        }
        
        gen.addVar(variableName, text);
        Report.updateTestLog(Action, 
            "Stored text '" + text + "' in variable " + variableName, 
            Status.DONE);
    } catch (PlaywrightException e) {
        Report.updateTestLog(Action, 
            "Error: " + e.getMessage(), 
            Status.FAIL);
    }
}
