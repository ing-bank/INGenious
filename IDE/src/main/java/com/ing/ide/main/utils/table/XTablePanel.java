
package com.ing.ide.main.utils.table;

import com.ing.ide.main.settings.TMSettingsControl;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Utility;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
        setBorder(BorderFactory.createRaisedBevelBorder());
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.add(new JScrollPane(table), "Table");
        cardPanel.add(new JScrollPane(textArea), "TextArea");

        add(cardPanel, BorderLayout.CENTER);
        add(toolBar = getTopToolBar(), BorderLayout.NORTH);

        textArea.setBorder(BorderFactory.createTitledBorder("S"));
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
        toolbar.setPreferredSize(new Dimension(92, 32));
        toolbar.setMinimumSize(new Dimension(92, 32));

        JButton addRow = new JButton(
                new javax.swing.ImageIcon(getClass().getResource("/ui/resources/add.png")));
        addRow.setToolTipText("Add Row");
        addRow.addActionListener((ActionEvent ae) -> {
            JtableUtils.addrow(table);
        });
        JButton delete = new JButton(
                new javax.swing.ImageIcon(getClass().getResource("/ui/resources/rem.png")));
        delete.setToolTipText("Delete Rows");
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
