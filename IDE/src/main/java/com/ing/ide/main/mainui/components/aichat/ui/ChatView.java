package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.utils.WebViewCompat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/** The AI chat transcript. Methods may be called from any thread. */
public interface ChatView {
    /** Set to {@code true} to force the simpler Swing transcript instead of the WebView. */
    String FORCE_SWING_PROPERTY = "ingenious.chat.swing";

    /**
     * Creates the transcript: the styled JavaFX WebView on every JDK (fed through a local script
     * bridge where {@code executeScript} is unsafe, see {@link WebViewCompat}), or a Swing HTML
     * view if forced or if the bridge cannot start.
     */
    static ChatView create() {
        if (Boolean.getBoolean(FORCE_SWING_PROPERTY)) {
            return new ChatSwingView();
        }
        try {
            return new ChatWebView();
        } catch (RuntimeException ex) {
            Logger
                .getLogger(ChatView.class.getName())
                .log(Level.WARNING, "Chat WebView unavailable, using the Swing transcript", ex);
            return new ChatSwingView();
        }
    }

    JComponent getComponent();

    /** Appends a finalized message bubble for the given role. */
    void addMessage(ChatMessage message);

    /** Starts a new, empty assistant bubble that subsequent updates fill in. */
    void beginAssistantMessage();

    /** Replaces the in-progress assistant bubble with the full accumulated text so far. */
    void updateAssistantMessage(String fullTextSoFar);

    void endAssistantMessage();

    /** Appends a small credit-consumption note beneath the last message. */
    void appendUsage(String text);

    void clear();

    void showThinking();

    void hideThinking();

    void showError(String text);

    void appendToolCall(String id, String toolName, String argsSummary);

    void resolveToolCall(String id, String resultSummary, boolean isError, String detail);

    void appendActivityReport(com.ing.engine.aicli.ai.ActivityReport.Result result);

    /** Increases the transcript font size; a no-op where the view cannot be resized. */
    default void zoomIn() {}

    /** Decreases the transcript font size; a no-op where the view cannot be resized. */
    default void zoomOut() {}

    /** Restores the default transcript font size; a no-op where the view cannot be resized. */
    default void resetZoom() {}
}
