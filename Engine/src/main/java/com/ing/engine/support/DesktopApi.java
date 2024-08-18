
package com.ing.engine.support;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesktopApi {

    private static final Logger LOG = LoggerFactory.getLogger(DesktopApi.class.getName());

    public static boolean browse(URI uri) {
        if (openSystemSpecific(uri.toString())) {
            return true;
        }
        return browseDESKTOP(uri);
    }

    public static boolean open(File file) {
        logOut("Trying to Open - " + file.getAbsolutePath());
        if (openSystemSpecific(file.getAbsolutePath())) {
            return true;
        }
        return openDESKTOP(file);
    }

    public static boolean edit(File file) {

        // you can try something like
        // runCommand("gimp", "%s", file.getPath())
        // based on user preferences.
        if (openSystemSpecific(file.getAbsolutePath())) {
            return true;
        }
        return editDESKTOP(file);
    }

    private static boolean openSystemSpecific(String what) {

        EnumOS os = getOs();

        if (os.isLinux()) {
            if (runCommand("kde-open", "%s", what)) {
                return true;
            }
            if (runCommand("gnome-open", "%s", what)) {
                return true;
            }
            if (runCommand("xdg-open", "%s", what)) {
                return true;
            }
        }

        if (os.isMac()) {
            if (runCommand("open", "%s", what)) {
                return true;
            }
        }

        if (os.isWindows()) {
            if (runCommand("explorer", "%s", what)) {
                return true;
            }
        }

        return false;
    }

    private static boolean browseDESKTOP(URI uri) {

        logOut("Trying to use Desktop.getDesktop().browse() with " + uri.toString());
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.");
                return false;
            }

            if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                logErr("BROWSE is not supported.");
                return false;
            }

            Desktop.getDesktop().browse(uri);

            return true;
        } catch (Throwable t) {
            logErr("Error using desktop browse.", t);
            return false;
        }
    }

    private static boolean openDESKTOP(File file) {

        logOut("Trying to use Desktop.getDesktop().open() with " + file.toString());
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.");
                return false;
            }

            if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                logErr("OPEN is not supported.");
                return false;
            }

            Desktop.getDesktop().open(file);

            return true;
        } catch (Throwable t) {
            logErr("Error using desktop open.", t);
            return false;
        }
    }

    private static boolean editDESKTOP(File file) {

        logOut("Trying to use Desktop.getDesktop().edit() with " + file);
        try {
            if (!Desktop.isDesktopSupported()) {
                logErr("Platform is not supported.");
                return false;
            }

            if (!Desktop.getDesktop().isSupported(Desktop.Action.EDIT)) {
                logErr("EDIT is not supported.");
                return false;
            }

            Desktop.getDesktop().edit(file);

            return true;
        } catch (Throwable t) {
            logErr("Error using desktop edit.", t);
            return false;
        }
    }

    private static boolean runCommand(String command, String args, String file) {

        logOut(String.format("Trying to exec: [%s] [%s] [%s]", command,args,file));

        String[] parts = prepareCommand(command, args, file);
        Process p = null;
        try {
            //String location =
            if(!Pattern.matches("[0-9A-Za-z@.]+", parts.toString()))
             p = Runtime.getRuntime().exec(parts);
            if (p == null) {
                logErr("Process Not Created.");
                return false;
            }

            try {
                int retval = p.exitValue();
                if (retval == 0) {
                    logErr("Process ended immediately.");
                    return false;
                } else {
                    logErr("Process crashed.");
                    return false;
                }
            } catch (IllegalThreadStateException itse) {
                logErr("Process is running.", itse);
                return true;
            }
        } catch (IOException e) {
            logErr("Error running command.", e);
            return false;
        }
    }

    private static String[] prepareCommand(String command, String args, String file) {

        List<String> parts = new ArrayList<>();
        parts.add(command);

        if (args != null) {
            for (String s : args.split(" ")) {
                s = String.format(s, file); // put in the filename thing

                parts.add(s.trim());
            }
        }

        return parts.toArray(new String[parts.size()]);
    }

    private static void logErr(String msg, Throwable t) {
        LOG.debug(msg, t);
    }

    private static void logErr(String msg) {
        LOG.error(msg);
    }

    private static void logOut(String msg) {
        LOG.info(msg);
    }

    public static enum EnumOS {

        linux, macos, solaris, unknown, windows;

        public boolean isLinux() {

            return this == linux || this == solaris;
        }

        public boolean isMac() {

            return this == macos;
        }

        public boolean isWindows() {

            return this == windows;
        }
    }

    public static EnumOS getOs() {

        String s = System.getProperty("os.name").toLowerCase();

        if (s.contains("win")) {
            return EnumOS.windows;
        }

        if (s.contains("mac")) {
            return EnumOS.macos;
        }

        if (s.contains("solaris")) {
            return EnumOS.solaris;
        }

        if (s.contains("sunos")) {
            return EnumOS.solaris;
        }

        if (s.contains("linux")) {
            return EnumOS.linux;
        }

        if (s.contains("unix")) {
            return EnumOS.linux;
        } else {
            return EnumOS.unknown;
        }
    }
}
