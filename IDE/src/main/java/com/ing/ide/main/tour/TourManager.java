package com.ing.ide.main.tour;

import com.ing.ide.main.Main;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.settings.AppSettings;
import java.awt.Component;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JLayeredPane;
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
            new TourStep(
                "Welcome to INGenious Playwright Studio!",
                "This quick tour introduces the key areas of the IDE. Use the arrow keys " +
                "or buttons below to navigate. You can re-launch this tour any time from " +
                "Help \u2192 Start Tour.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.NONE
            ),
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
            new TourStep(
                "Test Execution",
                "Run individual test cases or entire test sets from here. Configure the " +
                "target browser, environment, thread count, and reporting options. " +
                "Live logs stream in real time as tests execute.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.TEST_EXECUTION
            ),
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
        switchView(step.getViewToShow());

        // Allow the view switch to complete before computing spotlight bounds
        Timer settle = new Timer(
            150,
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
