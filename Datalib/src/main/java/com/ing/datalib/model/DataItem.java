
package com.ing.datalib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "id",
    "name",
    "tags",
    "attributes"
})

/**
 *
 * 
 */
public class DataItem {

    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("attributes")
    private Attributes attributes = new Attributes();
    @JsonProperty("tags")
    private Tags tags = new Tags();

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("attributes")
    public Attributes getAttributes() {
        return attributes;
    }

    @JsonProperty("attributes")
    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes.addAll(attributes);
    }

    @JsonProperty("tags")
    public Tags getTags() {
        return tags;
    }

    @JsonProperty("tags")
    public void setTags(Tags tags) {
        this.tags = tags;
    }

    @JsonIgnore
    public void setTags(List<Tag> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }

    @JsonIgnore
    public boolean hasScenario(String scn) {
        return attributes.contains(Meta.Attributes.scenario.name(), scn);
    }

    @JsonIgnore
    public boolean hasScenario() {
        return attributes.contains(Meta.Attributes.scenario.name());
    }

    @JsonIgnore
    public String getScenario() {
        if (hasScenario()) {
            return attributes.get(Meta.Attributes.scenario.name()).getValue();
        }
        return "";
    }

    public static DataItem create(String name) {
        DataItem di = new DataItem();
        di.setName(name);
        di.setId(name);
        return di;
    }

    public static DataItem createTestCase(String name, String scn) {
        DataItem di = create(name);
        di.getAttributes().add(Attribute.create(Meta.Attributes.scenario.name(), scn));
        return di;
    }
}
