package com.ing.engine.commands;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.*;
import java.util.List;

public class Switch extends Command {

    public Switch(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.PLAYWRIGHT, desc = "Switch to new Page", input = InputType.NO)
    public void clickAndSwitchToNewPage() {
        try {
            Page popup = Page.waitForPopup(() -> {
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
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to new Page", input = InputType.YES)
    public void createAndSwitchToNewPage() {
        try {
            Page page = BrowserContext.newPage();
            page.navigate(Data);
            AObject.setPage(page);
            Page = page;
            Page.bringToFront();
            Driver.setPage(page);

            Report.updateTestLog(Action, "Successfully switched to new Page with URL: " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.BROWSER, desc = "Switch to new Browser Context", input = InputType.YES)
    public void createAndSwitchToNewContext() {
        try {
            /*
        Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();
        newContextOptions.setHttpCredentials(userName, Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty(userName));
             */

            Browser browser = BrowserContext.browser();
            BrowserContext = browser.newContext();
            Page = BrowserContext.newPage();
            Page.navigate(Data);
            AObject.setPage(Page);
            Page.bringToFront();
            Driver.setPage(Page);

            Report.updateTestLog(Action, "Successfully switched to new Context with URL: " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, "Something went wrong" + e.getMessage(), Status.DEBUG);
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
        }
    }

}
