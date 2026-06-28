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
                "Organize your automated tests into Projects, Scenarios, and Test Cases. " +
                "Drag and drop built-in keywords to build powerful test steps \u2014 " +
                "no coding required. The Object Repository lives here too.",
                TourStep.TargetComponent.NONE,
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
                "AI Copilot",
                "The built-in AI assistant helps you write test steps from natural language, " +
                "explains failures, suggests fixes, and generates test scenarios " +
                "automatically. Powered by GitHub Models.",
                TourStep.TargetComponent.NONE,
                TourStep.ViewToShow.AI_COPILOT
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
                {
                    Component toolbar = frame.getFXToolBar();
                    if (toolbar != null && toolbar.isShowing()) {
                        return SwingUtilities.convertRectangle(
                            toolbar.getParent(),
                            toolbar.getBounds(),
                            frame.getLayeredPane()
                        );
                    }
                    break;
                }
            default:
                break;
        }
        return null;
    }
}
