
package com.ing.ide.main.mainui.components.testdesign.or.sap;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.datalib.or.sap.SapORPage;
import com.ing.ide.main.utils.Utils;
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
import javax.swing.table.DefaultTableModel;

public class SapORTable extends JPanel implements ActionListener {

    private final XTable table;

    private final SapORPanel sapOR;
    private final ToolBar toolBar;
    private final PopupMenu popupMenu;

    public SapORTable(SapORPanel sapOR) {
        this.sapOR = sapOR;
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

    public void loadObject(SapORObject object) {
        table.setModel(object);
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
                getObject().removeAttribute(attr);
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
            if (getObject().moveRowsUp(from, to)) {
                table.getSelectionModel().setSelectionInterval(from - 1, to - 1);
            }
        }
    }

    private void moveDown() {
        if (table.getSelectedRows().length > 0) {
            stopCellEditing();
            int from = table.getSelectedRows()[0];
            int to = table.getSelectedRows()[table.getSelectedRowCount() - 1];
            if (getObject().moveRowsDown(from, to)) {
                table.getSelectionModel().setSelectionInterval(from + 1, to + 1);
            }
        }
    }

    private List<ORObjectInf> getSelectedObjects() {
        return sapOR.getObjectTree().getSelectedObjects();
    }

    private void clearFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((SapORObject) object).setAttributeByName(attr, "");
                });
            }
        }
    }

    private void clearFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (SapORPage page : getObject().getPage().getRoot().getPages()) {
                clearFromPage(page, attrs);
            }
        }
    }

    private void clearFromPage() {
        if (table.getSelectedRowCount() > 0) {
            clearFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void clearFromPage(SapORPage page, String[] attrs) {
        for (ObjectGroup<SapORObject> objectGroup : page.getObjectGroups()) {
            for (SapORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.setAttributeByName(attr, "");
                }
            }
        }
    }

    private void removeFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (SapORPage page : getObject().getPage().getRoot().getPages()) {
                removeFromPage(page, attrs);
            }
        }
    }

    private void removeFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((SapORObject) object).removeAttribute(attr);
                });
            }
        }
    }

    private void removeFromPage() {
        if (table.getSelectedRowCount() > 0) {
            removeFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void removeFromPage(SapORPage page, String[] attrs) {
        for (ObjectGroup<SapORObject> objectGroup : page.getObjectGroups()) {
            for (SapORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.removeAttribute(attr);
                }
            }
        }
    }

    private void addToSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((SapORObject) object).addNewAttribute(attr);
                });
            }
        }
    }

    private void addToAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (SapORPage page : getObject().getPage().getRoot().getPages()) {
                addToPage(page, attrs);
            }
        }
    }

    private void addToPage() {
        if (table.getSelectedRowCount() > 0) {
            addToPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void addToPage(SapORPage page, String[] attrs) {
        for (ObjectGroup<SapORObject> objectGroup : page.getObjectGroups()) {
            for (SapORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.addNewAttribute(attr);
                }
            }
        }
    }

    private void setPriorityToAll() {
        stopCellEditing();
        SapORObject currObj = getObject();
        for (SapORPage page : getObject().getPage().getRoot().getPages()) {
            setPriorityToPage(page, currObj);
        }
    }

    private void setPriorityToSelected() {
        stopCellEditing();
        SapORObject currObj = getObject();
        getSelectedObjects().stream().forEach((object) -> {
            reorderAttributes(currObj.getAttributes(), ((SapORObject) object).getAttributes());
        });
    }

    private void setPriorityToPage() {
        stopCellEditing();
        SapORObject currObj = getObject();
        setPriorityToPage(getObject().getPage(), currObj);
    }

    private void setPriorityToPage(SapORPage page, SapORObject currObj) {
        for (ObjectGroup<SapORObject> objectGroup : page.getObjectGroups()) {
            for (SapORObject object : objectGroup.getObjects()) {
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

    public SapORObject getObject() {
        if (table.getModel() instanceof SapORObject) {
            return (SapORObject) table.getModel();
        }
        return null;
    }

    class ToolBar extends JToolBar {

        public ToolBar() {
            init();
            setBorder(BorderFactory.createEtchedBorder());
        }

        private void init() {
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
            setFloatable(false);

            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 32767)));
            JLabel label = new JLabel("Properties");
            label.setFont(new Font("Default", Font.BOLD, 12));
            add(label);

            add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767)));

            add(Utils.createButton("Add Row", "add", "Ctrl+Plus", SapORTable.this));
            add(Utils.createButton("Delete Rows", "remove", "Ctrl+Minus", SapORTable.this));
            addSeparator();
            add(Utils.createButton("Move Rows Up", "up", "Ctrl+Up", SapORTable.this));
            add(Utils.createButton("Move Rows Down", "down", "Ctrl+Down", SapORTable.this));
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

            setPriority.add(Utils.createMenuItem("Set Priority to Page", SapORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to All", SapORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to Selected", SapORTable.this));
            add(setPriority);
            clearProp.add(Utils.createMenuItem("Clear from Page", SapORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from All", SapORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from Selected", SapORTable.this));
            add(clearProp);
            deleteProp.add(Utils.createMenuItem("Remove from Page", SapORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from All", SapORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from Selected", SapORTable.this));
            add(deleteProp);
            addProp.add(Utils.createMenuItem("Add to Page", SapORTable.this));
            addProp.add(Utils.createMenuItem("Add to All", SapORTable.this));
            addProp.add(Utils.createMenuItem("Add to Selected", SapORTable.this));
            add(addProp);
        }

    }
}
