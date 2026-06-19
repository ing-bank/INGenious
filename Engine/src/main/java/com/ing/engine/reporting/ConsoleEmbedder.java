package com.ing.engine.reporting;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embeds the contents of {@code console.txt} directly into each generated HTML
 * report (summary, detailed, testcase). Doing this at report-finalization time
 * means the in-page Console Viewer no longer depends on browser file-access
 * policies (which block {@code fetch()} / {@code XMLHttpRequest} for local
 * {@code file://} pages in most modern browsers).
 *
 * <p>The HTML templates contain a marker element:</p>
 * <pre>{@code <script type="text/plain" id="ing-console-data">PLACEHOLDER</script>}</pre>
 * after the run completes its full text is rewritten with the actual
 * {@code console.txt} content (HTML-escaped, so it is safe inside a script
 * tag and can be retrieved client-side via {@code element.textContent}).
 */
public final class ConsoleEmbedder {
    private static final Logger LOG = Logger.getLogger(ConsoleEmbedder.class.getName());

    private static final String DATA_ID = "ing-console-data";

    private ConsoleEmbedder() {}

    /**
     * Embed the contents of {@code resultsDir/console.txt} into every
     * {@code *.html} file in {@code resultsDir}.
     */
    public static void embedInto(File resultsDir) {
        if (resultsDir == null || !resultsDir.isDirectory()) {
            return;
        }
        File consoleFile = new File(resultsDir, "console.txt");
        if (!consoleFile.isFile()) {
            return;
        }
        String consoleText;
        try {
            consoleText =
                new String(Files.readAllBytes(consoleFile.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Unable to read console.txt for embedding", ex);
            return;
        }
        String escaped = escapeForScriptBlock(consoleText);
        File[] htmlFiles = resultsDir.listFiles((d, name) -> name.toLowerCase().endsWith(".html"));
        if (htmlFiles == null) {
            return;
        }
        for (File html : htmlFiles) {
            try {
                embedInFile(html, escaped);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Failed to embed console.txt into " + html.getName(), ex);
            }
        }
    }

    private static void embedInFile(File html, String escapedContent) throws IOException {
        Path path = html.toPath();
        String original = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        String marker = "id=\"" + DATA_ID + "\"";
        int idx = original.indexOf(marker);
        if (idx < 0) {
            return; // template doesn't have the data island; nothing to do
        }
        int openEnd = original.indexOf('>', idx);
        if (openEnd < 0) {
            return;
        }
        int closeStart = original.indexOf("</script>", openEnd);
        if (closeStart < 0) {
            return;
        }
        StringBuilder sb = new StringBuilder(original.length() + escapedContent.length());
        sb.append(original, 0, openEnd + 1);
        sb.append(escapedContent);
        sb.append(original, closeStart, original.length());
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Inside a {@code <script>} element the parser treats the content as
     * CDATA-like text and does NOT decode HTML entities, but it does look for
     * the sequence {@code </script} (case-insensitive) to terminate the tag.
     * Break that sequence so the surrounding script element cannot be
     * accidentally closed by something the user printed to the console. The
     * client-side loader reverses the substitution before rendering.
     */
    private static String escapeForScriptBlock(String s) {
        // Case-insensitive replace of </script with <\/script
        StringBuilder out = new StringBuilder(s.length() + 16);
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '<' && i + 7 < n) {
                String window = s.substring(i, Math.min(i + 8, n));
                if (window.length() >= 8 && window.substring(1, 8).equalsIgnoreCase("/script")) {
                    out.append("<\\/script");
                    i += 8;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }
}
