package com.ing.ide.main.utils;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppFonts {
    private static final Logger LOG = Logger.getLogger(AppFonts.class.getName());

    private static final String ING_ME_RESOURCE = "/ui/resources/fonts/ingme_regular.ttf";

    private static boolean registrationAttempted;
    private static boolean registered;

    private AppFonts() {}

    public static synchronized boolean register() {
        if (registrationAttempted) {
            return registered;
        }

        registrationAttempted = true;

        try (InputStream input = AppFonts.class.getResourceAsStream(ING_ME_RESOURCE)) {
            if (input == null) {
                LOG.warning("ING Me font resource is missing");
                return false;
            }

            Font font = Font.createFont(Font.TRUETYPE_FONT, input);
            registered = GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);

            return registered;
        } catch (IOException | FontFormatException ex) {
            LOG.log(Level.WARNING, "Could not register ING Me font", ex);
            return false;
        }
    }
}
