@Action(object = ObjectType.PLAYWRIGHT, 
        desc = "Click on [<Object>]")
public void Click() {
    try {
        highlightElement();
        Locator.click();
        Report.updateTestLog(Action, 
            "Clicked on '" + ObjectName + "'", 
            Status.DONE);
    } catch (PlaywrightException e) {
        Report.updateTestLog(Action, 
            "Element not found: " + e.getMessage(), 
            Status.FAIL);
    } finally {
        removeHighlightFromElement();
    }
}

private void highlightElement() {
    Locator.scrollIntoViewIfNeeded();
    Locator.evaluate("element => element.style.outline = '2px solid red'");
}

private void removeHighlightFromElement() {
    Locator.evaluate("element => element.style.outline = ''");
}
