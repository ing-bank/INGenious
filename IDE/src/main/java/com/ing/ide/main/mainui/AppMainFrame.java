package com.ing.ide.main.mainui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.settings.testmgmt.Option;
import com.ing.datalib.settings.testmgmt.TestMgModule;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.util.data.FileScanner;
import com.ing.engine.core.TMIntegration;
import com.ing.ide.main.Main;
import com.ing.ide.main.dashboard.server.DashBoardManager;
import com.ing.ide.main.fx.FXDashBoard;
import com.ing.ide.main.fx.FXMenuBar;
import com.ing.ide.main.fx.FXStatusBar;
import com.ing.ide.main.fx.FXToolBar;
import com.ing.ide.main.mainui.components.aichat.AICopilot;
import com.ing.ide.main.mainui.components.apitester.APITester;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testexecution.TestExecution;
import com.ing.ide.main.shr.SHR;
import com.ing.ide.main.tour.TourManager;
import com.ing.ide.main.ui.About;
import com.ing.ide.main.ui.FXStartUp;
import com.ing.ide.main.utils.AppIcon;
import com.ing.ide.main.utils.LoaderScreen;
import com.ing.ide.main.utils.StepMap;
import com.ing.ide.main.utils.recentItem.RecentItems;
import com.ing.ide.settings.AppSettings;
import com.ing.ide.util.Notification;
import com.ing.ide.util.SystemInfo;
import com.ing.ide.util.Utility;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.commons.codec.binary.Base64;

public class AppMainFrame extends JFrame {
    private final SlideShow slideShow;

    private final SimpleDock docker;

    /** Split pane hosting the main SlideShow (left) and the AI sidebar (right). */
    private JSplitPane centerSplit;

    /** Last non-zero sidebar width, restored when the sidebar is re-shown. */
    private int aiSidebarWidth = 360;

    private boolean aiSidebarVisible = false;

    /** True while the divider is being positioned programmatically. */
    private boolean applyingAISidebar = false;
    private final AppMenuBar menuBar;

    private final AppToolBar toolBar;

    private FXMenuBar fxMenuBar;

    private FXToolBar fxToolBar;

    private FXStatusBar fxStatusBar;

    private final TestDesign testDesign;

    private final TestExecution testExecution;

    private final APITester apiTester;

    private final AICopilot aiCopilot;

    private final FXDashBoard dashBoard;

    private final DashBoardManager dashBoardManager;

    private final SHR spyHealReco;

    private final AppActionListener sActionListener;

    private final RecentItems recentItems;

    private final FXStartUp startUp;

    private final StepMap stepMap;

    private Project sProject;

    private final LoaderScreen loader;

    private QUIT_TYPE quitType = QUIT_TYPE.NORMAL;

    private enum QUIT_TYPE {
        NORMAL,
        FORCE,
        RESTART
    }

    private Consumer<Integer> onProgress;

    public AppMainFrame() throws IOException {
        this(null);
    }

    public AppMainFrame(Consumer<Integer> onProgress) throws IOException {
        this.onProgress = onProgress;
        recentItems = new RecentItems(this);
        startUp = new FXStartUp(this);
        progressed(25);

        toolBar = new AppToolBar(null);
        sActionListener = new AppActionListener(this, toolBar);
        toolBar.setActionListener(sActionListener);

        slideShow = new SlideShow();
        docker = new SimpleDock(this);
        progressed(35);
        testDesign = new TestDesign(this);
        progressed(45);
        testExecution = new TestExecution(this);
        progressed(50);
        apiTester = new APITester(this);
        progressed(52);
        aiCopilot = new AICopilot(this);
        dashBoard = new FXDashBoard(testExecution);
        progressed(60);
        dashBoardManager = new DashBoardManager(this);
        spyHealReco = new SHR(this);
        progressed(70);
        menuBar = new AppMenuBar(sActionListener);
        // toolBar = new AppToolBar(sActionListener);
        stepMap = new StepMap();
        loader = new LoaderScreen();
        progressed(75);
        init();
    }

    private void init() {
        // Use multi-resolution icons for elegant dock/taskbar display
        AppIcon.applyTo(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle(getAppTitle());
        setLayout(new BorderLayout());
        // Ensure content pane uses theme background for dark mode
        ((JPanel) getContentPane()).setOpaque(true);
        getContentPane().setBackground(UIManager.getColor("Panel.background"));
        setGlassPane(docker);
        // Start with Swing chrome (FX chrome added later via initFXChrome)
        setJMenuBar(menuBar);
        progressed(80);
        slideShow.addSlide("TestDesign", testDesign.getTestDesignUI());
        slideShow.addSlide("TestExecution", testExecution.getTestExecutionUI());
        slideShow.addSlide("DashBoard", dashBoard);
        slideShow.addSlide("APITester", apiTester.getAPITesterUI());
        slideShow.addSlideChangeListener(aiCopilot);
        progressed(85);
        add(buildCenter(), BorderLayout.CENTER);
        add(toolBar, BorderLayout.NORTH);
        add(simpleFiller(), BorderLayout.WEST);
        dashBoard.load();
        loader.setFrame(this);
        addWindowListener(
            new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent we) {
                    if (iCanQuit()) {
                        // Close StoryWriter editor if open
                        if (sActionListener != null) {
                            sActionListener.closeBddEditorIfOpen();
                        }
                        setDefaultCloseOperation(AppMainFrame.EXIT_ON_CLOSE);
                        if (quitType == QUIT_TYPE.RESTART) {
                            doRestart();
                        }
                        dispose();
                    }
                }
            }
        );
        progressed(90);
    }

    private void progressed(int val) {
        if (Objects.nonNull(this.onProgress)) {
            this.onProgress.accept(val);
        } else {
            onProgressed(val);
        }
    }

    public void onProgressed(int val) {}

    private JPanel simpleFiller() {
        JPanel filler = new JPanel();
        filler.setPreferredSize(new java.awt.Dimension(4, 0));
        filler.setOpaque(true);
        // Use subtle grey for dark theme, default panel background for light theme
        if (com.ing.ide.main.Main.isDarkMode()) {
            filler.setBackground(new java.awt.Color(50, 52, 55)); // Subtle dark grey
        } else {
            filler.setBackground(UIManager.getColor("Panel.background"));
        }

        filler.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent me) {
                    setGlassPane(docker);
                    SwingUtilities.invokeLater(
                        () -> {
                            getGlassPane().setVisible(true);
                        }
                    );
                }
            }
        );
        return filler;
    }

    /**
     * Swaps Swing menu bar and toolbar for JavaFX CSS-styled versions.
     * Must be called on the EDT AFTER the frame is visible and maximized.
     * This deferred approach prevents the macOS Glass native crash (NSTrackingRectTag)
     * that occurs when JFXPanel is resized before its native peer is ready.
     */
    public void initFXChrome() {
        fxMenuBar = new FXMenuBar(sActionListener);
        fxToolBar = new FXToolBar(sActionListener);
        fxStatusBar = new FXStatusBar();

        // Remove Swing chrome
        setJMenuBar(null);
        remove(toolBar);

        // Add FX chrome at top
        JPanel chromePanel = new JPanel();
        chromePanel.setLayout(new BoxLayout(chromePanel, BoxLayout.Y_AXIS));
        chromePanel.add(fxMenuBar);
        chromePanel.add(fxToolBar);
        add(chromePanel, BorderLayout.NORTH);

        // Add FX status bar at bottom
        add(fxStatusBar, BorderLayout.SOUTH);

        // Sync multi-env state if project is loaded
        if (sProject != null) {
            Boolean isMulti = sProject.getTestData().getNoOfEnvironments() > 1;
            fxMenuBar.setMultiEnvironment(isMulti);
            fxStatusBar.setProjectName(sProject.getName());
        }

        // Initialize Recent Projects menu
        fxMenuBar.updateRecentProjects(recentItems.getRECENT_ITEMS());

        fxStatusBar.setCurrentView("Test Design");

        revalidate();
        repaint();
    }

    public void showTestDesign() {
        getGlassPane().setVisible(false);
        slideShow.showSlide("TestDesign");
        if (fxStatusBar != null) fxStatusBar.setCurrentView("Test Design");
    }

    public void showTestExecution() {
        getGlassPane().setVisible(false);
        slideShow.showSlide("TestExecution");
        testExecution.getTestExecutionUI().adjustUI();
        if (fxStatusBar != null) fxStatusBar.setCurrentView("Test Execution");
    }

    public void showDashBoard() {
        getGlassPane().setVisible(false);
        slideShow.showSlide("DashBoard");
        dashBoard.loadTree();
        if (fxStatusBar != null) fxStatusBar.setCurrentView("DashBoard");
    }

    public void showAPITester() {
        getGlassPane().setVisible(false);
        slideShow.showSlide("APITester");
        if (fxStatusBar != null) fxStatusBar.setCurrentView("API Workbench");
    }

    public void showAICopilot() {
        getGlassPane().setVisible(false);
        toggleAISidebar();
        if (fxStatusBar != null) fxStatusBar.setCurrentView("AI Assistant");
    }

    /**
     * Builds the centre component: a horizontal split pane with the main
     * {@link SlideShow} on the left and the AI assistant sidebar on the right.
     * The sidebar starts collapsed and is toggled via {@link #toggleAISidebar()}.
     */
    private java.awt.Component buildCenter() {
        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setLeftComponent(slideShow);
        centerSplit.setRightComponent(aiCopilot.getAICopilotUI());
        centerSplit.setResizeWeight(1.0); // give extra space to the main view
        centerSplit.setContinuousLayout(true);
        centerSplit.setOneTouchExpandable(true);
        centerSplit.setBorder(null);

        // Restore persisted state.
        try {
            aiSidebarWidth =
                Integer.parseInt(
                    AppSettings.get(AppSettings.APP_SETTINGS.AI_SIDEBAR_WIDTH.getKey())
                );
        } catch (NumberFormatException ignore) {
            aiSidebarWidth = 360;
        }
        aiSidebarVisible =
            Boolean.parseBoolean(
                AppSettings.get(AppSettings.APP_SETTINGS.AI_SIDEBAR_VISIBLE.getKey())
            );

        // Persist width whenever the user drags the divider (only while visible).
        centerSplit.addPropertyChangeListener(
            JSplitPane.DIVIDER_LOCATION_PROPERTY,
            evt -> {
                // Ignore programmatic divider moves (show/hide/relayout); only
                // genuine user drags should update the persisted default width.
                if (applyingAISidebar) {
                    return;
                }
                if (aiSidebarVisible && centerSplit.getWidth() > 0) {
                    int w =
                        centerSplit.getWidth() -
                        centerSplit.getDividerLocation() -
                        centerSplit.getDividerSize();
                    if (w >= 200) {
                        aiSidebarWidth = w;
                        AppSettings.set(
                            AppSettings.APP_SETTINGS.AI_SIDEBAR_WIDTH.getKey(),
                            String.valueOf(w)
                        );
                    }
                }
            }
        );

        // Apply the initial visibility once the frame has a real size.
        SwingUtilities.invokeLater(() -> applyAISidebar(aiSidebarVisible));
        return centerSplit;
    }

    /** Shows the AI sidebar if hidden, hides it if shown. */
    public void toggleAISidebar() {
        setAISidebarVisible(!aiSidebarVisible);
    }

    /** Explicitly show or hide the AI sidebar and persist the choice. */
    public void setAISidebarVisible(boolean visible) {
        aiSidebarVisible = visible;
        AppSettings.set(
            AppSettings.APP_SETTINGS.AI_SIDEBAR_VISIBLE.getKey(),
            String.valueOf(visible)
        );
        AppSettings.store("AI sidebar state");
        applyAISidebar(visible);
    }

    public boolean isAISidebarVisible() {
        return aiSidebarVisible;
    }

    /** Applies the divider position for the current visibility on the EDT. */
    private void applyAISidebar(boolean visible) {
        if (centerSplit == null) {
            return;
        }
        java.awt.Component right = centerSplit.getRightComponent();
        if (right != null) {
            right.setVisible(visible);
        }
        int total = centerSplit.getWidth();
        if (total <= 0) {
            // Frame not laid out yet; retry after layout.
            SwingUtilities.invokeLater(() -> applyAISidebar(visible));
            return;
        }
        if (visible) {
            int clamped = Math.max(280, Math.min(aiSidebarWidth, total / 2));
            centerSplit.setDividerSize(8);
            final int targetDivider = total - clamped - centerSplit.getDividerSize();
            // Set the divider immediately for a correct first paint...
            applyingAISidebar = true;
            centerSplit.setDividerLocation(targetDivider);
            centerSplit.revalidate();
            applyingAISidebar = false;
            // ...then again after the freshly-revealed right component has been
            // laid out. On the first reveal Swing otherwise snaps the divider to
            // the component's minimum size (the "half open" state) until a later
            // interaction re-lays it out. Re-applying on a deferred EDT cycle
            // guarantees the sidebar always opens at its persisted default width.
            SwingUtilities.invokeLater(
                () -> {
                    int t = centerSplit.getWidth();
                    if (t <= 0) {
                        return;
                    }
                    int c = Math.max(280, Math.min(aiSidebarWidth, t / 2));
                    applyingAISidebar = true;
                    centerSplit.setDividerLocation(t - c - centerSplit.getDividerSize());
                    centerSplit.revalidate();
                    centerSplit.repaint();
                    applyingAISidebar = false;
                }
            );
        } else {
            applyingAISidebar = true;
            centerSplit.setDividerSize(0);
            centerSplit.setDividerLocation(total);
            centerSplit.revalidate();
            centerSplit.repaint();
            applyingAISidebar = false;
        }
    }

    private String getAppTitle() {
        return "INGenious " + About.getBuildVersion();
    }

    public String getCurrentSlide() {
        return slideShow.getCurrentCard();
    }

    public SlideShow getSlideShow() {
        return slideShow;
    }

    public Boolean isTestDesign() {
        return getCurrentSlide().equals("TestDesign");
    }

    public Boolean isTestExecution() {
        return getCurrentSlide().equals("TestExecution");
    }

    public Boolean isDashBoard() {
        return getCurrentSlide().equals("DashBoard");
    }

    public Boolean isAPITester() {
        return getCurrentSlide().equals("APITester");
    }

    public TestDesign getTestDesign() {
        return testDesign;
    }

    public TestExecution getTestExecution() {
        return testExecution;
    }

    public FXDashBoard getDashBoard() {
        return dashBoard;
    }

    public APITester getAPITester() {
        return apiTester;
    }

    public AICopilot getAICopilot() {
        return aiCopilot;
    }

    public DashBoardManager getDashBoardManager() {
        return dashBoardManager;
    }

    public SHR getSpyHealReco() {
        return spyHealReco;
    }

    public RecentItems getRecentItems() {
        return recentItems;
    }

    public StepMap getStepMap() {
        return stepMap;
    }

    public LoaderScreen getLoader() {
        return loader;
    }

    public void replaceToolBar(JToolBar newToolBar) {
        remove(toolBar);
        add(newToolBar, BorderLayout.NORTH);
    }

    public void resetToolBar(JToolBar oldToolBar) {
        remove(oldToolBar);
        add(toolBar, BorderLayout.NORTH);
    }

    // ── Tour ───────────────────────────────────────────────────────────────

    /**
     * Starts the tour unconditionally (e.g. from Help → Start Tour).
     * A small delay lets the UI settle before the overlay appears.
     */
    public void startTour() {
        javax.swing.Timer t = new javax.swing.Timer(350, e -> new TourManager(this).startTour());
        t.setRepeats(false);
        t.start();
    }

    /**
     * Shows the tour only if it has never been completed by this user.
     * Called automatically from {@link #afterProjectChange()} on first launch.
     */
    public void checkAndShowTour() {
        if (TourManager.shouldShowTour()) {
            startTour();
        }
    }

    /** Returns the JavaFX toolbar component (for spotlight targeting). */
    public com.ing.ide.main.fx.FXToolBar getFXToolBar() {
        return fxToolBar;
    }

    /** Returns the JavaFX menu bar component (for spotlight targeting). */
    public com.ing.ide.main.fx.FXMenuBar getFXMenuBar() {
        return fxMenuBar;
    }

    public void checkAndLoadRecent() {
        if (!AppSettings.canOpenRecentProjects() || recentItems.isEmpty()) {
            startUp.showIt();
        } else {
            setVisible(true);
            loadProject(recentItems.getRecentProjectLocation());
            toFront();
        }
    }

    public void loadProject(final String location) {
        beforeProjectChange();
        SwingUtilities.invokeLater(
            new Runnable() {

                @Override
                public void run() {
                    sProject = new Project(location);
                    if (sProject.getInfo().getVersion() == null) {
                        migrate(sProject);
                        Notification.show("Project Migration is done");
                        Logger
                            .getLogger(AppMainFrame.class.getName())
                            .log(Level.INFO, "Migration is Done ");
                    }
                    load();
                    afterProjectChange();
                }
            }
        );
    }

    private boolean migrate(Project project) {
        final String _Enc = " Enc";
        boolean isMigrted = true;
        try {
            //Updating new TM Properties
            List<TestMgModule> modules = project
                .getProjectSettings()
                .getTestMgmtModule()
                .getModules();
            for (TestMgModule module : modules) {
                List<Option> options = module.getOptions();
                for (Option option : options) {
                    String key = option.getName();
                    String value = option.getValue();
                    if (value != null && !value.isEmpty()) {
                        if (value.contains("TMENC:")) {
                            value = value.replaceFirst("TMENC:", "");
                            byte[] encoded = Base64.decodeBase64(value);
                            TMEncrypt(new String(encoded), module, option);
                        } else {
                            if (key.toLowerCase().contains("passw")) {
                                TMEncrypt(value, module, option);
                            }
                        }
                    }
                }
            }

            ObjectMapper objMapper = new ObjectMapper();
            List<TestMgModule> modules13 = objMapper.readValue(
                FileScanner.getResourceString("TMModules.json"),
                objMapper.getTypeFactory().constructCollectionType(List.class, TestMgModule.class)
            );
            modules13.forEach(
                module -> {
                    String modulename = module.getModule();
                    if (
                        modulename.equals("qTestManager") ||
                        modulename.equals("JiraCloud") ||
                        modulename.equals("TestRail")
                    ) {
                        Logger
                            .getLogger(AppMainFrame.class.getName())
                            .log(
                                Level.INFO,
                                "Adding 1.3 TM Module {0}  ",
                                new Object[] { module.getModule() }
                            );
                        project
                            .getProjectSettings()
                            .getTestMgmtModule()
                            .putValues(module.getModule(), module.getOptions());
                    }
                }
            );
            Logger
                .getLogger(AppMainFrame.class.getName())
                .log(Level.INFO, "Test Management settings are copied ");

            //Modify Encoding to Encryption in TestData and GlobalData
            Set<String> envs = project.getTestData().getEnvironments();
            envs.forEach(
                environments -> {
                    TestData testDataFor = project.getTestData().getTestDataFor(environments);
                    GlobalDataModel gbData = testDataFor.getGlobalData();
                    gbData.load();
                    int row_size = gbData.getRowCount();
                    int col_size = gbData.getColumnCount();

                    for (int row = 0; row < row_size; row++) {
                        for (int col = 0; col < col_size; col++) {
                            if (col == 0) {
                                continue;
                            }
                            String value = (String) gbData.getValueAt(row, col);
                            if (value != null && !value.trim().isEmpty()) {
                                if (value.endsWith(_Enc)) {
                                    value = value.substring(0, value.lastIndexOf(_Enc));
                                    Logger
                                        .getLogger(AppMainFrame.class.getName())
                                        .log(
                                            Level.INFO,
                                            "Migrating the {0} Environment {1} Global Data in the {2} row and {3} column",
                                            new Object[] {
                                                environments,
                                                gbData.getName(),
                                                row,
                                                col
                                            }
                                        );
                                    enableEncrypt(value, gbData, row, col);
                                }
                            }
                        }
                    }
                    gbData.saveChanges();
                    List<TestDataModel> testDataList = testDataFor.getTestDataList();
                    for (TestDataModel model : testDataList) {
                        model.load();
                        row_size = model.getRowCount();
                        col_size = model.getColumnCount();
                        for (int row = 0; row < row_size; row++) {
                            for (int col = 0; col < col_size; col++) {
                                if (col < 4) {
                                    continue;
                                }
                                String value = (String) model.getValueAt(row, col);
                                if (value != null && !value.trim().isEmpty()) {
                                    if (value.endsWith(_Enc)) {
                                        value = value.substring(0, value.lastIndexOf(_Enc));
                                        Logger
                                            .getLogger(AppMainFrame.class.getName())
                                            .log(
                                                Level.INFO,
                                                "Migrating the {0} Environment and {1} Test Data in the {2} row and {3} column",
                                                new Object[] {
                                                    environments,
                                                    model.getName(),
                                                    row,
                                                    col
                                                }
                                            );
                                        enableEncrypt(value, model, row, col);
                                    }
                                }
                            }
                        }
                        model.saveChanges();
                    }
                }
            );

            //Migrating Encrypted Actions
            List<Scenario> scenarios = project.getScenarios();
            for (Scenario scenario : scenarios) {
                List<TestCase> cases = scenario.getTestCases();
                enableEncryptTC(cases);
            }

            //Migrating Driver Settings
            Enumeration<Object> keysObj = project.getProjectSettings().getDriverSettings().keys();
            Iterator<Object> keys = keysObj.asIterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String property = project.getProjectSettings().getDriverSettings().getProperty(key);
                if (property != null && !property.isEmpty()) {
                    if (property.endsWith(_Enc)) {
                        property = property.substring(0, property.lastIndexOf(_Enc));
                        byte[] encoded = Base64.decodeBase64(property);
                        String encrypted = new String(encoded);
                        if (key.equals("proxyPassword")) {
                            encrypted = Utility.encrypt(new String(encoded));
                            Logger
                                .getLogger(AppMainFrame.class.getName())
                                .log(
                                    Level.INFO,
                                    "Migrating the Driver Settings Key {0} and Value {1}",
                                    new Object[] { key, encrypted }
                                );
                        }
                        project.getProjectSettings().getDriverSettings().put(key, encrypted);
                    } else {
                        if (key.equals("proxyPassword")) {
                            String encrypted = Utility.encrypt(property);
                            Logger
                                .getLogger(AppMainFrame.class.getName())
                                .log(
                                    Level.INFO,
                                    "Migrating the Driver Settings Key {0} and Value {1}",
                                    new Object[] { key, encrypted }
                                );
                            project.getProjectSettings().getDriverSettings().put(key, encrypted);
                        }
                    }
                }
            }

            //Migarting Test Management settings at Release level
            List<Release> releases = project.getReleases();
            for (Release release : releases) {
                List<TestSet> testsets = release.getTestSets();
                for (TestSet testset : testsets) {
                    Enumeration<Object> keys1 = project
                        .getProjectSettings()
                        .getExecSettings(release.getName(), testset.getName())
                        .getTestMgmgtSettings()
                        .keys();
                    Iterator<Object> keysls = keys1.asIterator();
                    while (keysls.hasNext()) {
                        String key = (String) keysls.next();
                        String property = project
                            .getProjectSettings()
                            .getExecSettings(release.getName(), testset.getName())
                            .getTestMgmgtSettings()
                            .getProperty(key);
                        if (property != null && !property.isEmpty()) {
                            if (property.contains("TMENC:")) {
                                property = property.replaceFirst("TMENC:", "");
                                byte[] encoded = Base64.decodeBase64(property);
                                String encrypt = TMIntegration.encrypt(new String(encoded));
                                Logger
                                    .getLogger(AppMainFrame.class.getName())
                                    .log(
                                        Level.INFO,
                                        "Migrating the Execution Settings of {0} Release ->  {1} Testset . Key {2} and Value {3}",
                                        new Object[] {
                                            release.getName(),
                                            testset.getName(),
                                            key,
                                            encrypt
                                        }
                                    );
                                project
                                    .getProjectSettings()
                                    .getExecSettings(release.getName(), testset.getName())
                                    .getTestMgmgtSettings()
                                    .put(key, encrypt);
                            } else {
                                if (key.toLowerCase().contains("passw")) {
                                    String encrypt = TMIntegration.encrypt(property);
                                    Logger
                                        .getLogger(AppMainFrame.class.getName())
                                        .log(
                                            Level.INFO,
                                            "Migrating the Execution Settings of {0} Release ->  {1} Testset . Key {2} and Value {3}",
                                            new Object[] {
                                                release.getName(),
                                                testset.getName(),
                                                key,
                                                encrypt
                                            }
                                        );
                                    project
                                        .getProjectSettings()
                                        .getExecSettings(release.getName(), testset.getName())
                                        .getTestMgmgtSettings()
                                        .put(key, encrypt);
                                }
                            }
                        }
                    }
                }
            }

            //Migarting Test Management settings at Design level
            Enumeration<Object> keys1 = project
                .getProjectSettings()
                .getExecSettings()
                .getTestMgmgtSettings()
                .keys();
            Iterator<Object> keysls = keys1.asIterator();
            while (keysls.hasNext()) {
                String key = (String) keysls.next();
                String property = project
                    .getProjectSettings()
                    .getExecSettings()
                    .getTestMgmgtSettings()
                    .getProperty(key);
                if (property != null && !property.isEmpty()) {
                    if (property.contains("TMENC:")) {
                        property = property.replaceFirst("TMENC:", "");
                        byte[] encoded = Base64.decodeBase64(property);
                        String encrypt = TMIntegration.encrypt(new String(encoded));
                        Logger
                            .getLogger(AppMainFrame.class.getName())
                            .log(
                                Level.INFO,
                                "Migrating the Execution Settings Key {0} and Value {1}",
                                new Object[] { key, encrypt }
                            );
                        project
                            .getProjectSettings()
                            .getExecSettings()
                            .getTestMgmgtSettings()
                            .put(key, encrypt);
                    } else {
                        if (key.toLowerCase().contains("passw")) {
                            String encrypt = TMIntegration.encrypt(property);
                            Logger
                                .getLogger(AppMainFrame.class.getName())
                                .log(
                                    Level.INFO,
                                    "Migrating the Execution Settings Key {0} and Value {1}",
                                    new Object[] { key, encrypt }
                                );
                            project
                                .getProjectSettings()
                                .getExecSettings()
                                .getTestMgmgtSettings()
                                .put(key, encrypt);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Notification.show("Project Migration is not successful. Refer logs");
            Logger
                .getLogger(AppMainFrame.class.getName())
                .log(Level.SEVERE, "Migration is not successful", ex.getMessage());
            return false;
        }

        project.getInfo().setVersion(About.getBuildVersion());
        project.save();
        return isMigrted;
    }

    private void TMEncrypt(String value, TestMgModule module, Option option) {
        String encrypt = TMIntegration.encrypt(value);
        Logger
            .getLogger(AppMainFrame.class.getName())
            .log(
                Level.INFO,
                "Migrating the {0} TM Module. Property {1} value {2} ",
                new Object[] { module.getModule(), option.getName(), encrypt }
            );
        option.setValue(encrypt);
    }

    private void enableEncryptTC(List<TestCase> cases) {
        for (TestCase tcase : cases) {
            tcase.loadTableModel();
            List<TestStep> steps = tcase.getTestSteps();
            for (TestStep step : steps) {
                if (step.getAction().contains("Encrypted")) {
                    String input = step.getInput();
                    if (input != null && input.startsWith("@")) {
                        input = input.substring(1);
                        input = input.substring(0, input.lastIndexOf(" Enc"));
                        byte[] decode = Base64.decodeBase64(input);
                        String encrypted = Utility.encrypt(new String(decode));
                        step.setInput("@" + encrypted);
                        Logger
                            .getLogger(AppMainFrame.class.getName())
                            .log(
                                Level.INFO,
                                "Encrypting the value in {0}_{1} -> Data in the step {2} ",
                                new Object[] {
                                    tcase.getScenario().getName(),
                                    tcase.getName(),
                                    step
                                }
                            );
                    }
                }
            }
        }
    }

    private void enableEncrypt(String value, AbstractDataModel model, int row, int col) {
        byte[] decode = Base64.decodeBase64(value);
        String encrypted = Utility.encrypt(new String(decode));
        model.setValueAt(encrypted, row, col);
    }

    public void createProject(final String name, final String location, final String testDatatype) {
        beforeProjectChange();
        SwingUtilities.invokeLater(
            () -> {
                sProject = new Project(name, location, testDatatype).createProject();
                load();
                afterProjectChange();
                saveLoadedProject();
            }
        );
    }

    public void saveLoadedProject() {
        if (sProject != null && !sProject.getName().isEmpty()) {
            sProject.save();
            testDesign.save();
            apiTester.saveData();
        }
    }

    private void load() {
        testDesign.load();
        testExecution.load();
        spyHealReco.load();
    }

    public void save() {
        if (sProject != null) {
            saveLoadedProject();
            Notification.show("Project [" + sProject.getName() + "] Saved");
        }
    }

    public void autoSave() {
        if (sProject != null) {
            saveLoadedProject();
        }
    }

    public Project getProject() {
        return sProject;
    }

    public Boolean renameProject(String newProjName) {
        saveLoadedProject();
        if (sProject.rename(newProjName)) {
            loadProject(sProject.getLocation());
            return true;
        }
        return false;
    }

    public void reloadBrowsers() {
        testDesign.reloadBrowsers();
        testExecution.reloadBrowsers();
    }

    public void reloadSettings() {
        testExecution.getTestSetComp().reloadSettings();
    }

    /**
     * Reloads the open project from disk in place and refreshes every view, so
     * changes made outside the IDE's in-memory model — e.g. by the AI assistant
     * or CLI/MCP tools (new test cases, steps, data rows, environments,
     * reusables, Object Repository entries) — appear immediately without a
     * restart or project switch.
     *
     * <p>Preserves tree expansion/selection and re-opens the currently displayed
     * test case. Does <b>not</b> save the in-memory model first, so it never
     * clobbers the external changes it is picking up. Safe to call from any
     * thread.</p>
     */
    public void reloadProject() {
        if (sProject == null || sProject.getName() == null || sProject.getName().isEmpty()) {
            return;
        }
        Runnable task = () -> {
            try {
                // Remember what the user has open so the reload isn't disruptive.
                String scenarioName = null;
                String testCaseName = null;
                com.ing.datalib.component.TestCase openTc = testDesign
                    .getTestCaseComp()
                    .getCurrentTestCase();
                if (openTc != null) {
                    testCaseName = openTc.getName();
                    scenarioName =
                        openTc.getScenario() == null ? null : openTc.getScenario().getName();
                }
                com.ing.ide.main.utils.tree.TreeStateSaver.State treeState = com.ing.ide.main.utils.tree.TreeStateSaver.capture(
                    testDesign.getProjectTree().getTree()
                );

                sProject.reload(); // re-read scenarios/testcases/data/env/reusables/OR from disk
                load(); // rebuild TestDesign + TestExecution views from the reloaded model

                // Re-open the same test case in the editor (if it still exists).
                if (scenarioName != null && testCaseName != null) {
                    com.ing.datalib.component.Scenario sc = sProject.getScenarioByName(
                        scenarioName
                    );
                    if (sc == null) {
                        sc = sProject.getReusableScenarioByName(scenarioName);
                    }
                    if (sc != null) {
                        com.ing.datalib.component.TestCase tc = sc.getTestCaseByName(testCaseName);
                        if (tc != null) {
                            testDesign.getTestCaseComp().loadTableModelForSelection(tc);
                        }
                    }
                }
                com.ing.ide.main.utils.tree.TreeStateSaver.restore(
                    testDesign.getProjectTree().getTree(),
                    treeState
                );
                afterProjectChange();
            } catch (Exception ex) {
                Logger
                    .getLogger(AppMainFrame.class.getName())
                    .log(Level.WARNING, "reloadProject failed", ex);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    void beforeProjectChange() {
        saveLoadedProject();
    }

    void afterProjectChange() {
        recentItems.addItem(sProject);
        testDesign.afterProjectChange();
        SwingUtilities.invokeLater(this::checkAndShowTour);
        testExecution.afterProjectChange();
        dashBoard.loadTree();
        dashBoardManager.onProjectChanged();
        apiTester.loadData();
        apiTester.registerSlideChangeListener(); // Register to listen for panel switches
        sActionListener.afterProjectChange();
        setTitle(sProject.getName() + " - " + getAppTitle());
        // Sync multi-environment state to both Swing and FX menus
        menuBar.setMultiEnvironment();
        if (fxMenuBar != null) {
            Boolean isMulti = sProject.getTestData().getNoOfEnvironments() > 1;
            fxMenuBar.setMultiEnvironment(isMulti);
            // Update Recent Projects menu
            fxMenuBar.updateRecentProjects(recentItems.getRECENT_ITEMS());
        }
        // Update status bar project name
        if (fxStatusBar != null) {
            fxStatusBar.setProjectName(sProject.getName());
        }
    }

    public void adjustUI() {
        testDesign.getTestDesignUI().adjustUI();
        testExecution.getTestExecutionUI().adjustUI();
    }

    public void quit() {
        SwingUtilities.invokeLater(
            () -> {
                dispatchEvent(new WindowEvent(AppMainFrame.this, WindowEvent.WINDOW_CLOSING));
            }
        );
    }

    public void forceQuit() {
        quitType = QUIT_TYPE.FORCE;
        quit();
    }

    private Boolean iCanQuit() {
        return iCanQuit(
            quitType == QUIT_TYPE.FORCE
                ? JOptionPane.YES_NO_OPTION
                : JOptionPane.YES_NO_CANCEL_OPTION
        );
    }

    private Boolean iCanQuit(int optionType) {
        int option = JOptionPane.YES_OPTION;
        if (sProject != null) {
            // Use styled quit confirmation dialog
            option = QuitConfirmationDialog.showConfirmation(this, optionType);

            if (option == JOptionPane.YES_OPTION) {
                saveLoadedProject();
            }
        }
        if (option == -1 || option == JOptionPane.CANCEL_OPTION) {
            return false;
        } else {
            recentItems.save();
            //            spyHealReco.stopServerIfAny();
            dashBoardManager.stopServer();
            Main.finish();
            return true;
        }
    }

    public void restart() {
        quitType = QUIT_TYPE.RESTART;
        quit();
    }

    private void doRestart() {
        try {
            File workingDir = new File(System.getProperty("user.dir"));
            ProcessBuilder pb;
            if (SystemInfo.isWindows()) {
                // Launch the batch file in a new console window, detached from this JVM
                pb = new ProcessBuilder("cmd", "/c", "start", "\"INGenious\"", "ingenious.bat");
            } else if (SystemInfo.osx()) {
                // 'open' launches the .command file via Terminal.app as an independent process
                pb = new ProcessBuilder("/usr/bin/open", "ingenious.command");
            } else {
                // Linux / other Unix: run the shell script directly
                File script = new File(workingDir, "ingenious.command");
                if (!script.canExecute()) {
                    script.setExecutable(true);
                }
                pb = new ProcessBuilder("/bin/sh", script.getAbsolutePath());
            }
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.start();
        } catch (Exception ex) {
            Logger
                .getLogger(AppMainFrame.class.getName())
                .log(Level.WARNING, "Failed to restart INGenious", ex);
        }
    }

    public AppActionListener getsActionListener() {
        return sActionListener;
    }
}
