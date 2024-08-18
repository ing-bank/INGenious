
package com.ing.engine.reporting.sync.azure;

import java.io.File;
import java.util.List;

/**
 *
 * 
 */
public class AzureTestData {
    
    String project;
    int testPlanId;
    String suite;
    String testcase;
    String status;
    List<File> attach;

    public AzureTestData(String project, int testPlanId, String suite, String testcase, String status, List<File> attach){
        this.project = project;
        this.testPlanId = testPlanId;
        this.suite = suite;
        this.testcase = testcase;
        this.status = status;
        this.attach = attach;
    }
    
}
