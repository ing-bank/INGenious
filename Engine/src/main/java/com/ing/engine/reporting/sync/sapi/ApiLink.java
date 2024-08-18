
package com.ing.engine.reporting.sync.sapi;

import com.ing.engine.constants.SystemDefaults;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * 
 */
public class ApiLink {

    private final RunStatus status;
    private final SAPIClient client;

    public ApiLink() throws MalformedURLException {
        status = new RunStatus();
        status.nopassTests = status.nofailTests = 0;      
        client = new SAPIClient(SystemDefaults.EnvVars);

    }
    

    public void setNoTests(int nos) {
        status.noTests = nos;
    }

    public void passed() {
        status.nopassTests++;
    }

    public void failed() {
        status.nofailTests++;
    }

    public void update() {
        try {
            client.put(client.url, new ObjectMapper().writeValueAsString(status));
        } catch (Exception ex) {
            Logger.getLogger(ApiLink.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void update(int pass, int fail) {
        status.nopassTests = pass;
        status.nofailTests = fail;
        update();
    }

    public void setThreads(int threadCount) {
        status.maxThreads = threadCount;
    }

    public void setStartTime(String runTime) {
        status.startTime = runTime;
    }

    public void setRunName(String runName) {
        status.runName = runName;
    }

    public void setExeMode(String mode) {
        status.runConfiguration = mode;
    }

    public void setIterMode(String mode) {
        status.iterationMode = mode;
    }

    public void clientData(JSONObject testSetData) {
        status.data = testSetData;
    }

}
