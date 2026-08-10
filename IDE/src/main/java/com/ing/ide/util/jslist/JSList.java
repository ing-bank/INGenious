package com.ing.ide.util.jslist;

import static java.util.stream.Collectors.toList;

import com.ing.ide.main.fx.INGIcons;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/**
 * A checkbox-based list component with support for adding, renaming,
 * deleting, and selecting items.
 *
 * @param <T> the type of items in the list
 */
public class JSList<T> extends JPanel {
    private static final javax.swing.Icon ADD_NEW_ICON = INGIcons.swingColored("icon.addNew", 16);

    private ListPanel listPanel;
    private TopBar topBar;
    private Consumer<List<T>> onSelect;
    private Consumer<T> onRemove;
    private BiConsumer<T, String> onUpdate;
    private Function<String, T> onAdd;
    private Function<T, String> mapper;
    private final Set<T> selected = new LinkedHashSet<>();
    private boolean isAdjusting = false;
    private FilterModel fltrmodel;

    public JSList(List srcmodel) {
        this(srcmodel, Object::toString, null);
    }

    public JSList(List srcmodel, Function<T, String> mapper, Function<String, T> onAdd) {
        this(srcmodel, mapper, onAdd, (e, k) -> k.isEmpty() || e.contains(k));
    }

    public JSList(
        List srcmodel,
        Function<T, String> mapper,
        Function<String, T> onAdd,
        BiPredicate<String, String> predicate
    ) {
        this.onAdd = onAdd;
        this.mapper = mapper;
        fltrmodel = new FilterModel(srcmodel, mapper, predicate);

        setLayout(new BorderLayout());

        topBar = new TopBar();
        add(topBar, BorderLayout.NORTH);

        listPanel = new ListPanel(fltrmodel, mapper);
        add(listPanel, BorderLayout.CENTER);

        setSize(300, 380);
    }

    public void setOnSelect(Consumer<List<T>> onSelect) {
        this.onSelect = onSelect;
    }

    public JSList<T> withOnRemove(Consumer<T> onRemove) {
        this.onRemove = onRemove;
        return this;
    }

    public JSList<T> withOnUpdate(BiConsumer<T, String> onUpdate) {
        this.onUpdate = onUpdate;
        return this;
    }

    public void setSelected(List<T> selected) {
        this.selected.clear();

        if (selected != null) {
            this.selected.addAll(selected.stream().distinct().collect(toList()));
        }

        listPanel.reselect();
    }

    public List<T> getSelected() {
        return listPanel.getSelected();
    }

    public void add(T t) {
        fltrmodel.srcmodel.add(t);
        selected.add(t);
        reload();
        listPanel.onSelect();
    }

    public void remove(T t) {
        if (onRemove != null && t != null) {
            onRemove.accept(t);
            fltrmodel.srcmodel.remove(t);
            selected.remove(t);
            reload();
        }
    }

    public void reload() {
        fltrmodel.doFilter("");
    }

    public void rename(T tag) {
        if (onUpdate == null || tag == null) {
            return;
        }

        String oldName = mapper.apply(tag);
        String newName = JOptionPane.showInputDialog(this, "Rename tag:", oldName);

        if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(oldName)) {
            onUpdate.accept(tag, newName.trim());
            reload();
        }
    }

    class FilterModel extends DefaultListModel {
        List<T> srcmodel;
        BiPredicate<T, Supplier<String>> predicate;

        public FilterModel(
            List srcmodel,
            Function<T, String> mapper,
            BiPredicate<String, String> predicate
        ) {
            this.srcmodel = srcmodel;
            this.predicate = (entry, key) -> predicate.test(mapper.apply(entry), key.get());
            srcmodel.stream().forEach(this::addElement);
        }

        private List<Object> items() {
            return Arrays.asList(toArray());
        }

        public void doFilter(Object keyword) {
            List<T> cselected = listPanel.getSelected();

            items()
                .stream()
                .filter(item -> !cselected.contains((T) item))
                .forEach(selected::remove);

            this.clear();

            srcmodel.stream().filter(by(keyword::toString)).forEach(this::addElement);

            listPanel.reselect();
        }

        public Predicate<T> by(Supplier<String> provider) {
            return entry -> predicate.test(entry, provider);
        }
    }

    class TopBar extends JPanel {
        JTextField addField = new JTextField(28);

        public TopBar() {
            JToolBar tbar = getToolbar();

            addField.addActionListener(e -> onAddT(addField.getText()));

            JPanel textfieldWithButton = new JPanel(new BorderLayout());
            textfieldWithButton.add(withToolbar(addField, getClearButton(addField)));
            textfieldWithButton.setBorder(addField.getBorder());

            tbar.add(textfieldWithButton);

            if (onAdd != null) {
                javax.swing.JButton addBtn = new javax.swing.JButton();
                addBtn.setIcon(ADD_NEW_ICON);
                addBtn.addActionListener(anything -> onAddT(addField.getText()));
                tbar.add(addBtn);
            }

            tbar.setLayout(new BoxLayout(tbar, BoxLayout.LINE_AXIS));

            setLayout(new BorderLayout());
            add(tbar, BorderLayout.CENTER);

            setSize(300, 40);
            setLocation(0, 0);
        }

        private JToolBar getToolbar() {
            JToolBar tbar = new JToolBar();
            tbar.setFloatable(false);
            tbar.setRollover(true);
            tbar.setBorderPainted(false);
            return tbar;
        }

        private JToolBar withToolbar(JComponent a, JComponent b) {
            JToolBar tbar = getToolbar();
            tbar.add(a);
            tbar.add(b);
            tbar.setLayout(new BoxLayout(tbar, BoxLayout.LINE_AXIS));
            return tbar;
        }

        private javax.swing.JButton getClearButton(JTextComponent parent) {
            javax.swing.JButton clear = new javax.swing.JButton(" x ");
            clear.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            clear.addActionListener((ActionEvent e) -> parent.setText(""));
            return clear;
        }

        private void onAddT(String txt) {
            if (!txt.trim().isEmpty() && onAdd != null) {
                JSList.this.add(onAdd.apply(txt));
                addField.setText("");
                addField.requestFocus();
            }
        }
    }

    class ListPanel extends JPanel {
        JList list;

        public ListPanel(FilterModel fltrmodel, Function<T, String> mapper) {
            setLayout(new BorderLayout());

            JScrollPane sp = new JScrollPane();

            list = new JList();
            list.setModel(fltrmodel);
            list.setCellRenderer(new CheckBoxListRenderer(mapper));

            sp.setViewportView(list);
            add(sp, BorderLayout.CENTER);

            list.setSelectionModel(new ListSelectionModel());
            list.addMouseListener(new ListMouseHandler());

            int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

            list
                .getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_A, SHORTCUT), "SelectAll");

            list
                .getActionMap()
                .put(
                    "SelectAll",
                    new AbstractAction() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            int size = list.getModel().getSize();

                            for (int i = 0; i < size; i++) {
                                T item = (T) list.getModel().getElementAt(i);
                                selected.add(item);
                            }

                            list.repaint();
                            onSelect();
                        }
                    }
                );
        }

        /**
         * Mouse zones:
         * checkbox: 0-25
         * label: 25 to width-60
         * rename: width-60 to width-30
         * delete: width-30 to end
         */
        class ListMouseHandler extends MouseAdapter {
            private static final int CHECKBOX_WIDTH = 25;
            private static final int BUTTON_WIDTH = 30;

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int idx = list.locationToIndex(e.getPoint());

                if (idx < 0 || idx >= list.getModel().getSize()) {
                    return;
                }

                Rectangle cellBounds = list.getCellBounds(idx, idx);

                if (cellBounds == null || !cellBounds.contains(e.getPoint())) {
                    return;
                }

                @SuppressWarnings("unchecked")
                T item = (T) list.getModel().getElementAt(idx);

                int relativeX = (int) (e.getX() - cellBounds.getX());
                int cellWidth = (int) cellBounds.getWidth();

                int renameBtnX = Math.max(cellWidth - BUTTON_WIDTH * 2, 0);
                int deleteBtnX = Math.max(cellWidth - BUTTON_WIDTH, 0);

                if (e.getClickCount() == 2) {
                    if (relativeX > CHECKBOX_WIDTH && relativeX < renameBtnX) {
                        rename(item);
                        return;
                    }
                } else {
                    if (relativeX < CHECKBOX_WIDTH) {
                        toggleSelection(item);
                        return;
                    }

                    if (relativeX >= renameBtnX && relativeX < deleteBtnX) {
                        rename(item);
                        return;
                    }

                    if (relativeX >= deleteBtnX) {
                        int confirm = JOptionPane.showConfirmDialog(
                            JSList.this,
                            "Delete tag \"" + mapper.apply(item) + "\"?",
                            "Delete Tag",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );

                        if (confirm == JOptionPane.YES_OPTION) {
                            JSList.this.remove(item);
                        }

                        return;
                    }
                }
            }

            private void toggleSelection(T item) {
                if (selected.contains(item)) {
                    selected.remove(item);
                } else {
                    selected.add(item);
                }

                list.repaint();
                onSelect();
            }
        }

        private void onSelect() {
            if (onSelect != null) {
                onSelect.accept(new ArrayList(selected));
            }
        }

        public List<T> getSelected() {
            return new ArrayList<>(selected);
        }

        private void reselect() {
            isAdjusting = true;

            int size = list.getModel().getSize();

            for (int i = 0; i < size; i++) {
                T item = (T) list.getModel().getElementAt(i);

                if (selected.contains(item)) {
                    list.addSelectionInterval(i, i);
                } else {
                    list.removeSelectionInterval(i, i);
                }
            }

            isAdjusting = false;
            list.repaint();
        }

        class ListSelectionModel extends DefaultListSelectionModel {

            @Override
            public void setSelectionInterval(int index0, int index1) {
                if (isAdjusting) {
                    super.setSelectionInterval(index0, index1);
                }
            }
        }

        class CheckBoxListRenderer extends JPanel implements ListCellRenderer<T> {
            private final Function<T, String> mapper;

            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            Font fontSel = new Font(Font.SANS_SERIF, Font.BOLD, 14);

            public CheckBoxListRenderer(Function<T, String> mapper) {
                super(new BorderLayout());
                this.mapper = mapper;

                setLayout(new BorderLayout());
                setFont(font);
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                setFocusable(false);
                setOpaque(true);
            }

            public boolean contains(T v) {
                return selected.stream().anyMatch(v::equals);
            }

            @Override
            public Component getListCellRendererComponent(
                JList<? extends T> list,
                T value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                removeAll();

                setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

                boolean checked = contains(value);

                Color purple = getPurpleColor();
                Color normalText = getNormalTextColor();

                /*
                 * Left checkbox.
                 *
                 * Important:
                 * Do NOT disable this checkbox.
                 * A disabled JCheckBox is painted using disabled LAF colors,
                 * which is why your purple checkbox disappeared.
                 *
                 * Since this is only a renderer component, making it enabled
                 * does not make it directly interactive.
                 */
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(checked);
                checkBox.setOpaque(false);
                checkBox.setFocusable(false);
                checkBox.setBorderPainted(false);
                checkBox.setContentAreaFilled(false);
                checkBox.setForeground(checked ? purple : normalText);

                /*
                 * Custom icons guarantee the purple checkbox/check mark even
                 * if the current Look & Feel ignores JCheckBox foreground.
                 */
                checkBox.setIcon(new PurpleCheckBoxIcon(false, purple));
                checkBox.setSelectedIcon(new PurpleCheckBoxIcon(true, purple));

                add(checkBox, BorderLayout.WEST);

                /*
                 * Label/text.
                 *
                 * This restores the old behavior:
                 * selected/checked rows use the ING focused foreground color.
                 */
                JLabel nameLabel = new JLabel(mapper.apply(value));
                nameLabel.setFont(checked ? fontSel : font);
                nameLabel.setForeground(checked ? purple : normalText);
                nameLabel.setOpaque(false);
                nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));

                add(nameLabel, BorderLayout.CENTER);

                JPanel rightPanel = new JPanel(new BorderLayout());
                rightPanel.setOpaque(false);

                if (onUpdate != null) {
                    JLabel renameLabel = new JLabel("✎");
                    renameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    renameLabel.setHorizontalAlignment(JLabel.CENTER);
                    renameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                    renameLabel.setOpaque(false);
                    renameLabel.setForeground(checked ? purple : normalText);
                    renameLabel.setToolTipText("Rename");

                    rightPanel.add(renameLabel, BorderLayout.WEST);
                }

                if (onRemove != null) {
                    JLabel deleteLabel = new JLabel("✕");
                    deleteLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                    deleteLabel.setHorizontalAlignment(JLabel.CENTER);
                    deleteLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                    deleteLabel.setOpaque(false);
                    deleteLabel.setForeground(checked ? purple : normalText);
                    deleteLabel.setToolTipText("Delete");

                    rightPanel.add(deleteLabel, BorderLayout.EAST);
                }

                add(rightPanel, BorderLayout.EAST);

                /*
                 * Keep background neutral so the visual style matches the old
                 * renderer: purple text + purple checkbox, not purple row bg.
                 */
                Color bg = list.getBackground();

                if (bg == null) {
                    bg = Color.WHITE;
                }

                setBackground(bg);

                return this;
            }

            private Color getPurpleColor() {
                Color purple = UIManager.getColor("ing.focusedForeground");

                if (purple == null) {
                    purple = UIManager.getColor("ing.selectionForeground");
                }

                if (purple == null) {
                    purple = new Color(128, 0, 128);
                }

                return purple;
            }

            private Color getNormalTextColor() {
                Color text = UIManager.getColor("text");

                if (text == null) {
                    text = UIManager.getColor("Label.foreground");
                }

                if (text == null) {
                    text = Color.BLACK;
                }

                return text;
            }
        }

        class PurpleCheckBoxIcon implements Icon {
            private static final int SIZE = 14;

            private final boolean checked;
            private final Color purple;

            PurpleCheckBoxIcon(boolean checked, Color purple) {
                this.checked = checked;
                this.purple = purple;
            }

            @Override
            public int getIconWidth() {
                return SIZE;
            }

            @Override
            public int getIconHeight() {
                return SIZE;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();

                try {
                    g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                    );

                    Color borderColor = checked ? purple : new Color(130, 130, 130);

                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(x + 1, y + 1, SIZE - 3, SIZE - 3, 3, 3);

                    g2.setColor(borderColor);
                    g2.drawRoundRect(x + 1, y + 1, SIZE - 3, SIZE - 3, 3, 3);

                    if (checked) {
                        g2.setColor(purple);
                        g2.setStroke(new BasicStroke(2f));

                        int x1 = x + 4;
                        int y1 = y + 7;
                        int x2 = x + 6;
                        int y2 = y + 10;
                        int x3 = x + 11;
                        int y3 = y + 4;

                        g2.drawLine(x1, y1, x2, y2);
                        g2.drawLine(x2, y2, x3, y3);
                    }
                } finally {
                    g2.dispose();
                }
            }
        }
    }
}
