package com.ing.engine.perf;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records browser traffic to a HAR file using Playwright's context-level
 * capture ({@code setRecordHarPath}) — the same mechanism the engine uses for
 * performance logging, but on demand and proxy-free (no MITM certificates).
 *
 * <p>Flow: {@link #start} opens a browser at the URL and records everything;
 * {@link Session#stop()} closes the context, which flushes the HAR. The
 * resulting file feeds {@code perf export <file>.har} (HTTP script + rules).
 */
public final class PerfRecorder {

    /** A live recording; keep it and call {@link #stop()} when done. */
    public static final class Session {
        public final String id;
        public final String url;
        public final File harFile;
        public final long startedAt;
        private final Playwright playwright;
        private final Browser browser;
        private final BrowserContext context;
        private boolean stopped;

        private Session(
            String id,
            String url,
            File harFile,
            Playwright playwright,
            Browser browser,
            BrowserContext context
        ) {
            this.id = id;
            this.url = url;
            this.harFile = harFile;
            this.startedAt = System.currentTimeMillis();
            this.playwright = playwright;
            this.browser = browser;
            this.context = context;
        }

        /** True while the browser process is still alive. */
        public boolean isAlive() {
            try {
                return !stopped && browser.isConnected();
            } catch (Exception e) {
                return false;
            }
        }

        /** Close context (flushes the HAR), browser and driver. Idempotent. */
        public synchronized File stop() {
            if (stopped) {
                return harFile;
            }
            stopped = true;
            try {
                context.close();
            } catch (Exception ignored) {}
            try {
                browser.close();
            } catch (Exception ignored) {}
            try {
                playwright.close();
            } catch (Exception ignored) {}
            return harFile;
        }
    }

    private PerfRecorder() {}

    /**
     * Launch chromium at {@code url} with HAR capture on.
     *
     * @param harFile  target .har file (parent dirs are created)
     * @param headless true for unattended capture, false to interact manually
     */
    public static Session start(String url, File harFile, boolean headless) {
        if (harFile.getParentFile() != null) {
            harFile.getParentFile().mkdirs();
        }
        Playwright playwright = Playwright.create();
        Browser browser = null;
        try {
            browser =
                playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setRecordHarPath(harFile.toPath())
            );
            Page page = context.newPage();
            page.navigate(url);
            String id = "rec_" + Long.toHexString(System.nanoTime());
            return new Session(id, url, harFile, playwright, browser, context);
        } catch (RuntimeException e) {
            if (browser != null) {
                try {
                    browser.close();
                } catch (Exception ignored) {}
            }
            try {
                playwright.close();
            } catch (Exception ignored) {}
            throw e;
        }
    }

    /** Default recording file name: {@code <host>_<timestamp>.har}. */
    public static String defaultName(String url) {
        String host;
        try {
            host = java.net.URI.create(url).getHost();
        } catch (Exception e) {
            host = null;
        }
        if (host == null || host.isEmpty()) {
            host = "recording";
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return host.replaceAll("[^A-Za-z0-9.-]", "_") + "_" + stamp + ".har";
    }
}
