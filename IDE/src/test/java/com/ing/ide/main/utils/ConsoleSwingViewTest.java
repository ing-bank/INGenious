package com.ing.ide.main.utils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.testng.annotations.Test;

public class ConsoleSwingViewTest {

    private static String textAfter(ConsoleSwingView view) throws Exception {
        AtomicReference<String> text = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> text.set(view.plainText()));
        return text.get();
    }

    @Test
    public void showsPlaceholderUntilFirstLine() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        assertTrue(textAfter(view).contains("Waiting for output"));
        view.appendLine("[PASS] Login worked");
        String text = textAfter(view);
        assertEquals(text.contains("Waiting for output"), false);
        assertTrue(text.contains("PASS"));
        assertTrue(text.contains("Login worked"));
    }

    @Test
    public void untaggedStderrLineGetsErrorLabel() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        view.appendErrorLine("something broke");
        String text = textAfter(view);
        assertTrue(text.contains("ERROR"));
        assertTrue(text.contains("something broke"));
    }

    @Test
    public void summaryLineIsRenderedAsSummaryNotRawJson() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        view.appendLine(
            "[SUMMARY]{\"status\":\"FAILED\",\"total\":5,\"passed\":4,\"failed\":1,\"duration\":\"0m 12s\"}"
        );
        String text = textAfter(view);
        assertTrue(text.contains("Execution Summary"));
        assertTrue(text.contains("FAILED"));
        assertTrue(text.contains("Total 5"));
        assertTrue(text.contains("Duration 0m 12s"));
        assertEquals(text.contains("{\"status\""), false);
    }

    @Test
    public void reportPathLineShowsPathWithOpenLink() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        view.appendLine("[REPORTPATH]/tmp/Results/Run1");
        String text = textAfter(view);
        assertTrue(text.contains("REPORT"));
        assertTrue(text.contains("/tmp/Results/Run1"));
        assertTrue(text.contains("Open"));
    }

    @Test
    public void emojiAndAnsiAreStrippedLikeTheWebConsole() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        view.appendLine("\u001B[32m[DONE]\u001B[0m finished \u2705");
        String text = textAfter(view);
        assertTrue(text.contains("DONE"));
        assertEquals(text.contains("\u001B"), false);
        assertEquals(text.contains("\u2705"), false);
    }

    @Test
    public void clearRestoresThePlaceholder() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        view.appendLine("[INFO] hello");
        view.clear();
        String text = textAfter(view);
        assertTrue(text.contains("Waiting for output"));
        assertEquals(text.contains("hello"), false);
    }

    @Test
    public void themeChangeKeepsExistingRows() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        view.appendLine("[WARNING] careful");
        view.appendLine("plain line");
        view.setDarkMode(true);
        String text = textAfter(view);
        assertTrue(text.contains("careful"));
        assertTrue(text.contains("plain line"));
    }

    @Test
    public void linesKeepTheirOrder() throws Exception {
        ConsoleSwingView view = new ConsoleSwingView(false);
        for (int i = 0; i < 50; i++) {
            view.appendLine("[INFO] line-" + i);
        }
        String text = textAfter(view);
        assertTrue(text.indexOf("line-0\n") < text.indexOf("line-1\n"));
        assertTrue(text.indexOf("line-9\n") < text.indexOf("line-49\n"));
    }
}
