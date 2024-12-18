package com.ing.engine.commands.browser;

import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.util.encryption.Encryption;
import com.microsoft.playwright.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

	@Action(object = ObjectType.BROWSER, desc = "Switch to new Browser Context", input = InputType.YES, condition = InputType.OPTIONAL)
	public void createAndSwitchToNewContext() {
		try {
			/*
			 * Browser.NewContextOptions newContextOptions = new
			 * Browser.NewContextOptions(); newContextOptions.setHttpCredentials(userName,
			 * Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().
			 * getProperty(userName));
			 */

			Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();
			String contextAlias = Condition;
			if (contextAlias.startsWith("#")) {
				contextAlias = contextAlias.replace("#", "");

				LinkedProperties contextDetails = getContextDetails(contextAlias);
				Boolean isContextAuthenticated = Boolean.valueOf(contextDetails.getProperty("authenticateContext"));
				Boolean useStorageState = Boolean.valueOf(contextDetails.getProperty("useStorageState"));
				if (isContextAuthenticated) {
					String userID = contextDetails.getProperty("userID");
					String password = contextDetails.getProperty("password");

					if (password.endsWith(" Enc")) {
						password = password.substring(0, password.lastIndexOf(" Enc"));
						byte[] valueDecoded = Encryption.getInstance().decrypt(password).getBytes();
						password = new String(valueDecoded);
					}
					newContextOptions.setHttpCredentials(userID, password);
				}
				if (useStorageState) {
					String storageStatePath = contextDetails.getProperty("storageStatePath");

					Path filePath = Paths.get(storageStatePath);
					if (filePath.toFile().exists()) {
						System.out.println("\n" + "========================" + "\n" + "Storage State used : +'"
								+ storageStatePath + "'" + "\n" + "========================" + "\n");
						newContextOptions.setStorageStatePath(filePath);
					} else
						System.out.println("\n" + "========================" + "\n"
								+ "Storage State Path does not exist. Skipping setting Storage State" + "\n"
								+ "========================" + "\n");

				}

				List<String> contextOptions = getContextOptions(contextAlias);
				if (!contextOptions.isEmpty()) {
					for (int i = 0; i < contextOptions.size(); i++) {
						String prop = contextOptions.get(i);
						String key = prop.split("=")[0];
						if (key.toLowerCase().contains("authenticatecontext")
								|| key.toLowerCase().contains("usestoragestate")
								|| key.toLowerCase().contains("storagestatepath")
								|| key.toLowerCase().contains("userid") || key.toLowerCase().contains("password")) {
						} else {
							String value = prop.split("=")[1];

							if (key.toLowerCase().contains("setgeolocation")) {
								Double latitude = Double.valueOf(value.split(",")[0]);
								Double longitude = Double.valueOf(value.split(",")[1]);
								newContextOptions.setGeolocation(latitude, longitude)
										.setPermissions(Arrays.asList("geolocation"));
							}

							if (key.toLowerCase().contains("setviewportsize")) {
								int width = Integer.parseInt(value.split(",")[0]);
								int height = Integer.parseInt(value.split(",")[1]);
								newContextOptions.setViewportSize(width, height);
							}
							if (key.toLowerCase().contains("startmaximized")) {
								newContextOptions.setViewportSize(null);
							}
							if (key.toLowerCase().contains("setdevicescalefactor")) {
								int factor = Integer.parseInt(value);
								newContextOptions.setDeviceScaleFactor(factor);
							}

							if (key.toLowerCase().contains("sethastouch")) {
								newContextOptions.setHasTouch(Boolean.parseBoolean(value));
							}

							if (key.toLowerCase().contains("setismobile")) {
								newContextOptions.setIsMobile(Boolean.parseBoolean(value));
							}

							if (key.toLowerCase().contains("setscreensize")) {
								int width = Integer.parseInt(value.split(",")[0]);
								int height = Integer.parseInt(value.split(",")[1]);
								newContextOptions.setScreenSize(width, height);
							}

							if (key.toLowerCase().contains("setuseragent")) {
								newContextOptions.setUserAgent(value);
							}

							if (key.toLowerCase().contains("setlocale")) {
								newContextOptions.setLocale(value);
							}

							if (key.toLowerCase().contains("settimezoneid")) {
								newContextOptions.setTimezoneId(value);
							}

							if (key.toLowerCase().contains("setoffline")) {
								newContextOptions.setOffline(Boolean.parseBoolean(value));
							}
						}
					}
				}
			}
			Page.NavigateOptions options = new Page.NavigateOptions();
			options.setTimeout(getTimeoutValue());
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

	private static List<String> getContextOptions(String contextName) {
		Properties prop = Control.getCurrentProject().getProjectSettings().getContextOption()
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

	private static LinkedProperties getContextDetails(String contextAlias) {
		return Control.getCurrentProject().getProjectSettings().getContextOption().getContextOptionsFor(contextAlias);
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
					Report.updateTestLog(Action,
							"Successfully switched to Context with Page title matching [" + Data + "]", Status.DONE);
					break;
				}
			}
			if (!found) {
				Report.updateTestLog(Action, "Context with Page title matching [" + Data + "] could not be found",
						Status.FAIL);
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
					Report.updateTestLog(Action,
							"Successfully switched to Context with Page URL matching [" + Data + "]", Status.DONE);
					break;
				}
			}
			if (!found) {
				Report.updateTestLog(Action, "Context with Page URL matching [" + Data + "] could not be found",
						Status.FAIL);
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
		if (Condition != null || !Condition.isEmpty()) {
			if (!Condition.startsWith("#")) {
				try {
					timeout = Double.parseDouble(Condition.trim());
				} catch (NumberFormatException e) {
					Report.updateTestLog(Action, "'" + Condition + "' cannot be converted to timeout of type Double",
							Status.DEBUG);
				}
			}
		}
		return timeout;
	}
}
