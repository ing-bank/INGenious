package com.ing.datalib.or.yaml;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML representation of a Mobile OR element.
 * 
 * Example YAML output:
 * <pre>
 * elements:
 *   loginButton:
 *     accessibility: login_button
 *     id: com.example.app:id/login
 *     exact:
 *       - accessibility
 *   usernameField:
 *     uiAutomator: new UiSelector().resourceId("username")
 *     xpath: //android.widget.EditText[@text="Username"]
 * </pre>
 * 
 * Only non-empty properties are serialized (75% size reduction from XML).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"uiAutomator", "uiAutomation", "id", "accessibility", 
                    "xpath", "css", "name", "tagName", "linkText", "className", "exact"})
public class YamlMobileElementDefinition {
    
    // Mobile-specific locator strategies
    private String uiAutomator;   // Android UiAutomator selector
    private String uiAutomation;  // iOS UiAutomation selector
    private String id;            // Resource ID
    private String accessibility; // Accessibility ID
    
    // Standard locators (shared with web)
    private String xpath;
    private String css;
    private String name;
    private String tagName;
    @JsonProperty("linkText")
    private String linkText;
    @JsonProperty("className")
    private String className;
    
    // List of locator names that should use exact matching
    private List<String> exact;
    
    public YamlMobileElementDefinition() {
    }
    
    // ==================== Getters and Setters ====================
    
    public String getUiAutomator() {
        return uiAutomator;
    }

    public void setUiAutomator(String uiAutomator) {
        this.uiAutomator = uiAutomator;
    }

    public String getUiAutomation() {
        return uiAutomation;
    }

    public void setUiAutomation(String uiAutomation) {
        this.uiAutomation = uiAutomation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(String accessibility) {
        this.accessibility = accessibility;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getCss() {
        return css;
    }

    public void setCss(String css) {
        this.css = css;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
    
    public List<String> getExact() {
        return exact;
    }
    
    public void setExact(List<String> exact) {
        this.exact = exact;
    }
    
    public void addExact(String locatorName) {
        if (this.exact == null) {
            this.exact = new ArrayList<>();
        }
        if (!this.exact.contains(locatorName.toLowerCase())) {
            this.exact.add(locatorName.toLowerCase());
        }
    }
    
    public boolean isExact(String locatorName) {
        return exact != null && exact.contains(locatorName.toLowerCase());
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert a MobileORObject to YamlMobileElementDefinition.
     * Maps MobileOR property names to YAML field names.
     */
    public static YamlMobileElementDefinition fromMobileORObject(MobileORObject obj) {
        YamlMobileElementDefinition yaml = new YamlMobileElementDefinition();
        
        for (ORAttribute attr : obj.getAttributes()) {
            String propName = attr.getName();
            String value = attr.getValue();
            
            if (value == null || value.isEmpty()) {
                continue;
            }
            
            // Map MobileOR property names to YAML fields
            switch (propName) {
                case "UiAutomator":
                    yaml.setUiAutomator(value);
                    break;
                case "UiAutomation":
                    yaml.setUiAutomation(value);
                    break;
                case "id":
                    yaml.setId(value);
                    break;
                case "Accessibility":
                    yaml.setAccessibility(value);
                    break;
                case "xpath":
                    yaml.setXpath(value);
                    break;
                case "css":
                    yaml.setCss(value);
                    break;
                case "name":
                    yaml.setName(value);
                    break;
                case "tagName":
                    yaml.setTagName(value);
                    break;
                case "link_text":
                    yaml.setLinkText(value);
                    break;
                case "class":
                    yaml.setClassName(value);
                    break;
                default:
                    // Unknown property, ignore
                    break;
            }
            
            // Add to exact list if the attribute has exact flag set
            if (attr.isExact()) {
                yaml.addExact(propName);
            }
        }
        
        return yaml;
    }
    
    /**
     * Convert YamlMobileElementDefinition to a MobileORObject.
     */
    public MobileORObject toMobileORObject(String name, ObjectGroup<MobileORObject> group) {
        MobileORObject obj = new MobileORObject(name, group);
        
        // Add attributes for each non-empty property with exact flag
        // Using MobileOR property names for compatibility
        setAttributeIfPresent(obj, "UiAutomator", uiAutomator, isExact("uiautomator"));
        setAttributeIfPresent(obj, "UiAutomation", uiAutomation, isExact("uiautomation"));
        setAttributeIfPresent(obj, "id", id, isExact("id"));
        setAttributeIfPresent(obj, "Accessibility", accessibility, isExact("accessibility"));
        setAttributeIfPresent(obj, "xpath", xpath, isExact("xpath"));
        setAttributeIfPresent(obj, "css", css, isExact("css"));
        setAttributeIfPresent(obj, "name", this.name, isExact("name"));
        setAttributeIfPresent(obj, "tagName", tagName, isExact("tagname"));
        setAttributeIfPresent(obj, "link_text", linkText, isExact("link_text"));
        setAttributeIfPresent(obj, "class", className, isExact("class"));
        
        return obj;
    }
    
    private void setAttributeIfPresent(MobileORObject obj, String propName, String value, boolean exactMatch) {
        ORAttribute attr = obj.getAttribute(propName);
        if (attr != null && value != null && !value.isEmpty()) {
            attr.setValue(value);
            attr.setExact(exactMatch);
        }
    }
    
    /**
     * Check if this element has any defined locators.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return isNullOrEmpty(uiAutomator) && isNullOrEmpty(uiAutomation)
            && isNullOrEmpty(id) && isNullOrEmpty(accessibility)
            && isNullOrEmpty(xpath) && isNullOrEmpty(css)
            && isNullOrEmpty(name) && isNullOrEmpty(tagName)
            && isNullOrEmpty(linkText) && isNullOrEmpty(className);
    }
    
    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
    
    /**
     * Get the primary locator value (first non-empty locator).
     * Priority: accessibility > id > uiAutomator > uiAutomation > xpath > css > name > tagName > linkText > className
     */
    @JsonIgnore
    public String getPrimaryLocatorValue() {
        if (!isNullOrEmpty(accessibility)) return accessibility;
        if (!isNullOrEmpty(id)) return id;
        if (!isNullOrEmpty(uiAutomator)) return uiAutomator;
        if (!isNullOrEmpty(uiAutomation)) return uiAutomation;
        if (!isNullOrEmpty(xpath)) return xpath;
        if (!isNullOrEmpty(css)) return css;
        if (!isNullOrEmpty(name)) return name;
        if (!isNullOrEmpty(tagName)) return tagName;
        if (!isNullOrEmpty(linkText)) return linkText;
        if (!isNullOrEmpty(className)) return className;
        return null;
    }
    
    /**
     * Get the primary locator type name.
     */
    @JsonIgnore
    public String getPrimaryLocatorType() {
        if (!isNullOrEmpty(accessibility)) return "Accessibility";
        if (!isNullOrEmpty(id)) return "id";
        if (!isNullOrEmpty(uiAutomator)) return "UiAutomator";
        if (!isNullOrEmpty(uiAutomation)) return "UiAutomation";
        if (!isNullOrEmpty(xpath)) return "xpath";
        if (!isNullOrEmpty(css)) return "css";
        if (!isNullOrEmpty(name)) return "name";
        if (!isNullOrEmpty(tagName)) return "tagName";
        if (!isNullOrEmpty(linkText)) return "link_text";
        if (!isNullOrEmpty(className)) return "class";
        return null;
    }
}
