
package com.ing.notification.slack;

import com.ing.engine.core.Control;
import com.ing.engine.core.RunManager;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Slack {

    static final String WEBHOOK_URL = "WEBHOOK_URL";

  public static boolean sendNotification(byte[] data) {
        boolean isSuccessful = false;
        Boolean sendNotification = Control.getCurrentProject().getProjectSettings().getExecSettings(RunManager.getGlobalSettings().getRelease(), RunManager.getGlobalSettings().getTestSet()).getRunSettings().isSendNotification();
        String slackURL = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty(WEBHOOK_URL);
        boolean useProxy = Control.getCurrentProject().getProjectSettings().getDriverSettings().useProxy();
        if (sendNotification && null != slackURL) {
            try {
                URL url = new URL(slackURL);
                Proxy proxy = null;
                HttpURLConnection connection = null;
                if (useProxy) {
                    String proxyHost = Control.getCurrentProject().getProjectSettings().getDriverSettings().getProperty("proxyHost");
                    String port = Control.getCurrentProject().getProjectSettings().getDriverSettings().getProperty("proxyPort");
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(port)));
                    connection = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    connection = (HttpURLConnection) url.openConnection();
                }
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                OutputStream out = connection.getOutputStream();
                out.write(data);
                out.flush();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Logger.getLogger(Slack.class.getName()).log(Level.SEVERE, "Failed : HTTP error code : {0}", connection.getResponseCode());
                }
                connection.disconnect();
                isSuccessful = true;
            } catch (MalformedURLException ex) {
                Logger.getLogger(Slack.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException | NumberFormatException ex) {
                Logger.getLogger(Slack.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return isSuccessful;
    }
}
