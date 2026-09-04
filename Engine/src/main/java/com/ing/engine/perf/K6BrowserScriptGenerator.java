package com.ing.engine.perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebORObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generates k6 BROWSER scripts (k6/browser module) from INGenious web test
 * cases. k6's browser API is Playwright-derived and the INGenious web engine
 * is Playwright Java, so actions and locators map nearly 1:1.
 *
 * <p>Object locators are resolved through the Datalib OR model
 * ({@code project.getObjectRepository().resolveWebObject(...)}) honouring the
 * step's Reference column ({@code [Project]/[Shared]} scopes). Locator
 * preference: css → xpath → TestId → Role(+name) → Label → Placeholder →
 * Text → Title → AltText (mirrors the engine's AutomationObject strategies).
 *
 * <p>Unsupported actions surface as {@code // TODO} lines plus warnings —
 * never dropped silently.
 */
public final class K6BrowserScriptGenerator {

    /** Extraction outcome: emitted JS statements plus generation warnings. */
    public static final class Result {
        /** Body statements (unindented; the emitter indents). */
        public final List<String> lines = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();
        public int checks;
        public int actions;
    }

    private K6BrowserScriptGenerator() {}

    // ==================================================================
    // Test case -> JS statements
    // ==================================================================

    public static Result fromTestCase(Project project, TestCase testCase) {
        Result result = new Result();
        for (TestStep step : TestCaseFlattener.flatten(project, testCase, result.warnings)) {
            handleStep(project, step, result);
        }
        if (result.actions == 0) {
            result.warnings.add("No translatable browser steps found in the test case.");
        }
        return result;
    }

    private static void handleStep(Project project, TestStep step, Result result) {
        String action = step.getAction() == null ? "" : step.getAction().trim();
        String object = step.getObject() == null ? "" : step.getObject().trim();
        String rawInput = step.getInput() == null ? "" : step.getInput().trim();
        String lower = action.toLowerCase(Locale.ROOT);
        String input = TestCaseFlattener.resolveInput(rawInput, result.warnings, action);
        String js = K6HttpScriptGenerator.js(input);

        switch (lower) {
            case "open":
            case "openbrowser":
            case "navigateto":
            case "navigate":
            case "goto":
                result.lines.add("await page.goto(" + js + ", { waitUntil: 'load' });");
                result.actions++;
                return;
            case "navigateback":
            case "goback":
                result.lines.add("await page.goBack();");
                result.actions++;
                return;
            case "navigateforward":
            case "goforward":
                result.lines.add("await page.goForward();");
                result.actions++;
                return;
            case "refresh":
            case "reload":
                result.lines.add("await page.reload();");
                result.actions++;
                return;
            case "closebrowser":
            case "closepage":
            case "closeallbrowsers":
                // page lifecycle is owned by the generated try/finally scaffold
                return;
            default:
            // fall through to element actions
        }

        String locator = locatorFor(project, step, result);
        if (locator == null) {
            // warning already recorded; keep the step visible in the script
            result.lines.add(
                "// TODO: could not resolve object '" + object + "' for action '" + action + "'"
            );
            return;
        }
        String label = K6HttpScriptGenerator.js(object + " " + lower);
        switch (lower) {
            case "click":
                result.lines.add("await " + locator + ".click();");
                break;
            case "doubleclick":
            case "dblclick":
                result.lines.add("await " + locator + ".dblclick();");
                break;
            case "fill":
            case "settext":
            case "set":
                result.lines.add("await " + locator + ".fill(" + js + ");");
                break;
            case "type":
            case "presssequentially":
                result.lines.add("await " + locator + ".type(" + js + ");");
                break;
            case "press":
                result.lines.add("await " + locator + ".press(" + js + ");");
                break;
            case "check":
                result.lines.add("await " + locator + ".check();");
                break;
            case "uncheck":
                result.lines.add("await " + locator + ".uncheck();");
                break;
            case "hover":
            case "mouseover":
                result.lines.add("await " + locator + ".hover();");
                break;
            case "selectbyvalue":
            case "select":
                result.lines.add("await " + locator + ".selectOption(" + js + ");");
                break;
            case "selectbytext":
            case "selectbyvisibletext":
                result.lines.add("await " + locator + ".selectOption({ label: " + js + " });");
                break;
            case "clear":
            case "cleartext":
                result.lines.add("await " + locator + ".clear();");
                break;
            case "waitforelementtobevisible":
            case "waitforelementvisible":
            case "waitforvisibility":
                result.lines.add("await " + locator + ".waitFor({ state: 'visible' });");
                break;
            case "waitforelementtobehidden":
                result.lines.add("await " + locator + ".waitFor({ state: 'hidden' });");
                break;
            case "assertelementisvisible":
            case "elementisvisible":
                result.lines.add(
                    "check(await " + locator + ".isVisible(), { " + label + ": (v) => v });"
                );
                result.checks++;
                break;
            case "assertelementcontainstext":
            case "elementcontainstext":
                result.lines.add(
                    "check(await " +
                    locator +
                    ".textContent(), { " +
                    label +
                    ": (t) => (t || '').includes(" +
                    js +
                    ") });"
                );
                result.checks++;
                break;
            case "assertelementisenabled":
                result.lines.add(
                    "check(await " + locator + ".isEnabled(), { " + label + ": (v) => v });"
                );
                result.checks++;
                break;
            default:
                result.lines.add(
                    "// TODO: unsupported action '" +
                    action +
                    "' on '" +
                    object +
                    "'" +
                    (input.isEmpty() ? "" : " (input: " + brief(input) + ")")
                );
                result.warnings.add("Unsupported browser action not translated: " + action);
                return;
        }
        result.actions++;
    }

    // ==================================================================
    // locator resolution
    // ==================================================================

    /** OR attribute preference order (engine-aligned). */
    private static final String[] PREFERENCE = {
        "css",
        "xpath",
        "TestId",
        "Role",
        "Label",
        "Placeholder",
        "Text",
        "Title",
        "AltText"
    };

    /**
     * Build the k6 locator expression (e.g. {@code page.locator('#id')} or
     * {@code page.getByRole('button', { name: 'Sign in' })}) for a step's
     * object, or null (with a warning) when the object cannot be resolved.
     */
    static String locatorFor(Project project, TestStep step, Result result) {
        String object = step.getObject() == null ? "" : step.getObject().trim();
        String reference = step.getReference() == null ? "" : step.getReference().trim();
        if (object.isEmpty() || reference.isEmpty()) {
            result.warnings.add("Step '" + step.getAction() + "' has no object/page reference.");
            return null;
        }
        ResolvedWebObject resolved = project
            .getObjectRepository()
            .resolveWebObject(ResolvedWebObject.PageRef.parse(reference), object);
        WebORObject webObject = resolved == null ? null : resolved.getObject();
        if (webObject == null) {
            result.warnings.add(
                "Object not found in OR: page '" + reference + "', object '" + object + "'"
            );
            return null;
        }
        String role = null;
        String roleName = null;
        for (String prop : PREFERENCE) {
            String value = attr(webObject, prop);
            if (value == null || value.isEmpty()) {
                continue;
            }
            String jsValue = K6HttpScriptGenerator.js(value);
            switch (prop) {
                case "css":
                case "xpath":
                    return "page.locator(" + jsValue + ")";
                case "TestId":
                    return "page.getByTestId(" + jsValue + ")";
                case "Role":
                    role = value;
                    roleName =
                        firstNonEmpty(
                            attr(webObject, "Label"),
                            attr(webObject, "Text"),
                            attr(webObject, "Title")
                        );
                    if (roleName != null) {
                        return (
                            "page.getByRole(" +
                            jsValue +
                            ", { name: " +
                            K6HttpScriptGenerator.js(roleName) +
                            " })"
                        );
                    }
                    return "page.getByRole(" + jsValue + ")";
                case "Label":
                    return "page.getByLabel(" + jsValue + ")";
                case "Placeholder":
                    return "page.getByPlaceholder(" + jsValue + ")";
                case "Text":
                    return "page.getByText(" + jsValue + ")";
                case "Title":
                    return "page.getByTitle(" + jsValue + ")";
                case "AltText":
                    return "page.getByAltText(" + jsValue + ")";
                default:
                // unreachable
            }
        }
        result.warnings.add(
            "Object '" + object + "' (page '" + reference + "') has no usable locator attribute."
        );
        return null;
    }

    private static String attr(WebORObject object, String name) {
        List<ORAttribute> attributes = object.getAttributes();
        if (attributes == null) {
            return null;
        }
        for (ORAttribute a : attributes) {
            if (name.equalsIgnoreCase(a.getName())) {
                String v = a.getValue();
                return v == null || v.trim().isEmpty() ? null : v.trim();
            }
        }
        return null;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    // ==================================================================
    // JS emission
    // ==================================================================

    /**
     * Emit the full k6 browser script: scenarios block with the browser
     * option, Web-Vitals + checks thresholds, and an async default function
     * wrapping the body in newPage()/try/finally.
     */
    public static String generate(
        String source,
        String regenerate,
        PerfProfile profile,
        List<String> lines,
        List<String> warnings
    ) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder body = new StringBuilder();
        body.append("import { browser } from 'k6/browser';\n");
        body.append("import { check } from 'k6';\n\n");
        if (!warnings.isEmpty()) {
            body.append("// Generation warnings:\n");
            for (String w : warnings) {
                body.append("//   - ").append(w.replace("\n", " ")).append('\n');
            }
            body.append('\n');
        }
        body.append("export const options = __ENV.K6_PERF_VALIDATE\n");
        body.append("  ? ");
        body.append(K6HttpScriptGenerator.prettyJson(mapper, validateOptions(mapper), 4));
        body.append("\n  : ");
        body.append(K6HttpScriptGenerator.prettyJson(mapper, browserOptions(mapper, profile), 4));
        body.append(";\n\n");
        body.append("export default async function () {\n");
        body.append("  const page = await browser.newPage();\n");
        body.append("  try {\n");
        for (String line : lines) {
            body.append("    ").append(line).append('\n');
        }
        body.append("  } finally {\n");
        body.append("    await page.close();\n");
        body.append("  }\n");
        body.append("}\n");
        return ScriptProvenance.wrap(source, regenerate, profile.name, body.toString());
    }

    /**
     * Options for a browser scenario: the profile's load shape inside
     * {@code scenarios.ui} (with the chromium browser option) plus
     * browser-appropriate thresholds. The built-in profiles' http_req_*
     * thresholds are meaningless for browser runs and are replaced by
     * checks + Web Vitals defaults; custom (non-http) thresholds from
     * project profiles are preserved.
     */
    static ObjectNode browserOptions(ObjectMapper mapper, PerfProfile profile) {
        ObjectNode options = mapper.createObjectNode();
        ObjectNode ui = options.putObject("scenarios").putObject("ui");
        if ("constant-vus".equals(profile.executor)) {
            ui.put("executor", "constant-vus");
            ui.put("vus", profile.vus);
            if (profile.duration != null) {
                ui.put("duration", profile.duration);
            }
        } else {
            ui.put("executor", "ramping-vus");
            ArrayNode stages = ui.putArray("stages");
            for (PerfProfile.Stage s : profile.stages) {
                ObjectNode n = stages.addObject();
                n.put("duration", s.duration);
                n.put("target", s.target);
            }
        }
        ui.putObject("options").putObject("browser").put("type", "chromium");
        ObjectNode thresholds = options.putObject("thresholds");
        thresholds.putArray("checks").add("rate==1.0");
        thresholds.putArray("browser_web_vital_lcp").add("p(95)<2500");
        for (Map.Entry<String, List<String>> e : profile.thresholds.entrySet()) {
            if (e.getKey().startsWith("http_req_")) {
                continue; // protocol metrics don't exist in browser runs
            }
            ArrayNode arr = thresholds.putArray(e.getKey());
            for (String expr : e.getValue()) {
                arr.add(expr);
            }
        }
        return options;
    }

    /**
     * Validator options (K6_PERF_VALIDATE=1): exactly one iteration with the
     * browser option intact. CLI flags like --vus/--iterations cannot be used
     * for browser scripts because they REPLACE the scenario definition and
     * drop {@code options.browser.type} — k6 then aborts with "browser not
     * found in registry".
     */
    static ObjectNode validateOptions(ObjectMapper mapper) {
        ObjectNode options = mapper.createObjectNode();
        ObjectNode ui = options.putObject("scenarios").putObject("ui");
        ui.put("executor", "shared-iterations");
        ui.put("vus", 1);
        ui.put("iterations", 1);
        ui.putObject("options").putObject("browser").put("type", "chromium");
        options.putObject("thresholds").putArray("checks").add("rate==1.0");
        return options;
    }

    private static String brief(String s) {
        return s.length() <= 60 ? s : s.substring(0, 57) + "...";
    }
}
