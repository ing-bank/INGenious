package com.ing.datalib.or.yaml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.datalib.or.sap.SapORPage;

/**
 * YAML representation of a SAP OR page.
 * 
 * Example YAML output:
 * <pre>
 * page: LoginPage
 * packageName: /SU01
 * description: SAP Logon screen with user and password fields
 * 
 * elements:
 *   usernameField:
 *     id: wnd[0]/usr/txtRSYST-BNAME
 *     text: User Name
 *   passwordField:
 *     id: wnd[0]/usr/pwdRSYST-BCODE
 *   loginButton:
 *     id: wnd[0]/usr/btnLOGIN
 *     text: Enter
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"page", "scope", "packageName", "description", "tags", "elements"})
public class YamlSapPageDefinition {
    
    private String page;
    private String packageName;  // SAP-specific: transaction code or screen name
    private String description;
    private List<String> tags;
    private Map<String, YamlSapElementDefinition> elements = new LinkedHashMap<>();
    private SapOR.ORScope scope;
    
    public YamlSapPageDefinition() {
    }
    
    public YamlSapPageDefinition(String page) {
        this.page = page;
    }
    
    // ==================== Getters and Setters ====================
    
    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public SapOR.ORScope getScope() {
        return scope;
    }

    public void setScope(SapOR.ORScope scope) {
        this.scope = scope;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, YamlSapElementDefinition> getElements() {
        return elements;
    }

    public void setElements(Map<String, YamlSapElementDefinition> elements) {
        this.elements = elements;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Create from SapORPage (for writing YAML).
     */
    public static YamlSapPageDefinition fromSapORPage(SapORPage page) {
        YamlSapPageDefinition yaml = new YamlSapPageDefinition(page.getName());
        yaml.setPackageName(page.getPackageName());
        
        if (page.getRoot() != null) {
            yaml.setScope(page.getRoot().getScope());
        }
        
        // Convert all object groups to YAML elements
        for (ObjectGroup<SapORObject> group : page.getObjectGroups()) {
            for (SapORObject object : group.getObjects()) {
                YamlSapElementDefinition element = YamlSapElementDefinition.fromSapORObject(object);
                // Add all objects, even if empty (matches Web OR behavior)
                yaml.getElements().put(object.getName(), element);
            }
        }
        
        return yaml;
    }
    
    /**
     * Convert to SapORPage (for reading YAML).
     */
    public SapORPage toSapORPage(SapOR root) {
        SapORPage page = new SapORPage(this.page, root);

        if (this.scope != null && root != null && this.scope != root.getScope()) { 
            throw new IllegalStateException("Scope mismatch: YAML page '" + page + "' declares scope " + scope + " but is loaded under OR scope " + root.getScope());
        }
        
        if (packageName != null && !packageName.isEmpty()) {
            page.setPackageName(packageName);
        }
        
        // Convert YAML elements to object groups
        for (Map.Entry<String, YamlSapElementDefinition> entry : elements.entrySet()) {
            String objectName = entry.getKey();
            YamlSapElementDefinition elementDef = entry.getValue();
            
            // Create object group with single object
            ObjectGroup<SapORObject> group = new ObjectGroup<>(objectName, page);
            SapORObject object = elementDef.toSapORObject(objectName, group);
            group.getObjects().add(object);
            page.getObjectGroups().add(group);
        }
        
        return page;
    }
    
    /**
     * Get count of elements for logging.
     */
    @JsonIgnore
    public int getElementCount() {
        return elements.size();
    }
}
