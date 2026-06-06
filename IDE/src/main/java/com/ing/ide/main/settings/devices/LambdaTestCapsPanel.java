package com.ing.ide.main.settings.devices;

import com.ing.datalib.util.data.LinkedProperties;
import com.ing.ide.main.fx.INGIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 * Cleaner, "accordion" style view for LambdaTest device capabilities.
 *
 * Instead of one long flat table with {@code -- section --} marker rows, the
 * capabilities are grouped into collapsible sections (one per category as
 * defined by {@link com.ing.datalib.settings.Devices#defaultLambdaTestCaps()}),
 * each with its own small property/value table.
 *
 * Unknown / user-added capabilities live under a separate "Additional"
 * section which is the only section that allows adding and removing rows.
 */
public class LambdaTestCapsPanel extends JPanel {

    private static final String ADDITIONAL_GROUP = "Additional";

    private final JPanel sectionsHolder;
    private final Map<String, Section> sections = new LinkedHashMap<>();
    private final List<TableModelListener> changeListeners = new ArrayList<>();

    /** Default capability groups in display order (excluding "Additional"). */
    private final Map<String, LinkedProperties> defaultGroups;

    public LambdaTestCapsPanel(Map<String, LinkedProperties> defaultGroups) {
        super(new BorderLayout());
        this.defaultGroups = defaultGroups;

        sectionsHolder = new JPanel();
        sectionsHolder.setLayout(new BoxLayout(sectionsHolder, BoxLayout.Y_AXIS));
        sectionsHolder.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Outer wrapper keeps sections anchored to the top.
        JPanel wrap = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.weighty = 0.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTH;
        wrap.add(sectionsHolder, gc);
        // glue
        gc.gridy = 1;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        wrap.add(new JPanel(), gc);

        JScrollPane scroll = new JScrollPane(
                wrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        rebuild(null);
    }

    /** Forwarded to every section table so callers can detect edits. */
    public void addTableChangeListener(TableModelListener listener) {
        changeListeners.add(listener);
        for (Section s : sections.values()) {
            s.model.addTableModelListener(listener);
        }
    }

    /**
     * Populate sections from {@code existing}. Known keys go into their
     * declared group; unrecognised keys fall into "Additional". A {@code null}
     * or empty input shows the defaults.
     */
    public void setProperties(LinkedProperties existing) {
        rebuild(existing);
    }

    /** Returns a flat ordered property set with section markers stripped. */
    public LinkedProperties getProperties() {
        LinkedProperties props = new LinkedProperties();
        for (Section s : sections.values()) {
            s.flushEditing();
            for (int i = 0; i < s.model.getRowCount(); i++) {
                String key = Objects.toString(s.model.getValueAt(i, 0), "").trim();
                if (key.isEmpty()) {
                    continue;
                }
                String val = Objects.toString(s.model.getValueAt(i, 1), "");
                props.setProperty(key, val);
            }
        }
        return props;
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private void rebuild(LinkedProperties existing) {
        sectionsHolder.removeAll();
        sections.clear();

        Set<String> knownKeys = new LinkedHashSet<>();
        for (Map.Entry<String, LinkedProperties> group : defaultGroups.entrySet()) {
            Section section = new Section(group.getKey(), false);
            for (Object k : group.getValue().orderedKeys()) {
                String key = k.toString();
                knownKeys.add(key);
                Object val = (existing != null && existing.containsKey(key))
                        ? existing.get(key)
                        : group.getValue().get(key);
                section.model.addRow(new Object[]{key, val});
            }
            section.adjustHeight();
            sections.put(group.getKey(), section);
            sectionsHolder.add(section);
            sectionsHolder.add(javax.swing.Box.createVerticalStrut(6));
        }

        // "Additional" section for unknown / user-added entries.
        Section additional = new Section(ADDITIONAL_GROUP, true);
        if (existing != null) {
            for (Object k : existing.orderedKeys()) {
                String key = k.toString();
                if (!knownKeys.contains(key)) {
                    additional.model.addRow(new Object[]{key, existing.get(key)});
                }
            }
        }
        additional.adjustHeight();
        sections.put(ADDITIONAL_GROUP, additional);
        sectionsHolder.add(additional);

        // Re-attach external listeners to the freshly created models.
        for (TableModelListener l : changeListeners) {
            for (Section s : sections.values()) {
                s.model.addTableModelListener(l);
            }
        }

        sectionsHolder.revalidate();
        sectionsHolder.repaint();
    }

    // ------------------------------------------------------------------
    // Section component
    // ------------------------------------------------------------------

    private static final class Section extends JPanel {

        private static final Color HEADER_BG = new Color(245, 240, 255);
        private static final Color HEADER_BG_HOVER = new Color(229, 214, 255);
        private static final Color HEADER_FG = new Color(77, 0, 32);
        private static final Color BORDER = new Color(200, 195, 210);
        private static final int ROW_HEIGHT = 22;

        final boolean editableStructure;
        final DefaultTableModel model;
        final JTable table;
        final JPanel body;
        final JLabel toggleLabel;
        final JLabel countLabel;
        boolean expanded = true;

        Section(String title, boolean editableStructure) {
            super(new BorderLayout());
            this.editableStructure = editableStructure;

            setBorder(BorderFactory.createLineBorder(BORDER, 1));

            // ---- Body (table) ---- (build first so listeners can reference it)
            model = new DefaultTableModel(new Object[]{"Property", "Value"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // For known groups, only the value column is editable —
                    // keys are defined by the platform. For the "Additional"
                    // section, everything is editable.
                    return Section.this.editableStructure || column == 1;
                }
            };
            table = new JTable(model);
            table.setRowHeight(ROW_HEIGHT);
            table.setFillsViewportHeight(false);
            table.getTableHeader().setReorderingAllowed(false);
            table.setShowGrid(true);
            table.setGridColor(BORDER);

            JScrollPane sp = new JScrollPane(table,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sp.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

            body = new JPanel(new BorderLayout());
            body.add(sp, BorderLayout.CENTER);

            // ---- Header ----
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(HEADER_BG);
            header.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 6));
            header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            toggleLabel = new JLabel("\u25BC"); // ▼
            toggleLabel.setForeground(HEADER_FG);
            toggleLabel.setFont(toggleLabel.getFont().deriveFont(Font.BOLD, 11f));
            toggleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(HEADER_FG);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.setOpaque(false);
            left.add(toggleLabel);
            left.add(titleLabel);

            countLabel = new JLabel("");
            countLabel.setForeground(HEADER_FG.darker());
            countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 11f));
            countLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            rightSide.setOpaque(false);

            if (editableStructure) {
                JButton add = makeHeaderButton(INGIcons.swingColored("icon.add", 14), "Add capability");
                add.addActionListener(e -> {
                    model.addRow(new Object[]{"", ""});
                    updateCountLabel();
                    adjustHeight();
                    if (!expanded) {
                        toggle();
                    }
                });
                JButton remove = makeHeaderButton(INGIcons.swingColored("icon.remove", 14), "Remove selected capability");
                remove.addActionListener(e -> {
                    if (table.isEditing()) {
                        table.getCellEditor().stopCellEditing();
                    }
                    int[] rows = table.getSelectedRows();
                    for (int i = rows.length - 1; i >= 0; i--) {
                        model.removeRow(rows[i]);
                    }
                    updateCountLabel();
                    adjustHeight();
                });
                rightSide.add(add);
                rightSide.add(remove);
            }
            rightSide.add(countLabel);

            header.add(left, BorderLayout.WEST);
            header.add(rightSide, BorderLayout.EAST);

            MouseAdapter toggler = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getSource() instanceof JButton) {
                        return;
                    }
                    toggle();
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    header.setBackground(HEADER_BG_HOVER);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    header.setBackground(HEADER_BG);
                }
            };
            header.addMouseListener(toggler);
            titleLabel.addMouseListener(toggler);
            toggleLabel.addMouseListener(toggler);
            left.addMouseListener(toggler);

            add(header, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);

            model.addTableModelListener(e -> updateCountLabel());
            updateCountLabel();
        }

        private static JButton makeHeaderButton(javax.swing.Icon icon, String tooltip) {
            JButton b = new JButton(icon);
            b.setToolTipText(tooltip);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusable(false);
            b.setMargin(new java.awt.Insets(0, 4, 0, 4));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        void toggle() {
            expanded = !expanded;
            body.setVisible(expanded);
            toggleLabel.setText(expanded ? "\u25BC" : "\u25B6"); // ▼ / ▶
            adjustHeight();
            revalidate();
            repaint();
        }

        void updateCountLabel() {
            int rows = 0;
            int filled = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                String k = Objects.toString(model.getValueAt(i, 0), "").trim();
                if (k.isEmpty()) {
                    continue;
                }
                rows++;
                String v = Objects.toString(model.getValueAt(i, 1), "").trim();
                if (!v.isEmpty()) {
                    filled++;
                }
            }
            countLabel.setText(rows == 0 ? "" : filled + " / " + rows + " set");
        }

        /** Size the embedded table to fit its rows exactly (no scrollbar). */
        void adjustHeight() {
            int rows = Math.max(model.getRowCount(), editableStructure ? 1 : 0);
            int tableH = rows * ROW_HEIGHT + table.getTableHeader().getPreferredSize().height;
            Dimension d = new Dimension(0, tableH);
            table.setPreferredScrollableViewportSize(d);
            revalidate();
        }

        void flushEditing() {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
        }
    }

    /** Utility used by the iterator-style accessors. */
    Iterator<Section> sectionIterator() {
        return sections.values().iterator();
    }
}
