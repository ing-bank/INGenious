package com.ing.ide.main.ui;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class About {
    private static final String DETAILS =
        "<html>\n" +
        "	<body>\n" +
        "			<strong>Build Version</strong>: ##bversion## \n" +
        "			<br />\n" +
        "			<strong>Build Date</strong>: ##bdate##\n" +
        "			<br />\n" +
        "			<strong>Java</strong>: ##jversion##\n" +
        "			<br />\n" +
        "			<strong>Installation directory</strong>: ##insdir##\n" +
        "	</body>\n" +
        "</html>";

    private static Properties buildProperties;

    public static void init() {
        load();
    }

    /**
     * Lazily load build.properties on first access so callers (e.g. the
     * startup banner) don't have to depend on {@link #init()} having run.
     */
    private static synchronized Properties load() {
        if (buildProperties == null) {
            Properties p = new Properties();
            try (
                java.io.InputStream in = About.class.getResourceAsStream(
                        "/ui/resources/build.properties"
                    )
            ) {
                if (in != null) {
                    p.load(in);
                }
            } catch (IOException ex) {
                Logger.getLogger(About.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
            buildProperties = p;
        }
        return buildProperties;
    }

    public static String getDetailsAsHTML() {
        return DETAILS
            .replace("##bversion##", getBuildVersion())
            .replace("##bdate##", getBuildDate())
            .replace("##jversion##", getJavaVersion())
            .replace("##insdir##", getRoot());
    }

    public static String getBuildVersion() {
        String v = load().getProperty("Bundle-Version");
        return v != null ? v : "dev";
    }

    public static String getBuildDate() {
        return load().getProperty("Build-Date");
    }

    public static String getJavaVersion() {
        return System.getProperty("java.home");
    }

    public static String getRoot() {
        return System.getProperty("user.dir");
    }
}
