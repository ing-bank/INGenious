
package com.ing.engine.reporting.impl.html.bdd;

import java.util.List;

public class Report {

    public String projectName;
    public String releaseName;
    public String testsetName;
    public String iterationMode;
    public String runConfiguration;
    public String maxThreads;
    public String startTime;
    public String endTime;
    public String exeTime;
    public String noTests;
    public String nopassTests;
    public  String nofailTests;
    public String theme;
    public List<String> themes;
    public String testRun;
    public List<Execution> EXECUTIONS;

    public List<Execution> getEXECUTIONS() {
        return EXECUTIONS;
    }

    public static class Execution {

        String scenarioName;

        public String getScenarioName() {
            return scenarioName;
        }

        public String getExeTime() {
            return exeTime;
        }

        public String getStatus() {
            return status;
        }

        public String testcaseName;
        public String description;
        public String iterations;

        public List<IterData> getIterData() {
            return STEPS;
        }

        public String iterationType;
        public String platform;
        public  String bversion;
        public  String startTime;
        public String endTime;
        public  String exeTime;
        public String noTests;
        public String nopassTests;
        public String nofailTests;
        public String browser;
        public String status;
        public List<IterData> STEPS;
		public char[] getScenarioName;

    }

    public static class IterData {

    	public String name;
    	public String type;

    	public String startTime;
    	public String endTime;
    	public String description;
    	public List<Step> data;

        public List<Step> getSteps() {
            return data;
        }

    }

    public static class Step {

    	public String name;
    	public String type;
    	public String description;
    	public String status;
    	public String startTime;
    	public String endTime;
    	public Object data;

        public String getStatus() {
            return this.status;
        }

       public enum StepInfo {
            stepno, stepName, action, description, status, tStamp, link,
            expected, actual, comparison, objects;
        }

    }

    public static class Data {

    	public String stepno;
        String stepName;
        String action;
        String description;
        String status;
        String tStamp;
        String link;
        String expected;
        String actual;
        String comparison;
        String objects;
        
        public String getDescription() {
            return description;
        }

        public String getLink() {
            return link;
        }
        
    }
}
