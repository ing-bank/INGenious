package com.ing.ide.main.mainui.components.aichat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A model entry from the GitHub Models catalog
 * ({@code GET /catalog/models}). Only the fields useful for the model selector
 * are mapped; unknown fields are ignored so the class is resilient to API
 * changes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelInfo {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("publisher")
    private String publisher;

    @JsonProperty("summary")
    private String summary;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    /** Display label used by the model selector dropdown. */
    public String displayName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return id;
    }

    @Override
    public String toString() {
        return displayName();
    }
}
