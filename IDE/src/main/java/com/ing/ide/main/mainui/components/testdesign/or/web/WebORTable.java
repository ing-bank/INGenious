
package com.ing.ide.main.mainui.components.testdesign.or.web;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.PropertyAttributeRenderer;
import com.ing.ide.main.utils.table.RoleCellEditor;
import com.ing.ide.main.utils.table.XTable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/**
 *
 * 
 */
public class WebORTable extends JPanel implements ActionListener, ItemListener {

    private final XTable table;

    private final FrameToolBar frameToolbar;

    private final ToolBar toolBar;

    private final PopupMenu popupMenu;

    private final WebORPanel webOR;

    private Boolean monitorFrameChange = true;
    
    private final RoleCellEditor roleCellEditor;

    public WebORTable(WebORPanel webOR) {
        this.webOR = webOR;
        
        // Create the role cell editor with dropdown + name field
        roleCellEditor = new RoleCellEditor();
        
        // Create custom XTable that returns role editor for Role attribute
        table = new XTable() {
            @Override
            public TableCellEditor getCellEditor(int row, int column) {
                // Column 1 is the Value column, Column 0 is the Attribute name
                if (column == 1 && row >= 0 && row < getRowCount()) {
                    Object attrName = getValueAt(row, 0);
                    if ("Role".equalsIgnoreCase(String.valueOf(attrName))) {
                        return roleCellEditor;
                    }
                }
                return super.getCellEditor(row, column);
            }
        };
        frameToolbar = new FrameToolBar();
        toolBar = new ToolBar();
        popupMenu = new PopupMenu();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(frameToolbar, BorderLayout.NORTH);
        frameToolbar.setVisible(false);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add(panel, BorderLayout.CENTER);
        add(toolBar, BorderLayout.NORTH);
        table.setComponentPopupMenu(popupMenu);
    }

    public XTable getTable() {
        return table;
    }

    public void loadObject(WebORObject object) {
        table.setModel(object);
        configureColumns();
        monitorFrameChange = false;
        frameToolbar.frameText.setText(object.getFrame());
        toolBar.frameToggle.setSelected(!frameToolbar.frameText.getText().isEmpty());
        monitorFrameChange = true;
    }

    private void configureColumns() {
        if (table.getColumnCount() >= 3) {
            // Set resize mode: only resize Value column, keep others fixed
            table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            
            // Column 0: Attribute - narrow width
            TableColumn attrCol = table.getColumnModel().getColumn(0);
            attrCol.setCellRenderer(new PropertyAttributeRenderer());
            attrCol.setPreferredWidth(100);
            attrCol.setMinWidth(80);
            attrCol.setMaxWidth(150);
            
            // Column 1: Value - takes remaining space (flexible)
            TableColumn valueCol = table.getColumnModel().getColumn(1);
            valueCol.setPreferredWidth(300);
            valueCol.setMinWidth(50);
            
            // Column 2: Exact - fixed width, centered
            TableColumn exactCol = table.getColumnModel().getColumn(2);
            exactCol.setPreferredWidth(45);
            exactCol.setMinWidth(45);
            exactCol.setMaxWidth(45);
            exactCol.setResizable(false);
            
            // Center-aligned header for Exact column
            DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
            headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            exactCol.setHeaderRenderer(headerRenderer);
            
            // Center-aligned checkbox renderer for Exact column
            exactCol.setCellRenderer(new DefaultTableCellRenderer() {
                private final JCheckBox checkBox = new JCheckBox();
                {
                    checkBox.setHorizontalAlignment(SwingConstants.CENTER);
                    checkBox.setOpaque(true);
                }
                
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    // Check if cell is editable (not xpath, css, TestId)
                    boolean editable = table.getModel().isCellEditable(row, column);
                    checkBox.setSelected(value != null && (Boolean) value);
                    checkBox.setEnabled(editable);
                    
                    if (isSelected) {
                        checkBox.setBackground(table.getSelectionBackground());
                    } else {
                        checkBox.setBackground(table.getBackground());
                    }
                    return checkBox;
                }
            });
        }
    }

    private void changeFrameText() {
        if (monitorFrameChange) {
            webOR.changeFrameData(frameToolbar.frameText.getText());
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
                case "From Page":
                    clearFrameFromPage();
                    break;
                case "From All":
                    clearFrameFromAll();
                    break;
                case "From Selected":
                    clearFrameFromSelected();
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
        return webOR.getObjectTree().getSelectedObjects();
    }

    private void clearFrameFromSelected() {
        frameToolbar.frameText.setText("");
        getSelectedObjects().stream().forEach((object) -> {
            ((WebORObject) object).setFrame("");
        });
    }

    private void clearFrameFromAll() {
        frameToolbar.frameText.setText("");
        for (WebORPage page : getObject().getPage().getRoot().getPages()) {
            clearFrameFromPage(page);
        }
    }

    private void clearFrameFromPage() {
        frameToolbar.frameText.setText("");
        clearFrameFromPage(getObject().getPage());
    }

    private void clearFrameFromPage(WebORPage page) {
        for (ObjectGroup<WebORObject> objectGroup : page.getObjectGroups()) {
            for (WebORObject object : objectGroup.getObjects()) {
                object.setFrame("");
            }
        }
    }

    private void clearFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((WebORObject) object).setAttributeByName(attr, "");
                });
            }
        }
    }

    private void clearFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (WebORPage page : getObject().getPage().getRoot().getPages()) {
                clearFromPage(page, attrs);
            }
        }
    }

    private void clearFromPage() {
        if (table.getSelectedRowCount() > 0) {
            clearFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void clearFromPage(WebORPage page, String[] attrs) {
        for (ObjectGroup<WebORObject> objectGroup : page.getObjectGroups()) {
            for (WebORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.setAttributeByName(attr, "");
                }
            }
        }
    }

    private void removeFromSelected() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (String attr : attrs) {
                getSelectedObjects().stream().forEach((object) -> {
                    ((WebORObject) object).removeAttribute(attr);
                });
            }
        }
    }

    private void removeFromAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (WebORPage page : getObject().getPage().getRoot().getPages()) {
                removeFromPage(page, attrs);
            }
        }
    }

    private void removeFromPage() {
        if (table.getSelectedRowCount() > 0) {
            removeFromPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void removeFromPage(WebORPage page, String[] attrs) {
        for (ObjectGroup<WebORObject> objectGroup : page.getObjectGroups()) {
            for (WebORObject object : objectGroup.getObjects()) {
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
                    ((WebORObject) object).addNewAttribute(attr);
                });
            }
        }
    }

    private void addToAll() {
        if (table.getSelectedRowCount() > 0) {
            String[] attrs = getSelectedAttrs();
            for (WebORPage page : getObject().getPage().getRoot().getPages()) {
                addToPage(page, attrs);
            }
        }
    }

    private void addToPage() {
        if (table.getSelectedRowCount() > 0) {
            addToPage(getObject().getPage(), getSelectedAttrs());
        }
    }

    private void addToPage(WebORPage page, String[] attrs) {
        for (ObjectGroup<WebORObject> objectGroup : page.getObjectGroups()) {
            for (WebORObject object : objectGroup.getObjects()) {
                for (String attr : attrs) {
                    object.addNewAttribute(attr);
                }
            }
        }
    }

    private void setPriorityToSelected() {
        stopCellEditing();
        WebORObject currObj = getObject();
        getSelectedObjects().stream().forEach((object) -> {
            reorderAttributes(currObj.getAttributes(), ((WebORObject) object).getAttributes());
        });
    }

    private void setPriorityToAll() {
        stopCellEditing();
        WebORObject currObj = getObject();
        for (WebORPage page : getObject().getPage().getRoot().getPages()) {
            setPriorityToPage(page, currObj);
        }
    }

    private void setPriorityToPage() {
        stopCellEditing();
        WebORObject currObj = getObject();
        setPriorityToPage(getObject().getPage(), currObj);
    }

    private void setPriorityToPage(WebORPage page, WebORObject currObj) {
        for (ObjectGroup<WebORObject> objectGroup : page.getObjectGroups()) {
            for (WebORObject object : objectGroup.getObjects()) {
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

    public WebORObject getObject() {
        if (table.getModel() instanceof WebORObject) {
            return (WebORObject) table.getModel();
        }
        return null;
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        frameToolbar.setVisible(ie.getStateChange() == ItemEvent.SELECTED);
    }

    class FrameToolBar extends JToolBar implements DocumentListener {

        private JTextField frameText;

        public FrameToolBar() {
            init();
        }

        private void init() {
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
            setFloatable(false);
            setOpaque(false);
            frameText = new JTextField();
            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767)));
            add(new JLabel("Frame"));
            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767)));
            add(frameText);

            frameText.getDocument().addDocumentListener(this);
        }

        @Override
        public void insertUpdate(DocumentEvent de) {
            changeFrameText();
        }

        @Override
        public void removeUpdate(DocumentEvent de) {
            changeFrameText();
        }

        @Override
        public void changedUpdate(DocumentEvent de) {
            changeFrameText();
        }
    }

    class ToolBar extends JToolBar {

        JToggleButton frameToggle;

        public ToolBar() {
            init();
            setOpaque(false);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground")));
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

            add(Utils.createLRButton("Add Row", "add",  WebORTable.this));
            add(Utils.createLRButton("Delete Rows", "remove",  WebORTable.this));
            addSeparator();
            add(Utils.createLRButton("Move Rows Up", "up",  WebORTable.this));
            add(Utils.createLRButton("Move Rows Down", "down",  WebORTable.this));
            addSeparator();
            frameToggle = new JToggleButton(INGIcons.swingColored("or.propViewer", 16));
            frameToggle.addItemListener(WebORTable.this);
            frameToggle.setToolTipText("Show/Hide Frame Property");
            frameToggle.setActionCommand("Toggle Frame");
            add(frameToggle);
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
            JMenu clearFrame = new JMenu("Clear Frame");

            setPriority.add(Utils.createMenuItem("Set Priority to Page", WebORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to All", WebORTable.this));
            setPriority.add(Utils.createMenuItem("Set Priority to Selected", WebORTable.this));
            add(setPriority);
            clearProp.add(Utils.createMenuItem("Clear from Page", WebORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from All", WebORTable.this));
            clearProp.add(Utils.createMenuItem("Clear from Selected", WebORTable.this));
            add(clearProp);
            deleteProp.add(Utils.createMenuItem("Remove from Page", WebORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from All", WebORTable.this));
            deleteProp.add(Utils.createMenuItem("Remove from Selected", WebORTable.this));
            add(deleteProp);
            addProp.add(Utils.createMenuItem("Add to Page", WebORTable.this));
            addProp.add(Utils.createMenuItem("Add to All", WebORTable.this));
            addProp.add(Utils.createMenuItem("Add to Selected", WebORTable.this));
            add(addProp);
            addSeparator();
            clearFrame.add(Utils.createMenuItem("From Page", WebORTable.this));
            clearFrame.add(Utils.createMenuItem("From All", WebORTable.this));
            clearFrame.add(Utils.createMenuItem("From Selected", WebORTable.this));
            add(clearFrame);
        }

    }

}
