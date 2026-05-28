package com.ing.ide.main.mainui.components.apitester.util;

import com.ing.ide.main.Main;

import javax.swing.*;
import java.awt.*;

/**
 * Theme-aware color provider for API Tester components.
 * Provides consistent colors that adapt to light/dark mode.
 */
public final class APITesterColors {

    private APITesterColors() {
        // Utility class
    }

    /**
     * Check if dark mode is active.
     */
    public static boolean isDarkMode() {
        return Main.isDarkMode();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Panel Backgrounds
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get the standard panel background color.
     */
    public static Color panelBackground() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : (isDarkMode() ? new Color(0x2D, 0x28, 0x38) : Color.WHITE);
    }

    /**
     * Get text area background color.
     */
    public static Color textAreaBackground() {
        Color c = UIManager.getColor("TextArea.background");
        return c != null ? c : (isDarkMode() ? new Color(0x1E, 0x1B, 0x26) : Color.WHITE);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Text Colors
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get primary text color.
     */
    public static Color textPrimary() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : (isDarkMode() ? new Color(0xE0, 0xE0, 0xE0) : Color.BLACK);
    }

    /**
     * Get secondary/muted text color (for labels, hints).
     */
    public static Color textSecondary() {
        return isDarkMode() ? new Color(0xA0, 0x9C, 0xAC) : new Color(0x6B, 0x72, 0x80);
    }

    /**
     * Get disabled/placeholder text color.
     */
    public static Color textDisabled() {
        return isDarkMode() ? new Color(0x70, 0x6C, 0x7C) : new Color(0x9C, 0xA3, 0xAF);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Status Colors (adapt slightly for dark mode visibility)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Success/green color for 2xx responses.
     */
    public static Color statusSuccess() {
        return isDarkMode() ? new Color(0x4A, 0xD9, 0x8F) : new Color(0x49, 0xCC, 0x90);
    }

    /**
     * Warning/orange color for 4xx responses.
     */
    public static Color statusWarning() {
        return isDarkMode() ? new Color(0xFF, 0xB8, 0x47) : new Color(0xFC, 0xA1, 0x30);
    }

    /**
     * Error/red color for 5xx responses and errors.
     */
    public static Color statusError() {
        return isDarkMode() ? new Color(0xFF, 0x6B, 0x6B) : new Color(0xF9, 0x3E, 0x3E);
    }

    /**
     * Neutral/gray color for unknown status.
     */
    public static Color statusNeutral() {
        return isDarkMode() ? new Color(0x8B, 0x88, 0x98) : new Color(0x6B, 0x72, 0x80);
    }

    // ═══════════════════════════════════════════════════════════════════
    // HTTP Method Colors
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Color for GET method.
     */
    public static Color methodGet() {
        return isDarkMode() ? new Color(0x4A, 0xD9, 0x8F) : new Color(0x38, 0xA1, 0x69);
    }

    /**
     * Color for POST method.
     */
    public static Color methodPost() {
        return isDarkMode() ? new Color(0xFF, 0xB8, 0x47) : new Color(0xD9, 0x7A, 0x06);
    }

    /**
     * Color for PUT method.
     */
    public static Color methodPut() {
        return isDarkMode() ? new Color(0x60, 0xA5, 0xFA) : new Color(0x25, 0x63, 0xEB);
    }

    /**
     * Color for PATCH method.
     */
    public static Color methodPatch() {
        return isDarkMode() ? new Color(0xA7, 0x8B, 0xFA) : new Color(0x7C, 0x3A, 0xED);
    }

    /**
     * Color for DELETE method.
     */
    public static Color methodDelete() {
        return isDarkMode() ? new Color(0xFF, 0x6B, 0x6B) : new Color(0xDC, 0x26, 0x26);
    }

    /**
     * Color for HEAD method.
     */
    public static Color methodHead() {
        return isDarkMode() ? new Color(0x4A, 0xD9, 0x8F) : new Color(0x38, 0xA1, 0x69);
    }

    /**
     * Color for OPTIONS method.
     */
    public static Color methodOptions() {
        return isDarkMode() ? new Color(0xF4, 0x72, 0xB6) : new Color(0xDB, 0x27, 0x77);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Button Colors
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Primary action button background (e.g., Send button).
     */
    public static Color buttonPrimary() {
        return isDarkMode() ? new Color(0x4A, 0xD9, 0x8F) : new Color(0x49, 0xCC, 0x90);
    }

    /**
     * Primary button text color.
     */
    public static Color buttonPrimaryText() {
        return isDarkMode() ? new Color(0x1A, 0x1A, 0x1A) : Color.WHITE;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Border Colors
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Standard border color.
     */
    public static Color border() {
        return isDarkMode() ? new Color(0x3A, 0x35, 0x45) : new Color(0xD1, 0xD5, 0xDB);
    }

    /**
     * Focus/highlight border color.
     */
    public static Color borderFocus() {
        return isDarkMode() ? new Color(0xFF, 0x66, 0x00) : new Color(0x77, 0x24, 0xFF);
    }
}
