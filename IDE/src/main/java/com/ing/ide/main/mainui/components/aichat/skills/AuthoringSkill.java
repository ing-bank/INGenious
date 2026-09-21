package com.ing.ide.main.mainui.components.aichat.skills;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the INGenious authoring skill that grounds the model in real INGenious
 * conventions (action grammar, Object Repository naming, step shape). The skill
 * text is injected as system context for both chat and agent turns.
 */
public final class AuthoringSkill {
    private static final Logger LOG = Logger.getLogger(AuthoringSkill.class.getName());

    private static final String RESOURCE = "/aichat/skills/INGeniousAuthoring.md";

    private static String cached;

    private AuthoringSkill() {}

    /** Returns the authoring skill text, or an empty string if unavailable. */
    public static synchronized String text() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    /**
     * Builds the full system prompt: a short role preamble plus the authoring
     * skill, used to ground chat and agent responses.
     */
    public static String systemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb
            .append("You are the INGenious AI assistant, embedded in the INGenious test ")
            .append("automation IDE. You act on the project currently open in the IDE.\n\n")
            .append(
                "You have a full set of INGenious tools available via function calling and you "
            )
            .append(
                "MUST use them to fulfil requests end to end, rather than refusing or telling the "
            )
            .append(
                "user to do it manually. Never claim you lack file-system or tool access \u2014 when a "
            )
            .append(
                "tool exists for a task, call it. If a request needs several steps, chain multiple "
            )
            .append("tool calls until it is done, then summarise what you did.\n\n")
            .append("Your tools cover the entire INGenious surface, including: projects; test ")
            .append("scenarios; test cases and steps; the Object Repository; test data sheets and ")
            .append(
                "environments; test sets and releases (create a test set under a release and add "
            )
            .append("test cases to it); running tests (browser/headless options) and reading run ")
            .append(
                "status, logs and reports; imports (cURL, Postman, Bruno, Playwright); and test "
            )
            .append(
                "generation. Releases, test sets, execution and reports are all IN SCOPE \u2014 use the "
            )
            .append("matching tools instead of declining.\n\n")
            .append("When running tests, leave `headless` unset (default false) for browser-based ")
            .append(
                "tests so they run headed with a visible window; only pass `headless:true` if the "
            )
            .append(
                "user explicitly asks for a headless run. Tests using browser 'No Browser' (API/AI "
            )
            .append("tests) are unaffected by this flag and keep running without a browser.\n\n")
            .append("Also leave `breakOnError` unset on ingenious_run/ingenious_run_async - it ")
            .append(
                "defaults to true so a run stops at the first failed step instead of paying the "
            )
            .append("default per-step wait for every remaining step in a long script; only pass ")
            .append("`breakOnError:false` if the user wants the run to continue past failures.\n\n")
            .append("When using ingenious_browser_discover / ingenious_browser_session_start for ")
            .append(
                "live Playwright-cli discovery, leave `headed` unset (default true) so the browser "
            )
            .append(
                "window stays visible and the exploration is transparent; only pass `headed:false` "
            )
            .append("if the user asks for a headless discovery session.\n\n")
            .append("Skill routing: for any request to author a browser/UI test, an API test, ")
            .append("migrate Selenium/Gherkin tests, create a plugin, or detect customizations, ")
            .append("FIRST call ingenious_skill_read to load the matching skill (or ")
            .append("ingenious_skill_list if unsure) and follow its playbook exactly. The skills ")
            .append(
                "are tool-first and their structure is mandatory: split the flow into reusable "
            )
            .append("components, create per-page Object Repository objects, and parameterize data ")
            .append("into sheets \u2014 never emit one monolithic test with hard-coded values.\n\n")
            .append(
                "Working style: inspect current state with the list/show tools before mutating; "
            )
            .append(
                "look up real action keywords and existing Object Repository elements with the "
            )
            .append(
                "action and object tools; never invent action keywords or OR elements that do not "
            )
            .append("exist. Be concise and use Markdown.\n\n")
            .append(
                "Important: only call a tool when tools are actually provided to you this turn. If "
            )
            .append(
                "no tools are available, do NOT fabricate tool calls, invent tool names, or ask the "
            )
            .append(
                "user to start an MCP server \u2014 instead answer directly, or tell the user to open a "
            )
            .append("project so the INGenious tools become available.\n\n");
        String skill = text();
        if (!skill.isEmpty()) {
            sb.append("# INGenious conventions\n\n").append(skill);
        }
        // Engine-authoritative conventions (same text every MCP client receives
        // via initialize.instructions) so IDE chat and REPL behave identically.
        try {
            sb
                .append("\n\n# INGenious tool conventions (authoritative)\n\n")
                .append(com.ing.engine.mcp.ConventionCatalog.condensedInstructions());
        } catch (Throwable t) {
            LOG.log(Level.FINE, "ConventionCatalog unavailable", t);
        }
        return sb.toString();
    }

    private static String load() {
        try (InputStream in = AuthoringSkill.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LOG.log(Level.WARNING, "Authoring skill resource not found: {0}", RESOURCE);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to load authoring skill", ex);
            return "";
        }
    }
}
