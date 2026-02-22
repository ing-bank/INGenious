package com.ing.datalib.or.yaml;

import com.ing.datalib.or.api.APIOR;
import com.ing.datalib.or.api.APIORObject;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML representation of an API OR element.
 * Only non-empty properties are serialized to YAML.
 * 
 * Example YAML output:
 * <pre>
 * userResponse:
 *   jsonPath: "$.data.users[0].name"
 *   xpath: "/response/data/users/user[1]/name"
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"jsonPath", "xpath", "description"})
public class YamlAPIElementDefinition {
    
    // API locator properties
    private String jsonPath;
    private String xpath;
    
    // Additional metadata
    private String description;
    
    // Capture any unknown properties for extensibility
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();
    
    public YamlAPIElementDefinition() {
    }
    
    // ==================== Getters and Setters ====================
    
    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
     * Convert an APIORObject to YamlAPIElementDefinition.
     * Only non-empty attributes are captured.
     */
    public static YamlAPIElementDefinition fromAPIORObject(APIORObject obj) {
        YamlAPIElementDefinition elem = new YamlAPIElementDefinition();
        
        // Map attributes to properties (only non-empty values)
        for (ORAttribute attr : obj.getAttributes()) {
            if (attr.getValue() != null && !attr.getValue().isEmpty()) {
                String attrName = attr.getName().toLowerCase();
                switch (attrName) {
                    case "jsonpath":
                        elem.setJsonPath(attr.getValue());
                        break;
                    case "xpath":
                        elem.setXpath(attr.getValue());
                        break;
                    default:
                        // Store unknown attributes in additionalProperties
                        elem.setAdditionalProperty(attr.getName(), attr.getValue());
                        break;
                }
            }
        }
        return elem;
    }
    
    /**
     * Convert YamlAPIElementDefinition to an APIORObject.
     */
    public APIORObject toAPIORObject(String name, ObjectGroup<APIORObject> group) {
        APIORObject obj = new APIORObject(name, group);
        
        // Set attribute values
        setAttributeIfPresent(obj, "JsonPath", jsonPath);
        setAttributeIfPresent(obj, "Xpath", xpath);
        
        return obj;
    }
    
    private void setAttributeIfPresent(APIORObject obj, String name, String value) {
        if (value != null && !value.isEmpty()) {
            ORAttribute attr = obj.getAttribute(name);
            if (attr != null) {
                attr.setValue(value);
            }
        }
    }
    
    /**
     * Check if this element has any locator properties defined.
     */
    @JsonIgnore
    public boolean hasLocators() {
        return isNotEmpty(jsonPath) || isNotEmpty(xpath);
    }
    
    private boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
