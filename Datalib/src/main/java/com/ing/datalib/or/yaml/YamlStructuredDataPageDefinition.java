package com.ing.datalib.or.yaml;

import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.common.ObjectGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML representation of an Structured Data OR page.
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
@JsonPropertyOrder({"page", "scope", "description", "tags", "elements"})
public class YamlStructuredDataPageDefinition {
    
    private String page;
    private String description;
    private List<String> tags;
    private Map<String, YamlStructuredDataElementDefinition> elements = new LinkedHashMap<>();
    private StructuredDataOR.ORScope scope;
    
    public YamlStructuredDataPageDefinition() {
    }
    
    public YamlStructuredDataPageDefinition(String page) {
        this.page = page;
    }
    
    // ==================== Getters and Setters ====================
    
    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public StructuredDataOR.ORScope getScope() {
        return scope;
    }

    public void setScope(StructuredDataOR.ORScope scope) {
        this.scope = scope;
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

    public Map<String, YamlStructuredDataElementDefinition> getElements() {
        return elements;
    }

    public void setElements(Map<String, YamlStructuredDataElementDefinition> elements) {
        this.elements = elements;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert an StructuredDataORPage to YamlStructuredDataPageDefinition.
     */
    public static YamlStructuredDataPageDefinition fromStructuredDataORPage(StructuredDataORPage page) {
        YamlStructuredDataPageDefinition yaml = new YamlStructuredDataPageDefinition();
        yaml.setPage(page.getName());

        if (page.getRoot() != null) {
            yaml.setScope(page.getRoot().getScope());
        }
        
        // Iterate through object groups and objects using Lists
        for (ObjectGroup<StructuredDataORObject> group : page.getObjectGroups()) {
            for (StructuredDataORObject obj : group.getObjects()) {
                YamlStructuredDataElementDefinition element = YamlStructuredDataElementDefinition.fromStructuredDataORObject(obj);
                // Use object name as key
                yaml.getElements().put(obj.getName(), element);
            }
        }
        
        return yaml;
    }
    
    /**
     * Convert YamlStructuredDataPageDefinition to an StructuredDataORPage.
     */
    public StructuredDataORPage toStructuredDataORPage(StructuredDataOR root) {
        StructuredDataORPage page = new StructuredDataORPage(this.page, root);
        
        if (this.scope != null && root != null && this.scope != root.getScope()) { 
            throw new IllegalStateException("Scope mismatch: YAML Structured Data page '" + page + "' declares scope " + scope + " but is loaded under OR scope " + root.getScope());
        }
        
        // Convert each element to StructuredDataORObject using direct list manipulation
        // to avoid calling factory methods that require ObjectRepository
        for (Map.Entry<String, YamlStructuredDataElementDefinition> entry : elements.entrySet()) {
            String elementName = entry.getKey();
            YamlStructuredDataElementDefinition elementDef = entry.getValue();
            
            // Create object group directly
            ObjectGroup<StructuredDataORObject> group = new ObjectGroup<>(elementName, page);
            
            // Create object and add to group
            StructuredDataORObject obj = elementDef.toStructuredDataORObject(elementName, group);
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
    public void addElement(String name, YamlStructuredDataElementDefinition element) {
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
    public YamlStructuredDataElementDefinition getElement(String name) {
        return elements.get(name);
    }
}
