
package com.ing.datalib.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "value"
})

/**
 *
 * 
 */
public class Tag {

    @JsonProperty("value")
    private String value;

    @JsonIgnore
    public boolean state = false;

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(String value) {
        this.value = value;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return value;
    }

    @JsonIgnore
    @Override
    public boolean equals(Object o) {
        if (o instanceof Tag) {
            o = ((Tag) o).value;
        }
        return Objects.deepEquals(value, o);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.value);
        return hash;
    }

    public static Tag create(String value) {
        Tag t = new Tag();
        t.setValue(value);
        return t;
    }
}
