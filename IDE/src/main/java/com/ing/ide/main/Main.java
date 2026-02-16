
package com.ing.ide.main;

import com.ing.datalib.testdata.TestDataFactory;
import com.ing.engine.cli.LookUp;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ide.main.cli.UICli;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.ModernSplash;
import com.ing.ide.main.ui.About;
import com.ing.ide.main.utils.AppIcon;
import com.ing.ide.util.logging.UILogger;
import com.ing.util.encryption.Encryption;
import com.ing.ide.main.fx.FXTheme;
import com.ing.ide.main.fx.INGIcons;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Timer;

import java.awt.Font;
import java.awt.FontFormatException;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.apache.commons.lang3.time.StopWatch;

public class Main {

    private static final StopWatch STOP_WATCH = new StopWatch();
    private static boolean isDarkMode = false;

    // ING Standard Brand Colors
    private static final Color ING_ORANGE     = Color.decode("#FF6200");
    private static final Color ING_PURPLE     = Color.decode("#7724FF");
    private static final Color ING_LIGHT_BLUE = Color.decode("#89D6FD");
    private static final Color ING_BURGUNDY   = Color.decode("#4D0020");

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tmS %1$tz [%4$-4s] %2$s:%5$s%6$s%n");
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0) {
            commandLineExecution(args);
        } else {
            UILogger.get();
            Logger.getLogger(Main.class.getName()).info("Launching INGenious Playwright Studio");
            STOP_WATCH.start();
            launchUI();
        }
    }

    private static void commandLineExecution(String[] args) {
        initCommonDependencies();
        if (!UICli.exe(args)) {
            LookUp.exe(args);
        }
    }

    private static void initCommonDependencies() {
        TestDataFactory.load();
        About.init();
        Encryption.getInstance();
        SystemDefaults.getClassesFromJar.set(true);
    }

    private static void launchUI() {
        // Initialize elegant dock/taskbar icon for macOS and Windows
        AppIcon.initialize();
        
        ModernSplash splash = new ModernSplash();
        splash.setVisible(true);
        // Initialize JavaFX toolkit early so JFXPanels can be created safely
        new JFXPanel();
        Platform.setImplicitExit(false);
        new Thread(() -> {
            setUpFlatLafUI();
            splash.progressed(10);
            initDependencies();
            splash.progressed(20);
            // Build & show UI on the EDT (required for safe Swing + JFXPanel interop)
            SwingUtilities.invokeLater(() -> {
                AppMainFrame mainFrame = null;
                try {
                    mainFrame = new AppMainFrame(splash::progressed);
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
                mainFrame.setVisible(false);
                mainFrame.setMinimumSize(new Dimension(800, 400));
                mainFrame.setPreferredSize(new Dimension(800, 400));
                mainFrame.setLocationRelativeTo(null);
                splash.progressed(100);
                
                // Wait for splash animation to reach 100% before hiding
                final AppMainFrame frame = mainFrame;
                Timer delayTimer = new Timer(600, e -> {
                    splash.setVisible(false);
                    frame.checkAndLoadRecent();
                    frame.setDefaultCloseOperation(AppMainFrame.DO_NOTHING_ON_CLOSE);
                    Boolean IS_MAXI_SUPPORTED = Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH);
                    if (IS_MAXI_SUPPORTED) {
                        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                    splash.dispose();
                    frame.setVisible(true);
                    // Swap Swing chrome for JavaFX CSS-styled chrome
                    frame.initFXChrome();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            });
        }, "UI:MainUI").start();

    }

    private static void initDependencies() {
        initCommonDependencies();
        MethodInfoManager.load();
        //ByObjectProp.load();
    }

    // ── Theme API ──

    /**
     * Returns whether dark mode is currently active.
     */
    public static boolean isDarkMode() {
        return isDarkMode;
    }

    /**
     * Toggle between light and dark themes at runtime.
     * Refreshes all open windows automatically.
     */
    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        try {
            UIManager.put("flatlaf.useWindowDecorations", false);
            if (isDarkMode) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            applyCustomColors();
            FlatLaf.updateUI();
            // Sync JavaFX theme
            FXTheme.toggleTheme(isDarkMode);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Failed to toggle theme", ex);
        }
    }

    /**
     * Sets up FlatLaf Light Look and Feel with ING brand customizations.
     */
    private static void setUpFlatLafUI() {
        try {
            // FlatLaf global properties - must be set BEFORE setup()
            UIManager.put("flatlaf.useWindowDecorations", false);
            
            // Filled triangle style for split pane and combo box arrows
            UIManager.put("Component.arrowType", "triangle");
            
            // Tree expand/collapse icons - set BEFORE FlatLaf.setup() for proper theming
            String expandedKey  = isDarkMode ? "tree.expandedDark"  : "tree.expandedLight";
            String collapsedKey = isDarkMode ? "tree.collapsedDark" : "tree.collapsedLight";
            javax.swing.Icon expandedIcon  = INGIcons.swingColored(expandedKey, 12);
            javax.swing.Icon collapsedIcon = INGIcons.swingColored(collapsedKey, 12);
            UIManager.put("Tree.expandedIcon", expandedIcon);
            UIManager.put("Tree.collapsedIcon", collapsedIcon);

            FlatLightLaf.setup();
            
            // Re-apply tree icons after setup to override FlatLaf defaults
            UIManager.put("Tree.expandedIcon", expandedIcon);
            UIManager.put("Tree.collapsedIcon", collapsedIcon);
            UIManager.getLookAndFeelDefaults().put("Tree.expandedIcon", expandedIcon);
            UIManager.getLookAndFeelDefaults().put("Tree.collapsedIcon", collapsedIcon);
            
            applyCustomColors();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Failed to set up FlatLaf", ex);
        }
    }

    /**
     * Applies ING brand colors for the current theme (light or dark).
     */
    private static void applyCustomColors() {
        if (isDarkMode) {
            applyDarkColors();
        } else {
            applyLightColors();
        }
        applyCommonSettings();
    }

    /**
     * Light theme: warm off-white base with subtle ING brand tints.
     * Replaces FlatLaf's default cool grey with warmer tones.
     */
    private static void applyLightColors() {
        // Warm base (no grey)
        Color warmBg      = Color.decode("#FAFAF8");
        Color warmControl = Color.decode("#F5F4F1");

        // Derived tints from ING brand
        Color purpleLight   = Color.decode("#E5D6FF");
        Color purpleVLight  = Color.decode("#F5F0FF");
        Color orangeLight   = Color.decode("#FFE0CC");
        Color orangeVLight  = Color.decode("#FFF5EE");

        // ── Warm backgrounds (removes cold grey) ──
        UIManager.put("Panel.background", warmBg);
        UIManager.put("control", warmControl);
        UIManager.put("window", warmBg);
        UIManager.put("menu", Color.WHITE);
        UIManager.put("Separator.foreground", Color.decode("#E5E2DE"));
        UIManager.put("Component.borderColor", Color.decode("#DDD8D3"));
        UIManager.put("Component.disabledBorderColor", Color.decode("#E5E2DE"));
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("FormattedTextField.background", Color.WHITE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("Spinner.background", Color.WHITE);
        UIManager.put("ToolBar.background", warmBg);
        UIManager.put("ScrollPane.background", warmBg);
        UIManager.put("Viewport.background", Color.WHITE);
        UIManager.put("SplitPane.background", warmBg);
        UIManager.put("SplitPane.dividerSize", 10);
        UIManager.put("SplitPane.dividerColor", Color.decode("#E8E4E0"));
        UIManager.put("SplitPane.centerOneTouchButtons", true);
        UIManager.put("SplitPane.oneTouchButtonSize", 8);
        UIManager.put("SplitPane.oneTouchButtonOffset", 2);
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("SplitPaneDivider.gripColor", Color.decode("#D0C8D8"));
        UIManager.put("SplitPaneDivider.gripDotCount", 3);
        UIManager.put("SplitPaneDivider.gripDotSize", 3);
        UIManager.put("SplitPaneDivider.gripGap", 2);
        UIManager.put("SplitPaneDivider.oneTouchArrowColor", ING_PURPLE);
        UIManager.put("SplitPaneDivider.oneTouchHoverArrowColor", Color.decode("#5A10CC"));
        UIManager.put("SplitPaneDivider.oneTouchPressedArrowColor", Color.decode("#4A0DAA"));
        UIManager.put("OptionPane.background", warmBg);

        // ── Test Design pane backgrounds (warm light theme) ──
        UIManager.put("ing.sidebarPane", Color.decode("#F6F4F2"));
        UIManager.put("ing.editorPane", Color.WHITE);
        UIManager.put("ing.dividerColor", Color.decode("#E8E4E0"));

        // ── App-specific custom keys ──
        UIManager.put("tableColor", purpleVLight);
        UIManager.put("subToolBar", purpleLight);
        UIManager.put("toolBar", Color.WHITE);
        UIManager.put("execPanel", orangeVLight);
        UIManager.put("execToolBar", orangeLight);
        UIManager.put("BDDEditor", purpleLight);
        UIManager.put("searchBox", Color.WHITE);
        UIManager.put("shadow", new Color(77, 0, 32, 40));
        UIManager.put("exec", ING_PURPLE);
        UIManager.put("text", ING_BURGUNDY);
        UIManager.put("gridColor", purpleVLight);
        UIManager.put("designTableHeader", purpleLight);
        UIManager.put("execTableHeader", orangeLight);
        UIManager.put("execColor", orangeLight);
        UIManager.put("designColor", purpleLight);
        UIManager.put("execTableColor", orangeVLight);

        // ── Tabs ──
        UIManager.put("TabbedPane.selectedBackground", ING_PURPLE);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.foreground", ING_BURGUNDY);
        UIManager.put("TabbedPane.background", warmBg);
        UIManager.put("TabbedPane.contentAreaColor", warmBg);
        UIManager.put("TabbedPane.hoverColor", purpleLight);
        UIManager.put("TabbedPane.focusColor", ING_PURPLE);
        UIManager.put("TabbedPane.underlineColor", ING_PURPLE);
        UIManager.put("TabbedPane.tabArc", 12);
        UIManager.put("TabbedPane.tabSelectionArc", 12);
        UIManager.put("TabbedPane.cardTabArc", 12);
        UIManager.put("TabbedPane.tabHeight", 32);
        UIManager.put("TabbedPane.tabInsets", new Insets(6, 12, 6, 12));
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(4, 8, 0, 8));
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", false);
        // Also set in L&F defaults for FlatLaf
        UIManager.getLookAndFeelDefaults().put("TabbedPane.foreground", ING_BURGUNDY);
        UIManager.getLookAndFeelDefaults().put("TabbedPane.selectedForeground", Color.WHITE);

        // ── Menu bar ──
        UIManager.put("MenuBar.background", Color.WHITE);
        UIManager.put("MenuBar.selectionBackground", ING_PURPLE);
        UIManager.put("MenuBar.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.selectionBackground", ING_PURPLE);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("CheckBoxMenuItem.selectionBackground", ING_PURPLE);
        UIManager.put("CheckBoxMenuItem.selectionForeground", Color.WHITE);

        // ── Selection colors ──
        UIManager.put("List.selectionBackground", purpleLight);
        UIManager.put("Tree.selectionBackground", purpleLight);
        UIManager.put("Tree.selectionInactiveBackground", purpleLight);
        UIManager.put("Tree.selectionForeground", ING_BURGUNDY);
        UIManager.put("Tree.background", Color.decode("#F6F8FA"));

        // ── Tree appearance (modern arrows) ──
        UIManager.put("Tree.showsRootHandles", true);
        UIManager.put("Tree.leftChildIndent", 8);
        UIManager.put("Tree.rightChildIndent", 12);
        UIManager.put("Tree.rowHeight", 26);
        UIManager.put("Tree.rendererMargins", new java.awt.Insets(1, 4, 1, 4));

        UIManager.put("Table.selectionBackground", purpleLight);
        UIManager.put("Table.selectionForeground", ING_BURGUNDY);

        // ── Table appearance (modern web table) ──
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.alternateRowColor", Color.decode("#FAFBFC"));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", Color.decode("#EAECEF"));
        UIManager.put("TableHeader.background", Color.decode("#F6F8FA"));
        UIManager.put("TableHeader.foreground", Color.decode("#24292F"));
        UIManager.put("TableHeader.separatorColor", Color.decode("#D0D7DE"));
        UIManager.put("TableHeader.bottomSeparatorColor", Color.decode("#D0D7DE"));

        // ── Buttons ──
        UIManager.put("Button.default.background", ING_PURPLE);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.default.hoverBackground", Color.decode("#6318E0"));
        UIManager.put("Button.default.pressedBackground", Color.decode("#5212C0"));

        // ── Scrollbar (warm neutral, not grey) ──
        UIManager.put("ScrollBar.thumbColor", Color.decode("#D5D0CC"));
        UIManager.put("ScrollBar.hoverThumbColor", Color.decode("#BDB8B4"));
        UIManager.put("ScrollBar.track", warmBg);

        // ── Links & toggles ──
        UIManager.put("ToggleButton.selectedBackground", purpleLight);

        // ── Custom keys for renderers ──
        UIManager.put("ing.selectionBackground", purpleLight);
        UIManager.put("ing.selectionInactiveBackground", purpleLight);
        UIManager.put("ing.selectionForeground", ING_BURGUNDY);
        UIManager.put("ing.searchHighlight", purpleLight);
        UIManager.put("ing.focusedSelectionBackground", Color.decode("#C9A8FF"));
        UIManager.put("ing.panelBackground", Color.WHITE);
        UIManager.put("ing.commentedForeground", Color.decode("#B0A8A0"));
        UIManager.put("ing.breakpointForeground", Color.decode("#1565C0"));
        UIManager.put("ing.selectedCellForeground", Color.WHITE);
        UIManager.put("ing.focusedForeground", ING_PURPLE);
        UIManager.put("ing.unfocusedForeground", ING_BURGUNDY);
        UIManager.put("ing.webserviceStartForeground", Color.decode("#1565C0"));
        UIManager.put("ing.webserviceStopForeground", Color.decode("#8D6E00"));
        UIManager.put("ing.webserviceRequestForeground", Color.decode("#00994D"));
        UIManager.put("ing.reusableForeground", Color.decode("#349651"));
        UIManager.put("ing.errorForeground", Color.decode("#D32F2F"));
        UIManager.put("ing.borderFocus", Color.decode("#13BEFF"));
        UIManager.put("ing.borderThumb", Color.decode("#9E9E9E"));
        UIManager.put("ing.borderThumbHover", Color.GRAY);
        UIManager.put("ing.borderThumbSelected", Color.decode("#616161"));
    }

    /**
     * Dark theme: deep dark backgrounds with elegant purplish tint and ING brand accent colors.
     */
    private static void applyDarkColors() {
        // Elegant purplish dark colors (matching toolbar/menu bar)
        Color darkBg      = Color.decode("#1E1A24");
        Color darkPanel   = Color.decode("#252030");
        Color darkSurface = Color.decode("#2D2838");
        Color warmText    = Color.decode("#E8E2E5");
        
        // Sidebar colors (purplish dark, matching main theme)
        Color sidebarDark = Color.decode("#1E1A24");
        Color editorDark  = Color.decode("#252030");
        Color dividerGray = Color.decode("#3A3545");

        // Muted tints for dark
        Color orangeDark   = Color.decode("#4D2800");
        Color orangeMuted  = Color.decode("#3D2010");
        Color purpleDark   = Color.decode("#2D1A45");
        Color blueDark     = Color.decode("#1A2A3D");
        Color blueMuted    = Color.decode("#1D2535");

        // ── Dark backgrounds ──
        UIManager.put("Panel.background", darkBg);
        UIManager.put("control", darkBg);
        UIManager.put("window", darkBg);
        UIManager.put("menu", darkPanel);
        UIManager.put("Separator.foreground", Color.decode("#3A3545"));
        UIManager.put("Component.borderColor", Color.decode("#3A3545"));
        UIManager.put("Component.disabledBorderColor", Color.decode("#302B38"));
        UIManager.put("TextField.background", darkPanel);
        UIManager.put("FormattedTextField.background", darkPanel);
        UIManager.put("ComboBox.background", darkPanel);
        UIManager.put("ComboBox.foreground", warmText);
        UIManager.put("Spinner.background", darkPanel);
        UIManager.put("ToolBar.background", sidebarDark);
        UIManager.put("ToolBar.foreground", warmText);
        UIManager.put("ScrollPane.background", sidebarDark);
        UIManager.put("Viewport.background", sidebarDark);
        UIManager.put("SplitPane.background", dividerGray);
        UIManager.put("SplitPane.dividerSize", 10);
        UIManager.put("SplitPane.dividerColor", dividerGray);
        UIManager.put("SplitPane.centerOneTouchButtons", true);
        UIManager.put("SplitPane.oneTouchButtonSize", 8);
        UIManager.put("SplitPane.oneTouchButtonOffset", 2);
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("SplitPaneDivider.gripColor", Color.decode("#5A5065"));
        UIManager.put("SplitPaneDivider.gripDotCount", 3);
        UIManager.put("SplitPaneDivider.gripDotSize", 3);
        UIManager.put("SplitPaneDivider.gripGap", 2);
        UIManager.put("SplitPaneDivider.oneTouchArrowColor", Color.decode("#BB86FC"));
        UIManager.put("SplitPaneDivider.oneTouchHoverArrowColor", Color.decode("#D4AAFF"));
        UIManager.put("SplitPaneDivider.oneTouchPressedArrowColor", Color.decode("#9F66E3"));
        UIManager.put("OptionPane.background", darkBg);

        // ── Test Design pane backgrounds (VS Code-like dark theme) ──
        UIManager.put("ing.sidebarPane", sidebarDark);
        UIManager.put("ing.editorPane", editorDark);
        UIManager.put("ing.dividerColor", dividerGray);

        // ── App-specific custom keys (dark variants) ──
        UIManager.put("tableColor", blueMuted);
        UIManager.put("subToolBar", blueDark);
        UIManager.put("toolBar", darkPanel);
        UIManager.put("execPanel", orangeMuted);
        UIManager.put("execToolBar", orangeDark);
        UIManager.put("BDDEditor", purpleDark);
        UIManager.put("searchBox", darkSurface);  // Search box with dark background
        UIManager.put("shadow", new Color(0, 0, 0, 80));
        UIManager.put("exec", ING_ORANGE);
        UIManager.put("text", warmText);
        UIManager.put("gridColor", blueMuted);
        UIManager.put("designTableHeader", blueDark);
        UIManager.put("execTableHeader", orangeDark);
        UIManager.put("execColor", blueDark);
        UIManager.put("designColor", blueDark);
        UIManager.put("execTableColor", orangeMuted);

        // ── Toolbar / Panel ──
        UIManager.put("ToolBar.background", darkPanel);
        UIManager.put("ToolBar.foreground", warmText);
        UIManager.put("ToolBar.borderColor", Color.decode("#2A2535"));
        UIManager.put("Panel.background", darkPanel);
        UIManager.put("Panel.foreground", warmText);
        UIManager.put("SplitPane.background", darkBg);
        UIManager.put("SplitPane.dividerColor", Color.decode("#2A2535"));
        UIManager.put("SplitPaneDivider.draggingColor", ING_ORANGE);
        UIManager.put("ScrollPane.background", darkBg);
        UIManager.put("Viewport.background", darkBg);
        UIManager.put("OptionPane.background", darkPanel);
        UIManager.put("OptionPane.messageForeground", warmText);

        // ── Tabs ──
        UIManager.put("TabbedPane.selectedBackground", ING_ORANGE);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
        UIManager.put("TabbedPane.hoverColor", orangeDark);
        UIManager.put("TabbedPane.focusColor", ING_ORANGE);
        UIManager.put("TabbedPane.underlineColor", ING_ORANGE);
        UIManager.put("TabbedPane.background", darkBg);
        // Use light grey for inactive tab text to be clearly visible in dark theme
        UIManager.put("TabbedPane.foreground", Color.decode("#B8B4BC"));
        // Also set in L&F defaults for FlatLaf
        UIManager.getLookAndFeelDefaults().put("TabbedPane.foreground", Color.decode("#B8B4BC"));
        UIManager.getLookAndFeelDefaults().put("TabbedPane.selectedForeground", Color.WHITE);

        // ── Menu bar ──
        UIManager.put("MenuBar.background", darkPanel);
        UIManager.put("MenuBar.foreground", warmText);
        UIManager.put("MenuBar.selectionBackground", ING_ORANGE);
        UIManager.put("MenuBar.selectionForeground", Color.WHITE);
        UIManager.put("Menu.foreground", warmText);
        UIManager.put("Menu.background", darkPanel);
        UIManager.put("MenuItem.background", darkPanel);
        UIManager.put("MenuItem.foreground", warmText);
        UIManager.put("MenuItem.selectionBackground", ING_ORANGE);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.background", darkPanel);
        UIManager.put("CheckBoxMenuItem.background", darkPanel);
        UIManager.put("CheckBoxMenuItem.foreground", warmText);
        UIManager.put("CheckBoxMenuItem.selectionBackground", ING_ORANGE);
        UIManager.put("CheckBoxMenuItem.selectionForeground", Color.WHITE);

        // ── Selection colors (subtle gray, not violet) ──
        Color subtleSelection = Color.decode("#33333D");  // Very subtle, nearly gray
        Color subtleSelectionInactive = Color.decode("#2A2A32");
        
        // Set in both UIManager and L&F defaults to ensure FlatLaf picks them up
        UIManager.put("List.selectionBackground", subtleSelection);
        UIManager.put("List.selectionForeground", warmText);
        UIManager.put("List.background", sidebarDark);
        UIManager.put("Tree.selectionBackground", subtleSelection);
        UIManager.put("Tree.selectionInactiveBackground", subtleSelectionInactive);
        UIManager.put("Tree.selectionForeground", warmText);
        UIManager.put("Tree.background", sidebarDark);
        UIManager.put("Tree.foreground", warmText);
        UIManager.put("Table.selectionBackground", subtleSelection);
        UIManager.put("Table.selectionForeground", warmText);
        UIManager.put("Table.background", editorDark);
        UIManager.put("Table.foreground", warmText);
        
        // Also set in LookAndFeelDefaults to ensure FlatLaf uses them for row painting
        UIManager.getLookAndFeelDefaults().put("Tree.selectionBackground", subtleSelection);
        UIManager.getLookAndFeelDefaults().put("Tree.selectionInactiveBackground", subtleSelectionInactive);
        UIManager.getLookAndFeelDefaults().put("List.selectionBackground", subtleSelection);
        UIManager.getLookAndFeelDefaults().put("Table.selectionBackground", subtleSelection);

        // ── Table appearance ──
        Color subtleGridLine = Color.decode("#3A3545");  // Subtle gray grid lines
        UIManager.put("Table.alternateRowColor", Color.decode("#221D2A"));
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", true);
        UIManager.put("Table.gridColor", subtleGridLine);
        UIManager.put("Table.cellFocusColor", subtleGridLine);
        UIManager.put("Table.focusCellHighlightBorder", javax.swing.BorderFactory.createLineBorder(subtleGridLine));
        UIManager.put("TableHeader.background", darkSurface);
        UIManager.put("TableHeader.foreground", warmText);
        UIManager.put("TableHeader.separatorColor", subtleGridLine);
        UIManager.put("TableHeader.bottomSeparatorColor", subtleGridLine);
        UIManager.getLookAndFeelDefaults().put("Table.gridColor", subtleGridLine);
        UIManager.getLookAndFeelDefaults().put("Table.cellFocusColor", subtleGridLine);

        // ── TextField / SearchBox ──
        UIManager.put("TextField.background", darkSurface);
        UIManager.put("TextField.foreground", warmText);
        UIManager.put("TextField.caretForeground", warmText);
        UIManager.put("TextField.selectionBackground", subtleSelection);
        UIManager.put("TextField.selectionForeground", warmText);
        UIManager.put("FormattedTextField.background", darkSurface);
        UIManager.put("FormattedTextField.foreground", warmText);
        UIManager.put("PasswordField.background", darkSurface);
        UIManager.put("PasswordField.foreground", warmText);
        UIManager.put("TextArea.background", darkSurface);
        UIManager.put("TextArea.caretForeground", warmText);
        UIManager.put("TextPane.background", darkSurface);
        UIManager.put("TextPane.caretForeground", warmText);
        UIManager.put("EditorPane.background", darkSurface);
        UIManager.put("EditorPane.caretForeground", warmText);
        UIManager.put("ComboBox.background", darkSurface);
        UIManager.put("ComboBox.foreground", warmText);
        UIManager.put("ComboBox.selectionBackground", subtleSelection);
        UIManager.put("ComboBox.selectionForeground", warmText);
        UIManager.put("ComboBox.buttonBackground", darkSurface);
        UIManager.put("Spinner.background", darkSurface);
        UIManager.put("Spinner.foreground", warmText);

        // ── Buttons ──
        UIManager.put("Button.background", darkSurface);
        UIManager.put("Button.foreground", warmText);
        UIManager.put("Button.default.background", ING_ORANGE);
        UIManager.put("Button.default.foreground", Color.WHITE);
        UIManager.put("Button.default.hoverBackground", Color.decode("#E55800"));
        UIManager.put("Button.default.pressedBackground", Color.decode("#CC4E00"));

        // ── Scrollbar ──
        UIManager.put("ScrollBar.thumbColor", Color.decode("#3A3545"));
        UIManager.put("ScrollBar.hoverThumbColor", Color.decode("#4A4555"));
        UIManager.put("ScrollBar.track", darkBg);

        // ── Text foregrounds ──
        UIManager.put("Label.foreground", warmText);
        UIManager.put("TextField.foreground", warmText);
        UIManager.put("TextArea.foreground", warmText);
        UIManager.put("TextPane.foreground", warmText);
        UIManager.put("EditorPane.foreground", warmText);
        UIManager.put("List.foreground", warmText);
        UIManager.put("TitledBorder.titleColor", warmText);
        UIManager.put("CheckBox.foreground", warmText);
        UIManager.put("RadioButton.foreground", warmText);

        // ── Links & toggles ──
        UIManager.put("ToggleButton.selectedBackground", purpleDark);

        // ── Progress ──
        UIManager.put("ProgressBar.foreground", ING_ORANGE);
        UIManager.put("ProgressBar.background", darkSurface);

        // ── Custom keys for renderers ──
        UIManager.put("ing.selectionBackground", subtleSelection);
        UIManager.put("ing.selectionInactiveBackground", subtleSelectionInactive);
        UIManager.put("ing.selectionForeground", warmText);
        UIManager.put("ing.searchHighlight", purpleDark);
        UIManager.put("ing.focusedSelectionBackground", Color.decode("#3D3D48"));
        UIManager.put("ing.panelBackground", darkPanel);
        UIManager.put("ing.commentedForeground", Color.decode("#6E6878"));
        UIManager.put("ing.breakpointForeground", Color.decode("#64B5F6"));
        UIManager.put("ing.selectedCellForeground", warmText);
        UIManager.put("ing.focusedForeground", ING_ORANGE);
        UIManager.put("ing.unfocusedForeground", warmText);
        UIManager.put("ing.webserviceStartForeground", Color.decode("#64B5F6"));
        UIManager.put("ing.webserviceStopForeground", Color.decode("#FFD54F"));
        UIManager.put("ing.webserviceRequestForeground", Color.decode("#81C784"));
        UIManager.put("ing.reusableForeground", Color.decode("#66BB6A"));
        UIManager.put("ing.errorForeground", Color.decode("#EF5350"));
        UIManager.put("ing.borderFocus", Color.decode("#BB86FC"));
        UIManager.put("ing.borderThumb", Color.decode("#4A4555"));
        UIManager.put("ing.borderThumbHover", Color.decode("#5A5565"));
        UIManager.put("ing.borderThumbSelected", Color.decode("#6A6575"));
    }

    /**
     * Settings common to both light and dark themes.
     */
    private static void applyCommonSettings() {
        UIManager.put("@accentColor", ING_PURPLE);
        UIManager.put("Component.focusColor", ING_PURPLE);
        UIManager.put("Component.linkColor", ING_PURPLE);
        UIManager.put("ProgressBar.foreground", ING_PURPLE);

        // ── Tree expand/collapse icons (elegant chevrons) ──
        String expandedKey  = isDarkMode ? "tree.expandedDark"  : "tree.expandedLight";
        String collapsedKey = isDarkMode ? "tree.collapsedDark" : "tree.collapsedLight";
        javax.swing.Icon expandedIcon  = INGIcons.swingColored(expandedKey, 12);
        javax.swing.Icon collapsedIcon = INGIcons.swingColored(collapsedKey, 12);
        // Set in both UIManager and L&F defaults to ensure FlatLaf respects them
        UIManager.put("Tree.expandedIcon", expandedIcon);
        UIManager.put("Tree.collapsedIcon", collapsedIcon);
        UIManager.getLookAndFeelDefaults().put("Tree.expandedIcon", expandedIcon);
        UIManager.getLookAndFeelDefaults().put("Tree.collapsedIcon", collapsedIcon);

        // ── Custom ING Me font ──
        registerCustomFont();

        Font ingMeRegular = new Font("ING Me", Font.PLAIN, 12);
        Font ingMeBold    = new Font("ING Me", Font.BOLD, 12);

        UIManager.put("defaultFont", ingMeRegular);
        UIManager.put("TableMenu.font", new Font("ING Me", Font.PLAIN, 11));
        UIManager.put("Table.font", ingMeBold);
        UIManager.put("Menu.font", ingMeBold);
        UIManager.put("MenuItem.font", ingMeRegular);
        UIManager.put("Label.font", ingMeRegular);
        UIManager.put("Button.font", ingMeRegular);
        UIManager.put("TextField.font", ingMeRegular);
        UIManager.put("TextArea.font", ingMeRegular);
        UIManager.put("ComboBox.font", ingMeRegular);
        UIManager.put("TabbedPane.font", ingMeBold);
        UIManager.put("Tree.font", ingMeRegular);
        UIManager.put("List.font", ingMeRegular);
        UIManager.put("ToolTip.font", new Font("ING Me", Font.PLAIN, 11));
        UIManager.put("TitledBorder.font", ingMeBold);
    }

    /**
     * Registers the ING Me custom font from the resources directory.
     */
    private static void registerCustomFont() {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT,
                    new File("resources/ui/resources/fonts/ingme_regular.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            Logger.getLogger(Main.class.getName()).log(Level.FINE, "Custom font not found, using defaults", e);
        }
    }

    public static void finish() {
       STOP_WATCH.stop();
       Logger.getLogger(Main.class.getName()).log(Level.INFO, "INGenious Playwright Studio has been Terminated - [ Total Time : {0} ]", STOP_WATCH.toString());
    }
     
}

