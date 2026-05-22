/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ing.engine.drivers;

import com.ing.datalib.settings.ProjectSettings;
import com.ing.engine.core.RunContext;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import java.nio.file.Paths;
import java.util.Properties;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 *
 * @author Jayant Borude
 */
public class SAPSessionFactory {
    // work for SAP

    static Process startSAPProcess(RunContext context, ProjectSettings settings) {
        try {
            DesiredCapabilities caps = getCapability(context.BrowserName, settings);
            String appPath = caps.getCapability("app").toString();
            String SAPappPath = Paths.get(appPath).toAbsolutePath().toString();
            Process sapProcess = Runtime.getRuntime().exec(SAPappPath);
            return sapProcess;
        } catch (Exception e) {
            System.out.println("Error in start SAP process : " + e.getMessage());
        }
        return null;
    }

    static ActiveXComponent createSAPSession(RunContext context, ProjectSettings settings) {

        DesiredCapabilities caps = getCapability(context.BrowserName, settings);
        String SAPconnectionName = caps.getCapability("connectionName").toString();
        String dllPath = caps.getCapability("dllPath").toString();
        String libraryPath = caps.getCapability("libraryPath").toString();
        String jcobdllPath = Paths.get(dllPath).toAbsolutePath().toString();
        String javalibraryPath = Paths.get(libraryPath).toAbsolutePath().toString();
        ActiveXComponent session = null;
        try {
            System.setProperty("jacob.dll.path", jcobdllPath);
            System.setProperty("java.library.path", javalibraryPath);
            ComThread.InitSTA(); // Initialize COM thread
            Thread.sleep(7000); // Wait for SAP Logon to load
            ActiveXComponent sapROTWrapper = new ActiveXComponent("SapROTWr.SapROTWrapper");
            Dispatch sapROTEntry = sapROTWrapper.invoke("GetROTEntry", "SAPGUI").toDispatch();
            Variant scriptEngine = Dispatch.call(sapROTEntry, "GetScriptingEngine");
            ActiveXComponent guiApp = new ActiveXComponent(scriptEngine.toDispatch());
            ActiveXComponent connection = new ActiveXComponent(guiApp.invoke("OpenConnection", SAPconnectionName).toDispatch());
            session = new ActiveXComponent(connection.invoke("Children", 0).toDispatch());
        } catch (Exception e) {
            System.out.println("Error in creating SAP session : " + e.getMessage());
        }

        return session;
    }

    private static DesiredCapabilities getCapability(String browserName, ProjectSettings settings) {
        DesiredCapabilities caps = new DesiredCapabilities();
        Properties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        if (prop != null) {
            prop.keySet().stream().forEach((key) -> {
                caps.setCapability(key.toString(),
                        getPropertyValueAsDesiredType(key.toString(), prop.getProperty(key.toString())));
            });
        }
        return caps;
    }

    private static Object getPropertyValueAsDesiredType(String key, String value) {
        if (value != null && !value.isEmpty()) {
            if (value.toLowerCase().matches("(true|false)")) {
                return Boolean.valueOf(value);
            }
            if (value.matches("\\d+")) {
                return Integer.valueOf(value);
            }
        }
        return value;
    }

}
