
package com.ing.datalib.or.web;

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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebORObject extends UndoRedoModel implements ORObjectInf {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(isAttribute = true)
    private String frame;

    @JacksonXmlProperty(localName = "Property")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Property")
    private List<ORAttribute> attributes;

    @JsonIgnore
    private ObjectGroup<WebORObject> group;

    /**
     * For ObjectHeal purpose
     */
    @JsonIgnore
    private Object found;

    public WebORObject() {
        setDefaultORAttributes();
    }

    public WebORObject(String name, ObjectGroup group) {
        this.name = name;
        this.group = group;
        setDefaultORAttributes();
    }

    @JsonIgnore
    public final void setDefaultORAttributes() {
        attributes = new ArrayList<>();
        for (int i = 0; i < WebOR.OBJECT_PROPS.size(); i++) {
            ORAttribute attr = new ORAttribute();
            attr.setName(WebOR.OBJECT_PROPS.get(i));
            attr.setValue("");
            attr.setPreference("" + (i + 1));
            attributes.add(attr);
        }
        setFrame("");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getFrame() {
        return frame;
    }

    public void setFrame(String frame) {
        this.frame = frame;
        changeSave();
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
    public ObjectGroup<WebORObject> getParent() {
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
        return 2;
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
        return WebOR.OBJECT_PROPS.indexOf(value) == -1;
    }

    @JsonIgnore
    @Override
    public boolean isCellEditable(int i, int i1) {
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
            ((WebORPage) group.getParent()).getRoot().setSaved(false);
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
    public WebORPage getPage() {
        return (WebORPage) group.getParent();
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
    public Class<?> getColumnClass(int i) {
        return super.getColumnClass(i);
    }

    @JsonIgnore
    @Override
    public Boolean rename(String newName) {
        Boolean flag = true;
        if (getParent().getChildCount() == 1) {
            flag = getParent().rename(newName);
        }
        if (flag && getParent().getObjectByName(newName) == null) {
            if (FileUtils.renameFile(getRepLocation(), newName)) {
                setName(newName);
                changeSave();
                return true;
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
    public WebORObject clone(ORObjectInf obj) {
        if (obj instanceof WebORObject) {
            WebORObject wObj = (WebORObject) obj;
            wObj.setFrame(frame);
            wObj.getAttributes().clear();
            for (ORAttribute attribute : attributes) {
                wObj.getAttributes().add(attribute.cloneAs());
            }
            wObj.changeSave();
            return wObj;
        }
        throw new UnsupportedOperationException();
    }

    @JsonIgnore
    public String getId() {
        return getAttributeByName("id");
    }

    @JsonIgnore
    public String getNameAttr() {
        return getAttributeByName("name");
    }

    @JsonIgnore
    public String getLinkText() {
        return getAttributeByName("link_text");
    }

    @JsonIgnore
    public String getClassName() {
        return getAttributeByName("class");
    }

    @JsonIgnore
    public String getCss() {
        return getAttributeByName("css");
    }

    @JsonIgnore
    public String getXpath() {
        return getAttributeByName("xpath");
    }

    @JsonIgnore
    public String getRelativeXpath() {
        return getAttributeByName("relative_xpath");
    }

    @JsonIgnore
    public String getType() {
        return getAttributeByName("type");
    }
    
    @JsonIgnore
    public String getNLPlocator() {
        return getAttributeByName("NLP_locator");
    }
    
    @JsonIgnore
    public String getUserdefinedLocator() {
        return getAttributeByName("user_defined_locator");
    }

    @JsonIgnore
    public String getAttributeByName(String attr) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equals(attr)) {
                return attribute.getValue();
            }
        }
        return null;
    }

    @JsonIgnore
    public ORAttribute getAttribute(String attr) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equals(attr)) {
                return attribute;
            }
        }
        return null;
    }

    @JsonIgnore
    public void setId(String val) {
        setAttributeByName("id", val);
    }

    @JsonIgnore
    public void setNameAttr(String val) {
        setAttributeByName("name", val);
    }

    @JsonIgnore
    public void setLinkText(String val) {
        setAttributeByName("link_text", val);
    }

    @JsonIgnore
    public void setClassName(String val) {
        setAttributeByName("class", val);
    }

    @JsonIgnore
    public void setCss(String val) {
        setAttributeByName("css", val);
    }

    @JsonIgnore
    public void setXpath(String val) {
        setAttributeByName("xpath", val);
    }

    @JsonIgnore
    public void setRelativeXpath(String val) {
        setAttributeByName("relative_xpath", val);
    }

    @JsonIgnore
    public void setType(String val) {
        setAttributeByName("type", val);
    }
    
    @JsonIgnore
    public void setNLPlocator(String val) {
        setAttributeByName("NLP_locator", val);
    }
    
    @JsonIgnore
    public void setUserdefinedLocator(String val) {
        setAttributeByName("user_defined_locator", val);
    }

    @JsonIgnore
    public void setAttributeByName(String attr, String val) {
        for (ORAttribute attribute : attributes) {
            if (attribute.getName().equals(attr)) {
                attribute.setValue(val);
                fireTableCellUpdated(attributes.indexOf(attribute), 1);
            }
        }
    }

    @JsonIgnore
    public void addNewAttribute() {
        String newAttrName = "NewProp";
        int i = 1;
        while (getAttribute(newAttrName) != null) {
            newAttrName = "NewProp" + i++;
        }
        addNewAttribute(newAttrName);
    }

    @JsonIgnore
    public void addNewAttribute(String attrName) {
        if (getAttribute(attrName) == null) {
            attributes.add(new ORAttribute(attrName, attributes.size()));
            super.rowAdded(attributes.size() - 1);
            fireTableRowsInserted(attributes.size() - 1, attributes.size() - 1);
        }
    }

    @JsonIgnore
    public void removeAttribute(String attrName) {
        if (WebOR.OBJECT_PROPS.indexOf(attrName) == -1) {
            if (getAttribute(attrName) != null) {
                int index = attributes.indexOf(getAttribute(attrName));
                super.rowDeleted(index);
                attributes.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
    }

    @JsonIgnore
    public Boolean moveRowsUp(int from, int to) {
        if (from - 1 < 0) {
            return false;
        }
        to = to + 1;
        Collections.rotate(attributes.subList(from - 1, to), -1);
        changeSave();
        return true;
    }

    @JsonIgnore
    public Boolean moveRowsDown(int from, int to) {
        if (to + 1 > attributes.size() - 1) {
            return false;
        }
        to += 1;
        Collections.rotate(attributes.subList(from, to + 1), 1);
        changeSave();
        return true;
    }

    @JsonIgnore
    @Override
    public Boolean isEqualOf(ORObjectInf obj) {
        WebORObject object = (WebORObject) obj;
        if (frame.equals(object.getFrame())) {
            for (ORAttribute attribute : attributes) {
                if (!Objects.equals(attribute.getValue(), object.getAttributeByName(attribute.getName()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @JsonIgnore
    public Object getFound() {
        return found;
    }

    @JsonIgnore
    public void setFound(Object found) {
        this.found = found;
    }

    @Override
    public void removeRow(int index) {
        super.rowDeleted(index);
        attributes.remove(index);
        fireTableRowsDeleted(index, index);
    }

    @Override
    public void insertRow(int row, Object[] values) {
        ORAttribute attr = new ORAttribute(values[0].toString(), row);
        attr.setValue(values[1].toString());
        attributes.add(row, attr);
        fireTableRowsInserted(row, row);
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
