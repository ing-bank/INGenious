package com.ing.engine.aicli.ui;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts a raw tool-result {@link JsonNode} into a human-friendly terminal
 * representation: rows of flat objects become tables; scalar fields become a
 * key/value info box; everything is laid out with box-drawing characters and
 * honours the active {@link Theme}.
 *
 * <p>Supports the JSON shapes that every INGenious tool actually returns:
 * <ul>
 *   <li>{@code {"items": [ {...}, ... ]}} — top-level array under an "items"
 *       / "tools" / "scenarios" / "testcases" / "objects" / "results" key.</li>
 *   <li>A bare top-level array.</li>
 *   <li>A flat object with scalar fields — info / create / validate results.</li>
 *   <li>A step array under the "steps" key.</li>
 *   <li>A "content" MCP-envelope — unwrapped transparently.</li>
 * </ul>
 */
public final class ResultRenderer {
    /** Working directory used to render absolute paths as relative. */
    private static final String CWD = System.getProperty("user.dir");

    /** Max visible width of any single cell before truncation. */
    private static final int MAX_CELL = 55;
    /** Max columns shown in a table before we drop the last ones. */
    private static final int MAX_COLS = 7;
    /** Max rows rendered in a table before we add a "…N more" footer. */
    private static final int MAX_ROWS = 80;

    private final Theme t;
    private final Panels panels;

    public ResultRenderer(Theme theme) {
        this.t = theme;
        this.panels = new Panels(theme);
    }

    // ------------------------------------------------------------------
    // public entry point
    // ------------------------------------------------------------------

    /** Render {@code result} to stdout; never throws. */
    public void print(String toolId, JsonNode result) {
        if (result == null) return;
        try {
            String rendered = render(toolId, result);
            System.out.println(rendered);
        } catch (Exception e) {
            // fallback to raw JSON so we never swallow data
            System.out.println(result.toPrettyString());
        }
    }

    // ------------------------------------------------------------------
    // renderer
    // ------------------------------------------------------------------

    private String render(String toolId, JsonNode node) {
        // 1. Unwrap MCP content envelope
        if (node.isObject() && node.has("content")) {
            JsonNode inner = node.get("structuredContent");
            if (inner == null) inner = node.path("content").path(0).path("text");
            if (inner != null && !inner.isMissingNode() && !inner.isNull()) {
                node = inner.isTextual() ? parseIfJson(inner.asText()) : inner;
            }
        }

        // 2. Try to find an array key — but only unwrap straight to a table when
        // that array is the object's sole meaningful payload. Objects with
        // several structured fields alongside an array (e.g. doctor's
        // jdk/playwrightCli/drivers/k6/project) must go through the info box so
        // none of those other fields get silently dropped.
        JsonNode array = findArray(node);
        if (array != null && array.size() > 0 && countComplexFields(node) <= 1) {
            return renderArray(toolId, array);
        }

        // 3. Flat object (info / create / validate results)
        if (node.isObject()) {
            return renderInfoBox(toolId, node);
        }

        // 4. Scalar
        return t.dim(node.asText());
    }

    /** Counts top-level fields that are objects or non-empty arrays. */
    private static int countComplexFields(JsonNode node) {
        if (!node.isObject()) return 0;
        int count = 0;
        Iterator<JsonNode> it = node.elements();
        while (it.hasNext()) {
            JsonNode v = it.next();
            if (v.isObject() || (v.isArray() && v.size() > 0)) count++;
        }
        return count;
    }

    // ------------------------------------------------------------------
    // table rendering
    // ------------------------------------------------------------------

    private String renderArray(String toolId, JsonNode array) {
        // Collect column names from the first MAX_COLS unique fields
        Set<String> colSet = new LinkedHashSet<>();
        for (JsonNode row : array) {
            if (!row.isObject()) continue;
            row.fieldNames().forEachRemaining(colSet::add);
            if (colSet.size() >= MAX_COLS) break;
        }
        if (colSet.isEmpty()) {
            // array of scalars
            return renderScalarArray(toolId, array);
        }
        List<String> cols = new ArrayList<>(colSet);
        if (cols.size() > MAX_COLS) cols = cols.subList(0, MAX_COLS);

        // Compute column widths
        int[] widths = new int[cols.size()];
        for (int c = 0; c < cols.size(); c++) {
            widths[c] = cols.get(c).length();
        }
        List<String[]> rows = new ArrayList<>();
        int shown = 0;
        for (JsonNode row : array) {
            if (shown >= MAX_ROWS) break;
            if (!row.isObject()) continue;
            String[] cells = new String[cols.size()];
            for (int c = 0; c < cols.size(); c++) {
                String val = cellText(row.path(cols.get(c)));
                cells[c] = val;
                widths[c] = Math.max(widths[c], Theme.visibleLength(val));
            }
            rows.add(cells);
            shown++;
        }
        int total = 0;
        for (JsonNode ignored : array) total++;
        int more = total - shown;

        StringBuilder sb = new StringBuilder();
        String title = titleFor(toolId, total);
        sb.append(tableTop(cols, widths, title));
        sb.append(tableHeader(cols, widths));
        sb.append(tableDivider(widths, false));
        for (int i = 0; i < rows.size(); i++) {
            sb.append(tableRow(rows.get(i), widths));
            if (i < rows.size() - 1) {
                sb.append(tableSpacerRow(widths));
            }
        }
        sb.append(tableBottom(widths));
        if (more > 0) {
            sb.append(t.dim("  … " + more + " more rows"));
        }
        return sb.toString().stripTrailing();
    }

    private String renderScalarArray(String toolId, JsonNode array) {
        List<String> lines = new ArrayList<>();
        int i = 0;
        for (JsonNode n : array) {
            if (i >= MAX_ROWS) break;
            if (i > 0) lines.add("");
            lines.add(t.purple("\u2022") + " " + cellText(n));
            i++;
        }
        int total = 0;
        for (JsonNode ignored : array) total++;
        if (total > MAX_ROWS) lines.add(t.dim("  … " + (total - MAX_ROWS) + " more"));
        return panels.box(titleFor(toolId, total), lines);
    }

    // ------------------------------------------------------------------
    // info box (flat object)
    // ------------------------------------------------------------------

    private String renderInfoBox(String toolId, JsonNode node) {
        List<String> lines = new ArrayList<>();
        int keyWidth = 0;
        // flatten one level: skip nested objects/arrays in first pass
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            entries.add(e);
            keyWidth = Math.max(keyWidth, e.getKey().length());
        }
        for (Map.Entry<String, JsonNode> e : entries) {
            String key = e.getKey();
            JsonNode val = e.getValue();
            if (!lines.isEmpty()) lines.add("");
            if (val.isObject() && val.size() > 0) {
                // nested object — flatten one level so its fields are actually visible
                appendNestedObject(lines, key, val);
            } else if (val.isArray() && val.size() > 0 && val.get(0).isObject()) {
                // array of objects (e.g. driver/present pairs) — one bullet row per item
                lines.add(t.bold(key) + t.dim("  (" + val.size() + ")"));
                for (JsonNode item : val) {
                    lines.add("    " + bulletForObject(item));
                }
            } else if (val.isArray()) {
                // array of scalars: inline bullet list
                List<String> scalars = new ArrayList<>();
                for (JsonNode s : val) scalars.add(s.asText());
                if (scalars.isEmpty()) {
                    lines.add(kv(key, t.dim("(empty)"), keyWidth));
                } else if (scalars.size() == 1) {
                    lines.add(kv(key, scalars.get(0), keyWidth));
                } else {
                    lines.add(kv(key, String.join(", ", scalars), keyWidth));
                }
            } else {
                String text = cellText(val);
                // colour certain known keys
                if (isOkKey(key, val)) {
                    lines.add(kv(key, t.green(text), keyWidth));
                } else if (isWarnKey(key, val)) {
                    lines.add(kv(key, t.yellow(text), keyWidth));
                } else if (isErrorKey(key)) {
                    lines.add(kv(key, t.red(text), keyWidth));
                } else if (isStepKey(key)) {
                    lines.add(kv(key, t.cyan(text), keyWidth));
                } else if (val.isBoolean()) {
                    lines.add(kv(key, val.asBoolean() ? t.green(text) : t.red(text), keyWidth));
                } else {
                    lines.add(kv(key, text, keyWidth));
                }
            }
        }
        // For steps arrays (testcase_show) render a nested step table
        if (node.has("steps") && node.get("steps").isArray() && node.get("steps").size() > 0) {
            return stepsResult(node);
        }
        String title = infoTitle(toolId, node);
        return panels.box(title, lines);
    }

    /**
     * Flattens one nested level of a field's object value into indented
     * "sub-key  value" rows instead of collapsing it to a bare {@code {N fields}}
     * summary, so e.g. {@code doctor}'s {@code jdk}/{@code k6}/{@code project}
     * blocks are actually readable.
     */
    private void appendNestedObject(List<String> lines, String key, JsonNode obj) {
        lines.add(t.bold(key) + ":");
        int subKeyWidth = 0;
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            entries.add(e);
            subKeyWidth = Math.max(subKeyWidth, e.getKey().length());
        }
        for (Map.Entry<String, JsonNode> e : entries) {
            String subKey = e.getKey();
            JsonNode v = e.getValue();
            String rendered;
            if (v.isObject()) {
                rendered = t.dim("{" + v.size() + " fields}");
            } else if (v.isArray()) {
                if (v.size() == 0) {
                    rendered = t.dim("(empty)");
                } else if (v.get(0).isObject()) {
                    rendered = t.dim("[" + v.size() + " items]");
                } else {
                    List<String> scalars = new ArrayList<>();
                    for (JsonNode s : v) scalars.add(s.asText());
                    rendered = String.join(", ", scalars);
                }
            } else {
                String text = cellText(v);
                if (isErrorKey(subKey)) {
                    rendered = t.red(text);
                } else if (v.isBoolean()) {
                    rendered = v.asBoolean() ? t.green(text) : t.red(text);
                } else {
                    rendered = text;
                }
            }
            lines.add("  " + kv(subKey, rendered, subKeyWidth));
        }
    }

    /**
     * Compact one-line rendering of an object inside an array (e.g.
     * {@code {"driver":"chromedriver","present":false}}): a boolean field
     * becomes a check/cross icon, the remaining fields are joined after it.
     */
    private String bulletForObject(JsonNode obj) {
        String boolKey = null;
        boolean boolVal = false;
        List<String> keys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            keys.add(e.getKey());
            if (boolKey == null && e.getValue().isBoolean()) {
                boolKey = e.getKey();
                boolVal = e.getValue().asBoolean();
            }
        }
        List<String> rest = new ArrayList<>();
        for (String k : keys) {
            if (k.equals(boolKey)) continue;
            rest.add(cellText(obj.path(k)));
        }
        String label = rest.isEmpty() ? obj.toString() : String.join(" ", rest);
        if (boolKey != null) {
            return (boolVal ? t.green(Theme.CHECK) : t.red(Theme.CROSS)) + " " + label;
        }
        return t.purple("\u2022") + " " + label;
    }

    private String stepsResult(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        // header box
        List<String> header = new ArrayList<>();
        for (String k : new String[] { "project", "scenario", "testcase" }) {
            if (node.has(k)) header.add(kv(k, node.path(k).asText(), 8));
        }
        sb.append(panels.box("Test Case", header)).append('\n').append('\n');
        // steps table
        List<String> stepCols = List.of(
            "Step",
            "Object Name",
            "Description",
            "Action",
            "Input",
            "Condition",
            "Reference"
        );
        int[] w = new int[stepCols.size()];
        for (int i = 0; i < stepCols.size(); i++) w[i] = stepCols.get(i).length();
        List<String[]> rows = new ArrayList<>();
        for (JsonNode s : node.get("steps")) {
            String[] cells = {
                s.path("step").asText(""),
                cellText(s.path("object")),
                cellText(s.path("description")),
                cellText(s.path("action")),
                cellText(s.path("input")),
                cellText(s.path("condition")),
                cellText(s.path("reference"))
            };
            for (int i = 0; i < cells.length; i++) w[i] =
                Math.max(w[i], Theme.visibleLength(cells[i]));
            rows.add(cells);
        }
        sb.append(tableTop(stepCols, w, "Steps (" + rows.size() + ")"));
        sb.append(tableHeader(stepCols, w));
        sb.append(tableDivider(w, false));
        for (int i = 0; i < rows.size(); i++) {
            sb.append(tableRow(rows.get(i), w));
            if (i < rows.size() - 1) {
                sb.append(tableSpacerRow(w));
            }
        }
        sb.append(tableBottom(w));
        return sb.toString().stripTrailing();
    }

    // ------------------------------------------------------------------
    // box-drawing helpers
    // ------------------------------------------------------------------

    private String tableTop(List<String> cols, int[] widths, String title) {
        int totalWidth = 1; // left border
        for (int w : widths) totalWidth += w + 3; // " cell "
        totalWidth++; // right border

        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            String inner = " " + title + " ";
            int fill = totalWidth - 2 - Theme.visibleLength(inner);
            int left = Math.max(0, fill / 2);
            int right = Math.max(0, fill - left);
            sb
                .append(t.purple("\u256d"))
                .append(t.purple("\u2500".repeat(left)))
                .append(t.bold(inner))
                .append(t.purple("\u2500".repeat(right)))
                .append(t.purple("\u256e"))
                .append('\n');
        } else {
            sb.append(t.purple("\u256d" + "\u2500".repeat(totalWidth - 2) + "\u256e")).append('\n');
        }
        return sb.toString();
    }

    private String tableHeader(List<String> cols, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.purple("\u2502"));
        for (int c = 0; c < cols.size(); c++) {
            sb.append(' ').append(t.bold(pad(cols.get(c), widths[c]))).append(' ');
            sb.append(t.purple("\u2502"));
        }
        sb.append('\n');
        return sb.toString();
    }

    private String tableDivider(int[] widths, boolean bottom) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.purple(bottom ? "\u2570" : "\u251c"));
        for (int c = 0; c < widths.length; c++) {
            sb.append(t.purple("\u2500".repeat(widths[c] + 2)));
            sb.append(
                t.purple(
                    c < widths.length - 1
                        ? (bottom ? "\u2534" : "\u253c")
                        : (bottom ? "\u256f" : "\u2524")
                )
            );
        }
        sb.append('\n');
        return sb.toString();
    }

    private String tableBottom(int[] widths) {
        return tableDivider(widths, true);
    }

    private String tableRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.purple("\u2502"));
        for (int c = 0; c < cells.length; c++) {
            sb.append(' ').append(pad(cells[c], widths[c])).append(' ');
            sb.append(t.purple("\u2502"));
        }
        sb.append('\n');
        return sb.toString();
    }

    private String tableSpacerRow(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.purple("\u2502"));
        for (int c = 0; c < widths.length; c++) {
            sb.append(' ').append(" ".repeat(widths[c])).append(' ');
            sb.append(t.purple("\u2502"));
        }
        sb.append('\n');
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // small utilities
    // ------------------------------------------------------------------

    private static JsonNode findArray(JsonNode node) {
        if (node.isArray()) return node;
        if (!node.isObject()) return null;
        for (String key : new String[] {
            "items",
            "tools",
            "scenarios",
            "testcases",
            "objects",
            "results",
            "runs",
            "failures",
            "entries",
            "archetypes",
            "testsets",
            "actions",
            "categories",
            "sheets",
            "environments",
            "drivers",
            "checks",
            "projects",
            "pages"
        }) {
            if (node.has(key) && node.get(key).isArray()) return node.get(key);
        }
        return null;
    }

    private static String cellText(JsonNode v) {
        if (v == null || v.isMissingNode() || v.isNull()) return "";
        if (v.isBoolean()) return v.asBoolean() ? "\u2713" : "\u2717";
        String s = v.isTextual() ? v.asText() : v.toString();
        s = relativize(s).replace('\n', ' ').replace('\r', ' ');
        return s.length() > MAX_CELL ? s.substring(0, MAX_CELL - 1) + "\u2026" : s;
    }

    /** Render absolute paths under the working directory as relative ones. */
    static String relativize(String s) {
        if (s == null || CWD == null || s.isEmpty()) return s;
        if (s.equals(CWD)) return ".";
        String prefix = CWD + java.io.File.separator;
        if (s.startsWith(prefix)) return s.substring(prefix.length());
        return s;
    }

    private static String pad(String s, int width) {
        int visible = Theme.visibleLength(s);
        int pad = Math.max(0, width - visible);
        return pad == 0 ? s : s + " ".repeat(pad);
    }

    private String kv(String key, String value, int keyWidth) {
        return t.dim(pad(key, keyWidth)) + "  " + value;
    }

    private static String titleFor(String toolId, int count) {
        if (toolId == null) return count + " items";
        String label = toolId.replace('_', ' ').replaceFirst("^ingenious ", "");
        return label + "  (" + count + ")";
    }

    private static String infoTitle(String toolId, JsonNode node) {
        // pick the most useful field as the box title
        for (String k : new String[] {
            "testcase",
            "scenario",
            "project",
            "name",
            "page",
            "sheet",
            "runId",
            "target"
        }) {
            if (node.has(k)) return node.path(k).asText();
        }
        if (toolId != null) return toolId.replace('_', ' ').replaceFirst("^ingenious ", "");
        return "Result";
    }

    private static boolean isOkKey(String key, JsonNode val) {
        return (
            ("valid".equals(key) || "created".equals(key) || "success".equals(key)) &&
            val.isBoolean() &&
            val.asBoolean()
        );
    }

    private static boolean isWarnKey(String key, JsonNode val) {
        return (
            ("valid".equals(key) && val.isBoolean() && !val.asBoolean()) ||
            ("warnings".equals(key) && val.isArray() && val.size() > 0)
        );
    }

    private static boolean isErrorKey(String key) {
        return "error".equals(key) || "errors".equals(key);
    }

    private static boolean isStepKey(String key) {
        return "action".equals(key) || "step".equals(key);
    }

    private static JsonNode parseIfJson(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
        } catch (Exception e) {
            return new com.fasterxml.jackson.databind.node.TextNode(text);
        }
    }
}
