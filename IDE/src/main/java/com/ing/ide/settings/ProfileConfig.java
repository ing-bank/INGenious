package com.ing.ide.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and saves the user profile (System Under Test + PCode)
 * to Configuration/userProfile.properties.
 *
 * The file is resolved relative to the working directory, matching
 * AppSettings (Configuration/app.settings). When the IDE is launched
 * from the release folder this lands in Dist/release/Configuration/.
 */
public class ProfileConfig {
    private static final Logger LOG = Logger.getLogger(ProfileConfig.class.getName());

    private static final File PROFILE_FILE = new File(
        "Configuration" + File.separator + "userProfile.properties"
    );

    public static final String KEY_SUT = "SUT";
    public static final String KEY_PCODE = "pcode";

    private static Properties props;

    private ProfileConfig() {}

    private static void check() {
        if (props == null) {
            load();
        }
    }

    private static void load() {
        props = new Properties();
        if (PROFILE_FILE.exists()) {
            try (FileInputStream in = new FileInputStream(PROFILE_FILE)) {
                props.load(in);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Failed to load profile.properties", ex);
            }
        }
    }

    public static String getSut() {
        check();
        return props.getProperty(KEY_SUT, "");
    }

    public static String getPcode() {
        check();
        return props.getProperty(KEY_PCODE, "");
    }

    public static boolean isConfigured() {
        check();
        return !getSut().trim().isEmpty() || !getPcode().trim().isEmpty();
    }

    /**
     * Saves the profile. Creates the Configuration folder if missing.
     */
    public static void save(String sut, String pcode) {
        check();
        props.setProperty(KEY_SUT, sut == null ? "" : sut.trim());
        props.setProperty(KEY_PCODE, pcode == null ? "" : pcode.trim());
        File parent = PROFILE_FILE.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream out = new FileOutputStream(PROFILE_FILE)) {
            props.store(out, "INGenious User Profile");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to save profile.properties", ex);
            throw new RuntimeException("Could not save profile.properties", ex);
        }
    }
}
