
package com.ing.ide.util.browser;

import java.awt.Component;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 *
 *
 */
public class JavaFXBrowser implements Browser {

    private final JFXPanel fxPanel = new JFXPanel();
    private WebEngine webEngine;
    WebView webView;

    @Override
    public void load() {
        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                webView = new WebView();
                fxPanel.setScene(new Scene(webView));
                webEngine = webView.getEngine();
            }
        });
    }

    @Override
    public Component getComponent() {
        return fxPanel;
    }

    @Override
    public void setUrl(final String url) {
        Platform.runLater(() -> webEngine.load(url));
    }

    @Override
    public void quit() {
    }

    @Override
    public void back() {
        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                if (webEngine.getHistory().getCurrentIndex() > 0) {
                    webEngine.getHistory().go(- 1);
                }
            }
        });
    }

    @Override
    public void next() {
        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                if (webEngine.getHistory().getMaxSize() > webEngine.getHistory().getCurrentIndex()) {
                    webEngine.getHistory().go(1);
                }
            }
        });
    }

    @Override
    public String getUrl() {
        return webEngine.getLocation();
    }

}
