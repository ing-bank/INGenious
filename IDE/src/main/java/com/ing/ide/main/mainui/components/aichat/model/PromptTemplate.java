package com.ing.ide.main.mainui.components.aichat.model;

import java.util.Arrays;
import java.util.List;

/**
 * A reusable natural-language prompt shown as a clickable chip in the AI
 * assistant's prompt library. The {@link #template} may contain
 * {@code ${token}} placeholders (e.g. {@code ${currentProject}}) that are
 * substituted with live IDE context when the chip is clicked.
 */
public final class PromptTemplate {
    private final String id;
    private final String section;
    private final String label;
    private final String template;
    private final List<String> tokens;

    private PromptTemplate(
        String id,
        String section,
        String label,
        String template,
        List<String> tokens
    ) {
        this.id = id;
        this.section = section;
        this.label = label;
        this.template = template;
        this.tokens = tokens;
    }

    public static PromptTemplate of(
        String id,
        String section,
        String label,
        String template,
        String... tokens
    ) {
        return new PromptTemplate(id, section, label, template, Arrays.asList(tokens));
    }

    public String getId() {
        return id;
    }

    public String getSection() {
        return section;
    }

    public String getLabel() {
        return label;
    }

    public String getTemplate() {
        return template;
    }

    public List<String> getTokens() {
        return tokens;
    }
}
