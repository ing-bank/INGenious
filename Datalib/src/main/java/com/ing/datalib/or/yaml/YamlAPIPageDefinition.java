package com.ing.datalib.or.yaml;

import com.ing.datalib.or.api.APIOR;
import com.ing.datalib.or.api.APIORObject;
import com.ing.datalib.or.api.APIORPage;
import com.ing.datalib.or.common.ObjectGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML representation of an API OR page.
 * 
 * Example YAML output:
 * <pre>
 * page: UserAPI
 * description: User API response elements
 * 
 * elements:
 *   userName:
 *     jsonPath: "$.data.user.name"
 *   userEmail:
 *     xpath: "/response/data/user/email"
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"page", "description", "tags", "elements"})
public class YamlAPIPageDefinition {
    
    private String page;
    private String description;
    private List<String> tags;
    private Map<String, YamlAPIElementDefinition> elements = new LinkedHashMap<>();
    
    public YamlAPIPageDefinition() {
    }
    
    public YamlAPIPageDefinition(String page) {
        this.page = page;
    }
    
    // ==================== Getters and Setters ====================
    
    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, YamlAPIElementDefinition> getElements() {
        return elements;
    }

    public void setElements(Map<String, YamlAPIElementDefinition> elements) {
        this.elements = elements;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert an APIORPage to YamlAPIPageDefinition.
     */
    public static YamlAPIPageDefinition fromAPIORPage(APIORPage page) {
        YamlAPIPageDefinition yaml = new YamlAPIPageDefinition();
        yaml.setPage(page.getName());
        
        // Iterate through object groups and objects using Lists
        for (ObjectGroup<APIORObject> group : page.getObjectGroups()) {
            for (APIORObject obj : group.getObjects()) {
                YamlAPIElementDefinition element = YamlAPIElementDefinition.fromAPIORObject(obj);
                // Use object name as key
                yaml.getElements().put(obj.getName(), element);
            }
        }
        
        return yaml;
    }
    
    /**
     * Convert YamlAPIPageDefinition to an APIORPage.
     */
    public APIORPage toAPIORPage(APIOR root) {
        APIORPage page = new APIORPage(this.page, root);
        
        // Convert each element to APIORObject using direct list manipulation
        // to avoid calling factory methods that require ObjectRepository
        for (Map.Entry<String, YamlAPIElementDefinition> entry : elements.entrySet()) {
            String elementName = entry.getKey();
            YamlAPIElementDefinition elementDef = entry.getValue();
            
            // Create object group directly
            ObjectGroup<APIORObject> group = new ObjectGroup<>(elementName, page);
            
            // Create object and add to group
            APIORObject obj = elementDef.toAPIORObject(elementName, group);
            group.getObjects().add(obj);
            
            // Add group to page directly
            page.getObjectGroups().add(group);
        }
        
        return page;
    }
    
    /**
     * Get the number of elements in this page.
     */
    @JsonIgnore
    public int getElementCount() {
        return elements.size();
    }
    
    /**
     * Add an element to this page.
     */
    public void addElement(String name, YamlAPIElementDefinition element) {
        elements.put(name, element);
    }
    
    /**
     * Remove an element from this page.
     */
    public void removeElement(String name) {
        elements.remove(name);
    }
    
    /**
     * Get an element by name.
     */
    public YamlAPIElementDefinition getElement(String name) {
        return elements.get(name);
    }
}
