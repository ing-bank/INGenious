package com.ing.ide.main.tour;

/**
 * Represents a single step in the INGenious first-time user tour.
 * Each step defines the text content, which UI component to spotlight,
 * and which IDE view to switch to when showing the step.
 */
public class TourStep {

    /**
     * Which component should be highlighted with the spotlight cutout.
     * NONE     = full dark overlay with a centred callout card.
     * TOOLBAR  = spotlight the FX toolbar strip.
     * TEST_PLAN     = Test Plan tree panel (top-left of Test Design).
     * TEST_STEPS    = Test Steps / Test Case canvas (centre panel).
     * REUSABLES     = Reusable Components panel (bottom-left).
     * OBJECT_REPO   = Object Repository panel (right side, full).
     * OBJECT_PROPS  = Object Properties table (bottom of OR panel).
     */
    public enum TargetComponent {
        NONE,
        TOOLBAR,
        TEST_PLAN,
        TEST_STEPS,
        REUSABLES,
        OBJECT_REPO,
        OBJECT_PROPS
    }

    /**
     * Which IDE view panel should be visible while this step is shown.
     * NONE means "don't change the current view".
     */
    public enum ViewToShow {
        NONE,
        TEST_DESIGN,
        TEST_EXECUTION,
        DASHBOARD,
        API_TESTER,
        AI_COPILOT
    }

    private final String title;
    private final String description;
    private final TargetComponent target;
    private final ViewToShow viewToShow;

    public TourStep(
        String title,
        String description,
        TargetComponent target,
        ViewToShow viewToShow
    ) {
        this.title = title;
        this.description = description;
        this.target = target;
        this.viewToShow = viewToShow;
    }

    /** Convenience constructor with no spotlight and no view switch. */
    public TourStep(String title, String description) {
        this(title, description, TargetComponent.NONE, ViewToShow.NONE);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TargetComponent getTarget() {
        return target;
    }

    public ViewToShow getViewToShow() {
        return viewToShow;
    }
}
