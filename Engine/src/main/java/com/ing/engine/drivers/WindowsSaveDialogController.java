package com.ing.engine.drivers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Drives the native Windows "Save As" common dialog that SAP's PDF export opens outside the SAP
 * GUI Scripting object model - neither JACOB nor Playwright can address it (it's not a
 * {@code GuiComponent}, nor a browser page element). Mirrors the Win32-via-PowerShell technique
 * already used by the IDE's {@code InspectorWindowController} ({@code Add-Type} +
 * {@code [DllImport("user32.dll")]}): find the dialog by its window class ({@code #32770}, the
 * standard Windows common-dialog class) and poll for it with a timeout before touching anything,
 * instead of blindly sending keystrokes to whatever currently has focus. The PowerShell script
 * itself lives at {@code sap/sap-save-dialog.ps1} (a resource, not a Java text block - the
 * prettier-java parser used by this build can't reliably lex large embedded text blocks).
 */
public class WindowsSaveDialogController {
    private static final String SCRIPT_RESOURCE = "/sap/sap-save-dialog.ps1";

    /**
     * Waits up to {@code timeoutMillis} for the native Save dialog to appear, then types
     * {@code filePath} into it and confirms.
     * @return true if the dialog was found and confirmed, false if it never appeared
     */
    public boolean confirmSaveDialog(String filePath, long timeoutMillis)
        throws IOException, InterruptedException {
        File script = File.createTempFile("sap-save-dialog", ".ps1");
        try {
            try (InputStream in = getClass().getResourceAsStream(SCRIPT_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Missing resource " + SCRIPT_RESOURCE);
                }
                Files.copy(in, script.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.getAbsolutePath(),
                "-FilePath",
                filePath,
                "-TimeoutMs",
                String.valueOf(timeoutMillis)
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
            );
            int exitCode = process.waitFor();
            return exitCode == 0 && output.contains("OK");
        } finally {
            script.delete();
        }
    }
}
