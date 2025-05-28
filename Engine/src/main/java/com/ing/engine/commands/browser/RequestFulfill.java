package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.*;
import com.microsoft.playwright.Page.GetByRoleOptions;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.execution.exception.ActionException;
import java.io.FileNotFoundException;
import java.util.Collection;

public class RequestFulfill extends Command {
    public RequestFulfill(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.BROWSER, desc = "Set Endpoint for mocking request", input = InputType.YES)
    public void RouteFulfillEndpoint() {
        try {
            String resource = handlePayloadorEndpoint(Data);
            mockEndPoints.put(key, resource);
            Report.updateTestLog(Action, "End point set : " + resource, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error setting the end point :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Set body for mocking request", input = InputType.YES)
    public void RouteFulfillSetBody() {
        try {
            Route.FulfillOptions fulfillOptions = new Route.FulfillOptions();
            Page.route(mockEndPoints.get(key), route -> {
                try {
                    route.fulfill(fulfillOptions.setBody(handlePayloadorEndpoint(Data)));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(RequestFulfill.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error setting the body :" + "\n" + ex.getMessage(), Status.DEBUG);
            throw new ActionException(ex);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Block Request", input = InputType.NO)
    public void RouteAbort() {
        try {
            Page.route(mockEndPoints.get(key), route -> {
                try {
                    route.abort();
                    Report.updateTestLog(Action, "Route Aborted", Status.DONE);
                } catch (Exception ex) {

                    Logger.getLogger(RequestFulfill.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error while aborting the Route :" + "\n" + ex.getMessage(), Status.DEBUG);
            throw new ActionException(ex);
        }
    }

    private String handlePayloadorEndpoint(String data) throws FileNotFoundException {
        String payloadstring = data;
        payloadstring = handleDataSheetVariables(payloadstring);
        payloadstring = handleuserDefinedVariables(payloadstring);
        System.out.println("Payload :" + payloadstring);
        return payloadstring;
    }

    private String handleDataSheetVariables(String payloadstring) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (payloadstring.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (payloadstring.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                        payloadstring = payloadstring.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col)));
                    }
                }
            }
        }
        return payloadstring;
    }

    private String handleuserDefinedVariables(String payloadstring) {
        Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                .values();
        for (Object prop : valuelist) {
            if (payloadstring.contains("{" + prop + "}")) {
                payloadstring = payloadstring.replace("{" + prop + "}", prop.toString());
            }
        }
        return payloadstring;
    }

}
