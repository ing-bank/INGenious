
package com.ing.datalib.or.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ORAttribute {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String value;

    @JacksonXmlProperty(isAttribute = true, localName = "pref")
    private String preference;
    
    @JacksonXmlProperty(isAttribute = true)
    private boolean exact;

    public ORAttribute() {
    }

    public ORAttribute(String name, int preference) {
        this.name = name;
        this.value = "";
        this.preference = String.valueOf(preference);
        this.exact = false;
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
    
    public boolean isExact() {
        return exact;
    }
    
    public void setExact(boolean exact) {
        this.exact = exact;
    }

    @Override
    public String toString() {
        return "ClassPojo [ref = " + name + ", value = " + value + ", pref = " + preference + ", exact = " + exact + "]";
    }

    @JsonIgnore
    public ORAttribute cloneAs() {
        ORAttribute attribute = new ORAttribute();
        attribute.setName(name);
        attribute.setPreference(preference);
        attribute.setValue(value);
        attribute.setExact(exact);
        return attribute;
    }
}
