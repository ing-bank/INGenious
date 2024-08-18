
package com.ing.ide.main.mainui.components.testdesign.or.mobile;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
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

public class MobileORTable extends JPanel implements ActionListener {

    private final XTable table;

    private final MobileORPanel mobileOR;
    private final ToolBar toolBar;
    private final PopupMenu popupMenu;

    public MobileORTable(MobileORPanel mobileOR) {
        this.mobileOR = mobileOR;
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

    public void loadObject(MobileORObject object) {
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
        return mobileOR.getObjectTree().getSelectedObjects();
    }

    private void clearFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((MobileORObject) object).setAttributeByName(attr, "");
                });
            }
        }
    }

    private void clearFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (MobileORPage page : getObject().getPage().getRoot().getPages()) {
                clearFromPage(page, attrs);
            }
        }
    }

    private void clearFromPage() {
        if (table.getSelectedRowCount() > 0) {
            clearFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void clearFromPage(MobileORPage page, String[] attrs) {
        for (ObjectGroup<MobileORObject> objectGroup : page.getObjectGroups()) {
            for (MobileORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.setAttributeByName(attr, "");
                }
            }
        }
    }

    private void removeFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (MobileORPage page : getObject().getPage().getRoot().getPages()) {
                removeFromPage(page, attrs);
            }
        }
    }

    private void removeFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((MobileORObject) object).removeAttribute(attr);
                });
            }
        }
    }

    private void removeFromPage() {
        if (table.getSelectedRowCount() > 0) {
            removeFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void removeFromPage(MobileORPage page, String[] attrs) {
        for (ObjectGroup<MobileORObject> objectGroup : page.getObjectGroups()) {
            for (MobileORObject object : objectGroup.getObjects()) {
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
                    ((MobileORObject) object).addNewAttribute(attr);
                });
            }
        }
    }

    private void addToAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (MobileORPage page : getObject().getPage().getRoot().getPages()) {
                addToPage(page, attrs);
            }
        }
    }

    private void addToPage() {
        if (table.getSelectedRowCount() > 0) {
            addToPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void addToPage(MobileORPage page, String[] attrs) {
        for (ObjectGroup<MobileORObject> objectGroup : page.getObjectGroups()) {
            for (MobileORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.addNewAttribute(attr);
                }
            }
        }
    }

    private void setPriorityToAll() {
        stopCellEditing();
        MobileORObject currObj = getObject();
        for (MobileORPage page : getObject().getPage().getRoot().getPages()) {
            setPriorityToPage(page, currObj);
        }
    }

    private void setPriorityToSelected() {
        stopCellEditing();
        MobileORObject currObj = getObject();
        getSelectedObjects().stream().forEach((object) -> {
            reorderAttributes(currObj.getAttributes(), ((MobileORObject) object).getAttributes());
        });
    }

    private void setPriorityToPage() {
        stopCellEditing();
        MobileORObject currObj = getObject();
        setPriorityToPage(getObject().getPage(), currObj);
    }

    private void setPriorityToPage(MobileORPage page, MobileORObject currObj) {
        for (ObjectGroup<MobileORObject> objectGroup : page.getObjectGroups()) {
            for (MobileORObject object : objectGroup.getObjects()) {
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

    public MobileORObject getObject() {
        if (table.getModel() instanceof MobileORObject) {
            return (MobileORObject) table.getModel();
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

            add(Utils.createButton("Add Row", "add", "Ctrl+Plus", MobileORTable.this));
            add(Utils.createButton("Delete Rows", "remove", "Ctrl+Minus", MobileORTable.this));
            addSeparator();
            add(Utils.createButton("Move Rows Up", "up", "Ctrl+Up", MobileORTable.this));
            add(Utils.createButton("Move Rows Down", "down", "Ctrl+Down", MobileORTable.this));
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

            setPriority.add(Utils.createMenuItem("Set Priority to Page", MobileORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to All", MobileORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to Selected", MobileORTable.this));
            add(setPriority);
            clearProp.add(Utils.createMenuItem("Clear from Page", MobileORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from All", MobileORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from Selected", MobileORTable.this));
            add(clearProp);
            deleteProp.add(Utils.createMenuItem("Remove from Page", MobileORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from All", MobileORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from Selected", MobileORTable.this));
            add(deleteProp);
            addProp.add(Utils.createMenuItem("Add to Page", MobileORTable.this));
            addProp.add(Utils.createMenuItem("Add to All", MobileORTable.this));
            addProp.add(Utils.createMenuItem("Add to Selected", MobileORTable.this));
            add(addProp);
        }

    }
}
