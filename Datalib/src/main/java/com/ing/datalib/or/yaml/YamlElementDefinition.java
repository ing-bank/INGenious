package com.ing.datalib.or.yaml;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML representation of a Web OR element.
 * Only non-empty properties are serialized to YAML.
 * 
 * Example YAML output:
 * <pre>
 * firstName:
 *   label: "First name *(required)"
 *   description: Input field for first name
 *   exact:
 *     - label
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"role", "text", "label", "placeholder", "testId", "css", "xpath", 
                    "altText", "title", "chainedLocator", "frame", "description", "exact"})
public class YamlElementDefinition {
    
    // Standard Playwright locator properties (lowercase for YAML convention)
    private String role;
    private String text;
    private String label;
    private String placeholder;
    private String xpath;
    private String css;
    private String altText;
    private String title;
    private String testId;
    private String chainedLocator;
    
    // Additional metadata
    private String frame;
    private String description;
    
    // List of locator names that should use exact matching
    private List<String> exact;
    
    // Capture any unknown properties for extensibility
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();
    
    public YamlElementDefinition() {
    }
    
    // ==================== Getters and Setters ====================
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
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

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getChainedLocator() {
        return chainedLocator;
    }

    public void setChainedLocator(String chainedLocator) {
        this.chainedLocator = chainedLocator;
    }

    public String getFrame() {
        return frame;
    }

    public void setFrame(String frame) {
        this.frame = frame;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }

    // ==================== Conversion Methods ====================
    
    /**
     * Convert a WebORObject to YamlElementDefinition.
     * Only non-empty attributes are captured.
     */
    public static YamlElementDefinition fromWebORObject(WebORObject obj) {
        YamlElementDefinition elem = new YamlElementDefinition();
        
        // Set frame if present
        if (obj.getFrame() != null && !obj.getFrame().isEmpty()) {
            elem.setFrame(obj.getFrame());
        }
        
        // Map attributes to properties (only non-empty values)
        for (ORAttribute attr : obj.getAttributes()) {
            if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                String attrName = attr.getName().toLowerCase();
                switch (attrName) {
                    case "role":
                        elem.setRole(attr.getValue());
                        break;
                    case "text":
                        elem.setText(attr.getValue());
                        break;
                    case "label":
                        elem.setLabel(attr.getValue());
                        break;
                    case "placeholder":
                        elem.setPlaceholder(attr.getValue());
                        break;
                    case "xpath":
                        elem.setXpath(attr.getValue());
                        break;
                    case "css":
                        elem.setCss(attr.getValue());
                        break;
                    case "alttext":
                        elem.setAltText(attr.getValue());
                        break;
                    case "title":
                        elem.setTitle(attr.getValue());
                        break;
                    case "testid":
                        elem.setTestId(attr.getValue());
                        break;
                    case "chainedlocator":
                        elem.setChainedLocator(attr.getValue());
                        break;
                    default:
                        // Store unknown attributes in additionalProperties
                        elem.setAdditionalProperty(attr.getName(), attr.getValue());
                        break;
                }
                
                // Add to exact list if the attribute has exact flag set
                if (attr.isExact()) {
                    elem.addExact(attr.getName());
                }
            }
        }
        return elem;
    }
    
    /**
     * Convert YamlElementDefinition to a WebORObject.
     */
    public WebORObject toWebORObject(String name, ObjectGroup<WebORObject> group) {
        WebORObject obj = new WebORObject(name, group);
        
        // Set frame
        if (frame != null && !frame.isEmpty()) {
            obj.setFrame(frame);
        }
        
        // Set attribute values and exact flags
        setAttributeIfPresent(obj, "Role", role, isExact("role"));
        setAttributeIfPresent(obj, "Text", text, isExact("text"));
        setAttributeIfPresent(obj, "Label", label, isExact("label"));
        setAttributeIfPresent(obj, "Placeholder", placeholder, isExact("placeholder"));
        setAttributeIfPresent(obj, "xpath", xpath, isExact("xpath"));
        setAttributeIfPresent(obj, "css", css, isExact("css"));
        setAttributeIfPresent(obj, "AltText", altText, isExact("alttext"));
        setAttributeIfPresent(obj, "Title", title, isExact("title"));
        setAttributeIfPresent(obj, "TestId", testId, isExact("testid"));
        setAttributeIfPresent(obj, "ChainedLocator", chainedLocator, isExact("chainedlocator"));
        
        return obj;
    }
    
    private void setAttributeIfPresent(WebORObject obj, String name, String value, boolean exactMatch) {
        if (value != null && !value.isEmpty()) {
            ORAttribute attr = obj.getAttribute(name);
            if (attr != null) {
                attr.setValue(value);
                attr.setExact(exactMatch);
            }
        }
    }
    
    /**
     * Check if this element has any locator properties defined.
     */
    @JsonIgnore
    public boolean hasLocators() {
        return isNotEmpty(role) || isNotEmpty(text) || isNotEmpty(label) || 
               isNotEmpty(placeholder) || isNotEmpty(xpath) || isNotEmpty(css) ||
               isNotEmpty(altText) || isNotEmpty(title) || isNotEmpty(testId) ||
               isNotEmpty(chainedLocator);
    }
    
    private boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
