
package com.ing.datalib.settings;

/**
 *
 *
 */
public class RunSettings extends AbstractPropSettings {

    public RunSettings(String location) {
        super(location, "RunSettings");
    }

    public void setExecutionMode(String value) {
        setProperty("ExecutionMode", value);
    }

    public String getExecutionMode() {
        return getProperty("ExecutionMode", "Local");
    }

    public Boolean isGridExecution() {
        return getExecutionMode().equalsIgnoreCase("grid");
    }

    public void useExistingDriver(Boolean value) {
        setProperty("ExistingDriver", String.valueOf(value));
    }

    public Boolean useExistingDriver() {
        return Boolean.valueOf(getProperty("ExistingDriver", "false"));
    }

    public void setExecutionTimeOut(String value) {
        setProperty("ExecutionTimeOut", value);
    }

    public Integer getExecutionTimeOut() {
        return Integer.valueOf(getProperty("ExecutionTimeOut", "300"));
    }

    public void setScreenShotFor(String value) {
        setProperty("ScreenShotFor", value);
    }

    public String getScreenShotFor() {
        return getProperty("ScreenShotFor", "Both");
    }

    public void setThreadCount(String value) {
        setProperty("ThreadCount", value);
    }

    public Integer getThreadCount() {
        return Integer.valueOf(getProperty("ThreadCount", "1"));
    }

    public void setTakeFullPageScreenShot(Boolean value) {
        setProperty("TakeFullPageScreenShot", String.valueOf(value));
    }

    public Boolean getTakeFullPageScreenShot() {
        return Boolean.valueOf(getProperty("TakeFullPageScreenShot", "true"));
    }

    public void setReportPerformanceLog(Boolean value) {
        setProperty("reportPerformanceLog", String.valueOf(value));
    }

    public Boolean isPerformanceLogEnabled() {
        return Boolean.valueOf(getProperty("reportPerformanceLog", "false"));
    }
    
    public void setVideoEnabled(Boolean value) {
        setProperty("recordVideo", String.valueOf(value));
    }

    public Boolean isVideoEnabled() {
        return Boolean.valueOf(getProperty("recordVideo", "false"));
    }
    
    public void setTracingEnabled(Boolean value) {
        setProperty("enableTracing", String.valueOf(value));
    }
    
    public Boolean isTracingEnabled() {
        return Boolean.valueOf(getProperty("enableTracing", "false"));
    }

    public void setHARrecordingEnabled(Boolean value) {
        setProperty("enableHAR", String.valueOf(value));
    }

    public Boolean isHARrecordingEnabled() {
        return Boolean.valueOf(getProperty("enableHAR", "false"));
    }
    
    
    public void setBddReport(Boolean value) {
        setProperty("bddReport", String.valueOf(value));
    }

    public Boolean isBddReportEnabled() {
        return Boolean.valueOf(getProperty("bddReport", "false"));
    }

    public void setRemoteGridURL(String value) {
        setProperty("RemoteGridURL", value);
    }

    public String getRemoteGridURL() {
        return getProperty("RemoteGridURL", "http://localhost:4444/wd/hub");
    }

    public void setIterationMode(String value) {
        setProperty("IterationMode", value);
    }

    public String getIterationMode() {
        return getProperty("IterationMode", "ContinueOnError");
    }

    public void setRerunTimes(String value) {
        setProperty("RerunTimes", value);
    }

    public Integer getRerunTimes() {
        return Integer.valueOf(getProperty("RerunTimes", "0"));
    }

    public void setTestEnv(String value) {
        setProperty("TestEnv", value);
    }

    public String getTestEnv() {
        return getProperty("TestEnv", "Default");
    }

    public Boolean isMailSend() {
        return Boolean.valueOf(getProperty("SendMail", "false"));
    }

    public void setMailSend(Boolean value) {
        setProperty("SendMail", String.valueOf(value));
    }

    public Boolean isExcelReport() {
        return Boolean.valueOf(getProperty("excelReport", "false"));
    }

    public void setExcelReport(Boolean value) {
        setProperty("excelReport", String.valueOf(value));
    }
    
    public Boolean isRPUpdate() {
        return Boolean.valueOf(getProperty("ReportPortal", "false"));
    }

    public void setRPUpdate(Boolean value) {
        setProperty("ReportPortal", String.valueOf(value));
    }
    
    public Boolean isAutoHealEnabled() {
        return Boolean.valueOf(getProperty("AutoHealMode", "false"));
    }
    
    public void setAutoHealMode(Boolean value) {
        setProperty("AutoHealMode", String.valueOf(value));
    }
    
    public Boolean isExtentReport() {
        return Boolean.valueOf(getProperty("ExtentReport", "false"));
    }
    
    public Boolean isAzureEnabled() {
        return Boolean.valueOf(getProperty("AzureReport", "false"));
    }

    public void setExtentReport(Boolean value) {
        setProperty("ExtentReport", String.valueOf(value));
    }
    
    public void setAzureReport(Boolean value) {
        setProperty("AzureReport", String.valueOf(value));
    }
    
    public Boolean isSendNotification(){
        return Boolean.valueOf(getProperty("slacknotify", "false"));
    }
    
    public void setSlackNotification(Boolean value) {
        setProperty("slacknotify", String.valueOf(value));
    }
}
