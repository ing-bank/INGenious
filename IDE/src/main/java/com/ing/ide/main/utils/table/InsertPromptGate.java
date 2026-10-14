package com.ing.ide.main.utils.table;

import javax.swing.Timer;

/**
 * Guards the inline insert-row / insert-column prompts against accidental
 * activation.
 *
 * <p>The prompt is only revealed after the pointer has rested on the same
 * insertion boundary for {@link #REVEAL_DELAY_MS}, and it only accepts a click
 * once it has been visible for {@link #ARM_DELAY_MS}. Together this prevents a
 * prompt from appearing under a moving pointer and being clicked before the
 * user has had a chance to see it.
 */
class InsertPromptGate {
    private static final int REVEAL_DELAY_MS = 400;
    private static final int ARM_DELAY_MS = 150;

    private final Timer revealTimer;

    private boolean revealed;
    private long revealedAt;

    /**
     * Creates a gate that notifies the owner once the prompt becomes visible.
     *
     * @param onReveal callback invoked on the EDT when the prompt is revealed
     */
    InsertPromptGate(Runnable onReveal) {
        revealTimer =
            new Timer(
                REVEAL_DELAY_MS,
                e -> {
                    revealed = true;
                    revealedAt = System.currentTimeMillis();
                    onReveal.run();
                }
            );
        revealTimer.setRepeats(false);
    }

    /**
     * Restarts the dwell countdown because the hovered insertion boundary changed.
     */
    void candidateChanged() {
        revealed = false;
        revealTimer.restart();
    }

    /**
     * Hides the prompt and cancels any pending dwell countdown.
     */
    void reset() {
        revealed = false;
        revealTimer.stop();
    }

    /**
     * Returns whether the prompt has been hovered long enough to be shown.
     *
     * @return {@code true} when the prompt may be painted
     */
    boolean isRevealed() {
        return revealed;
    }

    /**
     * Returns whether the prompt has been visible long enough to accept a click.
     *
     * @return {@code true} when the prompt may be activated
     */
    boolean isArmed() {
        return revealed && System.currentTimeMillis() - revealedAt >= ARM_DELAY_MS;
    }
}
