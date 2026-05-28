package com.ing.datalib.or.yaml;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapORObject;

/**
 * YAML representation of a SAP OR element.
 * Text values are NOT stored in OR - they only appear in test case Input column.
 * 
 * Example YAML output:
 * <pre>
 * elements:
 *   usernameField:
 *     id: wnd[0]/usr/txtRSYST-BNAME
 *     name: User Name Field
 *   passwordField:
 *     id: wnd[0]/usr/pwdRSYST-BCODE
 *   loginButton:
 *     id: wnd[0]/usr/btnLOGIN
 *   emptyObject: {}
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "name", "Text"})
public class YamlSapElementDefinition {
    
    private String id;
    private String name;
    private String Text;
    
    public YamlSapElementDefinition() {
    }
    
    // ==================== Getters and Setters ====================
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getText() {
        return Text;
    }

    public void setText(String text) {
        this.Text = text;
    }
    
    // ==================== Conversion Methods ====================
    
    /**
     * Create from SapORObject (reading OR).
     * Note: Text property is NOT stored in OR - it only appears in test case Input column.
     */
    public static YamlSapElementDefinition fromSapORObject(SapORObject object) {
        YamlSapElementDefinition def = new YamlSapElementDefinition();
        
        // Extract properties from attributes (id and name only, NOT text)
        for (ORAttribute attr : object.getAttributes()) {
            String attrName = attr.getName();
            String value = attr.getValue();
            
            if (value != null && !value.isEmpty()) {
                switch (attrName) {
                    case "id":
                        def.setId(value);
                        break;
                    case "name":
                        def.setName(value);
                        break;
                    case "Text":
                        def.setText(value);
                        break;
                }
            }
        }
        
        return def;
    }
    
    /**
     * Convert to SapORObject (writing OR).
     * Note: Text property is NOT stored in OR - it only appears in test case Input column.
     */
    public SapORObject toSapORObject(String objectName, ObjectGroup<SapORObject> group) {
        SapORObject object = new SapORObject(objectName, group);
        
        // Set properties as attributes (id and name only, NOT text)
        for (ORAttribute attr : object.getAttributes()) {
            String attrName = attr.getName();
            
            switch (attrName) {
                case "id":
                    if (id != null && !id.isEmpty()) {
                        attr.setValue(id);
                    }
                    break;
                case "name":
                    if (name != null && !name.isEmpty()) {
                        attr.setValue(name);
                    }
                    break;
                case "Text":
                    if (Text != null && !Text.isEmpty()) {
                        attr.setValue(Text);
                    }
                    break;
            }
        }
        
        return object;
    }
    
    /**
     * Check if this element has any properties defined.
     * Note: @JsonIgnore prevents this from being serialized as an 'empty' field.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return (id == null || id.isEmpty()) 
            && (name == null || name.isEmpty())
            && (Text == null || Text.isEmpty());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YamlSapElementDefinition that = (YamlSapElementDefinition) o;
        return Objects.equals(id, that.id) 
            && Objects.equals(name, that.name)
            && Objects.equals(Text, that.Text);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, name, Text);
    }
}
