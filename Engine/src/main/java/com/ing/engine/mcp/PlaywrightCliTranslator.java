package com.ing.engine.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Translates a single <a href="https://playwright.dev/agent-cli/introduction">Playwright
 * Agent CLI</a> command (e.g. {@code click e21}, {@code fill e5 "hello world"})
 * into zero or more INGenious test steps.
 *
 * <p>Element refs from the Playwright accessibility snapshot (e.g. {@code e21})
 * are carried through as the INGenious step's {@code object} placeholder so an
 * agent can later bind them to durable Object-Repository locators (see
 * {@code ingenious_object_import_page} / {@code ingenious_browser_inspect}).
 *
 * <p>This is intentionally a pure, dependency-free mapper so it can be unit
 * tested and reused by both the MCP server and any future CLI bridge.
 */
public final class PlaywrightCliTranslator {

    private PlaywrightCliTranslator() {}

    /** A minimal INGenious step: action + object + input. */
    public static final class Step {
        public final String action;
        public String object;
        public final String input;

        /**
         * The Playwright accessibility ref (e.g. {@code e21}) this step acted
         * on, or {@code null} for ref-less commands. Used to bind the step to a
         * durable Object-Repository locator at materialization time.
         */
        public final String ref;

        /**
         * The durable, comma-free INGenious locator resolved for {@link #ref}
         * (e.g. {@code role=button[name="Submit"]}), or {@code null} if not yet
         * resolved. Populated deterministically from the live snapshot.
         */
        public String locator;

        public Step(String action, String object, String input) {
            this(action, object, input, null);
        }

        public Step(String action, String object, String input, String ref) {
            this.action = action;
            this.object = object;
            this.input = input;
            this.ref = ref;
        }
    }

    /**
     * Map one Playwright CLI command to INGenious steps. Non-recordable
     * commands (e.g. {@code snapshot}, navigation helpers with no INGenious
     * equivalent) return an empty list.
     */
    public static List<Step> translate(String command) {
        List<Step> steps = new ArrayList<>();
        if (command == null || command.trim().isEmpty()) return steps;

        List<String> t = tokenize(command.trim());
        if (t.isEmpty()) return steps;

        String verb = t.get(0).toLowerCase(Locale.ROOT);
        String a1 = t.size() > 1 ? t.get(1) : "";
        String rest = t.size() > 1 ? String.join(" ", t.subList(1, t.size())) : "";
        String restFrom2 = t.size() > 2 ? String.join(" ", t.subList(2, t.size())) : "";

        switch (verb) {
            case "open":
            case "goto":
                steps.add(new Step("NavigateTo", "", a1));
                break;
            case "click":
                steps.add(new Step("Click", a1, "", a1));
                break;
            case "dblclick":
                steps.add(new Step("DoubleClick", a1, "", a1));
                break;
            case "fill":
                steps.add(new Step("SetText", a1, restFrom2, a1));
                break;
            case "type":
                steps.add(new Step("Type", "", rest));
                break;
            case "check":
                steps.add(new Step("Check", a1, "", a1));
                break;
            case "uncheck":
                steps.add(new Step("Uncheck", a1, "", a1));
                break;
            case "select":
                steps.add(new Step("SelectByValue", a1, restFrom2, a1));
                break;
            case "hover":
                steps.add(new Step("Hover", a1, "", a1));
                break;
            case "press":
                steps.add(new Step("PressKey", "", rest));
                break;
            case "upload":
                // upload <file> (per Playwright CLI docs, no ref)
                steps.add(new Step("UploadFile", "", rest));
                break;
            case "screenshot":
                steps.add(new Step("CaptureScreenshot", a1, ""));
                break;
            case "go-back":
                steps.add(new Step("NavigateBack", "", ""));
                break;
            case "go-forward":
                steps.add(new Step("NavigateForward", "", ""));
                break;
            case "reload":
                steps.add(new Step("Refresh", "", ""));
                break;
            default:
                // snapshot, console, tracing-*, video-*, tab-*, network, etc.
                // are not recorded as INGenious steps.
                break;
        }
        return steps;
    }

    /** Split a command line honouring double quotes. */
    static List<String> tokenize(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        java.util.regex.Matcher m = java
            .util.regex.Pattern.compile("\"([^\"]*)\"|(\\S+)")
            .matcher(s);
        while (m.find()) {
            out.add(m.group(1) != null ? m.group(1) : m.group(2));
        }
        return out;
    }
}
