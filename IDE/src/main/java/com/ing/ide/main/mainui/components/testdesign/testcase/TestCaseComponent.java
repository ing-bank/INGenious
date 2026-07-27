package com.ing.ide.main.mainui.components.testdesign.testcase;

import static com.ing.datalib.component.TestStep.HEADERS.Description;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.component.TestStep.HEADERS;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.datalib.or.web.WebORPage;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.LiveRecordingHook;
import com.ing.engine.core.LiveRecordingService;
import com.ing.engine.core.RunManager;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.EngineConfig;
import com.ing.ide.main.mainui.components.testdesign.ReusableComponentDialog;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.plugins.RecordingTargetPlugins;
import com.ing.ide.main.playwrightrecording.InspectorWindowController;
import com.ing.ide.main.playwrightrecording.LiveRecordingParser;
import com.ing.ide.main.playwrightrecording.PlaywrightRecordingParser;
import com.ing.ide.main.playwrightrecording.RecordingTargetDialog;
import com.ing.ide.main.utils.AppIcon;
import com.ing.ide.main.utils.ConsolePanel;
import com.ing.ide.main.utils.MenuScroller;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.main.utils.table.TableColumnManager;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Notification;
import com.ing.ide.util.WindowMover;
import com.ing.ingenious.api.contract.ui.RecordingTarget;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * Main UI component for creating, editing, validating, and executing
 * test cases within the Test Design module.
 * <p>
 * {@code TestCaseComponent} manages the test case table, toolbars,
 * popup menus, auto‑suggest systems, validations, breakpoints, comment
 * toggling, and history tracking. It also integrates execution and debug
 * workflows, invokes Playwright recording, handles table actions such as
 * insert/delete/move/replicate steps, supports reusable creation, and
 * synchronizes navigation to objects and test data.
 * </p>
 *
 * <p>
 * The component orchestrates multiple sub‑dialogs (console, debugger,
 * recorder), manages runner threads, ensures save lifecycle handling,
 * and provides a unified environment for building and running automated
 * test cases.
 * </p>
 */
public class TestCaseComponent extends JPanel implements ActionListener {
    private static final String PLAYWRIGHT_INSTALL_HINT =
        "mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install\"";

    private final TestDesign testDesign;

    private final TestCaseToolBar toolBar;

    private final ConsoleDialog consoleDialog;

    private final DebugDialog debugDialog;

    private final RecorderDialog recorderDialog;

    private final TestCasePopupMenu popupMenu;

    private final TestCaseValidator validator;

    private TestCaseAutoSuggest tcAutoSuggest;

    private final XTable testCaseTable;

    private SaveListener saveListener;

    private Thread runner;

    TableColumnManager tableColumnManager;

    private final TCHistory testCaseHistory;

    private final AppMainFrame sMainFrame;

    private CompletableFuture<Void> launchPlaywrightTask;

    private volatile Process activePlaywrightProcess;

    private volatile Thread liveRecordingWatcherThread;

    private volatile boolean recorderReadySignaled;

    private volatile boolean liveRecordingFinalized;

    private volatile boolean stopRequested;

    private volatile File liveRecordingOutputFile;

    private volatile LiveRecordingParser liveRecordingParser;

    private volatile TestCase liveRecordingTarget;

    private volatile String liveRecordingPageName;

    public static long INSTANCE_START_TIME;

    private boolean globalShortcutsRegistered = false;

    public TestCaseComponent(TestDesign testDesign, AppMainFrame sMainFrame) {
        this.testDesign = testDesign;
        this.sMainFrame = sMainFrame;
        toolBar = new TestCaseToolBar(this);
        popupMenu = new TestCasePopupMenu(this);
        testCaseTable = new XTable();
        tableColumnManager = new TableColumnManager(testCaseTable);
        consoleDialog = new ConsoleDialog();
        debugDialog = new DebugDialog();
        recorderDialog = new RecorderDialog(testDesign);
        testCaseHistory = new TCHistory();
        validator = new TestCaseValidator(testCaseTable);
        init();
        LiveRecordingService.setHook(new RecordFromHereHook());
    }

    private void init() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(testCaseTable), BorderLayout.CENTER);
        testCaseTable.setComponentPopupMenu(popupMenu);
        initTableListeners();
        initRunner();
        initTestCaseAccelerators();
    }

    /**
     * Registers keyboard shortcuts for the TestCase panel.
     * <p>
     * Global shortcuts (Record, Run, Debug) use a keyboard focus manager key event
     * post-processor that fires regardless of focused child component.
     * Focus-dependent shortcuts use WHEN_ANCESTOR_OF_FOCUSED_COMPONENT so they only
     * fire when focus is inside this panel.
     */
    private void initTestCaseAccelerators() {
        registerGlobalShortcuts();

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.SAVE, "Save");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.F5, "Reload");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.UP, "MoveUp");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.DOWN, "MoveDown");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.OPEN, "Open");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.FIND, "Search");

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.COMMENT, "Comment");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.BREAKPOINT, "BreakPoint");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.INSERT_ROW, "Insert");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.ADD_ROW, "Add");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keystroke.ADD_ROWX, "Add");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.REMOVE_ROW, "Delete");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.REMOVE_ROWX, "Delete");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.REPLICATE_ROW, "Replicate");
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(Keystroke.COPY_ABOVE, "Copy Above");
    }

    /**
     * Registers global shortcuts for Record, Run, and Debug.
     * These are intentionally not table-bound so they work even when focus is in
     * toolbar/search/other child components inside the main frame.
     */
    private void registerGlobalShortcuts() {
        if (globalShortcutsRegistered) {
            return;
        }
        globalShortcutsRegistered = true;

        KeyEventPostProcessor processor = e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (!isMainFrameFocused()) {
                return false;
            }
            if (!sMainFrame.isTestDesign()) {
                return false;
            }

            int code = e.getKeyCode();
            int mods = e.getModifiersEx();

            boolean isCtrlF6 = code == KeyEvent.VK_F6 && (mods & KeyEvent.CTRL_DOWN_MASK) != 0;
            boolean isCmdF6 = code == KeyEvent.VK_F6 && (mods & KeyEvent.META_DOWN_MASK) != 0;

            if (isCtrlF6 || isCmdF6) {
                debug();
                return true;
            }

            if (code == KeyEvent.VK_F6 && mods == 0) {
                run();
                return true;
            }

            boolean isCtrlAltR =
                code == KeyEvent.VK_R &&
                (mods & KeyEvent.CTRL_DOWN_MASK) != 0 &&
                (mods & KeyEvent.ALT_DOWN_MASK) != 0;

            boolean isCmdAltR =
                code == KeyEvent.VK_R &&
                (mods & KeyEvent.META_DOWN_MASK) != 0 &&
                (mods & KeyEvent.ALT_DOWN_MASK) != 0;

            if (isCtrlAltR || isCmdAltR) {
                try {
                    record();
                } catch (IOException ex) {
                    Logger.getLogger(TestCaseComponent.class.getName()).log(Level.SEVERE, null, ex);
                }
                return true;
            }

            return false;
        };

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(processor);
    }

    /** @return true if the main frame or one of its children currently has focus */
    private boolean isMainFrameFocused() {
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        java.awt.Component focusOwner = kfm.getFocusOwner();

        return (
            kfm.getFocusedWindow() == sMainFrame ||
            (focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, sMainFrame))
        );
    }

    public void loadTableModelForSelection(Object obj) {
        if (obj != null && obj instanceof TestCase) {
            // Save the current test case before switching to a new one
            TestCase currentTestCase = getCurrentTestCase();
            if (currentTestCase != null && !currentTestCase.isSaved()) {
                currentTestCase.save();
            }

            testCaseHistory.log();
            TestCase tc = (TestCase) obj;
            tc.setSaveListener(saveListener);
            getTestCaseTable().setModel(testDesign.getProject().getTableModelFor(tc));
            tcAutoSuggest.installForTestCase();
            validator.initValidations();
            changeSave(tc.isSaved());
            refreshTitle();

            // Check if migration occurred and show notification
            int migratedCount = tc.getMigratedReferencesCount();
            if (migratedCount > 0) {
                Notification.show(
                    String.format(
                        "Migrated %d object reference%s to explicit scope prefix in '%s'",
                        migratedCount,
                        migratedCount > 1 ? "es" : "",
                        tc.getName()
                    )
                );
            }
        }
    }

    public void resetTable() {
        getTestCaseTable().setModel(new DefaultTableModel());
        changeSave(true);
        toolBar.setPlaceHolderText("", null);
    }

    public void refreshTitle() {
        String scText = getCurrentTestCase().getScenario().getName();
        if (scText.length() > 20) {
            scText = scText.substring(0, 20) + "...";
        }
        String tcText = getCurrentTestCase().getName();
        if (tcText.length() > 20) {
            tcText = tcText.substring(0, 20) + "...";
        }
        //        String toolTip
        //                = getCurrentTestCase().getScenario().getName()
        //                + " - "
        //                + getCurrentTestCase().getName();
        toolBar.setPlaceHolderText(scText + " - " + tcText, null);
    }

    public void load() {
        tcAutoSuggest = new TestCaseAutoSuggest(testDesign.getProject(), testCaseTable, testDesign);
        testCaseHistory.clear();
        loadBrowsers();
    }

    public void loadBrowsers() {
        java.util.List<String> names = new java.util.ArrayList<>(
            testDesign.getProject().getProjectSettings().getEmulators().getEmulatorNames()
        );
        for (String d : testDesign
            .getProject()
            .getProjectSettings()
            .getDevices()
            .getDeviceNames()) {
            if (!names.contains(d)) names.add(d);
        }
        toolBar.loadBrowsers(names);
    }

    private void initTableListeners() {
        testCaseTable.setActionFor(
            "Comment",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleComment();
                }
            }
        );

        testCaseTable.setActionFor(
            "BreakPoint",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleBreakPoint();
                }
            }
        );

        testCaseTable.setActionFor(
            "Insert",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    insertRow();
                }
            }
        );
        testCaseTable.setActionFor(
            "Add",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    addRow();
                }
            }
        );
        testCaseTable.setActionFor(
            "Delete",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteSelectedRows();
                }
            }
        );

        testCaseTable.setActionFor(
            "Clear",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    clearValues();
                }
            }
        );

        testCaseTable.setActionFor(
            "Replicate",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    replicateRow();
                }
            }
        );
        testCaseTable.setActionFor(
            "Save",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    save();
                }
            }
        );
        testCaseTable.setActionFor(
            "Reload",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    reload();
                }
            }
        );
        testCaseTable.setActionFor(
            "Open",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    openWithSystemEditor();
                }
            }
        );
        testCaseTable.setActionFor(
            "Search",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    toolBar.focusSearch();
                }
            }
        );

        testCaseTable.setActionFor(
            "Copy Above",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    copyAbove();
                }
            }
        );

        testCaseTable.setActionFor(
            "MoveUp",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    moveRowUp();
                }
            }
        );
        testCaseTable.setActionFor(
            "MoveDown",
            new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    moveRowDown();
                }
            }
        );

        saveListener =
            new SaveListener() {

                @Override
                public void onSave(Boolean bln) {
                    changeSave(bln);
                    refreshTreeValidation();
                }
            };

        testCaseTable.setTransferHandler(new TestCaseTableDnD());
        testCaseTable.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent me) {
                    if (SwingUtilities.isLeftMouseButton(me) && me.isAltDown()) {
                        goToSelectedReusable();
                    } else if (SwingUtilities.isLeftMouseButton(me) && me.isShiftDown()) {
                        goToObject();
                    } else if (SwingUtilities.isLeftMouseButton(me)) {
                        addLastRow();
                    }
                }
            }
        );
    }

    private void initRunner() {
        runner =
            new Thread(
                () -> {
                    toolBar.setConsoleVisible(true);
                    toolBar.stopMode();
                    consoleDialog.start();
                    RunManager
                        .getGlobalSettings()
                        .setFor(getCurrentTestCase(), toolBar.getSelectedBrowser());
                    EngineConfig.runProject(testDesign.getProject());
                    debugDialog.setVisible(false);
                    toolBar.startMode();
                }
            );
    }

    private void changeSave(Boolean bln) {
        toolBar.setSave(!bln);
        popupMenu.setSave(!bln);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Record":
                {
                    try {
                        record();
                    } catch (IOException ex) {
                        Logger
                            .getLogger(TestCaseComponent.class.getName())
                            .log(Level.SEVERE, null, ex);
                    }
                }
                break;
            case "Open with System Editor":
                openWithSystemEditor();
                break;
            case "Add Row":
                insertRowBelow();
                break;
            case "Delete Rows":
                deleteSelectedRows();
                break;
            case "Save":
                save();
                break;
            case "Reload":
                reload();
                break;
            case "Search":
                testCaseTable.searchFor(((JTextField) ae.getSource()).getText());
                break;
            case "GoToNextSearch":
                testCaseTable.goToNextSearch();
                break;
            case "GoToPrevoiusSearch":
                testCaseTable.goToPrevoiusSearch();
                break;
            case "Cut":
            case "Copy":
            case "Paste":
                ccp(ae.getActionCommand());
                break;
            case "Create Reusable":
                createReusable();
                break;
            case "Move Rows Up":
                moveRowUp();
                break;
            case "Move Rows Down":
                moveRowDown();
                break;
            case "Run":
                run();
                break;
            case "Debug":
                debug();
                break;
            case "StopRun":
                stopExecution();
                break;
            case "Toggle BreakPoint":
                toggleBreakPoint();
                break;
            case "Toggle Comment":
                toggleComment();
                break;
            case "Console":
                consoleDialog.showConsole();
                break;
            case "Go To Reusable":
                goToSelectedReusable();
                break;
            case "Go To Object":
                goToObject();
                break;
            case "Go To TestData":
                goToTestData();
                break;
            case "Toggle Validation":
                validator.toggleValidation();
                break;
            case "Parameterize":
                parameterizeSelectedSteps();
                break;
            case "Hard Assertion":
                setHardAssertion(true);
                break;
            case "Soft Assertion":
                setHardAssertion(false);
                break;
            case "Up One Level":
                loadTableModelForSelection(testCaseHistory.visit());
                break;
            default:
                throw new UnsupportedOperationException(ae.getActionCommand());
        }
    }

    public TestCase getCurrentTestCase() {
        if (getTestCaseTable().getModel() instanceof TestCase) {
            return (TestCase) getTestCaseTable().getModel();
        }
        return null;
    }

    public void record() throws IOException {
        if (toolBar.isRecording()) {
            stopPlaywrightRecording();
            return;
        }

        if (launchPlaywrightTask != null && !launchPlaywrightTask.isDone()) {
            logPlaywright("Playwright recorder is already running.");
            SwingUtilities.invokeLater(() -> toolBar.enableRecordButton());
            return;
        }

        // A plugin that already knows what the user is working on answers here, and the target
        // chooser never opens. No plugin, or no answer, and the dialog behaves exactly as before.
        RecordingTarget pluginTarget = RecordingTargetPlugins.currentTarget();

        TestCase target;
        if (pluginTarget != null) {
            target =
                createOrResolveTarget(
                    pluginTarget.getScenarioName(),
                    pluginTarget.getTestCaseName(),
                    pluginTarget.isReusableScenario()
                );
        } else {
            RecordingTargetDialog.Selection selection = RecordingTargetDialog.showDialog(
                this,
                testDesign.getProject(),
                getCurrentTestCase()
            );
            if (selection == null) {
                SwingUtilities.invokeLater(() -> toolBar.enableRecordButton());
                return;
            }
            target = resolveRecordingTarget(selection);
        }

        if (target == null) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to resolve recording target.",
                "Playwright Recorder",
                JOptionPane.WARNING_MESSAGE
            );
            SwingUtilities.invokeLater(() -> toolBar.enableRecordButton());
            return;
        }

        loadTableModelForSelection(target);
        liveRecordingTarget = target;
        liveRecordingFinalized = false;
        stopRequested = false;
        recorderReadySignaled = false;
        INSTANCE_START_TIME = System.currentTimeMillis();

        int firstInsertIndex = firstEmptyRowIndex(target);
        PlaywrightRecordingParser baseParser = new PlaywrightRecordingParser(sMainFrame);
        WebORPage objectPage = baseParser.createLiveRecordingPage(target.getName());
        liveRecordingPageName = baseParser.getLiveRecordingPageName();
        String reference = "[Project] " + liveRecordingPageName;
        liveRecordingParser =
            new LiveRecordingParser(baseParser, target, firstInsertIndex, reference, objectPage);

        liveRecordingOutputFile = prepareLiveRecordingOutputFile();
        final String startUrl = resolveRecordingStartUrl(pluginTarget);

        toolBar.setConsoleVisible(true);
        consoleDialog.clear();
        consoleDialog.showConsole();
        logPlaywright("🎬 Playwright Recording is being initiated...");
        // The user was not asked where this goes, so the console has to say it.
        if (pluginTarget != null) {
            logPlaywright(
                "🎯 Recording into " + target.getScenario().getName() + " / " + target.getName()
            );
        }
        if (startUrl != null) {
            logPlaywright("🌐 Opening " + startUrl);
        }
        logPlaywright(
            "============================== Playwright Log Started =============================="
        );

        startLiveRecordingWatcher();

        launchPlaywrightTask =
            CompletableFuture.runAsync(
                () -> {
                    try {
                        launchPlaywright(liveRecordingOutputFile, startUrl);
                    } catch (IOException ex) {
                        logPlaywrightError("Error launching Playwright: " + ex.getMessage());
                        Logger
                            .getLogger(TestCaseComponent.class.getName())
                            .log(Level.SEVERE, "Error launching Playwright", ex);
                    } finally {
                        finalizeLiveRecording();
                    }
                }
            );
    }

    /**
     * Live recording hook used by the Engine's {@code RecordFromHere} action. When a running test
     * case reaches a {@code RecordFromHere} step, the Engine enables the Playwright recorder on the
     * live browser context and notifies this hook so the recorded steps are appended into the
     * editor in real time (highlighted green) from the current step onwards.
     */
    private class RecordFromHereHook implements LiveRecordingHook {

        @Override
        public String onRecordingStarted(TestCase engineTestCase, int insertAfterStepIndex) {
            final TestCase target = resolveHookTarget(engineTestCase);
            if (target == null) {
                Logger
                    .getLogger(TestCaseComponent.class.getName())
                    .log(Level.WARNING, "RecordFromHere: unable to resolve editable test case.");
                return null;
            }

            final int firstInsertIndex = Math.max(insertAfterStepIndex + 1, 0);
            final java.util.concurrent.atomic.AtomicReference<File> fileRef = new java.util.concurrent.atomic.AtomicReference<>();

            Runnable setup = () -> {
                try {
                    loadTableModelForSelection(target);
                    liveRecordingTarget = target;
                    liveRecordingFinalized = false;
                    stopRequested = false;
                    recorderReadySignaled = false;
                    INSTANCE_START_TIME = System.currentTimeMillis();

                    PlaywrightRecordingParser baseParser = new PlaywrightRecordingParser(
                        sMainFrame
                    );
                    WebORPage objectPage = baseParser.createLiveRecordingPage(target.getName());
                    liveRecordingPageName = baseParser.getLiveRecordingPageName();
                    String reference = "[Project] " + liveRecordingPageName;
                    liveRecordingParser =
                        new LiveRecordingParser(
                            baseParser,
                            target,
                            firstInsertIndex,
                            reference,
                            objectPage
                        );

                    liveRecordingOutputFile = prepareLiveRecordingOutputFile();

                    toolBar.setConsoleVisible(true);
                    consoleDialog.clear();
                    consoleDialog.showConsole();
                    logPlaywright("🎬 Recording from current step...");
                    startLiveRecordingWatcher();
                    fileRef.set(liveRecordingOutputFile);
                } catch (Exception ex) {
                    Logger
                        .getLogger(TestCaseComponent.class.getName())
                        .log(Level.SEVERE, "Unable to start live recording for RecordFromHere", ex);
                }
            };

            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    setup.run();
                } else {
                    SwingUtilities.invokeAndWait(setup);
                }
            } catch (Exception ex) {
                Logger
                    .getLogger(TestCaseComponent.class.getName())
                    .log(Level.WARNING, "RecordFromHere setup failed", ex);
                return null;
            }

            File file = fileRef.get();
            return file == null ? null : file.getAbsolutePath();
        }

        @Override
        public void onRecordingReady() {
            if (!recorderReadySignaled) {
                onRecorderReady();
            }
        }

        @Override
        public void onRecordingStopped() {
            finalizeLiveRecording();
        }
    }

    /**
     * Maps the Engine's (copied) running test case back to the editable project test case so
     * recorded steps and saves apply to the persistent model shown in the editor.
     */
    private TestCase resolveHookTarget(TestCase engineTestCase) {
        if (engineTestCase == null) {
            return null;
        }

        Scenario engineScenario = engineTestCase.getScenario();
        String scenarioName = engineScenario != null ? engineScenario.getName() : null;
        String testCaseName = engineTestCase.getName();
        if (scenarioName == null || testCaseName == null) {
            return null;
        }

        boolean reusable = engineScenario.isReusableScenario();
        Scenario scenario = reusable
            ? testDesign.getProject().getReusableScenarioByName(scenarioName)
            : testDesign.getProject().getScenarioByName(scenarioName);
        if (scenario == null) {
            return null;
        }
        return scenario.getTestCaseByName(testCaseName);
    }

    public Process startPlaywrightProcess(String processArgs) {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String classpath;
            if (osName.contains("win")) {
                String userHome = System.getProperty("user.home");
                String printDepsDir = userHome + "\\AppData\\Local\\ms-playwright\\winldd-1007";
                String printDepsPath = printDepsDir + "\\PrintDeps.exe";
                File printDeps = new File(printDepsPath);
                if (!printDeps.exists()) {
                    new File(printDepsDir).mkdirs();

                    try (
                        InputStream in = getClass()
                            .getResourceAsStream("/Engine/winldd-1007/PrintDeps.exe")
                    ) {
                        if (in == null) {
                            throw new FileNotFoundException(
                                "PrintDeps.exe not found in resources!"
                            );
                        }
                        Files.copy(in, Path.of(printDepsPath), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                classpath = "lib/*;."; // Windows
            } else {
                classpath = "lib/*:."; // Mac
            }

            String javaCommand = String.format(
                "java -cp \"%s\" com.microsoft.playwright.CLI %s",
                classpath,
                processArgs
            );

            String[] command = osName.contains("windows")
                ? new String[] { "cmd", "/c", javaCommand }
                : new String[] { "bash", "-l", "-c", javaCommand };

            return new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (Exception ex) {
            logPlaywrightError("Error starting Playwright process: " + ex.getMessage());
        }

        return null;
    }

    //    public void initialization(PlaywrightSpinner playwrightSpinnerGUI){
    //        try{
    //            String[] command = new String[0];
    //            String osName = System.getProperty("os.name").toLowerCase();
    //            if (osName.contains("windows")) {
    //                // Windows command
    //
    //                command = new String[]{"cmd", "/c", "mvn initialize -f engine/pom.xml"};
    //            } else if (osName.contains("mac")) {
    //                // Mac command
    //                command = new String[]{"bash", "-l", "-c", "mvn initialize -f engine/pom.xml"};
    //            }
    //           Runtime.getRuntime().exec(command);
    //       }catch (Exception ex){
    //         System.out.println(ex.getMessage());
    //         //playwrightSpinnerGUI.appendLog(ex.getMessage());
    //       }
    //    }

    public void launchPlaywright(File outputFile) throws IOException {
        launchPlaywright(outputFile, null);
    }

    /**
     * Starts the Playwright recorder, optionally on a given page.
     *
     * @param outputFile file codegen writes the recorded script to
     * @param startUrl page to open, or {@code null} for codegen's blank page
     * @throws IOException when the recorder process cannot be started
     */
    public void launchPlaywright(File outputFile, String startUrl) throws IOException {
        String escapedPath = outputFile
            .getAbsolutePath()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
        String processArgs = "codegen --target java --output \"" + escapedPath + "\"";
        if (startUrl != null) {
            // Quoted: the command is handed to cmd/bash as one string, and an unquoted query
            // string would be cut at its first '&'. Validation upstream has already ruled out
            // anything that could break out of these quotes.
            processArgs += " \"" + startUrl + "\"";
        }
        runPlaywrightProcess(processArgs);
        logPlaywright(
            "============================== Playwright Log Ended =============================="
        );
    }

    private Process runPlaywrightProcess(String processArgs) throws IOException {
        Process process = startPlaywrightProcess(processArgs);
        if (process == null) {
            return null;
        }

        activePlaywrightProcess = process;

        boolean codegenCommand = processArgs.trim().startsWith("codegen");

        try (
            BufferedReader processOutput = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            )
        ) {
            String line;
            while ((line = processOutput.readLine()) != null) {
                logPlaywright(line);
                if (codegenCommand && !recorderReadySignaled) {
                    onRecorderReady();
                }
                if (codegenCommand && line.contains(PLAYWRIGHT_INSTALL_HINT)) {
                    waitForProcess(process, "Playwright codegen");
                    logPlaywright("Playwright browser binaries are missing. Starting install...");
                    Process installProcess = runPlaywrightProcess("install");
                    waitForProcess(installProcess, "Playwright install");
                    logPlaywright("Playwright install completed. Restarting recorder...");
                    return runPlaywrightProcess(processArgs);
                }
            }
        }

        return process;
    }

    private void waitForProcess(Process process, String processName) {
        if (process == null) {
            return;
        }

        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logPlaywrightError(processName + " wait interrupted: " + ex.getMessage());
        }
    }

    private void logPlaywright(String message) {
        System.out.println(message);
        consoleDialog.appendLine(message);
    }

    private void logPlaywrightError(String message) {
        System.err.println(message);
        consoleDialog.appendErrorLine(message);
    }

    private void onRecorderReady() {
        recorderReadySignaled = true;
        SwingUtilities.invokeLater(
            () -> {
                consoleDialog.setVisible(false);
                toolBar.setRecordingState(true);
                toolBar.enableRecordButton();
            }
        );
        CompletableFuture.runAsync(() -> InspectorWindowController.minimizeInspectorBestEffort());
    }

    private void stopPlaywrightRecording() {
        stopRequested = true;
        Process process = activePlaywrightProcess;
        if (process != null && process.isAlive()) {
            destroyProcessTree(process);
        }
        finalizeLiveRecording();
    }

    /**
     * Forcibly terminates the Playwright process and all of its descendants. The codegen CLI
     * spawns the "Google Chrome for Testing" browser as a child process, so destroying only the
     * parent wrapper would leave that browser window open. Collecting descendants before
     * destroying the parent ensures the browser window is closed too.
     */
    private void destroyProcessTree(Process process) {
        if (process == null) {
            return;
        }
        try {
            List<ProcessHandle> descendants = process
                .descendants()
                .collect(java.util.stream.Collectors.toList());
            process.destroyForcibly();
            for (ProcessHandle handle : descendants) {
                handle.destroyForcibly();
            }
        } catch (Exception ex) {
            Logger
                .getLogger(TestCaseComponent.class.getName())
                .log(Level.WARNING, "Unable to terminate Playwright browser process tree", ex);
        }
    }

    private void finalizeLiveRecording() {
        synchronized (this) {
            if (liveRecordingFinalized) {
                return;
            }
            liveRecordingFinalized = true;
        }

        // Parse any remaining recorder output before shutting down watcher/parser state.
        flushPendingLiveRecordingLines();

        stopLiveRecordingWatcher();

        if (liveRecordingParser != null && liveRecordingTarget != null) {
            try {
                Runnable finalizeTask = () -> {
                    int updates = liveRecordingParser.finalizeDeferredInputs();
                    liveRecordingTarget.save();
                    testCaseTable.revalidate();
                    testCaseTable.repaint();
                    if (updates > 0) {
                        logPlaywright("Updated " + updates + " deferred text input step(s).");
                    }
                };

                if (SwingUtilities.isEventDispatchThread()) {
                    finalizeTask.run();
                } else {
                    SwingUtilities.invokeAndWait(finalizeTask);
                }
            } catch (Exception ex) {
                Logger
                    .getLogger(TestCaseComponent.class.getName())
                    .log(Level.WARNING, "Unable to finalize live recording", ex);
            }
        }

        activePlaywrightProcess = null;
        liveRecordingParser = null;
        liveRecordingTarget = null;
        liveRecordingOutputFile = null;
        recorderReadySignaled = false;

        SwingUtilities.invokeLater(
            () -> {
                toolBar.setRecordingState(false);
                toolBar.enableRecordButton();
            }
        );
    }

    private void startLiveRecordingWatcher() {
        if (liveRecordingOutputFile == null || liveRecordingParser == null) {
            return;
        }

        liveRecordingWatcherThread =
            new Thread(
                () -> {
                    while (!liveRecordingFinalized && !Thread.currentThread().isInterrupted()) {
                        try {
                            if (liveRecordingOutputFile.exists()) {
                                List<String> lines = Files.readAllLines(
                                    liveRecordingOutputFile.toPath()
                                );
                                if (!recorderReadySignaled && lines.size() > 0) {
                                    onRecorderReady();
                                }
                                syncLiveRecording(lines);
                            }
                            Thread.sleep(300);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception ex) {
                            Logger
                                .getLogger(TestCaseComponent.class.getName())
                                .log(Level.WARNING, "Live recording watcher iteration failed", ex);
                        }
                    }
                },
                "playwright-live-recording-watcher"
            );
        liveRecordingWatcherThread.setDaemon(true);
        liveRecordingWatcherThread.start();
    }

    private void stopLiveRecordingWatcher() {
        Thread watcher = liveRecordingWatcherThread;
        if (watcher != null) {
            watcher.interrupt();
        }
        liveRecordingWatcherThread = null;
    }

    private void flushPendingLiveRecordingLines() {
        if (liveRecordingOutputFile == null || !liveRecordingOutputFile.exists()) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(liveRecordingOutputFile.toPath());
            syncLiveRecording(lines);
        } catch (Exception ex) {
            Logger
                .getLogger(TestCaseComponent.class.getName())
                .log(Level.WARNING, "Unable to flush pending live recording lines", ex);
        }
    }

    private void syncLiveRecording(List<String> lines) {
        if (liveRecordingParser == null || lines == null) {
            return;
        }

        Runnable parserTask = () -> {
            if (liveRecordingParser != null && liveRecordingTarget != null) {
                boolean changed = liveRecordingParser.syncFromLines(lines, this::logPlaywright);
                if (changed) {
                    liveRecordingTarget.save();
                    testCaseTable.revalidate();
                    testCaseTable.repaint();
                    testDesign.getObjectRepo().refreshWebOR(liveRecordingPageName);
                }
            }
        };

        try {
            if (SwingUtilities.isEventDispatchThread()) {
                parserTask.run();
            } else {
                SwingUtilities.invokeAndWait(parserTask);
            }
        } catch (Exception ex) {
            Logger
                .getLogger(TestCaseComponent.class.getName())
                .log(Level.WARNING, "Unable to sync live recording", ex);
        }
    }

    /**
     * Decides which page the recorder opens: what the plugin asked for, else what the project
     * configured, else nothing — which is codegen's blank page, i.e. the behaviour every
     * existing project already has.
     *
     * @param pluginTarget the plugin's target, or {@code null} when the user chose by hand
     * @return a usable URL, or {@code null} to start on a blank page
     */
    private String resolveRecordingStartUrl(RecordingTarget pluginTarget) {
        String fromPlugin = pluginTarget == null ? null : pluginTarget.getStartUrl();
        if (fromPlugin != null) {
            if (isUsableStartUrl(fromPlugin)) {
                return fromPlugin.trim();
            }
            logPlaywright("Ignoring unusable recording URL from plugin: " + fromPlugin);
        }

        String fromProject = "";
        try {
            fromProject =
                testDesign.getProject().getProjectSettings().getRecorderSettings().getStartUrl();
        } catch (RuntimeException ex) {
            Logger
                .getLogger(TestCaseComponent.class.getName())
                .log(Level.WARNING, "Unable to read the project's recorder settings", ex);
        }
        if (!fromProject.isEmpty()) {
            if (isUsableStartUrl(fromProject)) {
                return fromProject.trim();
            }
            logPlaywright("Ignoring unusable recording URL in project settings: " + fromProject);
        }

        return null;
    }

    /**
     * An absolute http(s) address and nothing else.
     *
     * <p>The recorder command is assembled as one string and handed to a shell, so a value that
     * is not a plain URL must not reach it. Rejecting here means a mistyped setting starts a
     * blank recording with a note in the console, rather than a broken or surprising command.
     *
     * @param value the configured value
     * @return {@code true} when it is safe to pass to the recorder
     */
    private boolean isUsableStartUrl(String value) {
        if (value == null) {
            return false;
        }
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.indexOf('"') >= 0 || candidate.indexOf('%') >= 0) {
            // '%' is legal in a URL but is what a Windows shell expands, so a percent-encoded
            // address is refused rather than silently mangled on the way to the recorder.
            return false;
        }
        try {
            java.net.URI uri = new java.net.URI(candidate);
            String scheme = uri.getScheme();
            return (
                uri.isAbsolute() &&
                uri.getHost() != null &&
                ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
            );
        } catch (java.net.URISyntaxException ex) {
            return false;
        }
    }

    private TestCase resolveRecordingTarget(RecordingTargetDialog.Selection selection) {
        if (selection == null) {
            return null;
        }

        switch (selection.getMode()) {
            case CURRENT_OPEN_TEST_CASE:
                return getCurrentTestCase();
            case NEW_TEST_SCENARIO:
                return createOrResolveTarget(
                    selection.getScenarioName(),
                    selection.getTestCaseName(),
                    false
                );
            case NEW_REUSABLE_SCENARIO:
                return createOrResolveTarget(
                    selection.getScenarioName(),
                    selection.getTestCaseName(),
                    true
                );
            case EXISTING_TEST_CASE:
                return findExistingTarget(
                    selection.getExistingScenarioName(),
                    selection.getTestCaseName(),
                    selection.isExistingReusable()
                );
            default:
                return null;
        }
    }

    private TestCase createOrResolveTarget(
        String scenarioName,
        String testCaseName,
        boolean reusable
    ) {
        Scenario scenario = findScenarioByName(scenarioName, reusable);
        if (scenario == null) {
            scenario =
                reusable
                    ? testDesign.getProject().addReusableScenario(scenarioName)
                    : testDesign.getProject().addScenario(scenarioName);
        }
        if (scenario == null) {
            return null;
        }

        TestCase testCase = scenario.getTestCaseByName(testCaseName);
        if (testCase == null) {
            testCase = scenario.addTestCase(testCaseName);
        }

        registerTargetInTree(testCase, reusable);
        return testCase;
    }

    /**
     * Registers a newly created/resolved recording target in the project tree so it becomes
     * visible immediately without requiring a full project reload.
     */
    private void registerTargetInTree(TestCase testCase, boolean reusable) {
        if (testCase == null) {
            return;
        }
        SwingUtilities.invokeLater(
            () -> {
                try {
                    if (reusable) {
                        testDesign.getReusableTree().getTreeModel().addTestCase(testCase);
                    } else {
                        testDesign.getProjectTree().getTreeModel().addTestCase(testCase);
                    }
                } catch (Exception ex) {
                    Logger
                        .getLogger(TestCaseComponent.class.getName())
                        .log(Level.WARNING, "Unable to register recording target in tree", ex);
                }
            }
        );
    }

    private TestCase findExistingTarget(
        String scenarioName,
        String testCaseName,
        boolean reusable
    ) {
        Scenario scenario = findScenarioByName(scenarioName, reusable);
        return scenario == null ? null : scenario.getTestCaseByName(testCaseName);
    }

    private Scenario findScenarioByName(String scenarioName, boolean reusable) {
        List<Scenario> scenarios = reusable
            ? testDesign.getProject().getReusableScenarios()
            : testDesign.getProject().getScenarios();

        for (Scenario scenario : scenarios) {
            if (scenario.getName().equalsIgnoreCase(scenarioName)) {
                return scenario;
            }
        }
        return null;
    }

    private int firstEmptyRowIndex(TestCase testCase) {
        if (testCase == null) {
            return 0;
        }

        List<TestStep> steps = testCase.getTestSteps();
        for (int i = 0; i < steps.size(); i++) {
            TestStep step = steps.get(i);
            if (isStepBlank(step)) {
                return i;
            }
        }
        return steps.size();
    }

    private boolean isStepBlank(TestStep step) {
        return (
            step == null ||
            (
                isBlank(step.getObject()) &&
                isBlank(step.getAction()) &&
                isBlank(step.getInput()) &&
                isBlank(step.getCondition()) &&
                isBlank(step.getReference()) &&
                isBlank(step.getDescription())
            )
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private File prepareLiveRecordingOutputFile() throws IOException {
        File recordingDir = new File(
            sMainFrame.getProject().getLocation() + File.separator + "Recording"
        );
        if (!recordingDir.exists()) {
            recordingDir.mkdirs();
        }
        File output = new File(recordingDir, "live_recording_" + INSTANCE_START_TIME + ".java");
        if (!output.exists()) {
            output.createNewFile();
        }
        return output;
    }

    private void stopCellEditing() {
        if (testCaseTable.getCellEditor() != null) {
            testCaseTable.getCellEditor().stopCellEditing();
        }
    }

    private void insertRow() {
        stopCellEditing();
        if (testCaseTable.getSelectedRow() != -1) {
            getCurrentTestCase().addNewStepAt(testCaseTable.getSelectedRow());
        }
    }

    public TestStep getSelectedStep() {
        if (testCaseTable.getSelectedRow() != -1) {
            return getCurrentTestCase().getTestSteps().get(testCaseTable.getSelectedRow());
        }
        if (testCaseTable.getRowCount() > 0) {
            return getCurrentTestCase().getTestSteps().get(testCaseTable.getRowCount() - 1);
        }
        return null;
    }

    public TestStep getLastStep() {
        if (testCaseTable.getRowCount() > 0) {
            return getCurrentTestCase().getTestSteps().get(testCaseTable.getRowCount() - 1);
        }
        return null;
    }

    public TestStep insertRowBelow() {
        stopCellEditing();
        if (
            testCaseTable.getSelectedRow() != -1 &&
            testCaseTable.getSelectedRow() + 1 < testCaseTable.getRowCount()
        ) {
            return getCurrentTestCase().addNewStepAt(testCaseTable.getSelectedRow() + 1);
        } else {
            return getCurrentTestCase().addNewStep();
        }
    }

    private void addLastRow() {
        int row = testCaseTable.getSelectedRow();
        int column = testCaseTable.getSelectedColumn();
        if (
            row == testCaseTable.getRowCount() - 1 && column == testCaseTable.getColumnCount() - 1
        ) {
            addRow();
        }
    }

    public TestStep addRow() {
        stopCellEditing();
        return getCurrentTestCase().addNewStep();
    }

    private void replicateRow() {
        stopCellEditing();
        if (testCaseTable.getSelectedRow() != -1) {
            getCurrentTestCase().replicateStepAt(testCaseTable.getSelectedRow());
        }
    }

    private void copyAbove() {
        stopCellEditing();
        int row = testCaseTable.getSelectedRow();
        if (row > 0) {
            for (int col : testCaseTable.getSelectedColumns()) {
                String value = Objects.toString(testCaseTable.getValueAt(row - 1, col), "");
                testCaseTable.setValueAt(value, row, col);
            }
        }
    }

    private void moveRowUp() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testCaseTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            if (getCurrentTestCase().moveRowsUp(from, to)) {
                testCaseTable.getSelectionModel().setSelectionInterval(from - 1, to - 1);
            }
        }
    }

    private void moveRowDown() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testCaseTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            if (getCurrentTestCase().moveRowsDown(from, to)) {
                testCaseTable.getSelectionModel().setSelectionInterval(from + 1, to + 1);
            }
        }
    }

    private void clearValues() {
        stopCellEditing();
        if (testCaseTable.getSelectedRowCount() > 0) {
            getCurrentTestCase()
                .clearValues(testCaseTable.getSelectedRows(), testCaseTable.getSelectedColumns());
        }
    }

    private void deleteSelectedRows() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase()
                .removeSteps(Utils.getReverseSorted(testCaseTable.getSelectedRows()));
        }
    }

    private void parameterizeSelectedSteps() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testCaseTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            TestStep fstep = getCurrentTestCase().getTestSteps().get(from);
            TestStep tstep = getCurrentTestCase().getTestSteps().get(to);
            if (fstep.getCondition().isEmpty()) {
                fstep.setCondition("Start Param");
            } else if (!fstep.getCondition().equals("Start Param")) {
                insertFiller(from).setCondition("Start Param");
                to++;
            }
            if (tstep.getCondition().isEmpty()) {
                tstep.setCondition("End Param");
            } else if (!tstep.getCondition().contains("End Param")) {
                insertFiller(++to).setCondition("End Param");
            }
        }
    }

    private TestStep insertFiller(int row) {
        return getCurrentTestCase().addNewStepAt(row).setObject("Browser").setAction("filler");
    }

    private void toggleComment() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase().toggleComment(testCaseTable.getSelectedRows());
        }
    }

    private void toggleBreakPoint() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase().toggleBreakPoint(testCaseTable.getSelectedRows());
        }
    }

    private void setHardAssertion(boolean hard) {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase().setHardAssertion(testCaseTable.getSelectedRows(), hard);
        }
    }

    private void openWithSystemEditor() {
        save();
        Utils.openWithSystemEditor(getCurrentTestCase().getLocation());
    }

    private void save() {
        stopCellEditing();
        populateDescription();
        TestCase current = getCurrentTestCase();
        clearNewlyRecordedFlags(current);
        current.save();
    }

    /**
     * Repaints the Test Plan and Reusable Component trees so that scenario and
     * test-case nodes are (re)marked in red whenever their validation state
     * changes due to an edit or save.
     */
    private void refreshTreeValidation() {
        if (testDesign.getProjectTree() != null) {
            testDesign.getProjectTree().getTree().repaint();
        }
        if (testDesign.getReusableTree() != null) {
            testDesign.getReusableTree().getTree().repaint();
        }
    }

    /**
     * Clears the transient "newly recorded" highlight so steps captured during live recording
     * revert to the default colour once the user explicitly saves.
     */
    private void clearNewlyRecordedFlags(TestCase testCase) {
        if (testCase == null) {
            return;
        }
        boolean cleared = false;
        for (TestStep testStep : testCase.getTestSteps()) {
            if (testStep.isNewlyRecorded()) {
                testStep.setNewlyRecorded(false);
                cleared = true;
            }
        }
        if (cleared) {
            testCaseTable.repaint();
        }
    }

    private void populateDescription() {
        int i = 0;
        for (TestStep testStep : getCurrentTestCase().getTestSteps()) {
            if (!testStep.getAction().isEmpty() && testStep.getDescription().isEmpty()) {
                String desc = MethodInfoManager.getDescriptionFor(testStep.getAction());
                testCaseTable.setValueAt(desc, i, Description.getIndex());
            }
            i++;
        }
    }

    public void reload() {
        stopCellEditing();
        getCurrentTestCase().reload();
        tableColumnManager.reset();
        tcAutoSuggest.installForTestCase();
        validator.initValidations();
    }

    private void ccp(String operation) {
        switch (operation) {
            case "Cut":
                testCaseTable.cut();
                break;
            case "Copy":
                testCaseTable.copy();
                break;
            case "Paste":
                testCaseTable.paste();
                break;
        }
    }

    private void createReusable() {
        if (testCaseTable.getSelectedRowCount() > 0) {
            int from = testCaseTable.getSelectedRows()[0];
            int to = testCaseTable.getSelectedRows()[testCaseTable.getSelectedRowCount() - 1];
            TestCase current = getCurrentTestCase();
            ReusableComponentDialog.Result result = ReusableComponentDialog.prompt(
                this,
                current.getProject()
            );
            if (result != null) {
                Scenario targetScenario;
                if (result.isSharedScope()) {
                    targetScenario =
                        current
                            .getProject()
                            .getSharedReusableScenarioByName(result.getScenarioName());
                    if (targetScenario == null) {
                        targetScenario =
                            current
                                .getProject()
                                .addSharedReusableScenario(result.getScenarioName());
                    }
                } else {
                    targetScenario =
                        current.getProject().getReusableScenarioByName(result.getScenarioName());
                    if (targetScenario == null) {
                        targetScenario =
                            current.getProject().addReusableScenario(result.getScenarioName());
                    }
                }
                TestCase reusable = current.createAsReusable(
                    targetScenario,
                    result.getReusableName(),
                    from,
                    to
                );
                if (reusable != null) {
                    current.save();
                    if (result.isSharedScope()) {
                        testDesign.getSharedReusableTree().getTreeModel().addTestCase(reusable);
                    } else {
                        testDesign.getReusableTree().getTreeModel().addTestCase(reusable);
                    }
                } else {
                    Notification.show("Couldn't Create Reusable - " + result.getReusableName());
                }
            }
        }
    }

    public XTable getTestCaseTable() {
        return testCaseTable;
    }

    private void debug() {
        run(true);
    }

    private void run() {
        run(false);
    }

    private void run(Boolean debugMode) {
        if (!runner.isAlive()) {
            save();
            getCurrentTestCase().getProject().save();
            stopCellEditing();
            SystemDefaults.debugMode.set(debugMode);
            initRunner();
            runner.start();
            if (debugMode) {
                debugDialog.showDebugDialog();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Already Running");
        }
    }

    private void stopExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(false);
            SystemDefaults.stopCurrentIteration.set(true);
            SystemDefaults.stopExecution.set(true);
        }
    }

    private void pauseExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(true);
        }
    }

    private void continueExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(false);
        }
    }

    private void nextStepExecution() {
        if (runner.isAlive()) {
            SystemDefaults.nextStepflag.set(false);
        }
    }

    private void goToSelectedReusable() {
        if (testCaseTable.getSelectedRow() != -1) {
            TestStep tStep = getCurrentTestCase()
                .getTestSteps()
                .get(testCaseTable.getSelectedRow());

            // Go To Reusable is only available for PROJECT and SHARED scope reusables
            if (!tStep.isReusableStep()) {
                Notification.showWarning("Selected step is not a reusable step.");
                return;
            }

            String[] reusableData = tStep.getReusableData();
            if (reusableData != null) {
                ReusableRef ref;
                try {
                    ref = tStep.getEffectiveReusableRef();
                } catch (IllegalArgumentException ex) {
                    ref =
                        new ReusableRef(
                            ReusableRef.Scope.UNSCOPED,
                            reusableData[0],
                            reusableData[1]
                        );
                }
                if (ref == null) {
                    ref =
                        new ReusableRef(
                            ReusableRef.Scope.UNSCOPED,
                            reusableData[0],
                            reusableData[1]
                        );
                }

                // Only allow navigation for PROJECT and SHARED scoped reusables
                if (ref.getScope() == ReusableRef.Scope.UNSCOPED) {
                    Notification.showWarning(
                        "Cannot navigate to unscoped reusable. Please explicitly scope the reference as [Project] or [Shared] in the Action column."
                    );
                    return;
                }

                Scenario scenario = null;
                if (ref.getScope() == ReusableRef.Scope.PROJECT) {
                    scenario =
                        testDesign.getProject().getReusableScenarioByName(ref.getScenarioName());
                } else if (ref.getScope() == ReusableRef.Scope.SHARED) {
                    scenario =
                        testDesign
                            .getProject()
                            .getSharedReusableScenarioByName(ref.getScenarioName());
                }

                if (scenario != null) {
                    TestCase testCase = scenario.getTestCaseByName(ref.getTestCaseName());
                    if (testCase != null) {
                        testDesign.loadTableModelForSelection(testCase);
                    } else {
                        Notification.show(
                            "TestCase [" +
                            ref.getTestCaseName() +
                            "] not present in the Scenario [" +
                            ref.getScenarioName() +
                            "]"
                        );
                    }
                } else {
                    Notification.show(
                        "Scenario [" +
                        ref.getScenarioName() +
                        "] not present in " +
                        ref.getScope() +
                        " reusable scope"
                    );
                }
            }
        }
    }

    private void goToTestData() {
        if (testCaseTable.getSelectedRow() != -1) {
            TestStep tStep = getCurrentTestCase()
                .getTestSteps()
                .get(testCaseTable.getSelectedRow());
            String[] tdFromInput = tStep.getTestDataFromInput();
            if (tdFromInput != null) {
                if (
                    !testDesign.getTestDatacomp().navigateToTestData(tdFromInput[0], tdFromInput[1])
                ) {
                    Notification.show(
                        "Test Data [" +
                        tdFromInput[0] +
                        ":" +
                        tdFromInput[1] +
                        "] not found in Test Data"
                    );
                }
            }
        }
    }

    private void goToObject() {
        if (testCaseTable.getSelectedRow() != -1) {
            TestStep tStep = getCurrentTestCase()
                .getTestSteps()
                .get(testCaseTable.getSelectedRow());
            String[] objectPage = tStep.getPageObject();
            if (objectPage != null) {
                if (!testDesign.getObjectRepo().navigateToObject(objectPage[0], objectPage[1])) {
                    Notification.show(objectPage[0] + " - Object not found in Object Repository");
                }
            }
        }
    }

    public String getDefaultBrowser() {
        return toolBar.getSelectedBrowser();
    }

    public TCHistory getTestCaseHistory() {
        return testCaseHistory;
    }

    public TestCaseToolBar getToolBar() {
        return toolBar;
    }

    public TestDesign getTestDesign() {
        return testDesign;
    }

    class ConsoleDialog extends JDialog {
        private final ConsolePanel cPanel;

        public ConsoleDialog() {
            super(new JFrame());
            setAlwaysOnTop(true);
            setLayout(new BorderLayout());
            cPanel = new ConsolePanel();
            add(cPanel, BorderLayout.CENTER);
            setTitle("Console");
            AppIcon.applyTo(this);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
            getRootPane()
                .registerKeyboardAction(
                    e -> dispose(),
                    KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW
                );
        }

        public void showConsole() {
            if (!isVisible()) {
                pack();
                setSize(690, 400);
                setLocationRelativeTo(null);
                setVisible(true);
            } else {
                toFront();
            }
        }

        public void start() {
            cPanel.start();
        }

        public void clear() {
            cPanel.clear();
        }

        public void appendLine(String message) {
            cPanel.appendLine(message);
        }

        public void appendErrorLine(String message) {
            cPanel.appendErrorLine(message);
        }
    }

    class DebugDialog extends JDialog implements ActionListener {

        public DebugDialog() {
            super(new JFrame());
            init();
            setUndecorated(true);
        }

        private void init() {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            JButton drag = new JButton("   ");

            toolBar.add(drag);
            registerDrag(drag);

            toolBar.add(create("Show Console", "console"));
            toolBar.add(create("Continue Execution", "continue"));
            toolBar.add(create("Go to Next Step", "stepover"));
            toolBar.add(create("Pause the Execution", "pause"));
            toolBar.add(create("Stop the Execution", "stop"));

            add(toolBar);
        }

        private JButton create(String tooltip, String icon) {
            JButton button = new JButton();
            button.setActionCommand(tooltip);
            button.setToolTipText(tooltip);
            button.setIcon(Utils.getIconByResourceName("/ui/resources/testdesign/debug/" + icon));
            button.addActionListener(this);
            return button;
        }

        private void registerDrag(JButton drag) {
            drag.setContentAreaFilled(false);
            drag.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            WindowMover.register(this, drag, WindowMover.MOVE_BOTH);
        }

        void showDebugDialog() {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
            Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
            pack();
            setLocation((int) rect.getCenterX(), Canvas.Window.winStart.y);
            setAlwaysOnTop(true);
            setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            switch (ae.getActionCommand()) {
                case "Show Console":
                    consoleDialog.showConsole();
                    break;
                case "Continue Execution":
                    continueExecution();
                    break;
                case "Go to Next Step":
                    nextStepExecution();
                    break;
                case "Pause the Execution":
                    pauseExecution();
                    break;
                case "Stop the Execution":
                    stopExecution();
                    break;
            }
        }
    }

    class TCHistory extends JMenu implements ActionListener {
        private final LinkedList<String> historyList = new LinkedList<>();

        private final int max = 20;

        private Boolean allowed = false;

        public TCHistory() {
            setText("Recent TestCases");
            MenuScroller.setScrollerFor(this, 10);
        }

        public void log() {
            if (getCurrentTestCase() != null) {
                String val =
                    getCurrentTestCase().getScenario().getName() +
                    ":" +
                    getCurrentTestCase().getName();
                log(val);
            }
        }

        public void log(String val) {
            if (allowed) {
                if (historyList.contains(val)) {
                    int index = historyList.indexOf(val);
                    historyList.remove(index);
                    remove(index);
                }
                if (historyList.size() == max) {
                    historyList.removeLast();
                    remove(getItemCount() - 1);
                }
                historyList.push(val);
                insert(val, 0);
            } else {
                allowed = true;
            }
        }

        @Override
        public void insert(String string, int i) {
            super.insert(string.split(":")[1], i);
            getItem(i).setToolTipText(string);
            getItem(i).setActionCommand(string);
            getItem(i).addActionListener(this);
        }

        public TestCase visit() {
            if (!historyList.isEmpty()) {
                String[] val = historyList.peek().split(":");
                Scenario scenario = testDesign.getProject().getScenarioByName(val[0]);
                if (scenario != null) {
                    return scenario.getTestCaseByName(val[1]);
                }
            }
            return null;
        }

        public void clear() {
            historyList.clear();
            allowed = false;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            log(ae.getActionCommand());
            loadTableModelForSelection(visit());
        }
    }
}
