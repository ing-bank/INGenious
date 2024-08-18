
package com.ing.engine.reporting.sync.azure;

import com.ing.engine.constants.FilePath;
import com.ing.engine.support.DLogger;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * 
 */
public class AzureClient {

    private static final Logger LOGGER = Logger.getLogger(AzureClient.class.getName());
    private final AzureHttpClient httpClient;
    public String serverUrl;
    final String PAT;
    URL url;

    public AzureClient(String url, String PAT,Map config) {
        this.setUrl(url);
        this.PAT = PAT;
        httpClient = new AzureHttpClient(getUrl(serverUrl), PAT,config);
    }

    private void setUrl(String url) {
        try {
            if (!url.endsWith("/")) {
                url = url.concat("/");
            }
            this.url = new URL(url);
            serverUrl = url;
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private URL getUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String buildUrl(String rest) {
        return serverUrl.concat(rest);
    }

    private JSONObject getTestPoint(JSONArray testpoints, String testcaseTitle) {
        for (Object v : testpoints) {
            for (Object wiProps : (JSONArray) ((JSONObject) v).get("workItemProperties")) {
                JSONObject wi = (JSONObject) ((JSONObject) wiProps).get("workItem");
                if (wi.get("key").equals("System.Title") && wi.get("value").equals(testcaseTitle)) {
                    return (JSONObject) v;
                }
            }
        }
        return null;
    }

    private int getTestSuiteId(String project, int testPlanId, String suite) {
        try {
            JSONObject res = httpClient.Get(getUrl(buildUrl(getTestPlanUrl(project, testPlanId)
                    + "/suites?api-version=1.0")));

            for (Object i : (JSONArray) res.get("value")) {
                JSONObject suiteSet = (JSONObject) i;
                if (suiteSet.get("name").equals(suite)) {
                    return Integer.parseInt(suiteSet.get("id").toString());
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public boolean containsProject(String project) {

        try {
            String res = httpClient.Get(new URL(httpClient.url + "_apis/projects?api-version=5.0")).toString();
            return res.contains("\"name\":\"" + project + "\"");

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public boolean isConnected() {
        try {
            DLogger.Log(httpClient.Get(new URL(httpClient.url
                    + "_apis/projects?api-version=5.0")).toString());
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private int getTestPointId(String project, int testPlanId, String suite, String testCase) {

        try {
            JSONObject res = httpClient.Get(getUrl(buildUrl(getTestPlanUrl(project, testPlanId)
                    + "/suites/" + getTestSuiteId(project, testPlanId, suite)
                    + "/points?witFields=System.Title")));
            JSONObject testpoint = getTestPoint((JSONArray) res.get("value"), testCase);
            if (Objects.nonNull(testpoint)) {
                return Integer.parseInt(testpoint.get("id").toString());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    private String getTestPlanUrl(String project, int testPlanId) {
        return project + "/_apis/test/plans/" + testPlanId;
    }

    private int getResultId(String project, String testcase, int runId) {
        try {
            JSONObject res = httpClient.Get(getUrl(buildUrl(project + "/_apis/test/runs/" + runId + "/results")));

            for (Object r : (JSONArray) res.get("value")) {
                JSONObject run = (JSONObject) r;
                if (((JSONObject) run.get("testCase")).get("name").equals(testcase)) {
                    return Integer.parseInt(run.get("id").toString());
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    public void createNewTestRun(ArrayList<AzureTestData> listOTest) {
        try {
            String testPoints = listOTest.stream()
                    .filter(td -> td.testPlanId > 0)
                    .map(td -> getTestPointId(td.project, td.testPlanId, td.suite, td.testcase))
                    .map(String::valueOf)
                    .collect(joining(","));
            LOGGER.log(Level.INFO, "Conneting Azure DevOps to update results");
            JSONObject res = httpClient.post(getUrl(buildUrl(listOTest.get(0).project + "/_apis/test/runs?api-version=5.0")),
                    "{\"name\": \"" + FilePath.getCurrentReportFolderName()
                    + "\", \"plan\": { \"id\": " + listOTest.get(0).testPlanId
                    + " }, \"pointIds\": [ " + testPoints + " ] }");

            int runId = Integer.parseInt(res.get("id").toString());
            listOTest.stream()
                    .filter(td -> td.testPlanId > 0)
                    .forEach(test -> {
                        System.out.print(String.format(
                                "Azure DevOps: updating //%s/%s result(%s) with %s attachments... ",
                                test.suite, test.testcase, test.status, test.attach.size()));
                        updateResults(test.project, runId,
                                getResultId(test.project, test.testcase, runId),
                                test.status, test.attach);
                    });
            updateRunStatus(listOTest.get(0).project, runId);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateRunStatus(String project, int runId){
        try {
            httpClient.patch(getUrl(buildUrl(project + "/_apis/test/runs/"
                    + runId + "?api-version=6.0")), "{ \"state\": \"Completed\"}");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    public void updateResults(String project, int runId, int resultId, String status, List<File> attach) {

        try {
            httpClient.patch(getUrl(buildUrl(project + "/_apis/test/runs/"
                    + runId + "/results?api-version=6.0-preview.6")), "[{ \"id\": " + resultId
                    + ", \"state\": \"Completed\", \"outcome\": \"" + status + "\"}]");
            for (File f : attach) {
                sendAttachment(project, runId, f, resultId);
            }
           // System.out.println("Done!");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void sendAttachment(String project, int runId, File attach, int resultId) {

        try {
            byte[] encodedBytes = Base64.getEncoder().encode(FileUtils.readFileToByteArray(attach));
            String content = new String(encodedBytes);
            httpClient.post(getUrl(buildUrl(project + "/_apis/test/Runs/"
                    + runId + "/Results/" + resultId + "/attachments?api-version=6.0-preview.1")),
                    "{ \"stream\": \"" + content + "\", \"fileName\": \"" + attach.getName() + "\"}");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

}
