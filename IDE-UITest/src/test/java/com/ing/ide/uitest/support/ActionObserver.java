package com.ing.ide.uitest.support;

import java.awt.Point;

/**
 * Follows each user action performed by the {@link UiDriver}, e.g. to show it in a recording.
 * Per action the calls are: {@link #actionStarting()}, {@link #pointerMoved} for every step of the
 * pointer moving to the target, {@link #beforeAction}, then {@link #actionFinished()}.
 */
public interface ActionObserver {
    /** Observer that ignores all actions. */
    ActionObserver NONE = new ActionObserver() {};

    /**
     * A user action is about to start.
     */
    default void actionStarting() {}

    /**
     * The pointer moved one step towards the action's target.
     *
     * @param screenPoint pointer position in screen coordinates
     * @param description what the user is about to do
     */
    default void pointerMoved(final Point screenPoint, final String description) {}

    /**
     * The pointer is on the target and the action is performed next.
     *
     * @param screenPoint where the action happens on screen, or {@code null} if unknown
     * @param description what the user does, e.g. {@code Click "Create Project"}
     */
    default void beforeAction(final Point screenPoint, final String description) {}

    /**
     * The action has been performed.
     */
    default void actionFinished() {}
}
