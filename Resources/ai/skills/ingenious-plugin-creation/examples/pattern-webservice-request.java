@Action(object = ObjectType.WEBSERVICE, 
        desc = "POST Rest Request", 
        input = InputType.YES)
public void postRestRequest() {
    try {
        gen.createHttpRequest(RequestMethod.POST);
        
        // Update local fields with response
        this.responseCode = gen.ResponseCode();
        this.responseBody = gen.ResponseBody();
        
        // createHttpRequest already logs, this is optional
        // Report.updateTestLog(Action, "POST successful", Status.DONE);
        
    } catch (Exception e) {
        Report.updateTestLog(Action,
            "Error: " + e.getMessage(),
            Status.FAIL);
    }
}
