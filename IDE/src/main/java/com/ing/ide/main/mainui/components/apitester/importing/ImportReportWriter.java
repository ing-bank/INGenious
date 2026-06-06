package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes a Markdown report describing a collection import.
 */
public final class ImportReportWriter {

    private ImportReportWriter() {}

    public static File write(String projectLocation, NormalizedCollection nc, ImportResult result) throws IOException {
        Path dir = Paths.get(projectLocation, "api", "import-reports");
        Files.createDirectories(dir);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve(ts + "-" + safe(nc.getName()) + ".md");

        StringBuilder sb = new StringBuilder();
        sb.append("# Import report — ").append(nc.getName()).append("\n\n");
        sb.append("- Source: ").append(nc.getSource()).append("\n");
        sb.append("- Timestamp: ").append(LocalDateTime.now()).append("\n");
        sb.append("- Requests read: ").append(result.getRequestsRead()).append("\n");
        sb.append("- Items created: ").append(result.getReusablesCreated()).append("\n");
        sb.append("- Items skipped: ").append(result.getReusablesSkipped()).append("\n");
        sb.append("- Environments created: ").append(result.getEnvironmentsCreated()).append("\n\n");

        sb.append("## Scenarios created\n");
        if (result.getCreatedScenarios().isEmpty()) {
            sb.append("_(none)_\n");
        } else {
            for (String s : result.getCreatedScenarios()) sb.append("- ").append(s).append("\n");
        }

        sb.append("\n## Items created\n");
        if (result.getCreatedReusables().isEmpty()) {
            sb.append("_(none)_\n");
        } else {
            for (String s : result.getCreatedReusables()) sb.append("- ").append(s).append("\n");
        }

        sb.append("\n## Warnings\n");
        if (result.getWarnings().isEmpty()) {
            sb.append("_(none)_\n");
        } else {
            for (ImportWarning w : result.getWarnings()) {
                sb.append("- **").append(w.getSeverity()).append("** ")
                        .append(w.getLocation()).append(" — ").append(w.getMessage()).append("\n");
            }
        }

        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    private static String safe(String s) {
        if (s == null) return "collection";
        return s.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}
