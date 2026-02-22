package com.ing.datalib.or.yaml;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML representation of a Web OR page.
 * 
 * Example YAML output:
 * <pre>
 * page: ContactUs
 * description: Contact form page with inquiry submission
 * urlPattern: /contact-us
 * 
 * elements:
 *   firstName:
 *     label: "First name *(required)"
 *   submitButton:
 *     role: button
 *     text: Send
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"page", "description", "urlPattern", "tags", "elements"})
public class YamlPageDefinition {
    
    private String page;
    private String description;
    private String urlPattern;
    private List<String> tags;
    private Map<String, YamlElementDefinition> elements = new LinkedHashMap<>();
    
    public YamlPageDefinition() {
    }
    
    public YamlPageDefinition(String page) {
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

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, YamlElementDefinition> getElements() {
        return elements;
    }

    public void setElements(Map<String, YamlElementDefinition> elements) {
        this.elements = elements;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert a WebORPage to YamlPageDefinition.
     */
    public static YamlPageDefinition fromWebORPage(WebORPage page) {
        YamlPageDefinition yaml = new YamlPageDefinition();
        yaml.setPage(page.getName());
        
        // Iterate through object groups and objects using Lists
        for (ObjectGroup<WebORObject> group : page.getObjectGroups()) {
            for (WebORObject obj : group.getObjects()) {
                YamlElementDefinition element = YamlElementDefinition.fromWebORObject(obj);
                // Use object name as key
                yaml.getElements().put(obj.getName(), element);
            }
        }
        
        return yaml;
    }
    
    /**
     * Convert YamlPageDefinition to a WebORPage.
     */
    public WebORPage toWebORPage(WebOR root) {
        WebORPage page = new WebORPage(this.page, root);
        
        // Convert each element to WebORObject using direct list manipulation
        // to avoid calling factory methods that require ObjectRepository
        for (Map.Entry<String, YamlElementDefinition> entry : elements.entrySet()) {
            String elementName = entry.getKey();
            YamlElementDefinition elementDef = entry.getValue();
            
            // Create object group directly
            ObjectGroup<WebORObject> group = new ObjectGroup<>(elementName, page);
            
            // Create object and add to group
            WebORObject obj = elementDef.toWebORObject(elementName, group);
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
    public void addElement(String name, YamlElementDefinition element) {
        elements.put(name, element);
    }
    
    /**
     * Get an element by name.
     */
    public YamlElementDefinition getElement(String name) {
        return elements.get(name);
    }
    
    /**
     * Check if page has an element with the given name.
     */
    public boolean hasElement(String name) {
        return elements.containsKey(name);
    }
}
