
package com.ing.datalib.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 *
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name", "value"
})
public class Attribute {

    @JsonProperty("name")
    private String name;

    @JsonProperty("value")
    private String value;

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }

    public boolean equals(Attribute a) {
        return Objects.equals(name, a.name) && Objects.equals(value, a.value);
    }

    public static Attribute create(Object name, Object value) {
        if (Objects.nonNull(value) && Objects.nonNull(name)) {
            Attribute attr = new Attribute();
            attr.setName(name.toString());
            attr.setValue(value.toString());
            return attr;
        }
        return null;
    }

}
