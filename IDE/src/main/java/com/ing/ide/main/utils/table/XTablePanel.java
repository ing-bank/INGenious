
package com.ing.ide.main.utils.table;

import com.ing.ide.main.settings.TMSettingsControl;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Utility;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import com.ing.ide.main.fx.INGIcons;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/**
 *
 *
 */
public class XTablePanel extends JPanel {

    public final XTable table;

    private final JTextArea textArea;

    private JPanel cardPanel;

    private CardLayout cardLayout;

    private int expandedRow;

    public JToolBar toolBar;

    public boolean addEncryption;
    
    // Theme-aware color helpers
    private static boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }
    
    private static Color getBgColor() {
        return isDarkMode() ? new Color(30, 26, 36) : Color.WHITE;
    }
    
    private static Color getPanelBgColor() {
        return isDarkMode() ? new Color(37, 32, 48) : new Color(250, 250, 248);
    }
    
    private static Color getBorderColor() {
        return isDarkMode() ? new Color(60, 50, 80) : new Color(229, 214, 255);
    }
    
    private static Color getTextColor() {
        return isDarkMode() ? new Color(232, 226, 229) : new Color(77, 0, 32);
    }
    
    private static Color getInputBgColor() {
        return isDarkMode() ? new Color(45, 40, 55) : Color.WHITE;
    }
    
    private static Color getAccentColor() {
        return isDarkMode() ? new Color(255, 102, 0) : new Color(119, 36, 255);
    }

    public XTablePanel(boolean addEncryption) {
        super();
        table = new XTable(new DefaultTableModel(new Object[]{"Property", "Value"}, 0));
        textArea = new JTextArea();
        this.addEncryption = addEncryption;
        init();
        addExpandArea();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(getBgColor());
        setOpaque(true);
        
        // Use theme-aware border instead of raised bevel
        setBorder(BorderFactory.createLineBorder(getBorderColor(), 1));
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(getBgColor());
        
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBackground(getBgColor());
        tableScrollPane.getViewport().setBackground(getInputBgColor());
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        JScrollPane textScrollPane = new JScrollPane(textArea);
        textScrollPane.setBackground(getBgColor());
        textScrollPane.getViewport().setBackground(getInputBgColor());
        
        cardPanel.add(tableScrollPane, "Table");
        cardPanel.add(textScrollPane, "TextArea");

        add(cardPanel, BorderLayout.CENTER);
        add(toolBar = getTopToolBar(), BorderLayout.NORTH);

        // Style text area with theme-aware colors
        textArea.setBackground(getInputBgColor());
        textArea.setForeground(getTextColor());
        textArea.setCaretColor(getAccentColor());
        textArea.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(getBorderColor()), "S",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
            null, getTextColor()));
        
        // Style table with theme-aware colors
        table.setBackground(getInputBgColor());
        table.setForeground(getTextColor());
        table.setGridColor(getBorderColor());
        table.setSelectionBackground(getAccentColor());
        table.setSelectionForeground(Color.WHITE);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setBackground(getPanelBgColor());
            table.getTableHeader().setForeground(getTextColor());
        }
        
        if (addEncryption) {
            addEncryptionAction();
        }
    }

    private void addExpandArea() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.isControlDown() && SwingUtilities.isRightMouseButton(me)) {
                    int row = table.rowAtPoint(me.getPoint());
                    int col = table.columnAtPoint(me.getPoint());
                    if (row >= 0 && col == 1) {
                        expandedRow = row;
                        ((TitledBorder) textArea.getBorder()).setTitle(
                                Objects.toString(table.getValueAt(row, 0), "Prop")
                        );
                        textArea.setText(Objects.toString(table.getValueAt(row, col), ""));
                        cardLayout.show(cardPanel, "TextArea");
                    }
                }
            }
        });

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.isControlDown() && SwingUtilities.isRightMouseButton(me)) {
                    table.setValueAt(textArea.getText(), expandedRow, 1);
                    cardLayout.show(cardPanel, "Table");
                }
            }
        });
    }

    public void addToolBarComp(JComponent comp) {
        toolBar.addSeparator();
        toolBar.add(comp);
    }

    private JToolBar getTopToolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(getPanelBgColor());
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getBorderColor()));
        toolbar.setPreferredSize(new Dimension(92, 32));
        toolbar.setMinimumSize(new Dimension(92, 32));

        JButton addRow = new JButton(
                INGIcons.swingColored("icon.add", 16));
        addRow.setToolTipText("Add Row");
        addRow.setBackground(getPanelBgColor());
        addRow.setBorderPainted(false);
        addRow.setFocusPainted(false);
        addRow.addActionListener((ActionEvent ae) -> {
            JtableUtils.addrow(table);
        });
        JButton delete = new JButton(
                INGIcons.swingColored("icon.rem", 16));
        delete.setToolTipText("Delete Rows");
        delete.setBackground(getPanelBgColor());
        delete.setBorderPainted(false);
        delete.setFocusPainted(false);
        delete.addActionListener((ActionEvent ae) -> {
            JtableUtils.deleterow(table);
        });
        toolbar.add(new Box.Filler(
                new Dimension(0, 0), new Dimension(0, 0), new Dimension(32767, 32767)));
        toolbar.add(addRow);
        toolbar.add(delete);

        return toolbar;
    }

    public void addEncryptionAction() {
        InputMap imTD = table.getInputMap(WHEN_FOCUSED);
        ActionMap amTD = table.getActionMap();
        JPopupMenu popup = new JPopupMenu();
        JMenuItem mItemEnc = new JMenuItem("Encrypt");
        popup.add(mItemEnc);
        Action enc = getEncryptAction(table);
        mItemEnc.setAccelerator(Keystroke.ENCRYPT);
        mItemEnc.addActionListener(enc);
        imTD.put(Keystroke.ENCRYPT, "encrypt");
        amTD.put("encrypt", enc);
        table.setComponentPopupMenu(popup);
        JtableUtils.addlisteners(table, Boolean.FALSE);
    }

    private static AbstractAction getEncryptAction(final JTable table) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent me) {
                try {
                    int col = table.getSelectedColumn();
                    int row = table.getSelectedRow();
                    if (col > -1 && row > -1) {
                        String data = table.getValueAt(row, col).toString();
                        table.setValueAt(Utility.encrypt(data), row, col);
                    }
                } catch (HeadlessException ex) {
                    Logger.getLogger(TMSettingsControl.class.getName())
                            .log(Level.SEVERE, ex.getMessage(), ex);
                }

            }
        };
    }

}
