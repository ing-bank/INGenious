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
            .append("automation IDE. Help the user design and edit test scenarios, test cases, ")
            .append("steps, and Object Repository entries. Be concise and use Markdown. ")
            .append("Always follow INGenious conventions described below. Never invent action ")
            .append("keywords or Object Repository elements that do not exist.\n\n");
        String skill = text();
        if (!skill.isEmpty()) {
            sb.append("# INGenious conventions\n\n").append(skill);
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
