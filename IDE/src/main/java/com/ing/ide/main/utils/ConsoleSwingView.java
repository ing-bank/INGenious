package com.ing.ide.main.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.support.DesktopApi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Pure-Swing execution console used instead of {@link ConsoleWebView} where the JavaFX WebView
 * cannot be scripted (see {@link WebViewCompat}). It shows the same rows (timestamp, coloured
 * status label, message), summary and clickable report link, and follows the app's light/dark
 * theme. All public methods may be called from any thread.
 */
public class ConsoleSwingView implements ConsoleView {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(
        "HH:mm:ss.SSS"
    );
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Object REPORT_KEY = "reportPath";
    private static final int DIVIDER_WIDTH = 72;
    private static final int MIN_LABEL_WIDTH = 6;
    private static final int STICK_TO_BOTTOM_PX = 48;
    private static final Color ACCENT = Color.decode("#7724FF");
    private static final List<ConsoleSwingView> INSTANCES = new ArrayList<>();

    private enum Kind {
        ROW,
        SPACER,
        DIVIDER,
        REPORT,
        SUMMARY
    }

    /** One console entry; which fields are set depends on {@link #kind}. */
    private static final class Entry {
        final Kind kind;
        final String time;
        final String label;
        final String bucket;
        final String text;
        final String[] summary;

        private Entry(
            Kind kind,
            String time,
            String label,
            String bucket,
            String text,
            String[] summary
        ) {
            this.kind = kind;
            this.time = time;
            this.label = label;
            this.bucket = bucket;
            this.text = text;
            this.summary = summary;
        }

        static Entry row(String time, String label, String bucket, String text) {
            return new Entry(Kind.ROW, time, label, bucket, text, null);
        }

        static Entry simple(Kind kind) {
            return new Entry(kind, null, null, null, null, null);
        }

        static Entry report(String path) {
            return new Entry(Kind.REPORT, null, null, null, path, null);
        }

        /** {@code values}: status, total, passed, failed, duration. */
        static Entry summary(String[] values) {
            return new Entry(Kind.SUMMARY, null, null, null, null, values);
        }
    }

    private final JPanel root = new JPanel(new BorderLayout());
    private final JTextPane pane = new JTextPane();
    private final JScrollPane scroll = new JScrollPane(pane);
    private final List<Entry> entries = new ArrayList<>();
    private boolean dark;
    private boolean showingPlaceholder;

    public ConsoleSwingView() {
        this(com.ing.ide.main.Main.isDarkMode());
    }

    ConsoleSwingView(boolean dark) {
        this.dark = dark;
        pane.setEditable(false);
        pane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        ((DefaultCaret) pane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        installReportLinkHandling();
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        applyColors();
        showPlaceholder();
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
    }

    /** Notifies every live Swing console that the app-wide theme changed. */
    public static void applyThemeToAll(boolean dark) {
        List<ConsoleSwingView> snapshot;
        synchronized (INSTANCES) {
            snapshot = new ArrayList<>(INSTANCES);
        }
        for (ConsoleSwingView view : snapshot) {
            view.setDarkMode(dark);
        }
    }

    @Override
    public JComponent getComponent() {
        return root;
    }

    @Override
    public void appendLine(String text) {
        String line = ConsoleWebView.sanitize(text);
        if (line.startsWith(ConsoleWebView.REPORT_PATH_TAG)) {
            add(Entry.report(line.substring(ConsoleWebView.REPORT_PATH_TAG.length())));
            return;
        }
        if (line.startsWith(ConsoleWebView.SUMMARY_TAG)) {
            String[] summary = parseSummary(line.substring(ConsoleWebView.SUMMARY_TAG.length()));
            if (summary != null) {
                add(Entry.summary(summary));
            }
            return;
        }
        if (line.trim().isEmpty()) {
            add(Entry.simple(Kind.SPACER));
            return;
        }
        if (ConsoleWebView.DIVIDER.matcher(line.trim()).matches()) {
            add(Entry.simple(Kind.DIVIDER));
            return;
        }
        addRow(line, ConsoleWebView.classify(line, false));
    }

    @Override
    public void appendErrorLine(String text) {
        String line = ConsoleWebView.sanitize(text);
        addRow(line, ConsoleWebView.classify(line, true));
    }

    @Override
    public void clear() {
        SwingUtilities.invokeLater(
            () -> {
                entries.clear();
                clearDocument();
                showPlaceholder();
            }
        );
    }

    @Override
    public void setDarkMode(boolean dark) {
        SwingUtilities.invokeLater(
            () -> {
                this.dark = dark;
                applyColors();
                clearDocument();
                if (entries.isEmpty()) {
                    showPlaceholder();
                } else {
                    for (Entry entry : entries) {
                        render(entry);
                    }
                }
            }
        );
    }

    /** Current document text; call on the Swing thread. Used by tests. */
    String plainText() {
        StyledDocument doc = pane.getStyledDocument();
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void addRow(String text, String[] tag) {
        String line = text == null ? "null" : text;
        add(Entry.row(LocalTime.now().format(TIME_FORMAT), tag[0], tag[1], line));
    }

    private void add(Entry entry) {
        SwingUtilities.invokeLater(
            () -> {
                if (showingPlaceholder) {
                    clearDocument();
                }
                entries.add(entry);
                boolean stick = nearBottom();
                render(entry);
                if (stick) {
                    scrollToBottom();
                }
            }
        );
    }

    private void render(Entry entry) {
        switch (entry.kind) {
            case ROW:
                Color color = bucketColor(entry.bucket);
                insert(entry.time + " ", style(mutedColor(), false, null));
                insert(
                    pill(entry.label),
                    style(color, true, blend(color, backgroundColor(), 0.18f))
                );
                insert("  " + entry.text + "\n", style(foregroundColor(), false, null));
                break;
            case SPACER:
                insert("\n", style(foregroundColor(), false, null));
                break;
            case DIVIDER:
                insert("\u2500".repeat(DIVIDER_WIDTH) + "\n", style(mutedColor(), false, null));
                break;
            case REPORT:
                renderReport(entry.text);
                break;
            case SUMMARY:
                renderSummary(entry.summary);
                break;
            default:
                break;
        }
    }

    private void renderReport(String path) {
        Color accent = bucketColor("log");
        insert(pill("REPORT"), style(accent, true, blend(accent, backgroundColor(), 0.18f)));
        SimpleAttributeSet link = style(accent, false, null);
        StyleConstants.setUnderline(link, true);
        link.addAttribute(REPORT_KEY, path);
        insert("  " + path, link);
        SimpleAttributeSet open = style(accent, true, null);
        open.addAttribute(REPORT_KEY, path);
        insert("  Open", open);
        insert("\n", style(foregroundColor(), false, null));
    }

    private void renderSummary(String[] values) {
        boolean passed = "PASSED".equals(values[0]);
        Color color = bucketColor(passed ? "pass" : "fail");
        insert("\n", style(foregroundColor(), false, null));
        insert(" Execution Summary ", style(foregroundColor(), true, null));
        insert(pill(values[0]), style(color, true, blend(color, backgroundColor(), 0.18f)));
        insert("\n", style(foregroundColor(), false, null));
        insert(
            "  Total " +
            values[1] +
            "    Passed " +
            values[2] +
            "    Failed " +
            values[3] +
            "    Duration " +
            values[4] +
            "\n",
            style(foregroundColor(), false, null)
        );
    }

    private static String pill(String label) {
        String text = label == null ? "" : label;
        return " " + String.format(Locale.ROOT, "%-" + MIN_LABEL_WIDTH + "s", text) + " ";
    }

    private void insert(String text, AttributeSet attributes) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), text, attributes);
        } catch (BadLocationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void showPlaceholder() {
        showingPlaceholder = true;
        SimpleAttributeSet attributes = style(mutedColor(), false, null);
        StyleConstants.setItalic(attributes, true);
        insert("Waiting for output\u2026", attributes);
    }

    private void clearDocument() {
        showingPlaceholder = false;
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private boolean nearBottom() {
        JScrollBar bar = scroll.getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - STICK_TO_BOTTOM_PX;
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(
            () -> {
                JScrollBar bar = scroll.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            }
        );
    }

    private static SimpleAttributeSet style(Color foreground, boolean bold, Color background) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes, Font.MONOSPACED);
        StyleConstants.setFontSize(attributes, 12);
        StyleConstants.setForeground(attributes, foreground);
        StyleConstants.setBold(attributes, bold);
        if (background != null) {
            StyleConstants.setBackground(attributes, background);
        }
        return attributes;
    }

    /** Returns status, total, passed, failed, duration; null when the JSON is unreadable. */
    private static String[] parseSummary(String json) {
        try {
            JsonNode data = JSON.readTree(json);
            return new String[] {
                data.path("status").asText(),
                data.path("total").asText(),
                data.path("passed").asText(),
                data.path("failed").asText(),
                data.path("duration").asText()
            };
        } catch (Exception ex) {
            return null;
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        JLabel title = new JLabel("CONSOLE");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 11f));
        JLabel live = new JLabel("\u25CF LIVE");
        live.setForeground(Color.WHITE);
        live.setFont(live.getFont().deriveFont(Font.PLAIN, 10f));
        header.add(title, BorderLayout.WEST);
        header.add(live, BorderLayout.EAST);
        return header;
    }

    private void installReportLinkHandling() {
        pane.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent event) {
                    String path = reportPathAt(event);
                    if (path != null && SwingUtilities.isLeftMouseButton(event)) {
                        DesktopApi.open(new File(path));
                    }
                }
            }
        );
        pane.addMouseMotionListener(
            new MouseAdapter() {

                @Override
                public void mouseMoved(MouseEvent event) {
                    int cursor = reportPathAt(event) != null
                        ? Cursor.HAND_CURSOR
                        : Cursor.TEXT_CURSOR;
                    pane.setCursor(Cursor.getPredefinedCursor(cursor));
                }
            }
        );
    }

    private String reportPathAt(MouseEvent event) {
        int position = pane.viewToModel2D(event.getPoint());
        if (position < 0) {
            return null;
        }
        AttributeSet attributes = pane
            .getStyledDocument()
            .getCharacterElement(position)
            .getAttributes();
        Object path = attributes.getAttribute(REPORT_KEY);
        return path instanceof String ? (String) path : null;
    }

    private void applyColors() {
        pane.setBackground(backgroundColor());
        scroll.getViewport().setBackground(backgroundColor());
    }

    private Color backgroundColor() {
        return Color.decode(dark ? "#15121e" : "#fbfaff");
    }

    private Color foregroundColor() {
        return Color.decode(dark ? "#e4e0ee" : "#241c33");
    }

    private Color mutedColor() {
        return Color.decode(dark ? "#8b83a6" : "#7a7188");
    }

    private Color bucketColor(String bucket) {
        String hex;
        switch (bucket) {
            case "pass":
                hex = dark ? "#33ff9c" : "#1f9d55";
                break;
            case "done":
                hex = dark ? "#41e0c2" : "#0e8f8f";
                break;
            case "fail":
                hex = dark ? "#ff4d6d" : "#d1304a";
                break;
            case "warn":
                hex = dark ? "#ffb74d" : "#a6650a";
                break;
            case "debug":
                hex = dark ? "#b0a8cc" : "#6b6b76";
                break;
            case "info":
                hex = dark ? "#6fb3ff" : "#1f6fd6";
                break;
            default:
                hex = dark ? "#b487ff" : "#7724FF";
                break;
        }
        return Color.decode(hex);
    }

    private static Color blend(Color foreground, Color background, float foregroundWeight) {
        float w = Math.max(0f, Math.min(1f, foregroundWeight));
        return new Color(
            Math.round(foreground.getRed() * w + background.getRed() * (1 - w)),
            Math.round(foreground.getGreen() * w + background.getGreen() * (1 - w)),
            Math.round(foreground.getBlue() * w + background.getBlue() * (1 - w))
        );
    }
}
