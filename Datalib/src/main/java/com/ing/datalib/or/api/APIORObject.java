package com.ing.datalib.or.api;

import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORUtils;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.undoredo.UndoRedoModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.List;
import java.util.Objects;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * API Object Repository object class.
 * TableModel with 2 columns: Attribute and Value.
 * Supports only JsonPath and Xpath as locator attributes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIORObject extends UndoRedoModel implements ORObjectInf {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(localName = "Property")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Property")
    private List<ORAttribute> attributes;

    @JsonIgnore
    private ObjectGroup<APIORObject> group;

    public APIORObject() {
        setDefaultORAttributes();
    }

    public APIORObject(String name, ObjectGroup group) {
        this.name = name;
        this.group = group;
        setDefaultORAttributes();
    }

    @JsonIgnore
    public final void setDefaultORAttributes() {
        attributes = new ArrayList<>();
        for (int i = 0; i < APIOR.OBJECT_PROPS.size(); i++) {
            ORAttribute attr = new ORAttribute();
            attr.setName(APIOR.OBJECT_PROPS.get(i));
            attr.setValue("");
            attr.setPreference("" + (i + 1));
            attributes.add(attr);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public List<ORAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<ORAttribute> attributes) {
        this.attributes = attributes;
    }

    @JsonIgnore
    @Override
    public void removeFromParent() {
        changeSave();
        if (group.getObjects().size() == 1) {
            group.removeFromParent();
        }
        group.getObjects().remove(this);
        FileUtils.deleteFile(getRepLocation());
    }

    @JsonIgnore
    @Override
    public TreeNode getChildAt(int i) {
        return null;
    }

    @JsonIgnore
    @Override
    public int getChildCount() {
        return 0;
    }

    @JsonIgnore
    @Override
    public ObjectGroup<APIORObject> getParent() {
        return group;
    }

    @JsonIgnore
    @Override
    public int getIndex(TreeNode tn) {
        return -1;
    }

    @JsonIgnore
    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @JsonIgnore
    @Override
    public boolean isLeaf() {
        return true;
    }

    @JsonIgnore
    @Override
    public Enumeration children() {
        return null;
    }

    @JsonIgnore
    @Override
    public void setParent(ObjectGroup group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    @Override
    public int getRowCount() {
        return attributes.size();
    }

    @JsonIgnore
    @Override
    public int getColumnCount() {
        return 2; // Only Attribute and Value columns
    }

    @JsonIgnore
    @Override
    public Object getValueAt(int row, int column) {
        if (column == 0) {
            return attributes.get(row).getName();
        } else if (column == 1) {
            return attributes.get(row).getValue();
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        ORAttribute attr = attributes.get(rowIndex);
        if (columnIndex == 0) {
            if (isNotDefault(rowIndex) && getAttribute(value.toString()) == null) {
                super.setValueAt(value, rowIndex, columnIndex);
                attr.setName(value.toString());
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        } else if (columnIndex == 1) {
            if (!Objects.equals(attr.getValue(), value)) {
                super.setValueAt(value, rowIndex, columnIndex);
                attr.setValue(value.toString());
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    @JsonIgnore
    private Boolean isNotDefault(int rowIndex) {
        String value = getValueAt(rowIndex, 0).toString();
        return APIOR.OBJECT_PROPS.indexOf(value) == -1;
    }

    @JsonIgnore
    @Override
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    @JsonIgnore
    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "Attribute";
        } else if (column == 1) {
            return "Value";
        }
        return null;
    }

    @JsonIgnore
    private void changeSave() {
        if (group != null) {
            APIORPage page = (APIORPage) group.getParent();
            page.getRoot().setSaved(false);
            
            // Auto-save for YAML format
            if (page.getRoot().getObjectRepository() != null 
                && page.getRoot().getObjectRepository().isUsingYamlFormat()) {
                page.getRoot().getObjectRepository().saveAPIPageNow(page);
            }
        }
    }

    @Override
    public void fireTableChanged(TableModelEvent tme) {
        changeSave();
        super.fireTableChanged(tme);
    }

    @Override
    public void fireTableCellUpdated(int i, int i1) {
        changeSave();
        super.fireTableCellUpdated(i, i1);
    }

    @Override
    public void fireTableRowsDeleted(int i, int i1) {
        changeSave();
        super.fireTableRowsDeleted(i, i1);
    }

    @Override
    public void fireTableRowsUpdated(int i, int i1) {
        changeSave();
        super.fireTableRowsUpdated(i, i1);
    }

    @Override
    public void fireTableRowsInserted(int i, int i1) {
        changeSave();
        super.fireTableRowsInserted(i, i1);
    }

    @Override
    public void fireTableStructureChanged() {
        changeSave();
        super.fireTableStructureChanged();
    }

    @Override
    public void fireTableDataChanged() {
        changeSave();
        super.fireTableDataChanged();
    }

    @JsonIgnore
    @Override
    public TreeNode[] getPath() {
        return (TreeNode[]) ORUtils.getPath(this).getPath();
    }

    @JsonIgnore
    @Override
    public TreePath getTreePath() {
        return ORUtils.getPath(this);
    }

    @JsonIgnore
    @Override
    public APIORPage getPage() {
        return (APIORPage) group.getParent();
    }

    @JsonIgnore
    @Override
    public <T extends EventListener> T[] getListeners(Class<T> type) {
        return super.getListeners(type);
    }

    @JsonIgnore
    @Override
    public TableModelListener[] getTableModelListeners() {
        return super.getTableModelListeners();
    }

    @JsonIgnore
    @Override
    public void addTableModelListener(TableModelListener tl) {
        super.addTableModelListener(tl);
    }

    @JsonIgnore
    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @JsonIgnore
    @Override
    public Boolean rename(String newName) {
        Boolean flag = true;
        if (getParent().getChildCount() == 1) {
            flag = getParent().rename(newName);
        }
        if (flag && getParent().getObjectByName(newName) == null) {
            // Check if using YAML format
            if (getParent().getParent().getRoot().getObjectRepository().isUsingYamlFormat()) {
                // For YAML format, objects are stored within the page YAML file
                // Just update the name and mark as needing save
                setName(newName);
                changeSave();
                return true;
            } else {
                // Use original XML folder-based rename
                if (FileUtils.renameFile(getRepLocation(), newName)) {
                    setName(newName);
                    changeSave();
                    return true;
                }
            }
        }
        return false;
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return getParent().getRepLocation() + File.separator + getName();
    }

    @JsonIgnore
    @Override
    public APIORObject clone(ORObjectInf obj) {
        if (obj instanceof APIORObject) {
            APIORObject apiObj = (APIORObject) obj;
            apiObj.getAttributes().clear();
            for (ORAttribute attribute : attributes) {
                apiObj.getAttributes().add(attribute.cloneAs());
            }
            apiObj.changeSave();
            return apiObj;
        }
        throw new UnsupportedOperationException();
    }

    @JsonIgnore
    public ORAttribute getAttribute(String name) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(name)) {
                return attribute;
            }
        }
        return null;
    }

    @JsonIgnore
    public String getAttributeByName(String name) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(name)) {
                return attribute.getValue();
            }
        }
        return "";
    }

    @JsonIgnore
    public void setAttributeByName(String name, String value) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(name)) {
                attribute.setValue(value);
            }
        }
    }

    @JsonIgnore
    public void addOrUpdateAttribute(String name, String value) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equalsIgnoreCase(name)) {
                attribute.setValue(value);
                return;
            }
        }
        ORAttribute attr = new ORAttribute();
        attr.setName(name);
        attr.setValue(value);
        attr.setPreference(String.valueOf(attributes.size() + 1));
        attributes.add(attr);
        changeSave();
    }

    @JsonIgnore
    public void addNewAttribute() {
        String attrName = "NewAttribute";
        int i = 0;
        String name;
        do {
            name = attrName + i++;
        } while (getAttribute(name) != null);
        
        ORAttribute attr = new ORAttribute();
        attr.setName(name);
        attr.setValue("");
        attr.setPreference(String.valueOf(attributes.size() + 1));
        attributes.add(attr);
        changeSave();
        fireTableRowsInserted(attributes.size() - 1, attributes.size() - 1);
    }

    @JsonIgnore
    public void removeAttribute(int attrIndex) {
        if (attrIndex >= 0 && attrIndex < attributes.size()) {
            attributes.remove(attrIndex);
            changeSave();
            fireTableRowsDeleted(attrIndex, attrIndex);
        }
    }

    @Override
    public void removeRow(int row) {
        if (row >= 0 && row < attributes.size()) {
            attributes.remove(row);
            rowDeleted(row);
            fireTableRowsDeleted(row, row);
        }
    }

    @Override
    public void insertRow(int row, Object[] values) {
        if (row < 0 || row > attributes.size()) {
            return;
        }
        
        ORAttribute attr = new ORAttribute();
        if (values != null && values.length > 0) {
            attr.setName(values[0] != null ? values[0].toString() : "");
            if (values.length > 1) {
                attr.setValue(values[1] != null ? values[1].toString() : "");
            }
        }
        attr.setPreference(String.valueOf(row + 1));
        
        attributes.add(row, attr);
        rowAdded(row);
        fireTableRowsInserted(row, row);
    }

    @JsonIgnore
    @Override
    public Boolean isEqualOf(ORObjectInf object) {
        if (object == null || !(object instanceof APIORObject)) {
            return false;
        }
        APIORObject other = (APIORObject) object;
        return Objects.equals(this.name, other.name);
    }

    @Override
    public void insertColumnAt(int colIndex, String colName, Object[] values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeColumn(int colIndex) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
