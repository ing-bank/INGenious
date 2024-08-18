
package com.ing.ide.util.browser;

import com.ing.engine.support.DesktopApi;
import java.io.File;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/**
 *
 * 
 */
public class HelpBrowser extends JavaFXBrowser {

    private ContextMenu contextMenu;
    private MenuItem open;

    @Override
    public void load() {
        super.load();
        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                webView.setContextMenuEnabled(false);
                contextMenu = new ContextMenu();
                open = new MenuItem("Open in browser");
                addActionListener();
                addContextMenuListener();
                contextMenu.getItems().addAll(open);
            }
        });

    }

    private void addActionListener() {
        open.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                DesktopApi.open(new File("Help" + File.separator + "index.html"));
            }
        });
    }

    private void addContextMenuListener() {
        webView.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouse) {
                if (mouse.getButton() == MouseButton.SECONDARY) {
                    contextMenu.show(webView, mouse.getScreenX(), mouse.getScreenY());
                } else {
                    contextMenu.hide();
                }
            }
        });
    }
}
