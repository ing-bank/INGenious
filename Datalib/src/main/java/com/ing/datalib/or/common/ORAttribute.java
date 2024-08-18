
package com.ing.datalib.or.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ORAttribute {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "pref")
    private String preference;

    public ORAttribute() {
    }

    public ORAttribute(String name, int preference) {
        this.name = name;
        this.value = "";
        this.preference = String.valueOf(preference);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    @Override
    public String toString() {
        return "ClassPojo [ref = " + name + ", value = " + value + ", pref = " + preference + "]";
    }

    @JsonIgnore
    public ORAttribute cloneAs() {
        ORAttribute attribute = new ORAttribute();
        attribute.setName(name);
        attribute.setPreference(preference);
        attribute.setValue(value);
        return attribute;
    }
}
