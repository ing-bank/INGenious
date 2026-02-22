package com.ing.datalib.or.yaml;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML representation of a Mobile OR page.
 * 
 * Example YAML output:
 * <pre>
 * page: LoginScreen
 * packageName: com.example.app
 * description: Login screen with username and password fields
 * 
 * elements:
 *   usernameField:
 *     accessibility: username_input
 *     id: com.example.app:id/username
 *   passwordField:
 *     accessibility: password_input
 *   loginButton:
 *     accessibility: login_button
 *     uiAutomator: new UiSelector().text("Login")
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"page", "packageName", "description", "platform", "tags", "elements"})
public class YamlMobilePageDefinition {
    
    private String page;
    private String packageName;  // Mobile-specific: app package name
    private String description;
    private String platform;     // "android" | "ios" | "both"
    private List<String> tags;
    private Map<String, YamlMobileElementDefinition> elements = new LinkedHashMap<>();
    
    public YamlMobilePageDefinition() {
    }
    
    public YamlMobilePageDefinition(String page) {
        this.page = page;
    }
    
    // ==================== Getters and Setters ====================
    
    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, YamlMobileElementDefinition> getElements() {
        return elements;
    }

    public void setElements(Map<String, YamlMobileElementDefinition> elements) {
        this.elements = elements;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Convert a MobileORPage to YamlMobilePageDefinition.
     */
    public static YamlMobilePageDefinition fromMobileORPage(MobileORPage page) {
        YamlMobilePageDefinition yaml = new YamlMobilePageDefinition();
        yaml.setPage(page.getName());
        yaml.setPackageName(page.getPackageName());
        
        // Iterate through object groups and objects using Lists
        for (ObjectGroup<MobileORObject> group : page.getObjectGroups()) {
            for (MobileORObject obj : group.getObjects()) {
                YamlMobileElementDefinition element = YamlMobileElementDefinition.fromMobileORObject(obj);
                // Use object name as key
                yaml.getElements().put(obj.getName(), element);
            }
        }
        
        return yaml;
    }
    
    /**
     * Convert YamlMobilePageDefinition to a MobileORPage.
     */
    public MobileORPage toMobileORPage(MobileOR root) {
        MobileORPage page = new MobileORPage(this.page, root);
        if (this.packageName != null && !this.packageName.isEmpty()) {
            page.setPackageName(this.packageName);
        }
        
        // Convert each element to MobileORObject using direct list manipulation
        // to avoid calling factory methods that require ObjectRepository
        for (Map.Entry<String, YamlMobileElementDefinition> entry : elements.entrySet()) {
            String elementName = entry.getKey();
            YamlMobileElementDefinition elementDef = entry.getValue();
            
            // Create object group directly
            ObjectGroup<MobileORObject> group = new ObjectGroup<>(elementName, page);
            
            // Create object and add to group
            MobileORObject obj = elementDef.toMobileORObject(elementName, group);
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
    public void addElement(String name, YamlMobileElementDefinition element) {
        elements.put(name, element);
    }
    
    /**
     * Get an element by name.
     */
    public YamlMobileElementDefinition getElement(String name) {
        return elements.get(name);
    }
    
    /**
     * Check if page has an element with the given name.
     */
    public boolean hasElement(String name) {
        return elements.containsKey(name);
    }
}
