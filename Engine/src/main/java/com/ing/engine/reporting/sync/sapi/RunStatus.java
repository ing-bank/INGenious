
package com.ing.engine.reporting.sync.sapi;


/**
 *
 * 
 */
public class RunStatus {

   
    public RunStatus() {
        
    }

    public int noTests;
    public int nopassTests;
    public int nofailTests;
    public int maxThreads;

    public String iterationMode;
    public String runConfiguration;
    public String startTime;
    public String runName;

    public Object data;
}
