package com.ing.ide.main.playwrightrecording;

import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.util.Notification;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ClipboardMonitor {

    private static String lastContent = "";
    
    private final AppMainFrame sMainFrame;
    
    private volatile boolean running = false;

    public ClipboardMonitor(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    public void startMonitoring() {
        if (running) return;
        running = true;
        
        System.out.println("Clipboard monitoring: " + running);
        Thread monitorThread = new Thread(() -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                try {
                    Transferable contents = clipboard.getContents(null);
                    if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        lastContent = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    lastContent = "";
                }

                String projectLocation = sMainFrame.getProject().getLocation();
                Path recordingsDir = Paths.get(projectLocation, "Recording");

                if (!Files.exists(recordingsDir)) {
                    Files.createDirectories(recordingsDir);
                }

                while (running) {
                    try {
                        Transferable contents = clipboard.getContents(null);
                        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                            String currentContent = (String) contents.getTransferData(DataFlavor.stringFlavor);
                            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                            if (!currentContent.equals(lastContent) && !currentContent.trim().isEmpty()) {
                                if (currentContent.contains("import com.microsoft.playwright.*;")) {
                                    lastContent = currentContent;
                                    Path filePath = recordingsDir.resolve("recording_" + timestamp + ".txt");
                                    Files.writeString(filePath, currentContent, StandardOpenOption.CREATE);
                                    Notification.show("Saved recorded steps to temporary file: " + filePath);
                                }
                            }
                        }
                        Thread.sleep(250);
                    } catch (UnsupportedFlavorException | IOException e) {
                        Logger.getLogger(ClipboardMonitor.class.getName()).log(Level.WARNING, "Clipboard access failed", e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stopMonitoring() {
        running = false;
        System.out.println("Clipboard monitoring: " + running);
    }
}

