
package com.ing.ide.main.explorer.settings;

import com.ing.ide.util.Notification;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 *
 * 
 */
public class Settings {

    final static FileSystemView FSV = null;
    public static final JFileChooser FC_IMG_EDITOR = new JFileChooser("", FSV);
    public static final String[] SETTINGS = {"ImageEditor", "EditorArguments", "DefectModule", "URL", "UserName", "Password", "Domain", "Project"};
    public static final File CONFIG_FILE = new File(getConfigFileLoc());
    static Properties prop = new Properties();
    static Map<String, String> settingsMap = new LinkedHashMap();
    public static final CharSequence FILE_ARGS = "#file";
    static String screenshotLoc = ".", scriptLoc = ".";

    static {
        updateSettings();
    }

    static String getConfigFileLoc() {
        try {
            return getAppDir() + File.separator + "Configuration" + File.separator + "ExplorerConfig.properties";
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static String getDefectModule() {
        return settingsMap.get(SETTINGS[2]);
    }

    static void saveSettings(Map<String, String> settingsMap) {
        try {
            checkFile(CONFIG_FILE);
            StringBuilder sb = new StringBuilder();
            for (String key : settingsMap.keySet()) {
               // prop.setProperty(key, settingsMap.get(key));
               sb.append(key+"="+settingsMap.get(key)+"\n");
            }
           BufferedWriter writer = new BufferedWriter(new FileWriter(CONFIG_FILE));
           writer.write(sb.toString());
           writer.close();
           
          //  prop.store(new FileWriter(CONFIG_FILE), "Exploratory Module Settings!!");
            Notification.show("Settings Successfully Saved!!");
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        updateSettings();
    }

    public static String getSettings(String key) {
        return settingsMap.get(key);
    }

    public static Map<String, String> updateSettings() {
        settingsMap.clear();
        try {
            checkFile(CONFIG_FILE);
            prop.load(new FileReader(CONFIG_FILE));
            for (Object key : prop.keySet()) {
                settingsMap.put(key.toString(), prop.getProperty(key.toString()));
            }

        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        return settingsMap;
    }

    public static Map<String, String> getSettings() {
        return settingsMap;
    }

    private static String getAppDir() throws IOException {
        return new File(System.getProperty("user.dir")).getCanonicalPath();
    }

    private static void checkFile(File configFile) {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static String getScreenShotLoc() {
        return screenshotLoc;
    }

    public static String getScriptLoc() {
        return scriptLoc;
    }

    public static void setScreenShotLoc(String screenshotLoc) {
        Settings.screenshotLoc = screenshotLoc;
    }

    public static void setScriptLoc(String scriptLoc) {
        Settings.scriptLoc = scriptLoc;
    }

    public static String getImageEditor() {
        return settingsMap.get(SETTINGS[0]);
    }

    public static String getImageEditorArgs() {
        return settingsMap.get(SETTINGS[1]);
    }

}
