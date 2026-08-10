package com.ing.datalib.or.structureddata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StructuredDataAttribute {
    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "pref")
    private String preference;

    public StructuredDataAttribute() {}

    public StructuredDataAttribute(String name, int preference) {
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
        return "ClassPojo [ref = " + name + ", value = " + value + "]";
    }

    @JsonIgnore
    public StructuredDataAttribute cloneAs() {
        StructuredDataAttribute attribute = new StructuredDataAttribute();
        attribute.setName(name);
        attribute.setValue(value);
        return attribute;
    }
}
