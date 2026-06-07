package com.ing.ide.main.playwrightrecording;

import com.ing.datalib.api.importer.playwright.PlaywrightRecordingImporter;
import com.ing.ide.main.mainui.AppMainFrame;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IDE-side adapter for the Playwright recording importer. The actual parsing
 * and OR-creation logic lives in {@link PlaywrightRecordingImporter} (datalib)
 * so the CLI/MCP import paths use the exact same implementation.
 */
public class PlaywrightRecordingParser {

    private final AppMainFrame sMainFrame;

    public PlaywrightRecordingParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    public void playwrightParser(File file) {
        if (file == null || !file.exists()) return;
        try {
            PlaywrightRecordingImporter.importInto(
                    sMainFrame.getProject(), file, /*scenarioName*/ null, /*testCaseName*/ null);
        } catch (Exception ex) {
            Logger.getLogger(PlaywrightRecordingParser.class.getName())
                    .log(Level.SEVERE, "Playwright recording import failed", ex);
        }
    }
}
