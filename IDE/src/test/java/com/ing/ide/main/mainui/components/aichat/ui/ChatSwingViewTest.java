package com.ing.ide.main.mainui.components.aichat.ui;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.testng.annotations.Test;

public class ChatSwingViewTest {

    private static String htmlAfter(ChatSwingView view) throws Exception {
        AtomicReference<String> html = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> html.set(view.currentHtml()));
        return html.get();
    }

    @Test
    public void userAndAssistantMessagesAreRendered() throws Exception {
        ChatSwingView view = new ChatSwingView();
        view.addMessage(ChatMessage.user("How do I run a test?"));
        view.addMessage(ChatMessage.assistant("Use **Run** in the toolbar."));
        String html = htmlAfter(view);
        assertTrue(html.contains("You"));
        assertTrue(html.contains("How do I run a test?"));
        assertTrue(html.contains("<strong>Run</strong>") || html.contains("<b>Run</b>"));
    }

    @Test
    public void streamingUpdatesReplaceTheSameBubble() throws Exception {
        ChatSwingView view = new ChatSwingView();
        view.beginAssistantMessage();
        view.updateAssistantMessage("Hel");
        view.updateAssistantMessage("Hello world");
        view.endAssistantMessage();
        String html = htmlAfter(view);
        assertTrue(html.contains("Hello world"));
        assertEquals(html.contains("Hel</"), false);
        assertEquals(html.split("<table", -1).length - 1, 1);
    }

    @Test
    public void thinkingPlaceholderIsRemovedWhenAMessageArrives() throws Exception {
        ChatSwingView view = new ChatSwingView();
        view.showThinking();
        assertTrue(htmlAfter(view).contains("Thinking"));
        view.addMessage(ChatMessage.assistant("done"));
        assertEquals(htmlAfter(view).contains("Thinking"), false);
    }

    @Test
    public void toolCallIsMarkedDoneOrFailedWhenResolved() throws Exception {
        ChatSwingView view = new ChatSwingView();
        view.appendToolCall("1", "run_test", "{case}");
        assertTrue(htmlAfter(view).contains("run_test"));
        view.resolveToolCall("1", "passed", false, "{}");
        String ok = htmlAfter(view);
        assertTrue(ok.contains("\u2713"));
        assertTrue(ok.contains("passed"));
        view.appendToolCall("2", "save", "{}");
        view.resolveToolCall("2", "boom", true, "{}");
        assertTrue(htmlAfter(view).contains("\u2717"));
    }

    @Test
    public void errorsAndUsageAreEscapedNotInjectedAsMarkup() throws Exception {
        ChatSwingView view = new ChatSwingView();
        view.showError("<script>alert(1)</script>");
        view.appendUsage("<b>3 credits</b>");
        String html = htmlAfter(view);
        assertEquals(html.contains("<script>"), false);
        assertEquals(html.contains("<b>3 credits</b>"), false);
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    public void clearRemovesEverything() throws Exception {
        ChatSwingView view = new ChatSwingView();
        view.addMessage(ChatMessage.user("hi"));
        view.clear();
        assertEquals(htmlAfter(view).contains("hi"), false);
    }
}
