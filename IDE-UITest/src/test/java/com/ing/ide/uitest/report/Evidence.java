package com.ing.ide.uitest.report;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

/**
 * A screenshot or screen recording attached to a test case, optionally captioned and linked to
 * a step or assert of the RTM test case.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Evidence {
    @JsonProperty
    private final String title;

    @JsonProperty
    private final String path;

    @JsonProperty
    private final String caption;

    @JsonProperty
    private final String step;

    @JsonProperty("assert")
    private final String assertion;

    /**
     * @param title short name, e.g. {@code 02-new-project-form}
     * @param path file path; relative to the run folder (with {@code /}) once stored in a result
     * @param caption what the evidence shows, or {@code null}
     * @param step linked RTM step number, or {@code null}
     * @param assertion linked RTM assert number, or {@code null}
     */
    @JsonCreator
    public Evidence(
        @JsonProperty("title") final String title,
        @JsonProperty("path") final String path,
        @JsonProperty("caption") final String caption,
        @JsonProperty("step") final String step,
        @JsonProperty("assert") final String assertion
    ) {
        this.title = title;
        this.path = path;
        this.caption = caption;
        this.step = step;
        this.assertion = assertion;
    }

    /**
     * @param newPath the path to use instead
     * @return a copy with another path
     */
    public Evidence withPath(final String newPath) {
        return new Evidence(title, newPath, caption, step, assertion);
    }

    /**
     * @return short name
     */
    public String title() {
        return title;
    }

    /**
     * @return file path
     */
    public String path() {
        return path;
    }

    /**
     * @return what the evidence shows, or {@code null}
     */
    public String caption() {
        return caption;
    }

    /**
     * @return linked RTM step number, or {@code null}
     */
    public String step() {
        return step;
    }

    /**
     * @return linked RTM assert number, or {@code null}
     */
    public String assertion() {
        return assertion;
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
