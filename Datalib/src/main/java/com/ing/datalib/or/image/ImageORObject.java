
package com.ing.datalib.or.image;

import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORUtils;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.undoredo.UndoRedoModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.List;
import java.util.Objects;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class ImageORObject extends UndoRedoModel implements ORObjectInf {

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(localName = "Property")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Property")
    private List<ORAttribute> attributes;

    @JsonIgnore
    private ObjectGroup<ImageORObject> group;

    public ImageORObject() {
        setDefaultORAttributes();
    }

    public ImageORObject(String name, ObjectGroup group) {
        this.name = name;
        this.group = group;
        setDefaultORAttributes();
    }

    @JsonIgnore
    private void setDefaultORAttributes() {
        attributes = new ArrayList<>();
        for (int i = 0; i < ImageOR.OBJECT_PROPS.size(); i++) {
            ORAttribute attr = new ORAttribute();
            attr.setName(ImageOR.OBJECT_PROPS.get(i));
            attr.setValue("");
            attr.setPreference("" + (i + 1));
            attributes.add(attr);
        }
        setPrecision("0.7f");
        setOffset("0,0");
        setRoi("0,0," + getDefault().width + "," + getDefault().height);
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
        } else {
            group.getObjects().remove(this);
            FileUtils.deleteFile(getRepLocation());
        }
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
    public String getText() {
        return getAttributeByName("Text");
    }

    @JsonIgnore
    public void setText(String text) {
        getAttribute("Text").setValue(text);
    }

    @JsonIgnore
    public Integer getIndex() {
        String index = getAttribute("Index").getValue();
        if (index.matches("[0-9]+")) {
            return Integer.valueOf(index);
        }
        return 0;
    }

    @JsonIgnore
    public void setIndex(String index) {
        getAttribute("Index").setValue(index);
    }

    @JsonIgnore
    public String getImageLocation() {
        return getAttributeByName("Url");
    }

    @JsonIgnore
    public void setImageLocation(String imageLocation) {
        getAttribute("Url").setValue(imageLocation);
    }

    @JsonIgnore
    public Point getOffset() {
        String offset = getAttributeByName("Offset");
        if (offset.matches("[+-]?[0-9]+,[+-]?[0-9]+")) {
            return new Point(
                    Integer.valueOf(offset.split(",", 2)[0]),
                    Integer.valueOf(offset.split(",", 2)[1]));
        }
        return new Point();
    }

    @JsonIgnore
    public void setOffset(String offset) {
        getAttribute("Offset").setValue(offset);
    }

    @JsonIgnore
    public Float getPrecision() {
        String index = getAttributeByName("Precision");
        if (index.matches("^([+-]?\\d*\\.?\\d*)$")) {
            return Float.valueOf(index);
        }
        return 0.7f;
    }

    @JsonIgnore
    public void setPrecision(String precision) {
        getAttribute("Precision").setValue(precision);
    }

    @JsonIgnore
    public String getReferenceImageLocation() {
        return getAttributeByName("Reference");
    }

    @JsonIgnore
    public void setReferenceImageLocation(String referenceImageLocation) {
        getAttribute("Reference").setValue(referenceImageLocation);
    }

    @JsonIgnore
    public Rectangle getCoordinates() {
        return getRect(getAttributeByName("Coordinates"));
    }

    @JsonIgnore
    public void setCoordinates(String coordinates) {
        getAttribute("Coordinates").setValue(coordinates);
    }

    @JsonIgnore
    public Rectangle getRoi() {
        return getRect(getAttributeByName("ROI"));
    }

    @JsonIgnore
    public void setRoi(String roi) {
        getAttribute("ROI").setValue(roi);
    }

    private Rectangle getRect(String val) {
        if (val.matches("[0-9]+,[0-9]+,[0-9]+,[0-9]+")) {
            String[] r = val.split(",", -1);
            return new Rectangle(
                    Integer.valueOf(r[0]),
                    Integer.valueOf(r[1]),
                    Integer.valueOf(r[2]),
                    Integer.valueOf(r[3]));
        }
        return new Rectangle(0, 0, getDefault().width, getDefault().height);
    }

    private Dimension getDefault() {
        try {
            return java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        } catch (HeadlessException ex) {
            return new Dimension(1366, 768);
        }
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
    public ObjectGroup<ImageORObject> getParent() {
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
        return 8;
    }

    @JsonIgnore
    @Override
    public int getColumnCount() {
        return 2;
    }

    @JsonIgnore
    @Override
    public Object getValueAt(int row, int column) {
        switch (column) {
            case 0:
        }
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
            if (isNotDuplicate(value)) {
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
    @Override
    public Boolean isEqualOf(ORObjectInf obj) {
        ImageORObject object = (ImageORObject) obj;
        for (ORAttribute attribute : attributes) {
            if (!Objects.equals(attribute.getValue(), object.getAttributeByName(attribute.getName()))) {
                return false;
            }
        }
        return true;
    }

    @JsonIgnore
    private Boolean isNotDuplicate(Object value) {
        for (ORAttribute attribute : attributes) {
            if (Objects.equals(attribute.getName(), value)) {
                return false;
            }
        }
        return true;
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
            ((ImageORPage) group.getParent()).getRoot().setSaved(false);
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
    public ImageORPage getPage() {
        return (ImageORPage) group.getParent();
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

    @Override
    public ImageORObject clone(ORObjectInf obj) {
        if (obj instanceof ImageORObject) {
            ImageORObject wObj = (ImageORObject) obj;
            wObj.getAttributes().clear();
            for (ORAttribute attribute : attributes) {
                wObj.getAttributes().add(attribute.cloneAs());
            }
            wObj.changeSave();
            return wObj;
        }
        throw new UnsupportedOperationException();
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
