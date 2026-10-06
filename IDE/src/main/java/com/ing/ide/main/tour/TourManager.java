package com.ing.ide.main.tour;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORPage;
import com.ing.ide.main.Main;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.settings.AppSettings;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLayeredPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Orchestrates the INGenious first-time user tour.
 * <p>
 * Usage:
 * <pre>
 *   // Auto-show on first launch (call from afterProjectChange):
 *   if (TourManager.shouldShowTour()) {
 *       new TourManager(frame).startTour();
 *   }
 *
 *   // Re-launch from Help menu:
 *   new TourManager(frame).startTour();
 * </pre>
 *
 * Completion is persisted in {@link AppSettings} so the tour only shows
 * automatically once. It can always be re-launched via Help → Start Tour.
 */
public class TourManager {
    private static final Logger LOG = Logger.getLogger(TourManager.class.getName());

    /** AppSettings key used to track whether the tour has been completed. */
    static final String TOUR_COMPLETED_KEY = "tourCompleted";

    private final AppMainFrame frame;
    private TourOverlayPanel overlay;
    private int currentStep;
    private final List<TourStep> steps;

    public TourManager(AppMainFrame frame) {
        this.frame = frame;
        this.steps = buildSteps();
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the tour has never been completed and should be
     * shown automatically to a first-time user.
     */
    public static boolean shouldShowTour() {
        return !Boolean.parseBoolean(AppSettings.get(TOUR_COMPLETED_KEY));
    }

    /**
     * Starts (or restarts) the tour from step 0.
     * Safe to call from any thread — dispatches to the EDT internally.
     */
    public void startTour() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::startTour);
            return;
        }
        currentStep = 0;
        installOverlay();
        showStep(currentStep);
    }

    // ── Tour Steps ─────────────────────────────────────────────────────────

    private List<TourStep> buildSteps() {
        return Arrays.asList(
            // ── Welcome ───────────────────────────────────────────────────
            new TourStep(
                "Welcome to INGenious",
                "This quick tour introduces the key areas of the IDE. Use the arrow keys " +
                "or buttons below to navigate. You can re-launch this tour any time from " +
                "Help \u2192 Start Tour.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.NONE
            ),
            // ── Test Design ───────────────────────────────────────────────
            new TourStep(
                "Test Design",
                "The Test Design view is where you build your automation. The layout is " +
                "divided into four key areas \u2014 let\u2019s explore each one.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "Test Plan",
                "The Test Plan panel shows your project hierarchy: Projects contain Scenarios, " +
                "and Scenarios contain Test Cases. Click any item to open it in the editor.",
                TourStep.TargetComponent.TEST_PLAN,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "Test Steps Canvas",
                "The Test Steps canvas is where you design each Test Case. Drag keywords from " +
                "the palette, fill in object names and data values row by row, then save.",
                TourStep.TargetComponent.TEST_STEPS,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "Test Data",
                "The Test Data panel holds parameterised data for your test cases. Define " +
                "key\u2011value pairs here and reference them in test steps as {Key}. " +
                "Multiple environments are supported.",
                TourStep.TargetComponent.TEST_DATA,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "Reusable Components",
                "Define common step sequences once and call them from any Test Case \u2014 " +
                "ideal for login flows, shared setup steps, and teardown routines.",
                TourStep.TargetComponent.REUSABLES,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "Object Repository",
                "The Object Repository stores locators for every UI element you test. " +
                "Organise them into pages and groups; INGenious resolves them automatically at runtime.",
                TourStep.TargetComponent.OBJECT_REPO,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "Object Properties",
                "Each object\u2019s properties \u2014 locator type, value, and friendly name \u2014 are " +
                "shown here. Edit and validate locators without leaving the IDE.",
                TourStep.TargetComponent.OBJECT_PROPS,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            // ── Test Execution ────────────────────────────────────────────
            new TourStep(
                "Test Execution",
                "The Test Execution view lets you run test sets, monitor live output, and " +
                "configure execution parameters. Let\u2019s look at each panel.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
            new TourStep(
                "Test Lab",
                "The Test Lab tree organises your Releases and Test Sets. Select a Test Set " +
                "to load it into the canvas, then choose which test cases to run.",
                TourStep.TargetComponent.TEST_LAB,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
            new TourStep(
                "Test Set Canvas",
                "The Test Set canvas shows the test cases in the selected Test Set. Drag " +
                "test cases here from the right panel, enable them, and hit Run.",
                TourStep.TargetComponent.EXEC_TESTSET_CANVAS,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
            new TourStep(
                "Left Quick Settings",
                "Left Quick Settings control iteration mode (continue or break on error), " +
                "screenshot behaviour, and other per-run options for the current Test Set.",
                TourStep.TargetComponent.EXEC_LEFT_SETTINGS,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
            new TourStep(
                "Test Plan Tree",
                "The Test Plan tree on the right shows all Scenarios and Test Cases in your " +
                "project. Check items and click Pull to add them to the active Test Set.",
                TourStep.TargetComponent.EXEC_TESTPLAN_PANEL,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
            new TourStep(
                "Right Quick Settings",
                "Right Quick Settings configure the execution environment: browser, base URL, " +
                "thread count, and driver settings that apply to the current run.",
                TourStep.TargetComponent.EXEC_RIGHT_SETTINGS,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
            // ── Dashboard, API, Toolbar, Done ─────────────────────────────
            new TourStep(
                "Dashboard & Reports",
                "Explore rich HTML execution reports and trend analytics. Compare runs " +
                "side-by-side, drill into failures, and share results with your team " +
                "directly from the built-in report viewer.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.DASHBOARD
            ),
            new TourStep(
                "API Workbench",
                "Test REST and SOAP APIs directly inside the IDE. Build and save requests, " +
                "inspect responses, set up authentication, and integrate API validations " +
                "into your end-to-end test scenarios.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.API_TESTER
            ),
            new TourStep(
                "Toolbar & Navigation",
                "Switch between views, open or create projects, run tests, and toggle themes " +
                "using the toolbar above. Hover over any button to see its tooltip and " +
                "keyboard shortcut.",
                TourStep.TargetComponent.TOOLBAR,
                TourStep.ViewToShow.TEST_DESIGN
            ),
            new TourStep(
                "You\u2019re all set! \uD83D\uDE80",
                "Open the Tutorial project (File \u2192 Open Project \u2192 Projects/Tutorial) " +
                "to follow guided examples. The full documentation is always one click away " +
                "at Help \u2192 Help. Happy testing!",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.TEST_DESIGN
            )
        );
    }

    // ── Overlay Lifecycle ──────────────────────────────────────────────────

    private void installOverlay() {
        // Remove any previous overlay left over from an earlier tour run
        removeOverlay();

        JLayeredPane lp = frame.getLayeredPane();
        overlay = new TourOverlayPanel();
        overlay.setOnNext(this::nextStep);
        overlay.setOnPrev(this::prevStep);
        overlay.setOnSkip(this::endTour);
        overlay.setBounds(0, 0, lp.getWidth(), lp.getHeight());

        // Keep overlay sized to the full layered pane as the window is resized
        lp.addComponentListener(
            new java.awt.event.ComponentAdapter() {

                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    if (overlay != null && overlay.getParent() != null) {
                        overlay.setBounds(0, 0, lp.getWidth(), lp.getHeight());
                        overlay.doLayout();
                        overlay.repaint();
                    }
                }
            }
        );

        lp.add(overlay, JLayeredPane.POPUP_LAYER);
        overlay.setVisible(true);
        overlay.requestFocusInWindow();
        LOG.info("Tour overlay installed");
    }

    private void removeOverlay() {
        if (overlay != null) {
            JLayeredPane lp = frame.getLayeredPane();
            lp.remove(overlay);
            lp.repaint();
            overlay = null;
        }
    }

    // ── Step Navigation ────────────────────────────────────────────────────

    private void showStep(int index) {
        TourStep step = steps.get(index);
        prepareForStep(step); // seed data BEFORE view switch
        switchView(step.getViewToShow());

        // Allow the view switch and tree reloads to settle before spotlighting
        Timer settle = new Timer(
            220,
            e -> {
                if (overlay == null) return;
                Rectangle spot = computeSpotlight(step.getTarget());
                overlay.update(step, index, steps.size(), spot, Main.isDarkMode());
                overlay.revalidate();
                overlay.repaint();
            }
        );
        settle.setRepeats(false);
        settle.start();
    }

    private void nextStep() {
        if (currentStep < steps.size() - 1) {
            currentStep++;
            showStep(currentStep);
        } else {
            endTour();
        }
    }

    private void prevStep() {
        if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
        }
    }

    /**
     * Ends the tour, persists completion, and removes the overlay.
     */
    public void endTour() {
        AppSettings.set(TOUR_COMPLETED_KEY, "true");
        AppSettings.store("Tour completed");
        removeOverlay();
        LOG.info("Tour completed/skipped");
    }

    // ── View Switching ─────────────────────────────────────────────────────

    private void switchView(TourStep.ViewToShow view) {
        switch (view) {
            case TEST_DESIGN:
                frame.showTestDesign();
                break;
            case TEST_EXECUTION:
                frame.showTestExecution();
                break;
            case DASHBOARD:
                frame.showDashBoard();
                break;
            case API_TESTER:
                frame.showAPITester();
                break;
            case AI_COPILOT:
                frame.showAICopilot();
                break;
            default:
                break;
        }
    }

    // ── Pre-step Data Seeding ──────────────────────────────────────────────

    /**
     * Creates sample data in the project model so the spotlighted panel is
     * not empty when first-time users see it.  All operations are idempotent
     * (they check before creating) and are wrapped in try/catch so a failure
     * never aborts the tour.
     */
    private void prepareForStep(TourStep step) {
        Project project = frame.getProject();
        if (project == null) return;
        switch (step.getTarget()) {
            case TEST_DATA:
                ensureTestDataContent(project);
                break;
            case REUSABLES:
                ensureReusableContent(project);
                break;
            case OBJECT_REPO:
                ensureORContent(project);
                break;
            case OBJECT_PROPS:
                // Ensure data exists, then select the first object so properties appear
                ensureORContent(project);
                SwingUtilities.invokeLater(() -> selectFirstORObject(project));
                break;
            case TEST_LAB:
                ensureTestLabContent(project);
                break;
            default:
                break;
        }
    }

    private void ensureReusableContent(Project project) {
        try {
            if (project.getReusableScenarios().isEmpty()) {
                Scenario sc = project.addReusableScenario("SampleReusable");
                sc.addTestCase("TC_SampleStep");
                project.save();
            }
            com.ing.ide.main.mainui.components.testdesign.tree.ReusableTree rt = frame
                .getTestDesign()
                .getReusableTree();
            rt.load();
            SwingUtilities.invokeLater(() -> expandAllRows(rt.getTree()));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tour: could not seed Reusable data", ex);
        }
    }

    private void ensureORContent(Project project) {
        try {
            WebOR webOR = project.getObjectRepository().getWebOR();
            if (webOR.getPages().isEmpty()) {
                WebORPage page = webOR.addPage("SamplePage");
                page.addObject("SampleButton");
                project.save();
            }
            frame.getTestDesign().getObjectRepo().load();
            JTree orTree = frame.getTestDesign().getObjectRepo().getWebOR().getProjectTree().tree;
            SwingUtilities.invokeLater(() -> expandAllRows(orTree));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tour: could not seed OR data", ex);
        }
    }

    /**
     * Selects the first object in the Web OR project tree so the Object
     * Properties table is populated when that step is spotlighted.
     */
    @SuppressWarnings("unchecked")
    private void selectFirstORObject(Project project) {
        try {
            WebOR webOR = project.getObjectRepository().getWebOR();
            java.util.List pages = webOR.getPages();
            if (pages.isEmpty()) return;
            WebORPage page = (WebORPage) pages.get(0);
            java.util.List groups = page.getObjectGroups();
            if (groups.isEmpty()) return;
            com.ing.datalib.or.common.ObjectGroup grp = (com.ing.datalib.or.common.ObjectGroup) groups.get(
                0
            );
            java.util.List objs = grp.getObjects();
            if (objs.isEmpty()) return;
            com.ing.datalib.or.common.ORObjectInf obj = (com.ing.datalib.or.common.ORObjectInf) objs.get(
                0
            );
            JTree orTree = frame.getTestDesign().getObjectRepo().getWebOR().getProjectTree().tree;
            javax.swing.tree.TreePath path = obj.getTreePath();
            if (path != null) {
                orTree.setSelectionPath(path);
                orTree.scrollPathToVisible(path);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tour: could not select first OR object", ex);
        }
    }

    private void ensureTestLabContent(Project project) {
        try {
            if (project.getReleases().isEmpty()) {
                Release release = project.addRelease("SampleRelease");
                release.addTestSet("SampleTestSet");
                project.save();
            }
            com.ing.ide.main.mainui.components.testexecution.tree.TestSetTree tst = frame
                .getTestExecution()
                .getTestSetTree();
            tst.load();
            SwingUtilities.invokeLater(() -> expandAllRows(tst.getTree()));
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tour: could not seed Test Lab data", ex);
        }
    }

    /**
     * Seeds at least one Global Data row so the Test Data panel shows content,
     * then triggers the panel to load its data by selecting the Global Data tab.
     */
    private void ensureTestDataContent(Project project) {
        try {
            com.ing.datalib.component.TestData defaultData = project
                .getTestData()
                .getTestDataFor("Default");
            if (defaultData != null) {
                com.ing.datalib.testdata.model.GlobalDataModel gdModel = defaultData.getGlobalData();
                gdModel.load();
                if (gdModel.getRowCount() == 0) {
                    gdModel.addRecord();
                    gdModel.setValueAt("#SampleKey", 0, 0);
                    if (gdModel.getColumnCount() > 1) {
                        gdModel.setValueAt("SampleValue", 0, 1);
                    }
                    gdModel.saveChanges();
                }
            }
            com.ing.ide.main.mainui.components.testdesign.testdata.TestDataComponent tdc = frame
                .getTestDesign()
                .getTestDatacomp();
            tdc.load();
            // Programmatically select the Global Data tab (index 0) to trigger panel.load()
            SwingUtilities.invokeLater(
                () -> {
                    javax.swing.JTabbedPane envTab = tdc.getTestdataTab();
                    if (envTab != null && envTab.getTabCount() > 0) {
                        envTab.setSelectedIndex(0);
                        java.awt.Component c = envTab.getSelectedComponent();
                        if (c instanceof javax.swing.JTabbedPane) {
                            javax.swing.JTabbedPane inner = (javax.swing.JTabbedPane) c;
                            if (inner.getTabCount() > 0) {
                                inner.setSelectedIndex(0); // Global Data is always tab 0
                            }
                        }
                    }
                }
            );
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Tour: could not seed Test Data", ex);
        }
    }

    private void expandAllRows(JTree tree) {
        if (tree == null) return;
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    // ── Spotlight Bounds ───────────────────────────────────────────────────

    /**
     * Returns the bounding rectangle (in {@link JLayeredPane} coordinates)
     * of the component to spotlight, or {@code null} for no spotlight.
     */
    private Rectangle computeSpotlight(TourStep.TargetComponent target) {
        switch (target) {
            case TOOLBAR:
                return toLayeredPaneBounds(frame.getFXToolBar());
            case TEST_PLAN:
                return toLayeredPaneBounds(
                    frame.getTestDesign().getTestDesignUI().getTestPlanPanel()
                );
            case TEST_STEPS:
                return toLayeredPaneBounds(frame.getTestDesign().getTestCaseComponent());
            case TEST_DATA:
                return toLayeredPaneBounds(frame.getTestDesign().getTestDatacomp());
            case REUSABLES:
                return toLayeredPaneBounds(
                    frame.getTestDesign().getTestDesignUI().getReusablesPanel()
                );
            case OBJECT_REPO:
                return toLayeredPaneBounds(frame.getTestDesign().getObjectRepo());
            case OBJECT_PROPS:
                return toLayeredPaneBounds(
                    frame.getTestDesign().getObjectRepo().getWebOR().getObjectTable()
                );
            case TEST_LAB:
                return toLayeredPaneBounds(
                    frame.getTestExecution().getTestExecutionUI().getTestLabPanel()
                );
            case EXEC_LEFT_SETTINGS:
                return toLayeredPaneBounds(
                    frame.getTestExecution().getTestExecutionUI().getLeftQuickSettingsPanel()
                );
            case EXEC_TESTSET_CANVAS:
                return toLayeredPaneBounds(
                    frame.getTestExecution().getTestExecutionUI().getTestSetCanvas()
                );
            case EXEC_TESTPLAN_PANEL:
                return toLayeredPaneBounds(
                    frame.getTestExecution().getTestExecutionUI().getTestPlanTreePanel()
                );
            case EXEC_RIGHT_SETTINGS:
                return toLayeredPaneBounds(
                    frame.getTestExecution().getTestExecutionUI().getRightQuickSettingsPanel()
                );
            default:
                return null;
        }
    }

    /**
     * Converts a component's own bounds into {@link JLayeredPane} coordinate space.
     * Returns {@code null} if the component is null, not showing, or has no parent.
     */
    private Rectangle toLayeredPaneBounds(Component c) {
        if (c == null || !c.isShowing() || c.getParent() == null) return null;
        return SwingUtilities.convertRectangle(
            c.getParent(),
            c.getBounds(),
            frame.getLayeredPane()
        );
    }
}
