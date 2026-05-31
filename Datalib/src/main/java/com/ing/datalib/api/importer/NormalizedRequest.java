package com.ing.datalib.api.importer;

import com.ing.datalib.api.APIRequest;
import java.io.Serializable;
import java.util.List;

/**
 * A single request in the normalized import model. The request is already expressed
 * in INGenious {@link APIRequest} form so the mapper can hand it straight to
 * {@code APITester.convertRequestToReusable}. The {@link #folderPath} captures
 * where in the source folder hierarchy the request was located.
 */
public class NormalizedRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Folder path components from the collection root to this request's parent folder. */
    private List<String> folderPath;
    private APIRequest request;
    /** Original test/post-response script (best-effort translation already applied where possible). */
    private String testScript;
    /** Original pre-request script (Phase 1: captured for reporting only). */
    private String preRequestScript;

    public NormalizedRequest() {}

    public NormalizedRequest(List<String> folderPath, APIRequest request) {
        this.folderPath = folderPath;
        this.request = request;
    }

    public List<String> getFolderPath() { return folderPath; }
    public void setFolderPath(List<String> folderPath) { this.folderPath = folderPath; }

    public APIRequest getRequest() { return request; }
    public void setRequest(APIRequest request) { this.request = request; }

    public String getTestScript() { return testScript; }
    public void setTestScript(String testScript) { this.testScript = testScript; }

    public String getPreRequestScript() { return preRequestScript; }
    public void setPreRequestScript(String preRequestScript) { this.preRequestScript = preRequestScript; }
}
