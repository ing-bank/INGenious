package com.ing.ide.main.fx;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.materialdesign2.*;

import javafx.scene.paint.Color;
import javax.swing.UIManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Central icon mapping for the INGenious IDE.
 * Replaces all PNG/GIF bitmap icons with scalable vector web icons
 * (Material Design 2 + FontAwesome 5) via the Ikonli library.
 * <p>
 * Each icon has a semantic color assigned for visual clarity:
 * Orange = create/project, Green = save/run, Blue = search/spy,
 * Purple = data/import, Burgundy = default/structural.
 * <p>
 * Provides both JavaFX {@link org.kordamp.ikonli.javafx.FontIcon}
 * and Swing {@link org.kordamp.ikonli.swing.FontIcon} adapters.
 */
public final class INGIcons {

    private INGIcons() {}

    // ── ING Brand Colors (JavaFX) ──
    public static final Color ING_ORANGE     = Color.web("#FF6200");
    public static final Color ING_PURPLE     = Color.web("#7724FF");
    public static final Color ING_LIGHT_BLUE = Color.web("#89D6FD");
    public static final Color ING_BURGUNDY   = Color.web("#4D0020");
    public static final Color ING_GREEN      = Color.web("#349651");
    public static final Color ING_WARM_TEXT   = Color.web("#4D0020");

    // ── Semantic Icon Colors ──
    public static final Color CLR_CREATE     = Color.web("#FF6200"); // orange — new/create
    public static final Color CLR_OPEN       = Color.web("#D4880F"); // amber — open/folder
    public static final Color CLR_SAVE       = Color.web("#349651"); // green — save/success
    public static final Color CLR_RUN        = Color.web("#2EA043"); // green — play/run
    public static final Color CLR_STOP       = Color.web("#CF222E"); // red — stop/delete
    public static final Color CLR_DEBUG      = Color.web("#CF222E"); // red — debug/bug
    public static final Color CLR_SEARCH     = Color.web("#2188FF"); // blue — search/spy
    public static final Color CLR_CONFIG     = Color.web("#6E40C9"); // purple — settings
    public static final Color CLR_DATA       = Color.web("#7724FF"); // purple — data/import
    public static final Color CLR_NAV        = Color.web("#586069"); // grey — navigation
    public static final Color CLR_INFO       = Color.web("#2188FF"); // blue — help/info
    public static final Color CLR_TOOL       = Color.web("#0366D6"); // blue — tools
    public static final Color CLR_RECORD     = Color.web("#CF222E"); // red — record
    public static final Color CLR_TREE       = Color.web("#D4880F"); // amber — tree nodes
    public static final Color CLR_FILE       = Color.web("#586069"); // grey — file/document

    // ── Dark Mode Color Variants (brighter for visibility on dark backgrounds) ──
    private static final Color CLR_TOOL_DARK  = Color.web("#79C0FF"); // very bright blue
    private static final Color CLR_DATA_DARK  = Color.web("#D2A8FF"); // very bright purple
    private static final Color CLR_NAV_DARK   = Color.web("#A0A0A0"); // brighter grey
    private static final Color CLR_FILE_DARK  = Color.web("#A0A0A0"); // brighter grey

    // ── Icon Registry ──
    private static final Map<String, Ikon> ICON_MAP = new HashMap<>();
    // ── Per-Icon Color Registry ──
    private static final Map<String, Color> COLOR_MAP = new HashMap<>();
    // ── Per-Icon Dark Mode Color Registry ──
    private static final Map<String, Color> COLOR_MAP_DARK = new HashMap<>();

    private static void register(String key, Ikon ikon, Color color) {
        ICON_MAP.put(key, ikon);
        if (color != null) COLOR_MAP.put(key, color);
    }

    static {
        // ── Tree Expand/Collapse Icons ──
        register("tree.expanded",        MaterialDesignC.CHEVRON_DOWN,                  CLR_NAV);
        register("tree.collapsed",       MaterialDesignC.CHEVRON_RIGHT,                 CLR_NAV);
        register("tree.expandedLight",   MaterialDesignC.CHEVRON_DOWN,                  Color.web("#7724FF"));
        register("tree.collapsedLight",  MaterialDesignC.CHEVRON_RIGHT,                 Color.web("#7724FF"));
        register("tree.expandedDark",    MaterialDesignC.CHEVRON_DOWN,                  Color.web("#BB86FC"));
        register("tree.collapsedDark",   MaterialDesignC.CHEVRON_RIGHT,                 Color.web("#BB86FC"));

        // ── Split Pane Divider Arrows (solid filled triangles) ──
        register("split.left",           MaterialDesignM.MENU_LEFT,                     Color.web("#7724FF"));
        register("split.right",          MaterialDesignM.MENU_RIGHT,                    Color.web("#7724FF"));
        register("split.up",             MaterialDesignM.MENU_UP,                       Color.web("#7724FF"));
        register("split.down",           MaterialDesignM.MENU_DOWN,                     Color.web("#7724FF"));
        register("split.leftDark",       MaterialDesignM.MENU_LEFT,                     Color.web("#BB86FC"));
        register("split.rightDark",      MaterialDesignM.MENU_RIGHT,                    Color.web("#BB86FC"));
        register("split.upDark",         MaterialDesignM.MENU_UP,                       Color.web("#BB86FC"));
        register("split.downDark",       MaterialDesignM.MENU_DOWN,                     Color.web("#BB86FC"));

        // ── Tree: Test Plan ──
        register("testplan.Root",       MaterialDesignF.FLASK,                         CLR_DATA);
        register("testplan.Scenario",   MaterialDesignF.FOLDER,                        CLR_OPEN);
        register("testplan.TestCase",   MaterialDesignF.FILE_DOCUMENT_OUTLINE,         CLR_DATA);

        // ── Tree: Test Lab ──
        register("testlab.Root",        MaterialDesignB.BEAKER,                        CLR_DATA);
        register("testlab.Release",     MaterialDesignR.ROCKET_LAUNCH,                 CLR_CREATE);
        register("testlab.TestSet",     MaterialDesignP.PLAYLIST_CHECK,                CLR_SAVE);

        // ── Tree: Object Repository ──
        register("or.Root",             MaterialDesignD.DATABASE,                      CLR_DATA);
        register("or.Page",             MaterialDesignF.FILE_DOCUMENT,                 CLR_SAVE);
        register("or.Object",           MaterialDesignC.CUBE_OUTLINE,                  CLR_SEARCH);
        register("or.Group",            MaterialDesignF.FOLDER_MULTIPLE,               CLR_OPEN);
        register("or.Web",              MaterialDesignW.WEB,                           CLR_DATA);  // violet
        register("or.Mobile",           MaterialDesignC.CELLPHONE,                     CLR_DATA);  // violet
        register("or.propViewer",       MaterialDesignT.TABLE_EYE,                     CLR_SEARCH);
        
        // ── Dark Mode Colors for OR Icons (brighter violet for visibility) ──
        COLOR_MAP_DARK.put("or.Web",    CLR_DATA_DARK);
        COLOR_MAP_DARK.put("or.Mobile", CLR_DATA_DARK);
        COLOR_MAP_DARK.put("or.Root",   CLR_DATA_DARK);

        // ── Tree: Reusable ──
        register("reusable.Root",       MaterialDesignR.RECYCLE,                       CLR_SAVE);
        register("reusable.Folder",     MaterialDesignF.FOLDER,                        CLR_OPEN);
        register("reusable.TestCase",   MaterialDesignF.FILE_REFRESH,                  CLR_TOOL);

        // ── Toolbar / Main Actions ──
        register("NewProject",          MaterialDesignP.PLUS_BOX,                      CLR_CREATE);
        register("OpenProject",         MaterialDesignF.FOLDER_OPEN,                   CLR_OPEN);
        register("SaveProject",         MaterialDesignC.CONTENT_SAVE,                  CLR_SAVE);
        register("RunSettings",         MaterialDesignC.COG,                           CLR_CONFIG);
        register("BrowserConfiguration", MaterialDesignW.WEB,                          CLR_TOOL);
        register("APITester",           MaterialDesignA.API,                           Color.web("#00B4D8"));

        // ── Common Actions ──
        register("add",                 MaterialDesignP.PLUS,                          CLR_CREATE);
        register("remove",              MaterialDesignM.MINUS,                         CLR_STOP);
        register("delete",              MaterialDesignD.DELETE,                         CLR_STOP);
        register("run",                 MaterialDesignP.PLAY,                          CLR_RUN);
        register("exe",                 MaterialDesignP.PLAY_CIRCLE,                   CLR_RUN);
        register("stop",                MaterialDesignS.STOP,                          CLR_STOP);
        register("debug",               MaterialDesignB.BUG,                           CLR_DEBUG);
        register("record_start",        MaterialDesignR.RECORD,                        CLR_RECORD);
        register("record_stop",         MaterialDesignS.STOP_CIRCLE,                   CLR_STOP);
        register("search",              MaterialDesignM.MAGNIFY,                       CLR_SEARCH);
        register("refresh",             MaterialDesignR.REFRESH,                       CLR_TOOL);
        register("settings",            MaterialDesignC.COG,                           CLR_CONFIG);
        register("save",                MaterialDesignC.CONTENT_SAVE,                  CLR_SAVE);
        register("saveproj",            MaterialDesignC.CONTENT_SAVE_ALL,              CLR_SAVE);
        register("close",               MaterialDesignC.CLOSE,                         CLR_STOP);
        register("help",                MaterialDesignH.HELP_CIRCLE,                   CLR_INFO);
        register("ask",                 MaterialDesignH.HELP_CIRCLE_OUTLINE,           CLR_INFO);
        register("info",                MaterialDesignI.INFORMATION,                   CLR_INFO);

        // ── Dashboard / Navigation ──
        register("back",                MaterialDesignA.ARROW_LEFT,                    CLR_NAV);
        register("forward",             MaterialDesignA.ARROW_RIGHT,                   CLR_NAV);
        register("openInBrowser",       MaterialDesignO.OPEN_IN_NEW,                   CLR_TOOL);
        register("summary",             MaterialDesignC.CHART_BAR,                     CLR_CREATE);
        register("detailedSummary",     MaterialDesignC.CHART_BOX,                     CLR_DATA);
        register("latestSummary",       MaterialDesignF.FILE_CHART,                    CLR_CREATE);

        // ── Panel Header Actions ──
        register("up",                  MaterialDesignA.ARROW_UP,                      CLR_SEARCH);
        register("down",                MaterialDesignA.ARROW_DOWN,                    CLR_SEARCH);
        register("tag",                 MaterialDesignT.TAG,                           CLR_DATA);
        register("upOneLevel",          MaterialDesignA.ARROW_UP_BOLD,                 CLR_NAV);
        register("reload",              MaterialDesignR.RELOAD,                        CLR_TOOL);
        register("openwithsystemeditor", MaterialDesignO.OPEN_IN_NEW,                  CLR_TOOL);

        // ── Dock ──
        register("testdesign",          MaterialDesignP.PENCIL_RULER,                  CLR_TOOL);
        register("testexecution",       MaterialDesignP.PLAY_BOX,                      CLR_RUN);
        register("dashboard",           MaterialDesignV.VIEW_DASHBOARD,                CLR_CREATE);

        // ── Table ──
        register("cut",                 MaterialDesignC.CONTENT_CUT,                   CLR_NAV);
        register("copy",                MaterialDesignC.CONTENT_COPY,                  CLR_NAV);
        register("paste",               MaterialDesignC.CONTENT_PASTE,                 CLR_NAV);
        register("undo",                MaterialDesignU.UNDO,                          CLR_NAV);
        register("redo",                MaterialDesignR.REDO,                          CLR_NAV);

        // ── Automation Spy/Heal ──
        register("objectSpy",           MaterialDesignE.EYE,                           CLR_SEARCH);
        register("objectHeal",          FontAwesomeSolid.BAND_AID,                     CLR_SAVE);
        register("ORspy",               MaterialDesignE.EYE,                           CLR_SEARCH);
        register("ORHeal",              FontAwesomeSolid.BAND_AID,                     CLR_SAVE);
        register("imageSpy",            MaterialDesignI.IMAGE_SEARCH,                  CLR_SEARCH);
        register("mobileSpy",           MaterialDesignC.CELLPHONE_SCREENSHOT,          CLR_SEARCH);
        register("recorder",            MaterialDesignR.RECORD_CIRCLE,                 CLR_RECORD);
        register("appStore",            MaterialDesignC.CELLPHONE_LINK,                CLR_TOOL);

        // ── Status/Connection ──
        register("bulb_yellow",         MaterialDesignL.LIGHTBULB,                     Color.web("#D4880F"));
        register("bulb_green",          MaterialDesignL.LIGHTBULB_ON,                  CLR_SAVE);
        register("bulb_red",            MaterialDesignL.LIGHTBULB_OFF,                 CLR_STOP);

        // ── Misc / Explorer ──
        register("explorer",            MaterialDesignC.COMPASS,                       CLR_TOOL);
        register("checked",             MaterialDesignC.CHECK_BOX_OUTLINE,             CLR_SAVE);
        register("favicon",             MaterialDesignF.FLASK,                         CLR_CREATE);
        register("console",             MaterialDesignC.CONSOLE,                       CLR_NAV);
        register("filter",              MaterialDesignF.FILTER,                        CLR_CONFIG);
        register("export",              MaterialDesignE.EXPORT,                        CLR_TOOL);
        register("pull",                MaterialDesignD.DOWNLOAD,                      CLR_TOOL);
        register("Inject",              MaterialDesignI.IMPORT,                        CLR_DATA);
        
        // ── Code Editors ──
        register("format",              MaterialDesignA.AUTO_FIX,                      CLR_TOOL);
        register("beautify",            MaterialDesignT.TEXT_BOX_OUTLINE,              CLR_TOOL);

        // ── Toolbar actions (used via Utils.getIconByResourceName) ──
        register("tag",                 MaterialDesignT.TAG,                           CLR_DATA);
        register("tagsel",              MaterialDesignT.TAG_OUTLINE,                   CLR_DATA);
        register("uponelevel",          MaterialDesignA.ARROW_UP_BOLD,                 CLR_NAV);
        register("propViewer",          MaterialDesignE.EYE_OUTLINE,                   CLR_SEARCH);

        // ── Dock icons ──
        register("testplan",            MaterialDesignF.FLASK,                         CLR_DATA);
        register("testlab",             MaterialDesignB.BEAKER,                        CLR_DATA);
        register("objectrepository",    MaterialDesignD.DATABASE,                      CLR_DATA);

        // ── Menu icons (used via icon name extraction) ──
        register("FileMenu",            MaterialDesignF.FILE_DOCUMENT,                 CLR_FILE);
        register("AutomationMenu",      MaterialDesignR.ROBOT,                         CLR_SEARCH);
        register("TestDataMenu",        MaterialDesignD.DATABASE_IMPORT,               CLR_DATA);
        register("ConfigurationsMenu",  MaterialDesignC.COG,                           CLR_CONFIG);
        register("ToolsMenu",           MaterialDesignT.TOOLBOX,                       CLR_TOOL);
        register("WindowMenu",          MaterialDesignM.MONITOR,                       CLR_NAV);
        register("HelpMenu",            MaterialDesignH.HELP_CIRCLE,                   CLR_INFO);

        // ── Test Execution ──
        register("testExecution.export", MaterialDesignE.EXPORT,                       CLR_TOOL);
        register("testExecution.pull",   MaterialDesignD.DOWNLOAD,                     CLR_TOOL);

        // ── Debug ──
        register("debug.stepover",       MaterialDesignD.DEBUG_STEP_OVER,              CLR_DEBUG);
        register("debug.stepinto",       MaterialDesignD.DEBUG_STEP_INTO,              CLR_DEBUG);
        register("debug.stepout",        MaterialDesignD.DEBUG_STEP_OUT,               CLR_DEBUG);
        register("debug.continue",       MaterialDesignP.PLAY,                         CLR_RUN);
        register("debug.pause",          MaterialDesignP.PAUSE,                        Color.web("#D4880F"));

        // ── StartUp / Project ──
        register("startup.recent",       MaterialDesignH.HISTORY,                      CLR_NAV);
        register("startup.application",  MaterialDesignA.APPLICATION,                  CLR_TOOL);
        register("startup.system",       MaterialDesignF.FOLDER_SEARCH,                CLR_OPEN);
        register("startup.new",          MaterialDesignP.PLUS_CIRCLE,                  CLR_CREATE);
        register("startup.create",       MaterialDesignR.ROCKET_LAUNCH,                CLR_CREATE);
        register("startup.project",      MaterialDesignF.FOLDER_COG,                   CLR_CONFIG);
        register("startup.folder",       MaterialDesignF.FOLDER,                       CLR_OPEN);

        // ══════════════════════════════════════════════════════════════════
        // ══ PNG Replacement Icons ══
        // These replace bitmap PNGs with scalable vector icons
        // ══════════════════════════════════════════════════════════════════

        // ── Add/Remove/Delete (most used) ──
        register("icon.add",             MaterialDesignP.PLUS_CIRCLE,                  CLR_CREATE);
        register("icon.addNew",          MaterialDesignP.PLUS_BOX,                     CLR_CREATE);
        register("icon.addIcon",         MaterialDesignP.PLUS,                         CLR_CREATE);
        register("icon.remove",          MaterialDesignM.MINUS_CIRCLE,                 CLR_STOP);
        register("icon.rem",             MaterialDesignM.MINUS,                        CLR_STOP);
        register("icon.delete",          MaterialDesignD.DELETE,                       CLR_STOP);
        register("icon.deleteIcon",      MaterialDesignD.DELETE_OUTLINE,               CLR_STOP);

        // ── Screenshot/Camera ──
        register("icon.screenshot",      MaterialDesignC.CAMERA,                       CLR_DATA);
        register("icon.snap",            MaterialDesignC.CAMERA_IRIS,                  CLR_DATA);
        register("icon.crop",            MaterialDesignC.CROP,                         CLR_DATA);

        // ── Status Bulbs ──
        register("icon.bulb_yellow",     MaterialDesignL.LIGHTBULB,                    Color.web("#F5A623"));
        register("icon.bulb_green",      MaterialDesignL.LIGHTBULB_ON,                 CLR_SAVE);
        register("icon.bulb_red",        MaterialDesignL.LIGHTBULB_OFF,                CLR_STOP);

        // ── Edit/View Icons ──
        register("icon.testedit",        MaterialDesignP.PENCIL,                       CLR_DATA);
        register("icon.iedit",           MaterialDesignP.PENCIL_BOX,                   CLR_DATA);
        register("icon.editIcon",        MaterialDesignP.PENCIL_OUTLINE,               CLR_DATA);
        register("icon.spy",             MaterialDesignE.EYE,                          CLR_SEARCH);
        register("icon.Settings",        MaterialDesignC.COG,                          CLR_CONFIG);

        // ── Save/Export ──
        register("icon.save",            MaterialDesignC.CONTENT_SAVE,                 CLR_SAVE);
        register("icon.saveproj",        MaterialDesignC.CONTENT_SAVE_ALL,             CLR_SAVE);
        register("icon.csave",           MaterialDesignC.CONTENT_SAVE_OUTLINE,         CLR_SAVE);
        register("icon.export",          MaterialDesignE.EXPORT,                       CLR_TOOL);
        register("icon.import",          MaterialDesignI.IMPORT,                       CLR_DATA);

        // ── Close/Done ──
        register("icon.close",           MaterialDesignC.CLOSE_CIRCLE,                 CLR_STOP);
        register("icon.close16",         MaterialDesignC.CLOSE,                        CLR_STOP);
        register("icon.done",            MaterialDesignC.CHECK_CIRCLE,                 CLR_SAVE);
        register("icon.checked",         MaterialDesignC.CHECK,                        CLR_SAVE);
        register("icon.warning",         MaterialDesignA.ALERT,                        Color.web("#F5A623"));

        // ── Bug/Debug ──
        register("icon.bug",             MaterialDesignB.BUG,                          Color.web("#FF5722"));
        register("icon.heal",            MaterialDesignH.HEART_PULSE,                  Color.web("#E91E63"));

        // ── Navigation ──
        register("icon.backButton",      MaterialDesignA.ARROW_LEFT,                   CLR_NAV);
        register("icon.forwardButton",   MaterialDesignA.ARROW_RIGHT,                  CLR_NAV);
        register("icon.refresh",         MaterialDesignR.REFRESH,                      CLR_TOOL);
        register("icon.ref",             MaterialDesignL.LINK_VARIANT,                 CLR_TOOL);

        // ── Record ──
        register("icon.record_on",       MaterialDesignR.RECORD_CIRCLE,                CLR_STOP);
        register("icon.record_off",      MaterialDesignR.RECORD_CIRCLE_OUTLINE,        CLR_NAV);

        // ── Page/Document ──
        register("icon.pageOpen",        MaterialDesignB.BOOK_OPEN,                    CLR_DATA);
        register("icon.pageLock",        MaterialDesignB.BOOK_LOCK,                    CLR_DATA);
        register("icon.objects",         MaterialDesignC.CUBE_OUTLINE,                 CLR_DATA);
        register("icon.lock",            MaterialDesignL.LOCK,                         CLR_DATA);

        // ── Load/Download ──
        register("icon.loadIcon",        MaterialDesignD.DOWNLOAD,                     CLR_TOOL);
        register("icon.loadFromFileIcon", MaterialDesignF.FILE_DOWNLOAD,               CLR_TOOL);
        register("icon.Inject",          MaterialDesignC.CODE_BRACES,                  CLR_DATA);

        // ── Time/Latest ──
        register("icon.latest",          MaterialDesignC.CLOCK_OUTLINE,                CLR_DATA);
        register("icon.latestbub",       MaterialDesignC.CLOCK,                        CLR_DATA);

        // ── Details/Lists ──
        register("icon.detailed",        MaterialDesignF.FORMAT_LIST_BULLETED_TYPE,    CLR_DATA);
        register("icon.detailedbub",     MaterialDesignF.FORMAT_LIST_BULLETED,         CLR_DATA);

        // ── Explorer/Settings ──
        register("icon.explorer",        MaterialDesignF.FOLDER_SEARCH,                CLR_TOOL);
        register("icon.exploreSettings", MaterialDesignC.COG_OUTLINE,                  CLR_CONFIG);
        register("icon.exploreSettingsSel", MaterialDesignC.COG,                       CLR_CONFIG);

        // ── Platform Icons ──
        register("icon.android",         MaterialDesignA.ANDROID,                      Color.web("#3DDC84"));
        register("icon.apple",           MaterialDesignA.APPLE,                        CLR_NAV);
        register("icon.browser",         MaterialDesignW.WEB,                          CLR_TOOL);

        // ── Selected State Variants (lighter purple) ──
        register("icon.selected_add",    MaterialDesignP.PLUS_CIRCLE,                  Color.web("#BB86FC"));
        register("icon.selected_bug",    MaterialDesignB.BUG,                          Color.web("#BB86FC"));
        register("icon.selected_close",  MaterialDesignC.CLOSE_CIRCLE,                 Color.web("#BB86FC"));
        register("icon.selected_crop",   MaterialDesignC.CROP,                         Color.web("#BB86FC"));
        register("icon.selected_done",   MaterialDesignC.CHECK_CIRCLE,                 Color.web("#BB86FC"));
        register("icon.selected_export", MaterialDesignE.EXPORT,                       Color.web("#BB86FC"));
        register("icon.selected_iedit",  MaterialDesignP.PENCIL_BOX,                   Color.web("#BB86FC"));
        register("icon.selected_snap",   MaterialDesignC.CAMERA_IRIS,                  Color.web("#BB86FC"));
        register("icon.selected_testedit", MaterialDesignP.PENCIL,                     Color.web("#BB86FC"));

        // ── Import Menu Icons ──
        register("icon.ImportPlaywrightRecordingMenu", MaterialDesignI.IMPORT,         CLR_DATA);
    }

    // ── JavaFX Icon API ──

    /**
     * Creates a JavaFX FontIcon for the given logical icon name.
     * Returns null if the name is not mapped.
     *
     * @param name logical icon name (e.g. "NewProject", "testplan.Root")
     * @param size icon size in pixels
     * @return a styled FontIcon node, or null
     */
    public static org.kordamp.ikonli.javafx.FontIcon fx(String name, int size) {
        return fx(name, size, null);
    }

    /**
     * Creates a JavaFX FontIcon using the icon's registered semantic color.
     * Falls back to ING_BURGUNDY if no color is registered.
     *
     * @param name logical icon name
     * @param size icon size in pixels
     * @return a colorful FontIcon node, or null
     */
    public static org.kordamp.ikonli.javafx.FontIcon fxColored(String name, int size) {
        Color color = COLOR_MAP.getOrDefault(name, ING_BURGUNDY);
        return fx(name, size, color);
    }

    /**
     * Creates a JavaFX FontIcon with a custom color.
     *
     * @param name  logical icon name
     * @param size  icon size in pixels
     * @param color icon color (null = use default ING_BURGUNDY)
     * @return a styled FontIcon node, or null
     */
    public static org.kordamp.ikonli.javafx.FontIcon fx(String name, int size, Color color) {
        Ikon ikon = ICON_MAP.get(name);
        if (ikon == null) return null;
        org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(ikon);
        icon.setIconSize(size);
        icon.setIconColor(color != null ? color : ING_BURGUNDY);
        return icon;
    }

    /**
     * Creates a JavaFX FontIcon from an Ikon enum directly.
     */
    public static org.kordamp.ikonli.javafx.FontIcon fx(Ikon ikon, int size, Color color) {
        org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon(ikon);
        icon.setIconSize(size);
        icon.setIconColor(color != null ? color : ING_BURGUNDY);
        return icon;
    }

    // ── Swing Icon API ──

    /**
     * Creates a Swing Icon for the given logical icon name.
     * Returns null if the name is not mapped.
     *
     * @param name logical icon name
     * @param size icon size in pixels
     * @return a Swing-compatible Icon, or null
     */
    public static javax.swing.Icon swing(String name, int size) {
        return swing(name, size, (java.awt.Color) null);
    }

    /**
     * Creates a Swing Icon with a custom color.
     *
     * @param name  logical icon name
     * @param size  icon size in pixels
     * @param color icon color (null = use theme text color or ING_BURGUNDY)
     * @return a Swing-compatible Icon, or null
     */
    public static javax.swing.Icon swing(String name, int size, java.awt.Color color) {
        Ikon ikon = ICON_MAP.get(name);
        if (ikon == null) return null;
        java.awt.Color c = color;
        if (c == null) {
            c = UIManager.getColor("text");
            if (c == null) c = java.awt.Color.decode("#4D0020");
        }
        return org.kordamp.ikonli.swing.FontIcon.of(ikon, size, c);
    }

    /**
     * Creates a Swing Icon from an Ikon enum directly.
     */
    public static javax.swing.Icon swing(Ikon ikon, int size, java.awt.Color color) {
        java.awt.Color c = color;
        if (c == null) {
            c = UIManager.getColor("text");
            if (c == null) c = java.awt.Color.decode("#4D0020");
        }
        return org.kordamp.ikonli.swing.FontIcon.of(ikon, size, c);
    }

    /**
     * Detects if the current Look and Feel is a dark theme.
     */
    private static boolean isDarkMode() {
        // FlatLaf sets this property; also check background brightness as fallback
        java.awt.Color bg = UIManager.getColor("Panel.background");
        if (bg != null) {
            // Calculate perceived brightness (0-255)
            double brightness = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue());
            return brightness < 128;
        }
        return false;
    }

    /**
     * Creates a Swing Icon using the icon's registered semantic color.
     * Automatically uses brighter colors for dark mode themes.
     *
     * @param name logical icon name
     * @param size icon size in pixels
     * @return a colorful Swing-compatible Icon, or null
     */
    public static javax.swing.Icon swingColored(String name, int size) {
        Color fxColor;
        // Check for dark mode and use dark mode colors if available
        if (isDarkMode() && COLOR_MAP_DARK.containsKey(name)) {
            fxColor = COLOR_MAP_DARK.get(name);
        } else {
            fxColor = COLOR_MAP.get(name);
        }
        if (fxColor == null) return swing(name, size, (java.awt.Color) null);
        java.awt.Color awtColor = new java.awt.Color(
                (int)(fxColor.getRed() * 255),
                (int)(fxColor.getGreen() * 255),
                (int)(fxColor.getBlue() * 255));
        return swing(name, size, awtColor);
    }

    /**
     * Checks whether a logical icon name is registered.
     */
    public static boolean has(String name) {
        return ICON_MAP.containsKey(name);
    }

    /**
     * Gets the Ikon enum for a logical icon name.
     */
    public static Ikon get(String name) {
        return ICON_MAP.get(name);
    }

    /**
     * Convenience method to convert a named icon to a BufferedImage.
     * Useful for setIconImage() on JFrame/JDialog.
     */
    public static java.awt.image.BufferedImage toImage(String name, int size) {
        return toImage(swingColored(name, size));
    }

    /**
     * Converts any Swing Icon to a BufferedImage.
     * Useful for setIconImage() on JFrame/JDialog.
     */
    public static java.awt.image.BufferedImage toImage(javax.swing.Icon icon) {
        if (icon == null) {
            return new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        }
        if (icon instanceof javax.swing.ImageIcon) {
            java.awt.Image img = ((javax.swing.ImageIcon) icon).getImage();
            if (img instanceof java.awt.image.BufferedImage) {
                return (java.awt.image.BufferedImage) img;
            }
            java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                    img.getWidth(null), img.getHeight(null), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = bi.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            return bi;
        }
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0) w = 16;
        if (h <= 0) h = 16;
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = bi.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return bi;
    }
}
