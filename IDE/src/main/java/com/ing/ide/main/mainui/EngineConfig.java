
package com.ing.ide.main.mainui;

import com.ing.datalib.component.Project;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.Control;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.ide.settings.AppSettings;
import com.ing.ide.settings.AppSettings.APP_SETTINGS;
import com.ing.ide.util.logging.UILogger;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class EngineConfig {

    public static void runProject(Project project) {
        try {
            Duration defWaitTime = Duration.ofSeconds(Integer.valueOf(AppSettings.get(APP_SETTINGS.DEFAULT_WAIT_TIME.getKey())));
            Duration elementWaitTime = Duration.ofSeconds(Integer.valueOf(AppSettings.get(APP_SETTINGS.ELEMENT_WAIT_TIME.getKey())));
            SystemDefaults.elementWaitTime = (elementWaitTime);
            SystemDefaults.waitTime = (defWaitTime);
            Boolean standAlone = Boolean.valueOf(AppSettings.get(APP_SETTINGS.STANDALONE_REPORT.getKey()));
            if (standAlone) {
                SystemDefaults.CLVars.put("createStandaloneReport", "true");
            } else {
                SystemDefaults.CLVars.remove("createStandaloneReport");
            }
            Control.call(project);
            UILogger.get().revertToDefault();
        } catch (UnCaughtException ex) {
            Logger.getLogger(EngineConfig.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
