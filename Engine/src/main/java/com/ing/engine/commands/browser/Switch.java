package com.ing.engine.commands.browser;

import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.engine.core.Control;
import com.ing.util.encryption.Encryption;
import com.microsoft.playwright.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;

public class Switch extends Command {

    public Switch(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Switch to new Page", input = InputType.NO)
    public void clickAndSwitchToNewPage() {
        try {
            Page.WaitForPopupOptions options = new Page.WaitForPopupOptions();
            options.setTimeout(getTimeoutValue());

            Page popup = Page.waitForPopup(options, () -> {
                Locator.click();
            });

            BrowserContext = popup.context();
            AObject.setPage(popup);
            Page = popup;
            Page.bringToFront();
            Driver.setPage(popup);
            Report.updateTestLog(Action, "Successfully switched to new Page", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to new Page", input = InputType.YES)
    public void createAndSwitchToNewPage() {
        try {
            Page.NavigateOptions options = new Page.NavigateOptions();
            options.setTimeout(getTimeoutValue());
            Page page = BrowserContext.newPage();
            page.navigate(Data, options);
            AObject.setPage(page);
            Page = page;
            Page.bringToFront();
            Driver.setPage(page);

            Report.updateTestLog(Action, "Successfully switched to new Page with URL: " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to new Browser Context", input = InputType.YES, condition = InputType.YES)
    public void createAndSwitchToNewContext() {
        try {
            Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();
            Page.NavigateOptions options = new Page.NavigateOptions();
            if (!Condition.startsWith("#")) {
                options.setTimeout(getTimeoutValue());
            } else {                
                String contextAlias = Condition.replace("#", "");
                newContextOptions = enhancedContextOptions(newContextOptions,contextAlias);
                options.setTimeout(Double.parseDouble(getSpecificContextValue(contextAlias,"pageTimeout")));
            }
            Browser browser = BrowserContext.browser();
            BrowserContext = browser.newContext(newContextOptions);
            Page = BrowserContext.newPage();
            Page.navigate(Data, options);
            AObject.setPage(Page);
            Page.bringToFront();
            Driver.setPage(Page);

            Report.updateTestLog(Action, "Successfully switched to new Context with URL: " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to Page by index", input = InputType.YES)
    public void switchToPageByIndex() {
        try {
            int index = Integer.parseInt(Data);
            List<Page> pages = BrowserContext.pages();
            AObject.setPage(pages.get(index));
            Page = pages.get(index);
            Page.bringToFront();
            Driver.setPage(pages.get(index));
            Report.updateTestLog(Action, "Successfully switched to Page [" + index + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to Context by index", input = InputType.YES, condition = InputType.OPTIONAL)
    public void switchToContextByIndex() throws InterruptedException {
        try {
            int index = Integer.parseInt(Data);
            List<com.microsoft.playwright.BrowserContext> contexts = BrowserContext.browser().contexts();
            BrowserContext = contexts.get(index);
            Thread.sleep(500);
            int pageIndex = 0;
            if (!Condition.isEmpty()) {
                pageIndex = Integer.parseInt(Condition);
            }
            Page = BrowserContext.pages().get(pageIndex);
            AObject.setPage(Page);
            Page.bringToFront();
            Driver.setPage(Page);
            Report.updateTestLog(Action, "Successfully switched to Context [" + index + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to Context by Page Title", input = InputType.YES, condition = InputType.OPTIONAL)
    public void switchToContextByPageTitle() {
        try {
            List<com.microsoft.playwright.BrowserContext> contexts = BrowserContext.browser().contexts();
            int pageIndex = 0;
            boolean found = false;
            if (!Condition.isEmpty()) {
                pageIndex = Integer.parseInt(Condition);
            }
            for (com.microsoft.playwright.BrowserContext context : contexts) {
                if (context.pages().get(pageIndex).title().contains(Data)) {
                    BrowserContext = context;
                    Page = BrowserContext.pages().get(pageIndex);
                    AObject.setPage(Page);
                    Page.bringToFront();
                    Driver.setPage(Page);
                    found = true;
                    Report.updateTestLog(Action, "Successfully switched to Context with Page title matching [" + Data + "]", Status.DONE);
                    break;
                }
            }
            if (!found) {
                Report.updateTestLog(Action, "Context with Page title matching [" + Data + "] could not be found", Status.FAIL);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to Context by Page URL", input = InputType.YES, condition = InputType.OPTIONAL)
    public void switchToContextByPageURL() {
        try {
            List<com.microsoft.playwright.BrowserContext> contexts = BrowserContext.browser().contexts();
            int pageIndex = 0;
            boolean found = false;
            if (!Condition.isEmpty()) {
                pageIndex = Integer.parseInt(Condition);
            }
            for (com.microsoft.playwright.BrowserContext context : contexts) {
                if (context.pages().get(pageIndex).url().contains(Data)) {
                    BrowserContext = context;
                    Page = BrowserContext.pages().get(pageIndex);
                    AObject.setPage(Page);
                    Page.bringToFront();
                    Driver.setPage(Page);
                    found = true;
                    Report.updateTestLog(Action, "Successfully switched to Context with Page URL matching [" + Data + "]", Status.DONE);
                    break;
                }
            }
            if (!found) {
                Report.updateTestLog(Action, "Context with Page URL matching [" + Data + "] could not be found", Status.FAIL);
            }
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to new Page", input = InputType.NO)
    public void switchToMainPage() {
        try {
            List<Page> pages = BrowserContext.pages();
            AObject.setPage(pages.get(0));
            Page = pages.get(0);
            Page.bringToFront();
            Driver.setPage(pages.get(0));
            Report.updateTestLog(Action, "Successfully switched to main Page", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
            throw new ActionException(e);
        }
    }

    private double getTimeoutValue() {
        double timeout = 30000;
        if (StringUtils.isNotBlank(Condition)) {
            try {
                timeout = Double.parseDouble(Condition.trim());
            } catch (NumberFormatException e) {
                Report.updateTestLog(Action,
                        "'" + Condition + "' cannot be converted to timeout of type Double", Status.DEBUG);
            }
        }
        return timeout;
    }

    private static final String AUTHENTICATE_CONTEXT = "isAuthenticated";
    private static final String USER_ID = "userID";
    private static final String PASSWORD = "password";
    private static final String USE_STORAGE_STATE = "useStorageState";
    private static final String STORAGE_STATE_PATH = "storageStatePath";
    private static final String ENC_SUFFIX = " Enc";

    private Browser.NewContextOptions enhancedContextOptions(Browser.NewContextOptions newContextOptions, String contextAlias) {
        
            Properties contextDetails = getContextDetails(contextAlias);
            configureAuthentication(newContextOptions, contextDetails);
            configureStorageState(newContextOptions, contextDetails);

            List<String> contextOptions = getContextOptions(contextAlias);
            if (contextOptions != null && !contextOptions.isEmpty()) {
                for (String prop : contextOptions) {
                    String[] keyValue = prop.split("=", 2);
                    if (keyValue.length == 2) {
                        configureContextOption(newContextOptions, keyValue[0], keyValue[1]);
                    }
                }
            }

        return newContextOptions;
    }

    private void configureAuthentication(Browser.NewContextOptions newContextOptions, Properties contextDetails) {
        if (Boolean.parseBoolean(contextDetails.getProperty(AUTHENTICATE_CONTEXT))) {
            String userID = handleVariablesOrDatasheet(contextDetails.getProperty(USER_ID));
            String password = handleVariablesOrDatasheet(contextDetails.getProperty(PASSWORD));
            
            if (password.endsWith(ENC_SUFFIX)) {
                password = password.substring(0, password.lastIndexOf(ENC_SUFFIX));
                password = new String(Encryption.getInstance().decrypt(password).getBytes());
            }
            newContextOptions.setHttpCredentials(userID, password);
        }
    }

    private void configureStorageState(Browser.NewContextOptions newContextOptions, Properties contextDetails) {
        if (Boolean.parseBoolean(contextDetails.getProperty(USE_STORAGE_STATE))) {
            String storageStatePath = contextDetails.getProperty(STORAGE_STATE_PATH);
            Path filePath = Paths.get(storageStatePath);
            if (Files.exists(filePath)) {
                System.out.printf("\n========================\nStorage State used: '%s'\n========================\n", storageStatePath);
                newContextOptions.setStorageStatePath(filePath);
            } else {
                System.out.println("\n========================\nStorage State Path does not exist. Skipping setting Storage State\n========================\n");
            }
        }
    }

    private void configureContextOption(Browser.NewContextOptions newContextOptions, String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }

        switch (key.toLowerCase()) {
            case "setgeolocation":
                setGeolocation(newContextOptions, value);
                break;
            case "setviewportsize":
                setViewportSize(newContextOptions, value);
                break;
            case "setdevicescalefactor":
                newContextOptions.setDeviceScaleFactor(Integer.parseInt(value));
                break;
            case "sethastouch":
                newContextOptions.setHasTouch(Boolean.parseBoolean(value));
                break;
            case "setismobile":
                newContextOptions.setIsMobile(Boolean.parseBoolean(value));
                break;
            case "setrecordvideodir":
                newContextOptions.setRecordVideoDir(Paths.get(value));
                break;    
            case "setscreensize":
                setScreenSize(newContextOptions, value);
                break;
            case "setrecordvideosize":
                setRecordVideoSize(newContextOptions, value);
                break;    
            case "setuseragent":
                newContextOptions.setUserAgent(value);
                break;
            case "setlocale":
                newContextOptions.setLocale(value);
                break;
            case "settimezoneid":
                newContextOptions.setTimezoneId(value);
                break;
            case "setoffline":
                newContextOptions.setOffline(Boolean.parseBoolean(value));
                break;
            default:
                // Ignore unknown keys
                break;
        }
    }
    
    private static void setViewportSize(Browser.NewContextOptions newContextOptions, String value) {
        if (value.equals("maximized")) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            newContextOptions.setViewportSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
        } else {
            String[] dimensions = value.split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            newContextOptions.setViewportSize(width, height);
        }
    }
    
    private static void setGeolocation(Browser.NewContextOptions newContextOptions, String value) {
        String[] coordinates = value.split(",");
        double latitude = Double.parseDouble(coordinates[0]);
        double longitude = Double.parseDouble(coordinates[1]);
        newContextOptions.setGeolocation(latitude, longitude).setPermissions(Arrays.asList("geolocation"));
    }

    private static void setScreenSize(Browser.NewContextOptions newContextOptions, String value) {
        String[] dimensions = value.split(",");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        newContextOptions.setScreenSize(width, height);
    }

    private static void setRecordVideoSize(Browser.NewContextOptions newContextOptions, String value) {
        String[] dimensions = value.split(",");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        newContextOptions.setRecordVideoSize(width, height);
    }

    private static List<String> getContextOptions(String contextName) {
        Properties prop = Control.getCurrentProject().getProjectSettings().getContextSettings()
                .getContextOptionsFor(contextName);
        List<String> options = new ArrayList<>();
        if (prop != null) {
            prop.keySet().stream().forEach((key) -> {
                if (prop.getProperty(key.toString()) == null) {
                } else {
                    options.add(key.toString() + "=" + prop.getProperty(key.toString()));
                }
            });
        }
        return options;
    }

    private static Properties getContextDetails(String contextAlias) {
        return Control.getCurrentProject().getProjectSettings().getContextSettings().getContextOptionsFor(contextAlias);
    }
    
    private static String getSpecificContextValue(String contextAlias, String option) {
        Properties contextDetails = Control.getCurrentProject().getProjectSettings().getContextSettings().getContextOptionsFor(contextAlias);
        return contextDetails.getProperty(option);
        
    }
    
    private String handleDataSheet(String value) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (value.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (value.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                        value = value.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col)));
                    }
                }
            }
        }
        return value;
    }

    private String handleUserDefinedVariables(String value) {
        Collection<Object> keys = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().keySet();
        for (Object key : keys) {
            if (value.equals("%"+key+"%")) {   
                return Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty(key.toString());
            }
        }
        return value;
    }
    
    private String handleVariablesOrDatasheet(String value) {
        
        value = handleDataSheet(value);
        value = handleUserDefinedVariables(value);
        
        return value;
    }

}
