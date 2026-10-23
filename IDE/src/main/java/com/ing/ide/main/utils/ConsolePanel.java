package com.ing.ide.main.utils;

import java.awt.BorderLayout;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ConsolePanel extends JPanel {
    private final ConsoleWebView webView;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        webView = new ConsoleWebView();
        add(webView.getComponent(), BorderLayout.CENTER);
    }

    /** Redirects System.out/System.err to the console for the duration of a run. */
    public void start() {
        clear();
        // Tells Engine (Control/SummaryReport) that output is being captured by this
        // HTML console, not a real terminal, so it should print its compact/tagged
        // pill-friendly forms instead of hand-padded box-drawing ASCII art - System.
        // console() alone isn't reliable here since the IDE itself may have been
        // launched from a terminal (e.g. during development).
        System.setProperty("ingenious.console.webview", "true");
        System.setOut(new PrintStream(new LineOutputStream(this::appendLine), true));
        System.setErr(new PrintStream(new LineOutputStream(this::appendErrorLine), true));
    }

    public void clear() {
        SwingUtilities.invokeLater(webView::clear);
    }

    public void appendLine(String text) {
        webView.appendLine(text);
    }

    public void appendErrorLine(String text) {
        webView.appendErrorLine(text);
    }

    /** Buffers written bytes and emits one {@code lineConsumer} call per completed line. */
    private static final class LineOutputStream extends OutputStream {
        private final Consumer<String> lineConsumer;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        LineOutputStream(Consumer<String> lineConsumer) {
            this.lineConsumer = lineConsumer;
        }

        @Override
        public synchronized void write(int b) {
            if (b == '\n') {
                flushLine();
            } else if (b != '\r') {
                buffer.write(b);
            }
        }

        private void flushLine() {
            String line = buffer.toString(StandardCharsets.UTF_8);
            buffer.reset();
            lineConsumer.accept(line);
        }
    }
}
