package com.ing.ide.main.mainui.components.health;

import java.awt.BorderLayout;
import java.awt.Window;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javax.swing.JDialog;

/**
 * Modal overlay that renders a {@link ProjectHealthReport} as a modern HTML
 * dashboard inside an embedded JavaFX {@link WebView}.
 */
public class ProjectHealthDialog extends JDialog {

    private ProjectHealthDialog(Window owner, ProjectHealthReport report) {
        super(owner, "Project Health", ModalityType.APPLICATION_MODAL);
        JFXPanel fxPanel = new JFXPanel();
        setLayout(new BorderLayout());
        add(fxPanel, BorderLayout.CENTER);
        setSize(940, 760);
        setMinimumSize(new java.awt.Dimension(720, 560));
        setLocationRelativeTo(owner);

        final String html = buildHtml(report);
        Platform.runLater(
            () -> {
                WebView webView = new WebView();
                webView.setContextMenuEnabled(false);
                webView.getEngine().loadContent(html);
                fxPanel.setScene(new Scene(webView));
            }
        );
    }

    /** Builds and shows the dialog for the given report. */
    public static void showReport(Window owner, ProjectHealthReport report) {
        new ProjectHealthDialog(owner, report).setVisible(true);
    }

    // ------------------------------------------------------------------
    // HTML generation
    // ------------------------------------------------------------------

    private static String buildHtml(ProjectHealthReport r) {
        String accent = gradeColor(r.grade);
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
        sb.append("<style>");
        sb.append(
            "*{box-sizing:border-box;margin:0;padding:0;}" +
            "body{font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;" +
            "background:#f4f1fb;color:#1e1b2e;padding:0;}" +
            ".wrap{max-width:900px;margin:0 auto;padding:24px;}" +
            ".hero{background:linear-gradient(135deg,#7724ff 0%,#B487FF 100%);" +
            "border-radius:18px;padding:26px 30px;color:#fff;display:flex;" +
            "align-items:center;gap:28px;box-shadow:0 10px 30px rgba(119,36,255,.25);}" +
            ".hero h1{font-size:22px;font-weight:600;margin-bottom:4px;}" +
            ".hero .sub{opacity:.85;font-size:13px;}" +
            ".ring{flex:0 0 auto;}" +
            ".gradebadge{display:inline-block;margin-top:12px;padding:4px 14px;" +
            "border-radius:999px;background:rgba(255,255,255,.2);font-weight:600;" +
            "font-size:13px;letter-spacing:.5px;}" +
            ".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(120px,1fr));" +
            "gap:14px;margin-top:22px;}" +
            ".card{background:#fff;border-radius:14px;padding:16px 18px;" +
            "box-shadow:0 2px 10px rgba(30,27,46,.06);}" +
            ".card .num{font-size:26px;font-weight:700;color:#7724ff;}" +
            ".card .lbl{font-size:12px;color:#6b647e;margin-top:2px;text-transform:uppercase;" +
            "letter-spacing:.4px;}" +
            "section{margin-top:26px;}" +
            "section h2{font-size:15px;font-weight:600;margin-bottom:12px;color:#2c2740;}" +
            ".dim{margin-bottom:12px;}" +
            ".dim .top{display:flex;justify-content:space-between;font-size:13px;" +
            "margin-bottom:5px;color:#3a3450;}" +
            ".bar{height:9px;border-radius:6px;background:#e7e0f5;overflow:hidden;}" +
            ".bar > span{display:block;height:100%;border-radius:6px;}" +
            ".panel{background:#fff;border-radius:14px;padding:14px 18px;" +
            "box-shadow:0 2px 10px rgba(30,27,46,.06);}" +
            "table{width:100%;border-collapse:collapse;font-size:13px;}" +
            "th{text-align:left;padding:9px 10px;color:#6b647e;font-weight:600;" +
            "border-bottom:2px solid #eee7f9;font-size:11px;text-transform:uppercase;" +
            "letter-spacing:.4px;}" +
            "td{padding:9px 10px;border-bottom:1px solid #f1ecfa;}" +
            "tr:last-child td{border-bottom:none;}" +
            ".pill{display:inline-block;padding:2px 9px;border-radius:999px;font-size:11px;" +
            "font-weight:600;}" +
            ".ok{background:#e3f7ec;color:#1e874b;}" +
            ".warn{background:#fff4e0;color:#b5730b;}" +
            ".err{background:#fde8e8;color:#c0392b;}" +
            ".muted{color:#9a93ab;}" +
            "ul.issues{list-style:none;}" +
            "ul.issues li{padding:8px 12px;border-radius:9px;margin-bottom:7px;font-size:13px;}" +
            "ul.issues li.e{background:#fde8e8;color:#a5281b;}" +
            "ul.issues li.w{background:#fff4e0;color:#8a5a08;}" +
            ".empty{color:#1e874b;font-size:13px;padding:6px 2px;}"
        );
        sb.append("</style></head><body><div class='wrap'>");

        // Hero
        sb.append("<div class='hero'>");
        sb.append("<div class='ring'>").append(scoreRing(r.overallScore, accent)).append("</div>");
        sb.append("<div>");
        sb.append("<h1>").append(esc(nn(r.projectName, "Project"))).append(" &middot; Health</h1>");
        sb
            .append("<div class='sub'>")
            .append(r.scenarios)
            .append(" scenarios &nbsp;&bull;&nbsp; ")
            .append(r.testCases)
            .append(" test cases &nbsp;&bull;&nbsp; ")
            .append(r.totalSteps)
            .append(" steps</div>");
        sb
            .append("<span class='gradebadge'>Grade ")
            .append(esc(r.grade))
            .append(" &mdash; ")
            .append(r.overallScore)
            .append("/100</span>");
        sb.append("</div></div>");

        // Inventory cards
        sb.append("<div class='cards'>");
        card(sb, r.scenarios, "Scenarios");
        card(sb, r.testCases, "Test Cases");
        card(sb, r.reusableComponents, "Reusable Intents");
        card(sb, r.releases, "Releases");
        card(sb, r.testSets, "Test Sets");
        card(sb, r.taggedTestCases, "Tagged");
        sb.append("</div>");

        // Dimension scores
        sb.append("<section><h2>Quality dimensions</h2><div class='panel'>");
        dim(sb, "Structure", r.structureScore);
        dim(sb, "Modularity (reuse)", r.modularityScore);
        dim(sb, "Data parameterisation", r.dataScore);
        dim(sb, "Test-set coverage", r.testSetScore);
        dim(sb, "Tagging", r.tagScore);
        sb.append("</div></section>");

        // Issues
        sb.append("<section><h2>Issues</h2><div class='panel'>");
        if (r.errors.isEmpty() && r.warnings.isEmpty()) {
            sb.append("<div class='empty'>&#10003; No structural issues detected.</div>");
        } else {
            sb.append("<ul class='issues'>");
            for (String e : r.errors) {
                sb.append("<li class='e'>&#9888; ").append(esc(e)).append("</li>");
            }
            for (String w : r.warnings) {
                sb.append("<li class='w'>&#9679; ").append(esc(w)).append("</li>");
            }
            sb.append("</ul>");
        }
        sb.append("</div></section>");

        // Detail table
        sb.append("<section><h2>Test case quality</h2><div class='panel'>");
        if (r.rows.isEmpty()) {
            sb.append("<div class='muted' style='padding:8px 2px'>No test cases to show.</div>");
        } else {
            sb.append("<table><thead><tr>");
            sb.append(
                "<th>Scenario</th><th>Test Case</th><th>Kind</th><th>Steps</th>" +
                "<th>Reuse</th><th>Data</th><th>Tag</th></tr></thead><tbody>"
            );
            for (ProjectHealthReport.Row row : r.rows) {
                sb.append("<tr>");
                sb.append("<td class='muted'>").append(esc(row.scenario)).append("</td>");
                sb.append("<td>").append(esc(row.name)).append("</td>");
                sb.append("<td>").append(esc(row.kind)).append("</td>");
                sb.append("<td>").append(row.steps).append("</td>");
                sb.append("<td>").append(pct(row.reusablePct)).append("</td>");
                sb.append("<td>").append(pct(row.dataPct)).append("</td>");
                sb
                    .append("<td>")
                    .append(
                        row.tagged
                            ? "<span class='pill ok'>yes</span>"
                            : "<span class='pill warn'>no</span>"
                    )
                    .append("</td>");
                sb.append("</tr>");
            }
            sb.append("</tbody></table>");
        }
        sb.append("</div></section>");

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private static void card(StringBuilder sb, int num, String label) {
        sb
            .append("<div class='card'><div class='num'>")
            .append(num)
            .append("</div><div class='lbl'>")
            .append(esc(label))
            .append("</div></div>");
    }

    private static void dim(StringBuilder sb, String label, int value) {
        String color = scoreColor(value);
        sb.append("<div class='dim'><div class='top'><span>").append(esc(label));
        sb.append("</span><span>").append(value).append("</span></div>");
        sb
            .append("<div class='bar'><span style='width:")
            .append(Math.max(0, Math.min(100, value)))
            .append("%;background:")
            .append(color)
            .append(";'></span></div></div>");
    }

    private static String pct(int value) {
        String color = scoreColor(value);
        return "<span style='color:" + color + ";font-weight:600'>" + value + "%</span>";
    }

    private static String scoreRing(int score, String color) {
        int s = Math.max(0, Math.min(100, score));
        double circ = 2 * Math.PI * 42;
        double filled = circ * s / 100.0;
        StringBuilder sb = new StringBuilder();
        sb.append("<svg width='108' height='108' viewBox='0 0 108 108'>");
        sb.append(
            "<circle cx='54' cy='54' r='42' fill='none' stroke='rgba(255,255,255,.25)'" +
            " stroke-width='10'/>"
        );
        sb
            .append("<circle cx='54' cy='54' r='42' fill='none' stroke='#ffffff'")
            .append(" stroke-width='10' stroke-linecap='round'")
            .append(" stroke-dasharray='")
            .append(String.format(java.util.Locale.US, "%.1f", filled))
            .append(' ')
            .append(String.format(java.util.Locale.US, "%.1f", circ))
            .append("' transform='rotate(-90 54 54)'/>");
        sb
            .append("<text x='54' y='58' text-anchor='middle' fill='#fff'")
            .append(" font-size='26' font-weight='700'>")
            .append(s)
            .append("</text>");
        sb
            .append("<text x='54' y='74' text-anchor='middle' fill='rgba(255,255,255,.75)'")
            .append(" font-size='10'>SCORE</text>");
        sb.append("</svg>");
        return sb.toString();
    }

    private static String gradeColor(String grade) {
        if (grade == null) {
            return "#c0392b";
        }
        switch (grade) {
            case "A":
            case "B":
                return "#349651";
            case "C":
                return "#c9922b";
            case "D":
                return "#e07b1a";
            default:
                return "#c0392b";
        }
    }

    private static String scoreColor(int value) {
        if (value >= 80) {
            return "#349651";
        }
        if (value >= 60) {
            return "#c9922b";
        }
        return "#c0392b";
    }

    private static String nn(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
