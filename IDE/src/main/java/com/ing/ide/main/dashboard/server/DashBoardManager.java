
package com.ing.ide.main.dashboard.server;

import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class DashBoardManager {

    public DashBoardServer server;

    AppMainFrame sMainFrame;

    public DashBoardManager(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    public DashBoardServer server() {
        if (server == null || !server.isAlive()) {
            server = new DashBoardServer();
            server.prepare();
            server.start();
        }
        return server;
    }

    public void stopServer() {
        if (server != null && server.isAlive()) {
            server.stopServer();
        }
    }

    public void onProjectChanged() {
        try {
            DashBoardData.setProjLocation(sMainFrame.getProject().getLocation());
            HarCompareHandler.init();
        } catch (Exception ex) {
            Logger.getLogger(DashBoardManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void openHarComapareInBrowser() {
        String add = server().url() + "/dashboard/harCompare/home.html";
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URL(add).toURI());
            } catch (URISyntaxException | IOException ex) {
                Logger.getLogger(DashBoardManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
