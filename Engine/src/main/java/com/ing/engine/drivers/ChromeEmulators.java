
package com.ing.engine.drivers;

import com.ing.engine.constants.FilePath;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SystemUtils;

public class ChromeEmulators {

    private static final Logger LOG = Logger.getLogger(ChromeEmulators.class.getName());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> EMULATORS = new ArrayList<>();

    private static void load() {
        if (EMULATORS.isEmpty()) {
            try {
                LOG.info("Loading ChromeEmulators from file");
                File file = new File(FilePath.getChromeEmulatorsFile());
                if (file.exists()) {
                    EMULATORS.addAll(MAPPER.readValue(file, List.class));
                } else {
                    sync();
                }
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void saveList() {
        if (!EMULATORS.isEmpty()) {
            try {
                MAPPER.writeValue(new File(FilePath.getChromeEmulatorsFile()), EMULATORS);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void sync() {
        try {
            LOG.info("Trying to load emulators from Chrome Installation");
            File file = new File(getPrefLocation(), "Preferences");
            if (file.exists()) {
                Map map = MAPPER.readValue(file, Map.class);
                Map devtools = (Map) map.get("devtools");
                Map prefs = (Map) devtools.get("preferences");
                String stdemulators = (String) prefs.get("standardEmulatedDeviceList");
                List list = MAPPER.readValue(stdemulators, List.class);
                EMULATORS.clear();
                EMULATORS.addAll(
                        (List<String>) list.stream()
                        .map((device) -> {
                            return ((Map) device).get("title");
                        })
                        .map((val) -> val.toString())
                        .collect(Collectors.toList()));
                saveList();
            } else {
                LOG.severe("Either Chrome is not installed or OS not supported");
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * As per Chromium
     * https://www.chromium.org/user-experience/user-data-directory
     * @return Preferences location
     */
    public static String getPrefLocation() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return SystemUtils.getUserHome().getAbsolutePath() + "/AppData/Local/Google/Chrome/User Data/Default";
        }
        if (SystemUtils.IS_OS_MAC_OSX) {
            return SystemUtils.getUserHome().getAbsolutePath()+"/Library/Application Support/Google/Chrome/Default";
        }
        if (SystemUtils.IS_OS_LINUX) {
            return SystemUtils.getUserHome().getAbsolutePath()+"/.config/google-chrome/Default";
        }
        return "OSNotConfigured";
    }

    public static List<String> getEmulatorsList() {
        load();
        return EMULATORS;
    }
}
