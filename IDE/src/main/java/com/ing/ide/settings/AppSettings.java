
package com.ing.ide.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class AppSettings {

    private static final File APPSETT = new File("Configuration" + File.separator + "app.settings");
    private static Properties settings;

    public enum APP_SETTINGS {
        THEME("theme", "Indigo"),
        THEMES("themes", "Orange,Indigo,Fuchsia,Sky"),
        ADDON_PORT("addonPort", "8887"),
        HAR_PORT("harPort", "11234"),
        DEF_LOG_LEVEL("defaultLogLevel", "INFO"),
        SHOW_DATE_TIME("showDateTime", "true"),
        DATE_TIME_FORMAT("dateTimeFormat", "yyyy-MM-dd HH:mm:ss:SSS Z"),
        LOG_BACKUP_LOC("logBackupLoc", "backup/log"),
        LOG_FILE("logfile", "log.txt"),
        MAX_FILE_SIZE("maxFileSize", "4.5"),
        DEFAULT_WAIT_TIME("defaultWaitTime", "10"),
        ELEMENT_WAIT_TIME("elementWaitTime", "10"),
        LOAD_RECENT("loadRecentProject", "true"),
        STANDALONE_REPORT("standaloneReport", "false"),
        HELP_DOC("helpdoc", "https://theforge.ing.net/product/239813/documentation/latest/index");

        private final String key;
        private final String val;

        APP_SETTINGS(String key, String val) {
            this.key = key;
            this.val = val;
        }

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }

        public static String getByKey(String key) {
            for (APP_SETTINGS value : APP_SETTINGS.values()) {
                if (value.getKey().equals(key)) {
                    return value.getVal();
                }
            }
            return "";
        }

    }

    public static void load() {
        try {
            settings = new Properties();
            if (new File(APPSETT.getAbsolutePath()).exists()) {
                settings.load(new FileInputStream(APPSETT.getAbsolutePath()));
            } else {
                for (APP_SETTINGS value : APP_SETTINGS.values()) {
                    settings.put(value.getKey(), value.getVal());
                }
                store("Created");
            }
        } catch (IOException ex) {
            Logger.getLogger(AppSettings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void set(String key, String val) {
        check();
        settings.put(key, val);
    }

    public static String get(String key) {
        check();
        if (settings.containsKey(key)) {
            return settings.getProperty(key);
        }
        return getDefault(key);
    }

    public static String getDefault(String key) {
        String val = APP_SETTINGS.getByKey(key);
        settings.put(key, val);
        return val;
    }

    public static void store(String cmnt) {
        check();
        try {
            settings.store(new FileOutputStream(APPSETT.getAbsolutePath()), cmnt);
        } catch (IOException ex) {
            Logger.getLogger(AppSettings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static Boolean canOpenRecentProjects() {
        return Boolean.valueOf(AppSettings.get(APP_SETTINGS.LOAD_RECENT.getKey()));
    }

    public static void openRecentProjectsOnLaunch(Boolean value) {
        AppSettings.set(APP_SETTINGS.LOAD_RECENT.getKey(), String.valueOf(value));
    }

    public static String getHelpLoc() {
        return AppSettings.get(APP_SETTINGS.HELP_DOC.getKey());
    }

    private static void check() {
        if (settings == null) {
            load();
        }
    }

}
