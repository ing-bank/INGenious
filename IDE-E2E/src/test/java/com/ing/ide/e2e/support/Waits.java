package com.ing.ide.e2e.support;

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;

/**
 * Waits for UI state. Conditions run on the calling (test) thread, never on the Swing event thread,
 * because some check JavaFX state and blocking JavaFX from the Swing thread can deadlock embedded
 * JavaFX. Read Swing state through {@link #swing(Callable)}.
 */
public final class Waits {

    private Waits() {}

    /**
     * Waits up to {@link Timeouts#STEP} for a condition.
     *
     * @param description what is awaited, shown when the wait times out
     * @param condition the condition
     */
    public static void until(final String description, final BooleanSupplier condition) {
        Pause.pause(
            new Condition(description) {

                @Override
                public boolean test() {
                    return condition.getAsBoolean();
                }
            },
            Timeout.timeout(Timeouts.STEP.toMillis())
        );
    }

    /**
     * Polls a condition for a while without failing.
     *
     * @param condition the condition
     * @param millis how long to keep polling
     * @return whether the condition became true
     */
    public static boolean within(final BooleanSupplier condition, final long millis) {
        final long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Pause.pause(100);
        }
        return condition.getAsBoolean();
    }

    /**
     * Reads Swing state on the Swing event thread.
     *
     * @param query the query
     * @param <T> result type
     * @return the result
     */
    public static <T> T swing(final Callable<T> query) {
        return GuiActionRunner.execute(query);
    }
}
