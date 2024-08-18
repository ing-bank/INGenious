
package com.ing.engine.reporting.sync.azure;

import com.ing.engine.reporting.sync.Sync;
import com.ing.engine.reporting.util.TestInfo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 *
 */
public class AzureSync implements Sync {

    private AzureClient conn;
    private final ArrayList<AzureTestData> listOTest = new ArrayList<>();
    private String project = "";
    private int testPlanId;
    private static final Logger LOG = Logger.getLogger(AzureSync.class.getName());

    public AzureSync(String server, String PAT, String project, int testPlanId, Map config) {
        conn = new AzureClient(server, PAT, config);
        this.project = project;
        this.testPlanId = testPlanId;
    }

    /**
     *
     * @param options
     */
    public AzureSync(Properties options) {
        this(options.getProperty("AzureDevOps URL"), options.getProperty("PersonalAccessToken"),
                options.getProperty("AzureDevOps Project"), Integer.valueOf(options.getProperty("AzureDevOps TestPlanId")),
                options);
    }

    @Override
    public boolean isConnected() {
        try {
            return conn.isConnected() && conn.containsProject(project);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    @Override
    public void disConnect() {
        conn.createNewTestRun(listOTest);
        conn = null;
    }

    @Override
    public String getModule() {
        return "Azure DevOps Test Plan";
    }

    @Override
    public boolean updateResults(TestInfo tc, String status, List<File> attach) {
        AzureTestData test = new AzureTestData(project, testPlanId, tc.testScenario, tc.testCase, status, attach);
        listOTest.add(test);
        return true;
    }

    @Override
    public String createIssue(JSONObject issue, List<File> attach) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
