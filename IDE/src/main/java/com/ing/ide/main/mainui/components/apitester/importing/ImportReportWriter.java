package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.importer.ImportOptions;
import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.NormalizedEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes an HTML report describing a collection import with INGenious branding.
 */
public final class ImportReportWriter {

    private ImportReportWriter() {}

    /**
     * Writes an HTML import report to the project's api/import-reports directory.
     *
     * @param projectLocation the project root path
     * @param nc the normalized collection that was imported
     * @param result the import result with statistics and warnings
     * @param options the import options used
     * @param startTime when the import started
     * @param endTime when the import ended
     * @return the File pointing to the generated report
     */
    public static File write(
        String projectLocation,
        NormalizedCollection nc,
        ImportResult result,
        ImportOptions options,
        LocalDateTime startTime,
        LocalDateTime endTime
    )
        throws IOException {
        Path dir = Paths.get(projectLocation, "api", "import-reports");
        Files.createDirectories(dir);
        String ts = startTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path file = dir.resolve(ts + "-" + safe(nc.getName()) + ".html");

        Duration duration = Duration.between(startTime, endTime);
        String durationStr = String.format(
            "%d.%03d seconds",
            duration.getSeconds(),
            duration.toMillisPart()
        );

        String html = generateHtmlReport(nc, result, options, startTime, durationStr);
        Files.write(file, html.getBytes(StandardCharsets.UTF_8));

        // Also update the import history index
        updateHistoryIndex(dir, file.getFileName().toString(), nc.getName(), startTime, result);

        return file.toFile();
    }

    /**
     * Legacy method for backward compatibility.
     */
    public static File write(String projectLocation, NormalizedCollection nc, ImportResult result)
        throws IOException {
        LocalDateTime now = LocalDateTime.now();
        return write(projectLocation, nc, result, new ImportOptions(), now, now);
    }

    private static String generateHtmlReport(
        NormalizedCollection nc,
        ImportResult result,
        ImportOptions options,
        LocalDateTime timestamp,
        String duration
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append(
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
        );
        sb.append("    <title>Import Report — ").append(esc(nc.getName())).append("</title>\n");
        sb.append(getStyles());
        sb.append("</head>\n");
        sb.append("<body>\n");

        // Header
        sb.append("    <header>\n");
        sb.append("        <div class=\"logo\">INGenious</div>\n");
        sb.append("        <h1>Collection Import Report</h1>\n");
        sb.append("        <div class=\"subtitle\">").append(esc(nc.getName())).append("</div>\n");
        sb.append("    </header>\n");

        // Determine dynamic labels based on target type
        boolean isTestCase = options.getTargetType() == ImportOptions.TargetType.TEST_CASE;
        String assetTypeLabel = isTestCase ? "TestCases" : "Reusables";
        String assetTypeSingular = isTestCase ? "TestCase" : "Reusable";

        // Import Summary Section
        sb.append("    <section class=\"card\">\n");
        sb.append("        <h2><span class=\"icon\">📊</span> Import Summary</h2>\n");
        sb.append("        <div class=\"summary-grid\">\n");
        sb.append("            <div class=\"stat-box\">\n");
        sb
            .append("                <div class=\"stat-value\">")
            .append(result.getRequestsRead())
            .append("</div>\n");
        sb.append("                <div class=\"stat-label\">Requests Found</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-box success\">\n");
        sb
            .append("                <div class=\"stat-value\">")
            .append(result.getReusablesCreated())
            .append("</div>\n");
        sb
            .append("                <div class=\"stat-label\">")
            .append(assetTypeLabel)
            .append(" Created</div>\n");
        sb.append("            </div>\n");
        sb
            .append("            <div class=\"stat-box ")
            .append(result.getReusablesSkipped() > 0 ? "warning" : "")
            .append("\">\n");
        sb
            .append("                <div class=\"stat-value\">")
            .append(result.getReusablesSkipped())
            .append("</div>\n");
        sb
            .append("                <div class=\"stat-label\">")
            .append(assetTypeLabel)
            .append(" Skipped</div>\n");
        sb.append("            </div>\n");
        sb.append("            <div class=\"stat-box\">\n");
        sb.append("                <div class=\"stat-value\">").append(duration).append("</div>\n");
        sb.append("                <div class=\"stat-label\">Duration</div>\n");
        sb.append("            </div>\n");
        sb.append("        </div>\n");
        sb.append("        <table class=\"details-table\">\n");
        sb
            .append("            <tr><td>Source Format</td><td>")
            .append(esc(nc.getSource() != null ? nc.getSource().name() : "Unknown"))
            .append("</td></tr>\n");
        sb
            .append("            <tr><td>Timestamp</td><td>")
            .append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</td></tr>\n");
        sb
            .append("            <tr><td>Import As</td><td><strong>")
            .append(assetTypeLabel)
            .append("</strong></td></tr>\n");
        sb
            .append("            <tr><td>Naming Convention</td><td>")
            .append(options.getNamingConvention())
            .append("</td></tr>\n");
        sb
            .append("            <tr><td>Hierarchy Strategy</td><td>")
            .append(options.getHierarchyStrategy())
            .append("</td></tr>\n");
        sb
            .append("            <tr><td>Conflict Policy</td><td>")
            .append(options.getConflictPolicy())
            .append("</td></tr>\n");
        sb.append("        </table>\n");
        sb.append("    </section>\n");

        // Collection Summary Section
        sb.append("    <section class=\"card\">\n");
        sb.append("        <h2><span class=\"icon\">📁</span> Collection Summary</h2>\n");
        if (!result.getCreatedScenarios().isEmpty()) {
            sb
                .append("        <h3>Scenarios Created (")
                .append(result.getCreatedScenarios().size())
                .append(")</h3>\n");
            sb.append("        <ul class=\"item-list\">\n");
            for (String scenario : result.getCreatedScenarios()) {
                sb
                    .append("            <li class=\"success\">")
                    .append(esc(scenario))
                    .append("</li>\n");
            }
            sb.append("        </ul>\n");
        }
        if (!result.getCreatedReusables().isEmpty()) {
            sb
                .append("        <h3>")
                .append(assetTypeLabel)
                .append(" Created (")
                .append(result.getCreatedReusables().size())
                .append(")</h3>\n");
            // Render as a table with Scenario and TestCase/Reusable columns
            sb.append("        <table class=\"asset-mapping-table\">\n");
            sb.append("            <thead>\n");
            sb.append("                <tr>\n");
            sb.append("                    <th>Scenario</th>\n");
            sb.append("                    <th>").append(assetTypeSingular).append("</th>\n");
            sb.append("                </tr>\n");
            sb.append("            </thead>\n");
            sb.append("            <tbody>\n");
            for (String item : result.getCreatedReusables()) {
                // Items are in format "ScenarioName / TestCaseName"
                String scenario = "";
                String testCase = item;
                int separatorIdx = item.indexOf(" / ");
                if (separatorIdx > 0) {
                    scenario = item.substring(0, separatorIdx);
                    testCase = item.substring(separatorIdx + 3);
                }
                sb.append("                <tr>\n");
                sb.append("                    <td>").append(esc(scenario)).append("</td>\n");
                sb.append("                    <td>").append(esc(testCase)).append("</td>\n");
                sb.append("                </tr>\n");
            }
            sb.append("            </tbody>\n");
            sb.append("        </table>\n");
        }
        sb.append("    </section>\n");

        // Environment Summary Section
        if (options.isImportEnvironments()) {
            sb.append("    <section class=\"card\">\n");
            sb.append("        <h2><span class=\"icon\">🌍</span> Environment Summary</h2>\n");
            sb.append("        <div class=\"summary-grid\">\n");
            sb.append("            <div class=\"stat-box\">\n");
            sb
                .append("                <div class=\"stat-value\">")
                .append(result.getDataEnvironmentsCreated())
                .append("</div>\n");
            sb.append("                <div class=\"stat-label\">Environments Created</div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"stat-box\">\n");
            sb
                .append("                <div class=\"stat-value\">")
                .append(nc.getEnvironments().size())
                .append("</div>\n");
            sb.append("                <div class=\"stat-label\">Environments Processed</div>\n");
            sb.append("            </div>\n");
            sb.append("        </div>\n");
            if (!nc.getEnvironments().isEmpty()) {
                sb.append("        <h3>Environments</h3>\n");
                sb.append("        <ul class=\"item-list\">\n");
                for (NormalizedEnvironment env : nc.getEnvironments()) {
                    sb
                        .append("            <li><strong>")
                        .append(esc(env.getName()))
                        .append("</strong> — ");
                    sb.append(env.getVariables().size()).append(" variables</li>\n");
                }
                sb.append("        </ul>\n");
            }
            if (!result.getCreatedDataEnvironments().isEmpty()) {
                sb.append("        <h3>Data Environments Created</h3>\n");
                sb.append("        <ul class=\"item-list\">\n");
                for (String envName : result.getCreatedDataEnvironments()) {
                    sb
                        .append("            <li class=\"success\">")
                        .append(esc(envName))
                        .append("</li>\n");
                }
                sb.append("        </ul>\n");
            }
            sb.append("    </section>\n");
        }

        // Datasheet Summary Section
        if (result.getDatasheetsCreated() > 0 || result.getDatasheetColumnsCreated() > 0) {
            sb.append("    <section class=\"card\">\n");
            sb.append("        <h2><span class=\"icon\">📋</span> Datasheet Summary</h2>\n");
            sb.append("        <div class=\"summary-grid\">\n");
            sb.append("            <div class=\"stat-box\">\n");
            sb
                .append("                <div class=\"stat-value\">")
                .append(result.getDatasheetsCreated())
                .append("</div>\n");
            sb.append("                <div class=\"stat-label\">Datasheets Created</div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"stat-box\">\n");
            sb
                .append("                <div class=\"stat-value\">")
                .append(result.getDatasheetColumnsCreated())
                .append("</div>\n");
            sb.append("                <div class=\"stat-label\">Columns Created</div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"stat-box\">\n");
            sb
                .append("                <div class=\"stat-value\">")
                .append(result.getDatasheetRowsCreated())
                .append("</div>\n");
            sb.append("                <div class=\"stat-label\">Rows Created</div>\n");
            sb.append("            </div>\n");
            sb.append("        </div>\n");
            if (result.getDatasheetName() != null) {
                sb.append("        <table class=\"details-table\">\n");
                sb
                    .append("            <tr><td>Datasheet Name</td><td><code>")
                    .append(esc(result.getDatasheetName()))
                    .append("</code></td></tr>\n");
                sb.append("        </table>\n");
            }
            sb.append("    </section>\n");
        }

        // Parameterization Summary Section
        if (options.isImportEnvironments() && result.getDatasheetColumnsCreated() > 0) {
            sb.append("    <section class=\"card\">\n");
            sb.append("        <h2><span class=\"icon\">🔗</span> Parameterization Summary</h2>\n");
            sb.append(
                "        <p>Variable references in the collection have been converted to INGenious datasheet syntax:</p>\n"
            );
            sb.append("        <div class=\"conversion-example\">\n");
            sb.append(
                "            <div class=\"before\"><code>{{variable}}</code> or <code>%variable%</code></div>\n"
            );
            sb.append("            <div class=\"arrow\">→</div>\n");
            sb
                .append("            <div class=\"after\"><code>{")
                .append(esc(result.getDatasheetName()))
                .append(":variable}</code></div>\n");
            sb.append("        </div>\n");
            sb.append(
                "        <p class=\"info-note\">Test data is now driven from the datasheet. Each data environment folder contains the corresponding values.</p>\n"
            );
            sb.append("    </section>\n");
        }

        // Warnings Section
        sb.append("    <section class=\"card\">\n");
        sb.append("        <h2><span class=\"icon\">⚠️</span> Warnings & Errors</h2>\n");
        if (result.getWarnings().isEmpty()) {
            sb.append(
                "        <p class=\"success-message\">No warnings or errors occurred during import.</p>\n"
            );
        } else {
            sb.append("        <ul class=\"warning-list\">\n");
            for (ImportWarning w : result.getWarnings()) {
                String cssClass = w.getSeverity().name().toLowerCase();
                sb.append("            <li class=\"").append(cssClass).append("\">\n");
                sb
                    .append("                <span class=\"severity\">")
                    .append(w.getSeverity())
                    .append("</span>\n");
                sb
                    .append("                <span class=\"location\">")
                    .append(esc(w.getLocation()))
                    .append("</span>\n");
                sb
                    .append("                <span class=\"message\">")
                    .append(esc(w.getMessage()))
                    .append("</span>\n");
                sb.append("            </li>\n");
            }
            sb.append("        </ul>\n");
        }
        sb.append("    </section>\n");

        // Footer
        sb.append("    <footer>\n");
        sb.append("        <p>Generated by INGenious Playwright Studio</p>\n");
        sb
            .append("        <p class=\"timestamp\">")
            .append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</p>\n");
        sb.append("    </footer>\n");

        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }

    private static String getStyles() {
        return (
            "    <style>\n" +
            "        :root {\n" +
            "            --primary: #7724FF;\n" +
            "            --primary-light: #9B5CFF;\n" +
            "            --primary-dark: #5A1ACC;\n" +
            "            --success: #28A745;\n" +
            "            --warning: #FFC107;\n" +
            "            --error: #DC3545;\n" +
            "            --info: #17A2B8;\n" +
            "            --bg: #F8F9FA;\n" +
            "            --card-bg: #FFFFFF;\n" +
            "            --text: #212529;\n" +
            "            --text-muted: #6C757D;\n" +
            "            --border: #DEE2E6;\n" +
            "        }\n" +
            "        * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "        body {\n" +
            "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;\n" +
            "            background: var(--bg);\n" +
            "            color: var(--text);\n" +
            "            line-height: 1.6;\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "        header {\n" +
            "            background: linear-gradient(135deg, var(--primary), var(--primary-dark));\n" +
            "            color: white;\n" +
            "            padding: 30px;\n" +
            "            border-radius: 12px;\n" +
            "            margin-bottom: 24px;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        header .logo {\n" +
            "            font-size: 14px;\n" +
            "            font-weight: 600;\n" +
            "            text-transform: uppercase;\n" +
            "            letter-spacing: 2px;\n" +
            "            opacity: 0.9;\n" +
            "            margin-bottom: 8px;\n" +
            "        }\n" +
            "        header h1 {\n" +
            "            font-size: 28px;\n" +
            "            font-weight: 700;\n" +
            "            margin-bottom: 8px;\n" +
            "        }\n" +
            "        header .subtitle {\n" +
            "            font-size: 18px;\n" +
            "            opacity: 0.9;\n" +
            "        }\n" +
            "        .card {\n" +
            "            background: var(--card-bg);\n" +
            "            border-radius: 12px;\n" +
            "            padding: 24px;\n" +
            "            margin-bottom: 20px;\n" +
            "            box-shadow: 0 2px 8px rgba(0,0,0,0.08);\n" +
            "        }\n" +
            "        .card h2 {\n" +
            "            color: var(--primary);\n" +
            "            font-size: 20px;\n" +
            "            margin-bottom: 16px;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 8px;\n" +
            "        }\n" +
            "        .card h3 {\n" +
            "            color: var(--text);\n" +
            "            font-size: 16px;\n" +
            "            margin: 16px 0 8px 0;\n" +
            "        }\n" +
            "        .icon { font-size: 24px; }\n" +
            "        .summary-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));\n" +
            "            gap: 16px;\n" +
            "            margin-bottom: 16px;\n" +
            "        }\n" +
            "        .stat-box {\n" +
            "            background: var(--bg);\n" +
            "            border-radius: 8px;\n" +
            "            padding: 16px;\n" +
            "            text-align: center;\n" +
            "            border: 1px solid var(--border);\n" +
            "        }\n" +
            "        .stat-box.success { border-color: var(--success); background: rgba(40,167,69,0.1); }\n" +
            "        .stat-box.warning { border-color: var(--warning); background: rgba(255,193,7,0.1); }\n" +
            "        .stat-box.error { border-color: var(--error); background: rgba(220,53,69,0.1); }\n" +
            "        .stat-value {\n" +
            "            font-size: 28px;\n" +
            "            font-weight: 700;\n" +
            "            color: var(--primary);\n" +
            "        }\n" +
            "        .stat-box.success .stat-value { color: var(--success); }\n" +
            "        .stat-box.warning .stat-value { color: var(--warning); }\n" +
            "        .stat-box.error .stat-value { color: var(--error); }\n" +
            "        .stat-label {\n" +
            "            font-size: 13px;\n" +
            "            color: var(--text-muted);\n" +
            "            margin-top: 4px;\n" +
            "        }\n" +
            "        .details-table {\n" +
            "            width: 100%;\n" +
            "            border-collapse: collapse;\n" +
            "        }\n" +
            "        .details-table td {\n" +
            "            padding: 8px 12px;\n" +
            "            border-bottom: 1px solid var(--border);\n" +
            "        }\n" +
            "        .details-table td:first-child {\n" +
            "            font-weight: 600;\n" +
            "            width: 180px;\n" +
            "            color: var(--text-muted);\n" +
            "        }\n" +
            "        .item-list {\n" +
            "            list-style: none;\n" +
            "            padding: 0;\n" +
            "        }\n" +
            "        .item-list li {\n" +
            "            padding: 8px 12px;\n" +
            "            background: var(--bg);\n" +
            "            margin-bottom: 4px;\n" +
            "            border-radius: 6px;\n" +
            "            font-size: 14px;\n" +
            "        }\n" +
            "        .item-list li.success { border-left: 3px solid var(--success); }\n" +
            "        .item-list.scrollable {\n" +
            "            max-height: 300px;\n" +
            "            overflow-y: auto;\n" +
            "        }\n" +
            "        .warning-list {\n" +
            "            list-style: none;\n" +
            "            padding: 0;\n" +
            "        }\n" +
            "        .warning-list li {\n" +
            "            padding: 12px;\n" +
            "            margin-bottom: 8px;\n" +
            "            border-radius: 6px;\n" +
            "            display: flex;\n" +
            "            flex-wrap: wrap;\n" +
            "            gap: 8px;\n" +
            "            align-items: center;\n" +
            "        }\n" +
            "        .warning-list li.warn { background: rgba(255,193,7,0.15); border-left: 3px solid var(--warning); }\n" +
            "        .warning-list li.error { background: rgba(220,53,69,0.15); border-left: 3px solid var(--error); }\n" +
            "        .warning-list li.info { background: rgba(23,162,184,0.15); border-left: 3px solid var(--info); }\n" +
            "        .warning-list .severity {\n" +
            "            font-weight: 700;\n" +
            "            font-size: 12px;\n" +
            "            text-transform: uppercase;\n" +
            "            padding: 2px 8px;\n" +
            "            border-radius: 4px;\n" +
            "            background: rgba(0,0,0,0.1);\n" +
            "        }\n" +
            "        .warning-list .location {\n" +
            "            font-family: monospace;\n" +
            "            color: var(--text-muted);\n" +
            "            font-size: 13px;\n" +
            "        }\n" +
            "        .warning-list .message {\n" +
            "            flex-basis: 100%;\n" +
            "            font-size: 14px;\n" +
            "        }\n" +
            "        .success-message {\n" +
            "            color: var(--success);\n" +
            "            font-weight: 500;\n" +
            "            padding: 16px;\n" +
            "            background: rgba(40,167,69,0.1);\n" +
            "            border-radius: 8px;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        .conversion-example {\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            gap: 16px;\n" +
            "            padding: 20px;\n" +
            "            background: var(--bg);\n" +
            "            border-radius: 8px;\n" +
            "            margin: 16px 0;\n" +
            "        }\n" +
            "        .conversion-example code {\n" +
            "            background: rgba(119,36,255,0.1);\n" +
            "            padding: 8px 16px;\n" +
            "            border-radius: 6px;\n" +
            "            font-size: 14px;\n" +
            "            color: var(--primary);\n" +
            "        }\n" +
            "        .conversion-example .arrow {\n" +
            "            font-size: 24px;\n" +
            "            color: var(--primary);\n" +
            "        }\n" +
            "        .info-note {\n" +
            "            color: var(--text-muted);\n" +
            "            font-size: 14px;\n" +
            "            font-style: italic;\n" +
            "        }\n" +
            "        code {\n" +
            "            background: var(--bg);\n" +
            "            padding: 2px 6px;\n" +
            "            border-radius: 4px;\n" +
            "            font-family: 'Consolas', 'Monaco', monospace;\n" +
            "        }\n" +
            "        footer {\n" +
            "            text-align: center;\n" +
            "            padding: 20px;\n" +
            "            color: var(--text-muted);\n" +
            "            font-size: 13px;\n" +
            "        }\n" +
            "        footer .timestamp {\n" +
            "            font-family: monospace;\n" +
            "            margin-top: 4px;\n" +
            "        }\n" +
            "        .asset-mapping-table {\n" +
            "            width: 100%;\n" +
            "            border-collapse: collapse;\n" +
            "            margin-top: 12px;\n" +
            "            font-size: 14px;\n" +
            "        }\n" +
            "        .asset-mapping-table thead {\n" +
            "            background: var(--primary);\n" +
            "            color: white;\n" +
            "        }\n" +
            "        .asset-mapping-table th {\n" +
            "            padding: 12px 16px;\n" +
            "            text-align: left;\n" +
            "            font-weight: 600;\n" +
            "        }\n" +
            "        .asset-mapping-table tbody tr {\n" +
            "            border-bottom: 1px solid var(--border);\n" +
            "        }\n" +
            "        .asset-mapping-table tbody tr:hover {\n" +
            "            background: rgba(119,36,255,0.05);\n" +
            "        }\n" +
            "        .asset-mapping-table td {\n" +
            "            padding: 10px 16px;\n" +
            "        }\n" +
            "        .asset-mapping-table td:first-child {\n" +
            "            color: var(--text-muted);\n" +
            "            font-weight: 500;\n" +
            "        }\n" +
            "        .asset-mapping-table td:last-child {\n" +
            "            color: var(--primary);\n" +
            "        }\n" +
            "    </style>\n"
        );
    }

    /**
     * Updates the import history index file (HTML) with the new import entry.
     */
    private static void updateHistoryIndex(
        Path reportsDir,
        String reportFileName,
        String collectionName,
        LocalDateTime timestamp,
        ImportResult result
    )
        throws IOException {
        Path indexFile = reportsDir.resolve("index.html");

        String entry = String.format(
            "            <tr>\n" +
            "                <td><a href=\"%s\">%s</a></td>\n" +
            "                <td>%s</td>\n" +
            "                <td>%d</td>\n" +
            "                <td>%d</td>\n" +
            "                <td class=\"%s\">%d</td>\n" +
            "            </tr>\n",
            reportFileName,
            timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            esc(collectionName),
            result.getReusablesCreated(),
            result.getDataEnvironmentsCreated(),
            result.getWarnings().isEmpty() ? "success" : "warning",
            result.getWarnings().size()
        );

        if (Files.exists(indexFile)) {
            // Append new entry to existing index
            String content = new String(Files.readAllBytes(indexFile), StandardCharsets.UTF_8);
            int insertPos = content.indexOf("<!-- INSERT_NEW_ROWS_HERE -->");
            if (insertPos > 0) {
                content = content.substring(0, insertPos) + entry + content.substring(insertPos);
                Files.write(indexFile, content.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            // Create new index file
            String indexHtml = generateHistoryIndexHtml(entry);
            Files.write(indexFile, indexHtml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String generateHistoryIndexHtml(String firstEntry) {
        return (
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Import History</title>\n" +
            "    <style>\n" +
            "        :root { --primary: #7724FF; --success: #28A745; --warning: #FFC107; }\n" +
            "        * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #F8F9FA; padding: 20px; }\n" +
            "        header { background: linear-gradient(135deg, var(--primary), #5A1ACC); color: white; padding: 24px; border-radius: 12px; margin-bottom: 24px; text-align: center; }\n" +
            "        header .logo { font-size: 12px; text-transform: uppercase; letter-spacing: 2px; opacity: 0.9; }\n" +
            "        header h1 { font-size: 24px; margin-top: 8px; }\n" +
            "        table { width: 100%; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-collapse: collapse; }\n" +
            "        th { background: var(--primary); color: white; padding: 14px; text-align: left; font-weight: 600; }\n" +
            "        td { padding: 12px 14px; border-bottom: 1px solid #DEE2E6; }\n" +
            "        tr:hover { background: #F8F9FA; }\n" +
            "        a { color: var(--primary); text-decoration: none; font-weight: 500; }\n" +
            "        a:hover { text-decoration: underline; }\n" +
            "        .success { color: var(--success); font-weight: 600; }\n" +
            "        .warning { color: var(--warning); font-weight: 600; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <header>\n" +
            "        <h1>Import History</h1>\n" +
            "    </header>\n" +
            "    <table>\n" +
            "        <thead>\n" +
            "            <tr>\n" +
            "                <th>Timestamp</th>\n" +
            "                <th>Collection</th>\n" +
            "                <th>Items Created</th>\n" +
            "                <th>Environments</th>\n" +
            "                <th>Warnings</th>\n" +
            "            </tr>\n" +
            "        </thead>\n" +
            "        <tbody>\n" +
            "<!-- INSERT_NEW_ROWS_HERE -->" +
            firstEntry +
            "        </tbody>\n" +
            "    </table>\n" +
            "</body>\n" +
            "</html>\n"
        );
    }

    private static String safe(String s) {
        if (s == null) return "collection";
        return s.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
