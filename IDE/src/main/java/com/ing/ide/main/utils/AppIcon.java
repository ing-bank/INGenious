package com.ing.ide.main.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides elegant application icons for macOS dock and Windows taskbar.
 * Uses the INGenious logo on a purple gradient background with rounded corners.
 * Multi-resolution icon list ensures crisp display on all platforms and HiDPI/Retina screens.
 * <p>
 * On macOS, uses the Taskbar API to set the dock icon for a polished appearance.
 * On Windows, uses the multi-resolution icon list for proper taskbar display.
 */
public final class AppIcon {

    private static final Logger LOG = Logger.getLogger(AppIcon.class.getName());

    // App brand colors
    private static final Color APP_PURPLE = new Color(0x77, 0x24, 0xFF);
    private static final Color APP_PURPLE_LIGHT = new Color(0x9B, 0x4D, 0xFF);

    // Cached icon images
    private static List<Image> iconList = null;
    private static Image dockIcon = null;
    private static BufferedImage logoImage = null;

    private AppIcon() {}

    /**
     * Initialize the application icon for all platforms.
     * Should be called early in application startup.
     */
    public static void initialize() {
        try {
            // Generate the multi-resolution icon list
            getAppIconList();
            
            // Set dock icon on macOS using Taskbar API
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(getDockIcon());
                    LOG.info("Set macOS dock icon successfully");
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not set application dock icon", e);
        }
    }

    /**
     * Returns a list of application icons in multiple sizes for best display.
     * Sizes: 16, 24, 32, 48, 64, 128, 256, 512 pixels.
     */
    public static List<Image> getAppIconList() {
        if (iconList == null) {
            iconList = createIconList();
        }
        return iconList;
    }

    /**
     * Returns the large dock icon (512px) for macOS.
     */
    public static Image getDockIcon() {
        if (dockIcon == null) {
            dockIcon = createModernIcon(512);
        }
        return dockIcon;
    }

    /**
     * Returns the primary app icon at standard size (64px).
     */
    public static Image getAppIcon() {
        List<Image> icons = getAppIconList();
        // Return 64px version (index 4: 16, 24, 32, 48, 64...)
        return icons.size() > 4 ? icons.get(4) : icons.get(icons.size() - 1);
    }

    /**
     * Creates the multi-resolution icon list.
     */
    private static List<Image> createIconList() {
        List<Image> icons = new ArrayList<>();
        int[] sizes = {16, 24, 32, 48, 64, 128, 256, 512};
        
        for (int size : sizes) {
            icons.add(createModernIcon(size));
        }
        
        return icons;
    }

    /**
     * Creates a modern, elegant app icon with rounded corners and gradient background.
     * Features the INGenious logo centered on a purple gradient.
     */
    private static Image createModernIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Enable high quality rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        // Calculate corner radius (proportional to size, max ~20% of size)
        int cornerRadius = Math.max(4, size / 5);
        
        // Draw rounded rectangle background with gradient
        GradientPaint gradient = new GradientPaint(
            0, 0, APP_PURPLE,
            size, size, APP_PURPLE_LIGHT
        );
        g2.setPaint(gradient);
        g2.fillRoundRect(0, 0, size, size, cornerRadius, cornerRadius);
        
        // Draw subtle inner glow/highlight at top
        GradientPaint highlight = new GradientPaint(
            0, 0, new Color(255, 255, 255, 60),
            0, size / 3, new Color(255, 255, 255, 0)
        );
        g2.setPaint(highlight);
        g2.fillRoundRect(0, 0, size, size / 2, cornerRadius, cornerRadius);
        
        // Load and draw the INGenious logo
        BufferedImage logo = getLogoImage();
        if (logo != null) {
            // Calculate logo size (70% of icon size for better visibility)
            int logoSize = (int)(size * 0.7);
            int logoOffset = (size - logoSize) / 2;
            
            // Scale and draw the logo centered
            g2.drawImage(logo, logoOffset, logoOffset, logoSize, logoSize, null);
        }
        
        // Add subtle drop shadow effect at bottom edge for depth
        GradientPaint shadow = new GradientPaint(
            0, size - size/4, new Color(0, 0, 0, 0),
            0, size, new Color(0, 0, 0, 30)
        );
        g2.setPaint(shadow);
        g2.fillRoundRect(0, size - size/4, size, size/4, 0, 0);
        
        g2.dispose();
        return img;
    }

    /**
     * Loads and caches the INGenious logo image.
     */
    private static BufferedImage getLogoImage() {
        if (logoImage == null) {
            try (InputStream is = AppIcon.class.getResourceAsStream("/ui/resources/favicon.png")) {
                if (is != null) {
                    logoImage = ImageIO.read(is);
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Could not load INGenious logo", e);
            }
        }
        return logoImage;
    }

    /**
     * Returns the logo image for use in splash screens, etc.
     * @param size desired size in pixels
     * @return scaled logo image
     */
    public static BufferedImage getLogoImage(int size) {
        BufferedImage logo = getLogoImage();
        if (logo == null) return null;
        
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(logo, 0, 0, size, size, null);
        g2.dispose();
        return scaled;
    }

    /**
     * Apply the app icon to a JFrame, using multi-resolution icons for best display.
     */
    public static void applyTo(JFrame frame) {
        frame.setIconImages(getAppIconList());
    }

    /**
     * Apply the app icon to a JDialog, using multi-resolution icons.
     */
    public static void applyTo(JDialog dialog) {
        dialog.setIconImages(getAppIconList());
    }

    /**
     * Apply the app icon to a Window.
     */
    public static void applyTo(Window window) {
        window.setIconImages(getAppIconList());
    }
}
