package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.engine.core.InlineObjectProperty;
import com.ing.ide.main.mainui.components.testdesign.testdata.TestDataDetail;
import com.ing.ide.main.utils.dnd.DataFlavors;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

/**
 * Builder dialog for the <b>inline object-property override</b> feature. Lets the
 * author map one or more {@code #token} placeholders (from the selected Object
 * Repository element's locator) to values ({@code Sheet:Column}, {@code %var%},
 * {@code #globalData} or a literal) and produces a Condition-column expression:
 *
 * <pre>setProp: #token=value; #token2=value2</pre>
 *
 * <p>or the global variant {@code setGlobalProp: ...}. Round-trips an existing
 * expression when reopened.</p>
 */
public class InlinePropertyDialog extends JDialog {
    private final List<Row> rows = new ArrayList<>();
    private final JPanel rowsPanel = new JPanel();
    private final List<String> tokenChoices;
    private final List<String> valueChoices;
    private final JRadioButton objectScope = new JRadioButton("Object (this element)", true);
    private final JRadioButton globalScope = new JRadioButton("Global (all elements)");

    private String result;

    private static final class Row {
        final JPanel panel = new JPanel();
        final JComboBox<String> token = new JComboBox<>();
        final JComboBox<String> value = new JComboBox<>();
        final JTextField subIter = new JTextField();
    }

    private InlinePropertyDialog(
        Window owner,
        Collection<String> tokens,
        Collection<String> values,
        String existing
    ) {
        super(owner, "Inline Object Property Override", ModalityType.APPLICATION_MODAL);
        this.tokenChoices = new ArrayList<>(tokens);
        this.valueChoices = new ArrayList<>(values);
        buildUi();
        preload(existing);
        pack();
        setMinimumSize(new Dimension(560, Math.min(getHeight(), 520)));
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the builder and returns the serialized Condition expression, or
     * {@code null} if the user cancelled.
     *
     * @param parent   a component in the owning window
     * @param tokens   {@code #token} placeholders discovered on the element's locator
     * @param values   value suggestions ({@code Sheet:Column} and {@code %variables%})
     * @param existing current Condition cell text (parsed for round-trip editing)
     */
    public static String show(
        Component parent,
        Collection<String> tokens,
        Collection<String> values,
        String existing
    ) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        InlinePropertyDialog dialog = new InlinePropertyDialog(owner, tokens, values, existing);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void buildUi() {
        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        scopePanel.setBorder(BorderFactory.createTitledBorder("Scope"));
        ButtonGroup group = new ButtonGroup();
        group.add(objectScope);
        group.add(globalScope);
        scopePanel.add(objectScope);
        scopePanel.add(globalScope);

        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.add(headerRow());

        // Keep rows anchored to the top so they don't stretch vertically.
        JPanel rowsHolder = new JPanel(new BorderLayout());
        rowsHolder.add(rowsPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(rowsHolder);
        scroll.setBorder(BorderFactory.createTitledBorder("Token → Value"));
        scroll.setPreferredSize(new Dimension(540, 190));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JButton addBtn = new JButton("+ Add token");
        addBtn.addActionListener(
            e -> {
                addRow(null, null, null);
                repack();
            }
        );

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> onOk());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(
            e -> {
                result = null;
                dispose();
            }
        );

        JPanel south = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
        left.add(addBtn);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(ok);
        right.add(cancel);
        south.add(left, BorderLayout.WEST);
        south.add(right, BorderLayout.EAST);

        JLabel hint = new JLabel(
            "<html><small>Pick a <b>#token</b> from your element's locator, then choose a " +
            "value — a <b>Sheet:Column</b>, a <b>%variable%</b>, <b>#globalData</b> or a literal.<br>" +
            "Optional <b>Sub-iter</b> selects a specific data-sheet sub-iteration (row) for a " +
            "Sheet:Column value.<br>" +
            "Tip: you can also <b>drag a column from the Test Data grid</b> onto a value box.</small></html>"
        );
        hint.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel north = new JPanel(new BorderLayout());
        north.add(scopePanel, BorderLayout.CENTER);
        north.add(hint, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private JPanel headerRow() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        JLabel tokenLbl = new JLabel("Token");
        JLabel valueLbl = new JLabel("Value");
        tokenLbl.setPreferredSize(new Dimension(180, 20));
        tokenLbl.setMaximumSize(new Dimension(180, 20));
        header.add(tokenLbl);
        header.add(Box.createHorizontalStrut(24));
        header.add(valueLbl);
        header.add(Box.createHorizontalGlue());
        JLabel subLbl = new JLabel("Sub-iter");
        subLbl.setPreferredSize(new Dimension(64, 20));
        subLbl.setMaximumSize(new Dimension(64, 20));
        header.add(subLbl);
        header.add(Box.createHorizontalStrut(44));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return header;
    }

    private void preload(String existing) {
        if (InlineObjectProperty.isInline(existing)) {
            globalScope.setSelected(InlineObjectProperty.isGlobal(existing));
            objectScope.setSelected(!InlineObjectProperty.isGlobal(existing));
            List<String[]> pairs = InlineObjectProperty.parsePairs(
                InlineObjectProperty.stripMarker(existing)
            );
            for (String[] pair : pairs) {
                addRow(pair[0], pair[1], pair.length > 2 ? pair[2] : "");
            }
        }
        if (rows.isEmpty()) {
            addRow(null, null, null);
        }
    }

    private void addRow(String token, String value, String subIter) {
        Row row = new Row();

        row.token.setEditable(true);
        for (String choice : tokenChoices) {
            row.token.addItem(choice);
        }
        row.token.setSelectedItem(token == null ? "" : token);
        row.token.setToolTipText("A #token placeholder from the element's locator");

        row.value.setEditable(true);
        for (String choice : valueChoices) {
            row.value.addItem(choice);
        }
        row.value.setSelectedItem(value == null ? "" : value);
        row.value.setToolTipText(
            "Sheet:Column, %variable%, #globalData or a literal — or drag a Test Data column here"
        );
        installValueDrop(row.value);

        row.subIter.setText(subIter == null ? "" : subIter);
        row.subIter.setToolTipText(
            "Optional: data-sheet sub-iteration (row number) to read a Sheet:Column value from"
        );

        row.token.setPreferredSize(new Dimension(180, 26));
        row.token.setMaximumSize(new Dimension(180, 26));
        row.value.setPreferredSize(new Dimension(240, 26));
        row.value.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.subIter.setPreferredSize(new Dimension(56, 26));
        row.subIter.setMaximumSize(new Dimension(56, 26));

        JButton remove = new JButton("✕");
        remove.setMargin(new java.awt.Insets(0, 6, 0, 6));
        remove.setToolTipText("Remove this token");
        remove.addActionListener(
            e -> {
                if (rows.size() <= 1) {
                    row.token.setSelectedItem("");
                    row.value.setSelectedItem("");
                    row.subIter.setText("");
                    return;
                }
                rowsPanel.remove(row.panel);
                rows.remove(row);
                repack();
            }
        );

        row.panel.setLayout(new BoxLayout(row.panel, BoxLayout.X_AXIS));
        row.panel.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        row.panel.add(row.token);
        row.panel.add(Box.createHorizontalStrut(6));
        row.panel.add(new JLabel("="));
        row.panel.add(Box.createHorizontalStrut(6));
        row.panel.add(row.value);
        row.panel.add(Box.createHorizontalStrut(6));
        row.panel.add(row.subIter);
        row.panel.add(Box.createHorizontalStrut(6));
        row.panel.add(remove);
        row.panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        rows.add(row);
        rowsPanel.add(row.panel);
    }

    /** Accept a data-sheet column (or plain text) dropped onto a value box. */
    private void installValueDrop(JComboBox<String> combo) {
        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        editor.setTransferHandler(
            new TransferHandler() {

                @Override
                public boolean canImport(TransferSupport support) {
                    return (
                        support.isDataFlavorSupported(DataFlavors.TESTDATA_FLAVOR) ||
                        support.isDataFlavorSupported(DataFlavor.stringFlavor)
                    );
                }

                @Override
                public boolean importData(TransferSupport support) {
                    if (!canImport(support)) {
                        return false;
                    }
                    try {
                        Transferable t = support.getTransferable();
                        if (support.isDataFlavorSupported(DataFlavors.TESTDATA_FLAVOR)) {
                            TestDataDetail td = (TestDataDetail) t.getTransferData(
                                DataFlavors.TESTDATA_FLAVOR
                            );
                            if (!td.getColumnNames().isEmpty()) {
                                combo.setSelectedItem(
                                    td.getSheetName() + ":" + td.getColumnNames().get(0)
                                );
                                return true;
                            }
                            return false;
                        }
                        String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                        combo.setSelectedItem(text == null ? "" : text.trim());
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                }
            }
        );
    }

    private void repack() {
        rowsPanel.revalidate();
        rowsPanel.repaint();
        pack();
    }

    private void onOk() {
        boolean global = globalScope.isSelected();
        List<String[]> pairs = new ArrayList<>();
        for (Row row : rows) {
            String token = normalizeToken(comboText(row.token));
            String value = comboText(row.value);
            String subIter = row.subIter.getText().trim();
            if (token.isEmpty() && value.isEmpty()) {
                continue;
            }
            if (token.isEmpty() || value.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Each row needs both a #token and a value.",
                    "Incomplete row",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            if (!subIter.isEmpty() && !subIter.matches("\\d+")) {
                JOptionPane.showMessageDialog(
                    this,
                    "Sub-iteration must be a whole number.",
                    "Invalid sub-iteration",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            pairs.add(new String[] { token, value, subIter });
        }
        if (pairs.isEmpty()) {
            result = "";
            dispose();
            return;
        }
        result = InlineObjectProperty.serialize(global, pairs);
        dispose();
    }

    private static String comboText(JComboBox<String> combo) {
        Object item = combo.getEditor().getItem();
        if (item == null) {
            item = combo.getSelectedItem();
        }
        return item == null ? "" : item.toString().trim();
    }

    /** Ensures the token is prefixed with {@code #}. */
    private static String normalizeToken(String token) {
        String t = token.trim();
        if (t.isEmpty()) {
            return t;
        }
        return t.startsWith("#") ? t : "#" + t;
    }
}
