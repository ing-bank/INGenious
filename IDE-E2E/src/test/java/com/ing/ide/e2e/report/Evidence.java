package com.ing.ide.e2e.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

/**
 * A screenshot or screen recording attached to a test case.
 */
public final class Evidence {
    @JsonProperty
    private final String title;

    @JsonProperty
    private final String path;

    /**
     * @param title short caption, e.g. {@code 02-new-project-form}
     * @param path path relative to the run folder, with {@code /} separators
     */
    @JsonCreator
    public Evidence(
        @JsonProperty("title") final String title,
        @JsonProperty("path") final String path
    ) {
        this.title = title;
        this.path = path;
    }

    /**
     * @return short caption
     */
    public String title() {
        return title;
    }

    /**
     * @return path relative to the run folder
     */
    public String path() {
        return path;
    }

    /**
     * @return whether this is a screen recording rather than a screenshot
     */
    @JsonIgnore
    public boolean isRecording() {
        final String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov");
    }
}
