
package com.ing.ide.main.help;

import com.ing.ide.main.utils.Utils;
import com.ing.ide.settings.AppSettings;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 *
 */
public class Help {

    public static void openHelp() {
        Help.openInBrowser("Couldn't Open Help in default Browser", asURI(AppSettings.getHelpLoc()));
    }

    public static void openSchedulerHelp() {
        Help.openInBrowser(
                "Couldn't Open Help in default Browser",
                asURI(AppSettings.getHelpLoc() + "/faq/thingsushdknow/index.html#how-to-schedule-tasks-with-ingenious"));
    }

    public static void openEnvBasedExec() {
        Help.openInBrowser(
                "Couldn't Open Help in default Browser",
                asURI(AppSettings.getHelpLoc() + "/faq/thingsushdknow/index.html#environment-based-execution"));
    }

    public static void openTMHelp() {
        Help.openInBrowser(
                "Couldn't Open Help in default Browser",
                asURI(AppSettings.getHelpLoc() + "/faq/thirdpartytool/index.html#how-to-configure-your-test-management-tool"));
    }

    private static URI asURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            Logger.getLogger(Help.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * If possible this method opens the default browser to the specified web
     * page. If not it notifies the user of webpage's url so that they may
     * access it manually.
     *
     * @param message Error message to display
     * @param uri
     */
    public static void openInBrowser(String message, URI uri) {
        try {
            java.util.logging.Logger.getLogger(Help.class.getName()).log(Level.INFO, "Opening url {0}", uri);
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            } else {
                throw new UnsupportedOperationException("Desktop Api Not supported in this System");
            }
        } catch (Exception e) {
            java.util.logging.Logger.getLogger(Help.class.getName()).log(Level.WARNING, null, e);
            // Copy URL to the clipboard so the user can paste it into their browser
            Utils.copyTextToClipboard(uri.toString());
            // Notify the user of the failure
            JOptionPane.showMessageDialog(null, message + "\n"
                    + "The URL has been copied to your clipboard, simply paste into your browser to access.\n"
                    + "Webpage: " + uri);
        }
    }

}
