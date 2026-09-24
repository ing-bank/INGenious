package com.ing.engine.drivers;

import com.ing.engine.drivers.sap.SapGuiSession;
import com.jacob.com.Dispatch;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Captures a screenshot of the active SAP GUI window.
 * <p>
 * SAP GUI Scripting (via JACOB) exposes no bitmap-capture API of its own - only the on-screen
 * bounds of a {@code GuiVComponent} ({@code ScreenLeft}/{@code ScreenTop}/{@code Width}/
 * {@code Height}). This reads those bounds off the session's {@code ActiveWindow} and crops an
 * OS-level {@link Robot} capture to them, following the same technique already used by
 * {@link com.ing.engine.drivers.customWebDriver.EmptyDriver} for its full-desktop fallback.
 */
public class SapDriverCreation {
    private final SapGuiSession session;

    public SapDriverCreation(SapGuiSession session) {
        this.session = session;
    }

    public File createScreenShot() throws IOException {
        Rectangle bounds = resolveActiveWindowBounds();
        try {
            File file = File.createTempFile("sap-screenshot", ".png");
            ImageIO.write(new Robot().createScreenCapture(bounds), "png", file);
            return file;
        } catch (AWTException ex) {
            throw new IOException("Failed to capture SAP screenshot", ex);
        }
    }

    /**
     * Falls back to the full desktop if no active window can be resolved (e.g. a fake session,
     * or the {@code ActiveWindow} lookup fails) - safe here since the environment runs only the
     * SAP GUI, so a full-desktop capture never shows unrelated content.
     */
    private Rectangle resolveActiveWindowBounds() {
        try {
            Object raw = session == null ? null : session.raw();
            if (raw instanceof Dispatch) {
                Dispatch sessionDispatch = (Dispatch) raw;
                Dispatch activeWindow = Dispatch.get(sessionDispatch, "ActiveWindow").toDispatch();
                int left = Integer.parseInt(Dispatch.get(activeWindow, "ScreenLeft").toString());
                int top = Integer.parseInt(Dispatch.get(activeWindow, "ScreenTop").toString());
                int width = Integer.parseInt(Dispatch.get(activeWindow, "Width").toString());
                int height = Integer.parseInt(Dispatch.get(activeWindow, "Height").toString());
                return new Rectangle(left, top, width, height);
            }
        } catch (Exception ignored) {
            // Fall back to full desktop below.
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new Rectangle(0, 0, screenSize.width, screenSize.height);
    }
}
