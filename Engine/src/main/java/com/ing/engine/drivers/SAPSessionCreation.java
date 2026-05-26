
package com.ing.engine.drivers;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.execution.exception.UnCaughtException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jacob.activeX.ActiveXComponent;
/**
 *
 * @author Jayant Borude
 */
public class SAPSessionCreation {
	protected RunContext runContext;
	public ActiveXComponent session;
	public Process SAPProcess;
	
	/**
	 * Checks if the browser is a SAP-based browser.
	 * A browser is considered SAP-based if its name is "SAP" (case-insensitive).
	 * 
	 * @param browserName the name of the browser to check
	 * @return true if the browser is SAP-based, false otherwise
	 */
	public static boolean isSAPBrowser(String browserName) {
		return "SAP".equalsIgnoreCase(browserName);
	}
	
	/**
	 * Checks if SAP emulator is configured in the project settings.
	 * 
	 * @param browserName the name of the browser
	 * @return true if SAP emulator is configured, false otherwise
	 */
	private boolean isSAPEmulatorConfigured(String browserName) {
		try {
			Emulator emulator = Control.getCurrentProject().getProjectSettings().getEmulators()
					.getEmulator(browserName);
			return emulator != null;
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
			return false;
		}
	}

	public void launchSession(RunContext context) throws UnCaughtException {
		runContext = context;
		String browserName = runContext.BrowserName;
		
		// Validate that this is a SAP browser
		if (!isSAPBrowser(browserName)) {
			System.out.println("[SAP] Browser '" + browserName + "' is not SAP-based. Skipping SAP session initialization.");
			session = null;
			SAPProcess = null;
			return;
		}
		
		// Validate that SAP emulator is configured
		if (!isSAPEmulatorConfigured(browserName)) {
			System.out.println("[SAP] SAP emulator '" + browserName + "' is not configured. Skipping SAP session initialization.");
			session = null;
			SAPProcess = null;
			return;
		}
		
		System.out.println("Launching SAP session for " + browserName);
		
		try {
			SAPProcess = SAPSessionFactory.startSAPProcess(context, Control.getCurrentProject().getProjectSettings());
			session = SAPSessionFactory.createSAPSession(context, Control.getCurrentProject().getProjectSettings());		
		} catch (UnCaughtException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
			throw new UnCaughtException("[SAP Session Exception] Cannot initialize SAP session for '" + browserName + "': " + ex.getMessage());
		}
	}

	public void launchDriver(String browser) throws UnCaughtException {
		RunContext context = new RunContext();
		context.BrowserName = browser;
//		context.Browser = Browser.fromString(browser);
//		context.Platform = Platform.getCurrent();
		context.BrowserVersion = "default";
		launchSession(context);
	}

	public String getCurrentBrowser() {
		return runContext.BrowserName;
	}

	public String getCurrentBrowserVersion() {
		if (runContext != null && runContext.BrowserVersion != null) {
			return runContext.BrowserVersion;
		}
		return "NA";
	}

	public String getPlatform() {
		// Try to get platform from emulator configuration
		try {
			Emulator emulator = Control.getCurrentProject().getProjectSettings().getEmulators()
					.getEmulator(runContext.BrowserName);
			if (emulator != null) {
				String type = emulator.getType();
				if (type != null && !type.isEmpty()) {
					return type;
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
		}
		// Fallback to system platform information
		return System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
	}

	public String getDriverName(String browserName) {
		try {
			Emulator emulator = Control.getCurrentProject().getProjectSettings().getEmulators()
					.getEmulator(browserName);
			if (emulator != null) {
				return emulator.getDriver();
			}
		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
		}
		return browserName;
	}


}
