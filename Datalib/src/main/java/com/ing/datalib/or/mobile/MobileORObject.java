package com.ing.datalib.or.mobile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORUtils;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.undoredo.UndoRedoModel;
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
 * Represents a single mobile object inside a MobileOR page.
 * <p>
 * Each MobileORObject carries two independent attribute lists – one for
 * Android locators and one for iOS locators. The IDE Properties view toggles
 * between the two; at execution time the Engine picks the appropriate list
 * based on the Appium driver in use.
 * <p>
 * The {@link javax.swing.table.TableModel} contract is implemented against
 * the currently <em>active</em> platform (see {@link #setActivePlatform}).
 */
public class MobileORObject extends UndoRedoModel implements ORObjectInf {
    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    /** Android locator attributes – serialised as &lt;AndroidProperty&gt; elements. */
    @JacksonXmlProperty(localName = "AndroidProperty")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "AndroidProperty")
    private List<ORAttribute> androidAttributes;

    /** iOS locator attributes – serialised as &lt;IOSProperty&gt; elements. */
    @JacksonXmlProperty(localName = "IOSProperty")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "IOSProperty")
    private List<ORAttribute> iosAttributes;

    @JsonIgnore
    private ObjectGroup<MobileORObject> group;

    @JsonIgnore
    private MobilePlatform activePlatform = MobilePlatform.ANDROID;

    public MobileORObject() {
        seedDefaults();
    }

    public MobileORObject(String name, ObjectGroup group) {
        this.name = name;
        this.group = group;
        seedDefaults();
    }

    @JsonIgnore
    private void seedDefaults() {
        androidAttributes = buildDefaults(MobileOR.ANDROID_PROPS);
        iosAttributes = buildDefaults(MobileOR.IOS_PROPS);
    }

    @JsonIgnore
    private static List<ORAttribute> buildDefaults(List<String> propNames) {
        List<ORAttribute> list = new ArrayList<>();
        for (int i = 0; i < propNames.size(); i++) {
            ORAttribute attr = new ORAttribute();
            attr.setName(propNames.get(i));
            attr.setValue("");
            attr.setPreference("" + (i + 1));
            list.add(attr);
        }
        return list;
    }

    // ===================================================================
    // Active-platform (TableModel view) management
    // ===================================================================

    @JsonIgnore
    public MobilePlatform getActivePlatform() {
        return activePlatform;
    }

    /**
     * Switches which platform's attributes the {@link javax.swing.table.TableModel}
     * contract operates on. Fires {@link #fireTableStructureChanged()} so any
     * bound JTable refreshes.
     */
    @JsonIgnore
    public void setActivePlatform(MobilePlatform platform) {
        if (platform != null && this.activePlatform != platform) {
            this.activePlatform = platform;
            super.fireTableDataChanged();
        }
    }

    @JsonIgnore
    private List<ORAttribute> activeList() {
        return getAttributes(activePlatform);
    }

    // ===================================================================
    // Platform-aware attribute access
    // ===================================================================

    /**
     * Returns the attribute list for the currently active platform.
     * Use {@link #getAttributes(MobilePlatform)} when the caller knows
     * the target platform.
     */
    public List<ORAttribute> getAttributes() {
        return activeList();
    }

    public List<ORAttribute> getAttributes(MobilePlatform platform) {
        if (platform == MobilePlatform.IOS) {
            if (iosAttributes == null) {
                iosAttributes = buildDefaults(MobileOR.IOS_PROPS);
            }
            return iosAttributes;
        }
        if (androidAttributes == null) {
            androidAttributes = buildDefaults(MobileOR.ANDROID_PROPS);
        }
        return androidAttributes;
    }

    public void setAttributes(List<ORAttribute> attributes) {
        setAttributes(activePlatform, attributes);
    }

    public void setAttributes(MobilePlatform platform, List<ORAttribute> attributes) {
        if (platform == MobilePlatform.IOS) {
            this.iosAttributes = attributes;
        } else {
            this.androidAttributes = attributes;
        }
    }

    public List<ORAttribute> getAndroidAttributes() {
        return getAttributes(MobilePlatform.ANDROID);
    }

    public void setAndroidAttributes(List<ORAttribute> attributes) {
        this.androidAttributes = attributes;
    }

    public List<ORAttribute> getIosAttributes() {
        return getAttributes(MobilePlatform.IOS);
    }

    public void setIosAttributes(List<ORAttribute> attributes) {
        this.iosAttributes = attributes;
    }

    /**
     * Backward compatibility: legacy repositories serialise locators inside
     * a single &lt;Property&gt; list. When Jackson finds one we seed both
     * platform lists from it so existing object repositories keep working.
     */
    @JsonSetter("Property")
    @JacksonXmlProperty(localName = "Property")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Property")
    public void setLegacyAttributes(List<ORAttribute> legacy) {
        if (legacy == null || legacy.isEmpty()) {
            return;
        }
        androidAttributes = buildFromLegacy(legacy, MobileOR.ANDROID_PROPS);
        iosAttributes = buildFromLegacy(legacy, MobileOR.IOS_PROPS);
    }

    @JsonIgnore
    private static List<ORAttribute> buildFromLegacy(
        List<ORAttribute> legacy,
        List<String> defaults
    ) {
        List<ORAttribute> result = buildDefaults(defaults);
        for (ORAttribute src : legacy) {
            if (src == null || src.getName() == null) {
                continue;
            }
            ORAttribute target = findByName(result, src.getName());
            if (target != null) {
                target.setValue(src.getValue() == null ? "" : src.getValue());
                if (src.getPreference() != null && !src.getPreference().isEmpty()) {
                    target.setPreference(src.getPreference());
                }
                target.setExact(src.isExact());
            } else if (!defaults.contains(src.getName())) {
                // Custom (user-added) attribute – keep on both platforms.
                result.add(src.cloneAs());
            }
        }
        return result;
    }

    @JsonIgnore
    private static ORAttribute findByName(List<ORAttribute> list, String name) {
        for (ORAttribute a : list) {
            if (Objects.equals(a.getName(), name)) {
                return a;
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void removeFromParent() {
        changeSave();
        if (group.getObjects().size() == 1) {
            group.removeFromParent();
        }
        group.getObjects().remove(this);
        if (!group.getParent().getRoot().getObjectRepository().isUsingYamlFormat()) {
            FileUtils.deleteFile(getRepLocation());
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
    public ObjectGroup<MobileORObject> getParent() {
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
        return activeList().size();
    }

    @JsonIgnore
    @Override
    public int getColumnCount() {
        return 2;
    }

    @JsonIgnore
    @Override
    public Object getValueAt(int row, int column) {
        List<ORAttribute> attrs = activeList();
        if (row < 0 || row >= attrs.size()) {
            return null;
        }
        if (column == 0) {
            return attrs.get(row).getName();
        } else if (column == 1) {
            return attrs.get(row).getValue();
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        List<ORAttribute> attrs = activeList();
        if (rowIndex < 0 || rowIndex >= attrs.size()) {
            return;
        }
        ORAttribute attr = attrs.get(rowIndex);
        if (columnIndex == 0) {
            if (isNotDuplicate(activePlatform, value)) {
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
    private Boolean isNotDuplicate(MobilePlatform platform, Object value) {
        for (ORAttribute attribute : getAttributes(platform)) {
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
            MobileORPage page = (MobileORPage) group.getParent();
            page.getRoot().setSaved(false);

            // Auto-save for YAML format
            if (
                page.getRoot().getObjectRepository() != null &&
                page.getRoot().getObjectRepository().isUsingYamlFormat()
            ) {
                page.getRoot().getObjectRepository().saveMobilePageNow(page);
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

    @Override
    public MobileORPage getPage() {
        return (MobileORPage) group.getParent();
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
        if (getParent().getChildCount() == 1) {
            getParent().rename(newName);
        }
        if (newName == null || newName.isBlank()) {
            return false;
        }
        ORObjectInf existing = getParent().getObjectByName(newName);
        if (existing != null && existing != this) {
            return false;
        }
        setName(newName);
        changeSave();
        return true;
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return getParent().getRepLocation() + File.separator + getName();
    }

    @JsonIgnore
    @Override
    public MobileORObject clone(ORObjectInf obj) {
        if (obj instanceof MobileORObject) {
            MobileORObject wObj = (MobileORObject) obj;
            wObj.getAttributes(MobilePlatform.ANDROID).clear();
            for (ORAttribute attribute : getAttributes(MobilePlatform.ANDROID)) {
                wObj.getAttributes(MobilePlatform.ANDROID).add(attribute.cloneAs());
            }
            wObj.getAttributes(MobilePlatform.IOS).clear();
            for (ORAttribute attribute : getAttributes(MobilePlatform.IOS)) {
                wObj.getAttributes(MobilePlatform.IOS).add(attribute.cloneAs());
            }
            wObj.changeSave();
            return wObj;
        }
        throw new UnsupportedOperationException();
    }

    // ===================================================================
    // Convenience accessors – operate on the active platform list.
    // ===================================================================

    @JsonIgnore
    public String getId() {
        return getAttributeByName("id");
    }

    @JsonIgnore
    public String getNameAttr() {
        return getAttributeByName("name");
    }

    @JsonIgnore
    public String getClassName() {
        return getAttributeByName("class");
    }

    @JsonIgnore
    public String getXpath() {
        return getAttributeByName("xpath");
    }

    @JsonIgnore
    public String getAccessibility() {
        return getAttributeByName("Accessibility");
    }

    @JsonIgnore
    public String getUiAutomator() {
        return getAttributeByName(MobilePlatform.ANDROID, "UiAutomator");
    }

    @JsonIgnore
    public String getUiAutomation() {
        return getAttributeByName(MobilePlatform.IOS, "UiAutomation");
    }

    @JsonIgnore
    public String getAttributeByName(String attr) {
        return getAttributeByName(activePlatform, attr);
    }

    @JsonIgnore
    public String getAttributeByName(MobilePlatform platform, String attr) {
        for (ORAttribute attribute : getAttributes(platform)) {
            if (attribute.getName().equals(attr)) {
                return attribute.getValue();
            }
        }
        return null;
    }

    @JsonIgnore
    public ORAttribute getAttribute(String attr) {
        return getAttribute(activePlatform, attr);
    }

    @JsonIgnore
    public ORAttribute getAttribute(MobilePlatform platform, String attr) {
        for (ORAttribute attribute : getAttributes(platform)) {
            if (attribute.getName().equals(attr)) {
                return attribute;
            }
        }
        return null;
    }

    @JsonIgnore
    public void setId(String val) {
        setAttributeOnBothPlatforms("id", val);
    }

    @JsonIgnore
    public void setNameAttr(String val) {
        setAttributeOnBothPlatforms("name", val);
    }

    @JsonIgnore
    public void setClassName(String val) {
        setAttributeOnBothPlatforms("class", val);
    }

    @JsonIgnore
    public void setXpath(String val) {
        setAttributeOnBothPlatforms("xpath", val);
    }

    @JsonIgnore
    public void setAccessibility(String val) {
        setAttributeOnBothPlatforms("Accessibility", val);
    }

    /**
     * Writes a platform-agnostic locator (id / xpath / class / name / Accessibility)
     * to BOTH the Android and iOS attribute lists. The Mobile Object Spy and other
     * capture flows use the convenience setters above, and a single captured value
     * is expected to identify the element on either platform.
     */
    @JsonIgnore
    private void setAttributeOnBothPlatforms(String attr, String val) {
        setAttributeByName(MobilePlatform.ANDROID, attr, val);
        setAttributeByName(MobilePlatform.IOS, attr, val);
    }

    @JsonIgnore
    public void setUiAutomator(String val) {
        setAttributeByName(MobilePlatform.ANDROID, "UiAutomator", val);
    }

    @JsonIgnore
    public void setUiAutomation(String val) {
        setAttributeByName(MobilePlatform.IOS, "UiAutomation", val);
    }

    @JsonIgnore
    public void setAttributeByName(String attr, String val) {
        setAttributeByName(activePlatform, attr, val);
    }

    @JsonIgnore
    public void setAttributeByName(MobilePlatform platform, String attr, String val) {
        List<ORAttribute> attrs = getAttributes(platform);
        for (ORAttribute attribute : attrs) {
            if (attribute.getName().equals(attr)) {
                attribute.setValue(val);
                if (platform == activePlatform) {
                    fireTableCellUpdated(attrs.indexOf(attribute), 1);
                } else {
                    changeSave();
                }
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
        addNewAttribute(activePlatform, attrName);
    }

    @JsonIgnore
    public void addNewAttribute(MobilePlatform platform, String attrName) {
        if (getAttribute(platform, attrName) == null) {
            List<ORAttribute> attrs = getAttributes(platform);
            attrs.add(new ORAttribute(attrName, attrs.size()));
            if (platform == activePlatform) {
                super.rowAdded(attrs.size() - 1);
                fireTableRowsInserted(attrs.size() - 1, attrs.size() - 1);
            } else {
                changeSave();
            }
        }
    }

    @JsonIgnore
    public void removeAttribute(String attrName) {
        removeAttribute(activePlatform, attrName);
    }

    @JsonIgnore
    public void removeAttribute(MobilePlatform platform, String attrName) {
        List<String> defaults = MobileOR.defaultPropsFor(platform);
        if (defaults.indexOf(attrName) == -1) {
            ORAttribute existing = getAttribute(platform, attrName);
            if (existing != null) {
                List<ORAttribute> attrs = getAttributes(platform);
                int index = attrs.indexOf(existing);
                if (platform == activePlatform) {
                    super.rowDeleted(index);
                    attrs.remove(index);
                    fireTableRowsDeleted(index, index);
                } else {
                    attrs.remove(index);
                    changeSave();
                }
            }
        }
    }

    @JsonIgnore
    public Boolean moveRowsUp(int from, int to) {
        List<ORAttribute> attrs = activeList();
        if (from - 1 < 0) {
            return false;
        }
        to = to + 1;
        Collections.rotate(attrs.subList(from - 1, to), -1);
        changeSave();
        return true;
    }

    @JsonIgnore
    public Boolean moveRowsDown(int from, int to) {
        List<ORAttribute> attrs = activeList();
        if (to + 1 > attrs.size() - 1) {
            return false;
        }
        to += 1;
        Collections.rotate(attrs.subList(from, to + 1), 1);
        changeSave();
        return true;
    }

    @JsonIgnore
    @Override
    public Boolean isEqualOf(ORObjectInf obj) {
        MobileORObject object = (MobileORObject) obj;
        for (ORAttribute attribute : getAttributes(MobilePlatform.ANDROID)) {
            if (
                !Objects.equals(
                    attribute.getValue(),
                    object.getAttributeByName(MobilePlatform.ANDROID, attribute.getName())
                )
            ) {
                return false;
            }
        }
        for (ORAttribute attribute : getAttributes(MobilePlatform.IOS)) {
            if (
                !Objects.equals(
                    attribute.getValue(),
                    object.getAttributeByName(MobilePlatform.IOS, attribute.getName())
                )
            ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void removeRow(int index) {
        List<ORAttribute> attrs = activeList();
        super.rowDeleted(index);
        attrs.remove(index);
        fireTableRowsDeleted(index, index);
    }

    @Override
    public void insertRow(int row, Object[] values) {
        ORAttribute attr = new ORAttribute(values[0].toString(), row);
        attr.setValue(values[1].toString());
        activeList().add(row, attr);
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
