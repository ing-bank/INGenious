package com.ing.ide.main.mainui.components.testdesign.or.api;

import com.ing.datalib.or.api.APIOR;
import com.ing.datalib.or.api.APIORObject;
import com.ing.datalib.or.api.APIORPage;
import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.PropertyAttributeRenderer;
import com.ing.ide.main.utils.table.XTable;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * Table component for API Object Repository properties.
 * Shows JsonPath and Xpath attributes (2 columns: Attribute, Value).
 */
public class APIORTable extends JPanel implements ActionListener {

    private final XTable table;

    private final APIORPanel apiOR;
    private final ToolBar toolBar;
    private final PopupMenu popupMenu;

    public APIORTable(APIORPanel apiOR) {
        this.apiOR = apiOR;
        table = new XTable();
        toolBar = new ToolBar();
        popupMenu = new PopupMenu();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
        add(toolBar, BorderLayout.NORTH);
        table.setComponentPopupMenu(popupMenu);
    }

    public XTable getTable() {
        return table;
    }

    public void loadObject(APIORObject object) {
        table.setModel(object);
        configureColumns();
    }

    private void configureColumns() {
        if (table.getColumnCount() >= 2) {
            // Column 0: Attribute - narrow width
            TableColumn attrCol = table.getColumnModel().getColumn(0);
            attrCol.setCellRenderer(new PropertyAttributeRenderer());
            attrCol.setPreferredWidth(100);
            attrCol.setMinWidth(80);
            attrCol.setMaxWidth(150);
            
            // Column 1: Value - takes remaining space
            TableColumn valueCol = table.getColumnModel().getColumn(1);
            valueCol.setPreferredWidth(300);
        }
    }

    public void reset() {
        table.setModel(new DefaultTableModel());
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (getObject() != null) {
            switch (ae.getActionCommand()) {
                case "Add Row":
                    addRow();
                    break;
                case "Delete Rows":
                    removeRow();
                    break;
                case "Move Rows Up":
                    moveUp();
                    break;
                case "Move Rows Down":
                    moveDown();
                    break;
                case "Clear from Page":
                    clearFromPage();
                    break;
                case "Clear from All":
                    clearFromAll();
                    break;
                case "Clear from Selected":
                    clearFromSelected();
                    break;
                case "Remove from Page":
                    removeFromPage();
                    break;
                case "Remove from All":
                    removeFromAll();
                    break;
                case "Remove from Selected":
                    removeFromSelected();
                    break;
                case "Add to Page":
                    addToPage();
                    break;
                case "Add to All":
                    addToAll();
                    break;
                case "Add to Selected":
                    addToSelected();
                    break;
                case "Set Priority to Page":
                    setPriorityToPage();
                    break;
                case "Set Priority to All":
                    setPriorityToAll();
                    break;
                case "Set Priority to Selected":
                    setPriorityToSelected();
                    break;
            }
        }
    }

    private void addRow() {
        stopCellEditing();
        getObject().addNewAttribute();
    }

    private void removeRow() {
        if (table.getSelectedRows().length > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getObject().removeAttribute(getObject().getAttribute(attr) != null ? 
                    getObject().getAttributes().indexOf(getObject().getAttribute(attr)) : -1);
            }
        }
    }

    private String[] getSelectedAttrs() {
        stopCellEditing();
        String[] attrs = new String[table.getSelectedRows().length];
        for (int i = 0; i < table.getSelectedRows().length; i++) {
            attrs[i] = table.getValueAt(table.getSelectedRows()[i], 0).toString();
        }
        return attrs;
    }

    private void moveUp() {
        if (table.getSelectedRows().length > 0) {
            stopCellEditing();
            int from = table.getSelectedRows()[0];
            int to = table.getSelectedRows()[table.getSelectedRowCount() - 1];
            if (moveRowsUp(from, to)) {
                table.getSelectionModel().setSelectionInterval(from - 1, to - 1);
            }
        }
    }

    private boolean moveRowsUp(int from, int to) {
        if (from > 0) {
            List<ORAttribute> attrs = getObject().getAttributes();
            for (int i = from; i <= to; i++) {
                Collections.swap(attrs, i, i - 1);
            }
            table.repaint();
            return true;
        }
        return false;
    }

    private void moveDown() {
        if (table.getSelectedRows().length > 0) {
            stopCellEditing();
            int from = table.getSelectedRows()[0];
            int to = table.getSelectedRows()[table.getSelectedRowCount() - 1];
            if (moveRowsDown(from, to)) {
                table.getSelectionModel().setSelectionInterval(from + 1, to + 1);
            }
        }
    }

    private boolean moveRowsDown(int from, int to) {
        List<ORAttribute> attrs = getObject().getAttributes();
        if (to < attrs.size() - 1) {
            for (int i = to; i >= from; i--) {
                Collections.swap(attrs, i, i + 1);
            }
            table.repaint();
            return true;
        }
        return false;
    }

    private List<ORObjectInf> getSelectedObjects() {
        return apiOR.getObjectTree().getSelectedObjects();
    }

    private void clearFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((APIORObject) object).setAttributeByName(attr, "");
                });
            }
        }
    }

    private void clearFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (APIORPage page : getObject().getPage().getRoot().getPages()) {
                clearFromPage(page, attrs);
            }
        }
    }

    private void clearFromPage() {
        if (table.getSelectedRowCount() > 0) {
            clearFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void clearFromPage(APIORPage page, String[] attrs) {
        for (ObjectGroup<APIORObject> objectGroup : page.getObjectGroups()) {
            for (APIORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.setAttributeByName(attr, "");
                }
            }
        }
    }

    private void removeFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (APIORPage page : getObject().getPage().getRoot().getPages()) {
                removeFromPage(page, attrs);
            }
        }
    }

    private void removeFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    APIORObject apiObj = (APIORObject) object;
                    ORAttribute orAttr = apiObj.getAttribute(attr);
                    if (orAttr != null) {
                        apiObj.removeAttribute(apiObj.getAttributes().indexOf(orAttr));
                    }
                });
            }
        }
    }

    private void removeFromPage() {
        if (table.getSelectedRowCount() > 0) {
            removeFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void removeFromPage(APIORPage page, String[] attrs) {
        for (ObjectGroup<APIORObject> objectGroup : page.getObjectGroups()) {
            for (APIORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    ORAttribute orAttr = object.getAttribute(attr);
                    if (orAttr != null) {
                        object.removeAttribute(object.getAttributes().indexOf(orAttr));
                    }
                }
            }
        }
    }

    private void addToSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((APIORObject) object).addOrUpdateAttribute(attr, "");
                });
            }
        }
    }

    private void addToAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (APIORPage page : getObject().getPage().getRoot().getPages()) {
                addToPage(page, attrs);
            }
        }
    }

    private void addToPage() {
        if (table.getSelectedRowCount() > 0) {
            addToPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void addToPage(APIORPage page, String[] attrs) {
        for (ObjectGroup<APIORObject> objectGroup : page.getObjectGroups()) {
            for (APIORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.addOrUpdateAttribute(attr, "");
                }
            }
        }
    }

    private void setPriorityToAll() {
        stopCellEditing();
        APIORObject currObj = getObject();
        for (APIORPage page : getObject().getPage().getRoot().getPages()) {
            setPriorityToPage(page, currObj);
        }
    }

    private void setPriorityToSelected() {
        stopCellEditing();
        APIORObject currObj = getObject();
        getSelectedObjects().stream().forEach((object) -> {
            reorderAttributes(currObj.getAttributes(), ((APIORObject) object).getAttributes());
        });
    }

    private void setPriorityToPage() {
        stopCellEditing();
        APIORObject currObj = getObject();
        setPriorityToPage(getObject().getPage(), currObj);
    }

    private void setPriorityToPage(APIORPage page, APIORObject currObj) {
        for (ObjectGroup<APIORObject> objectGroup : page.getObjectGroups()) {
            for (APIORObject object : objectGroup.getObjects()) {
                reorderAttributes(currObj.getAttributes(), object.getAttributes());
            }
        }
    }

    private void reorderAttributes(List<ORAttribute> source, List<ORAttribute> dest) {
        for (int i = 0, c = 0; i < source.size(); i++) {
            ORAttribute val = source.get(i);
            for (int j = c; j < dest.size(); j++) {
                if (dest.get(j).getName().equals(val.getName())) {
                    Collections.swap(dest, c++, j);
                    break;
                }
            }
        }
    }

    private void stopCellEditing() {
        if (table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
    }

    public APIORObject getObject() {
        if (table.getModel() instanceof APIORObject) {
            return (APIORObject) table.getModel();
        }
        return null;
    }

    class ToolBar extends JToolBar {

        public ToolBar() {
            init();
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
        }

        private void init() {
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
            setFloatable(false);
            setOpaque(false);

            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 32767)));
            JLabel label = new JLabel("Properties");
            label.setFont(new Font("Default", Font.BOLD, 12));
            add(label);

            add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767)));

            add(Utils.createButton("Add Row", "add", "Ctrl+Plus", APIORTable.this));
            add(Utils.createButton("Delete Rows", "remove", "Ctrl+Minus", APIORTable.this));
            addSeparator();
            add(Utils.createButton("Move Rows Up", "up", "Ctrl+Up", APIORTable.this));
            add(Utils.createButton("Move Rows Down", "down", "Ctrl+Down", APIORTable.this));
        }

    }

    class PopupMenu extends JPopupMenu {

        public PopupMenu() {
            init();
        }

        private void init() {
            JMenu setPriority = new JMenu("Set Priority");
            JMenu addProp = new JMenu("Add Property");
            JMenu clearProp = new JMenu("Clear Property");
            JMenu deleteProp = new JMenu("Remove Property");

            setPriority.add(Utils.createMenuItem("Set Priority to Page", APIORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to All", APIORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to Selected", APIORTable.this));
            add(setPriority);
            clearProp.add(Utils.createMenuItem("Clear from Page", APIORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from All", APIORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from Selected", APIORTable.this));
            add(clearProp);
            deleteProp.add(Utils.createMenuItem("Remove from Page", APIORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from All", APIORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from Selected", APIORTable.this));
            add(deleteProp);
            addProp.add(Utils.createMenuItem("Add to Page", APIORTable.this));
            addProp.add(Utils.createMenuItem("Add to All", APIORTable.this));
            addProp.add(Utils.createMenuItem("Add to Selected", APIORTable.this));
            add(addProp);
        }

    }
}
