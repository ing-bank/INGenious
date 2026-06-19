package com.ing.ide.main.playwrightrecording;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InspectorWindowController {
    private static final Logger LOGGER = Logger.getLogger(
        InspectorWindowController.class.getName()
    );

    private static final int MAX_ATTEMPTS = 15;
    private static final long RETRY_DELAY_MILLIS = 800L;

    private InspectorWindowController() {}

    public static void minimizeInspectorBestEffort() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                if (attemptMinimize(osName)) {
                    return;
                }
                Thread.sleep(RETRY_DELAY_MILLIS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Inspector minimize attempt failed", ex);
            }
        }
    }

    private static boolean attemptMinimize(String osName) throws IOException {
        if (osName.contains("mac")) {
            String output = runCommand(
                "osascript",
                "-e",
                "tell application \"System Events\"\n" +
                "set didMinimize to false\n" +
                "repeat with proc in (every process)\n" +
                "try\n" +
                "repeat with w in (every window of proc)\n" +
                "try\n" +
                "if (name of w contains \"Playwright\") then\n" +
                "set value of attribute \"AXMinimized\" of w to true\n" +
                "set didMinimize to true\n" +
                "end if\n" +
                "end try\n" +
                "end repeat\n" +
                "end try\n" +
                "end repeat\n" +
                "end tell\n" +
                "return didMinimize"
            );
            return "true".equalsIgnoreCase(output.trim());
        }

        if (osName.contains("win")) {
            String output = runCommand(
                "powershell",
                "-NoProfile",
                "-Command",
                "$sig='[DllImport(\"user32.dll\")] public static extern bool ShowWindowAsync(IntPtr hWnd, int nCmdShow);';" +
                "Add-Type -MemberDefinition $sig -Name Win32Show -Namespace Win32;" +
                "$found=$false;" +
                "Get-Process | Where-Object {$_.MainWindowTitle -like '*Playwright*'} | ForEach-Object {[Win32.Win32Show]::ShowWindowAsync($_.MainWindowHandle, 2) | Out-Null; $found=$true};" +
                "Write-Output $found"
            );
            return output.trim().toLowerCase(Locale.ROOT).contains("true");
        }

        if (osName.contains("linux")) {
            String output = runCommand(
                "bash",
                "-lc",
                "command -v wmctrl >/dev/null 2>&1 && " +
                "wmctrl -l | grep -i 'Playwright' | awk '{print $1}' | " +
                "while read id; do wmctrl -i -r \"$id\" -b add,hidden && echo minimized; done"
            );
            return output.toLowerCase(Locale.ROOT).contains("minimized");
        }

        return false;
    }

    private static String runCommand(String... command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        StringBuilder out = new StringBuilder();
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }
}
