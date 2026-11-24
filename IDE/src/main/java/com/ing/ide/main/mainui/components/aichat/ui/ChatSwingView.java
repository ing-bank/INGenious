package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.engine.aicli.ai.ActivityReport;
import com.ing.engine.support.DesktopApi;
import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.mainui.components.aichat.util.MarkdownRenderer;
import java.awt.Color;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

/**
 * Pure-Swing chat transcript used instead of {@link ChatWebView} where the JavaFX WebView cannot
 * be scripted (see {@code WebViewCompat}). Messages are kept as a list and rendered to HTML in a
 * {@link JEditorPane}; renders are coalesced so token streaming stays responsive. Styling is
 * deliberately simpler than the WebView version (Swing renders HTML 3.2 plus a small CSS subset).
 * All public methods may be called from any thread.
 */
public class ChatSwingView implements ChatView {
    private static final int RENDER_DELAY_MS = 100;
    private static final int STICK_TO_BOTTOM_PX = 48;
    private static final String THINKING_ID = "thinking";

    private enum Kind {
        USER,
        ASSISTANT,
        ERROR,
        USAGE,
        ACTIVITY,
        TOOL,
        THINKING
    }

    private static final class Block {
        final Kind kind;
        final String id;
        String name;
        String html;
        Boolean toolError;

        Block(Kind kind, String id, String html) {
            this.kind = kind;
            this.id = id;
            this.html = html;
        }
    }

    private final JEditorPane pane = new JEditorPane();
    private final JScrollPane scroll = new JScrollPane(pane);
    private final List<Block> blocks = new ArrayList<>();
    private final Timer renderTimer = new Timer(RENDER_DELAY_MS, event -> render());
    private Block current;

    public ChatSwingView() {
        pane.setContentType("text/html");
        pane.setEditable(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.addHyperlinkListener(
            event -> {
                if (
                    event.getEventType() == HyperlinkEvent.EventType.ACTIVATED &&
                    event.getURL() != null
                ) {
                    try {
                        DesktopApi.browse(event.getURL().toURI());
                    } catch (URISyntaxException ex) {
                        // ignore malformed links produced by the model
                    }
                }
            }
        );
        renderTimer.setRepeats(false);
        // Colours are baked into the HTML, so re-render when the theme (look and feel) changes.
        UIManager.addPropertyChangeListener(
            event -> {
                if ("lookAndFeel".equals(event.getPropertyName())) {
                    SwingUtilities.invokeLater(this::renderSoon);
                }
            }
        );
    }

    @Override
    public JComponent getComponent() {
        return scroll;
    }

    @Override
    public void addMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        String html = MarkdownRenderer.toHtml(message.getContent());
        Kind kind = ChatMessage.ROLE_USER.equals(message.getRole()) ? Kind.USER : Kind.ASSISTANT;
        edt(() -> append(new Block(kind, null, html)));
    }

    @Override
    public void beginAssistantMessage() {
        edt(
            () -> {
                current = new Block(Kind.ASSISTANT, null, "");
                append(current);
            }
        );
    }

    @Override
    public void updateAssistantMessage(String fullTextSoFar) {
        String html = MarkdownRenderer.toHtml(fullTextSoFar);
        edt(
            () -> {
                if (current != null) {
                    current.html = html;
                    renderSoon();
                }
            }
        );
    }

    @Override
    public void endAssistantMessage() {
        edt(() -> current = null);
    }

    @Override
    public void appendUsage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String html = MarkdownRenderer.escape(text);
        edt(
            () -> {
                blocks.add(new Block(Kind.USAGE, null, html));
                renderSoon();
            }
        );
    }

    @Override
    public void clear() {
        edt(
            () -> {
                blocks.clear();
                current = null;
                renderSoon();
            }
        );
    }

    @Override
    public void showThinking() {
        edt(
            () -> {
                if (blocks.stream().noneMatch(b -> b.kind == Kind.THINKING)) {
                    blocks.add(new Block(Kind.THINKING, THINKING_ID, "Thinking\u2026"));
                    renderSoon();
                }
            }
        );
    }

    @Override
    public void hideThinking() {
        edt(
            () -> {
                if (blocks.removeIf(b -> b.kind == Kind.THINKING)) {
                    renderSoon();
                }
            }
        );
    }

    @Override
    public void showError(String text) {
        String html = MarkdownRenderer.escape(text);
        edt(() -> append(new Block(Kind.ERROR, null, html)));
    }

    @Override
    public void appendToolCall(String id, String toolName, String argsSummary) {
        edt(
            () -> {
                Block block = new Block(Kind.TOOL, id, argsSummary == null ? "" : argsSummary);
                block.name = toolName;
                append(block);
            }
        );
    }

    @Override
    public void resolveToolCall(String id, String resultSummary, boolean isError, String detail) {
        edt(
            () -> {
                for (Block block : blocks) {
                    if (block.kind == Kind.TOOL && block.id != null && block.id.equals(id)) {
                        block.toolError = isError;
                        block.html = resultSummary == null ? "" : resultSummary;
                        renderSoon();
                        return;
                    }
                }
            }
        );
    }

    @Override
    public void appendActivityReport(ActivityReport.Result result) {
        if (result == null || result.totalCalls == 0) {
            return;
        }
        String html = activityHtml(result);
        edt(() -> append(new Block(Kind.ACTIVITY, null, html)));
    }

    /** Current transcript HTML; call on the Swing thread. Used by tests. */
    String currentHtml() {
        return buildHtml();
    }

    private static void edt(Runnable action) {
        SwingUtilities.invokeLater(action);
    }

    private void append(Block block) {
        blocks.removeIf(b -> b.kind == Kind.THINKING);
        blocks.add(block);
        renderSoon();
    }

    private void renderSoon() {
        if (!renderTimer.isRunning()) {
            renderTimer.start();
        }
    }

    private void render() {
        JScrollBar bar = scroll.getVerticalScrollBar();
        boolean stick =
            bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - STICK_TO_BOTTOM_PX;
        int previous = bar.getValue();
        pane.setText(buildHtml());
        SwingUtilities.invokeLater(() -> bar.setValue(stick ? bar.getMaximum() : previous));
    }

    private String buildHtml() {
        boolean dark = isDark();
        StringBuilder html = new StringBuilder();
        html
            .append("<html><body text='")
            .append(dark ? "#e4e0ee" : "#241c33")
            .append("' bgcolor='")
            .append(hex(pane.getBackground()))
            .append("'>");
        for (Block block : blocks) {
            html.append(renderBlock(block, dark));
        }
        return html.append("</body></html>").toString();
    }

    private static String renderBlock(Block block, boolean dark) {
        String muted = dark ? "#8b83a6" : "#7a7188";
        switch (block.kind) {
            case USER:
                return card(dark ? "#3a2466" : "#ece3ff", "<b>You</b><br>" + block.html);
            case ASSISTANT:
                return card(dark ? "#241f33" : "#f4f1fa", block.html);
            case ERROR:
                return card(dark ? "#4a1c28" : "#fde8ec", block.html);
            case ACTIVITY:
                return card(dark ? "#1c1728" : "#f1ecfb", block.html);
            case USAGE:
                return "<font size='2' color='" + muted + "'>\u25C9 " + block.html + "</font><br>";
            case THINKING:
                return "<i><font color='" + muted + "'>" + block.html + "</font></i><br>";
            case TOOL:
                String icon = block.toolError == null
                    ? "\u25CC"
                    : (block.toolError ? "\u2717" : "\u2713");
                String color = block.toolError == null
                    ? muted
                    : (
                        block.toolError
                            ? (dark ? "#ff4d6d" : "#d1304a")
                            : (dark ? "#33ff9c" : "#1f9d55")
                    );
                return (
                    "<font size='2' color='" +
                    color +
                    "'>" +
                    icon +
                    " <b>" +
                    MarkdownRenderer.escape(block.name == null ? "" : block.name) +
                    "</b> " +
                    block.html +
                    "</font><br>"
                );
            default:
                return "";
        }
    }

    private static String card(String background, String inner) {
        return (
            "<table width='100%' cellpadding='8' cellspacing='0' border='0'><tr><td bgcolor='" +
            background +
            "'>" +
            inner +
            "</td></tr></table><font size='1'><br></font>"
        );
    }

    private static String activityHtml(ActivityReport.Result r) {
        List<String> parts = new ArrayList<>();
        if (r.okCount > 0) {
            parts.add(r.okCount + " OK");
        }
        if (r.infoCount > 0) {
            parts.add(r.infoCount + " INFO");
        }
        if (r.warnCount > 0) {
            parts.add(r.warnCount + " WARN");
        }
        if (r.failCount > 0) {
            parts.add(r.failCount + " FAIL");
        }
        String foot = r.totalCalls + (r.totalCalls == 1 ? " tool call" : " tool calls");
        if (r.minorCount > 0) {
            foot += " \u00b7 " + r.minorCount + " lookup" + (r.minorCount == 1 ? "" : "s");
        }
        if (r.retryCount > 0) {
            foot += " \u00b7 " + r.retryCount + " self-corrected";
        }
        String pills = parts.isEmpty() ? "" : "<br>" + String.join(" \u00b7 ", parts);
        return (
            "<b>Summary</b>" +
            pills +
            "<br><font size='2'>" +
            MarkdownRenderer.escape(foot) +
            "</font>"
        );
    }

    private static boolean isDark() {
        Color background = UIManager.getColor("Panel.background");
        if (background == null) {
            return false;
        }
        int luminance =
            (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) /
            1000;
        return luminance < 128;
    }

    private static String hex(Color color) {
        Color c = color == null ? Color.WHITE : color;
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
