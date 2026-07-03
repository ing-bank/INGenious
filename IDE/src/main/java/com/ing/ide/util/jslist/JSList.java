package com.ing.ide.util.jslist;

import static java.util.stream.Collectors.toList;

import com.ing.ide.main.fx.INGIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
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
import javax.swing.text.JTextComponent;

/**
 * A checkbox-based list component with support for adding (typing + Enter),
 * renaming (double-click or ✎ button → popup dialog), and deleting (✕ button with confirmation).
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

    /**
     * Creates a JSList with the given model, using toString() for display.
     * @param srcmodel the initial list of items
     */
    public JSList(List srcmodel) {
        this(srcmodel, Object::toString, null);
    }

    /**
     * Creates a JSList with a custom display mapper and an optional add handler.
     * @param srcmodel the initial list of items
     * @param mapper   function to convert items to display text
     * @param onAdd    callback invoked when the user creates a new item via the add field; null to disable adding
     */
    public JSList(List srcmodel, Function<T, String> mapper, Function<String, T> onAdd) {
        this(srcmodel, mapper, onAdd, (e, k) -> k.isEmpty() || e.contains(k));
    }

    /**
     * Creates a JSList with full control over display, add, and filtering.
     * @param srcmodel  the initial list of items
     * @param mapper    function to convert items to display text
     * @param onAdd     callback invoked when the user creates a new item; null to disable adding
     * @param predicate filter predicate: (displayText, searchKeyword) → true if item matches
     */
    public JSList(
        List srcmodel,
        Function<T, String> mapper,
        Function<String, T> onAdd,
        BiPredicate<String, String> predicate
    ) {
        this.onAdd = onAdd;
        this.mapper = mapper;
        fltrmodel = new FilterModel(srcmodel, mapper, predicate);
        setLayout(new java.awt.BorderLayout());
        topBar = new TopBar();
        add(topBar, java.awt.BorderLayout.NORTH);
        listPanel = new ListPanel(fltrmodel, mapper);
        add(listPanel, java.awt.BorderLayout.CENTER);
        setSize(300, 380);
    }

    /**
     * Registers a callback fired whenever the set of selected items changes.
     * @param onSelect consumer receiving the new selection list
     */
    public void setOnSelect(Consumer<List<T>> onSelect) {
        this.onSelect = onSelect;
    }

    /**
     * Registers a callback fired when an item is removed (e.g. via the ✕ button).
     * @param onRemove consumer receiving the removed item
     * @return this instance for chaining
     */
    public JSList withOnRemove(Consumer<T> onRemove) {
        this.onRemove = onRemove;
        return this;
    }

    /**
     * Registers a callback fired when an item is renamed.
     * @param onUpdate bi-consumer receiving (item, newName)
     * @return this instance for chaining
     */
    public JSList withOnUpdate(BiConsumer<T, String> onUpdate) {
        this.onUpdate = onUpdate;
        return this;
    }

    /**
     * Sets the currently selected items. The checkboxes will reflect this set.
     * @param selected items to mark as selected; null is treated as empty
     */
    public void setSelected(List<T> selected) {
        this.selected.clear();
        if (selected != null) {
            this.selected.addAll(selected.stream().distinct().collect(toList()));
        }
        listPanel.reselect();
    }

    /**
     * Returns the currently selected items.
     * @return list of selected items (never null)
     */
    public List<T> getSelected() {
        return listPanel.getSelected();
    }

    /**
     * Adds an item to the list model and marks it as selected.
     * @param t the item to add
     */
    public void add(T t) {
        fltrmodel.srcmodel.add(t);
        selected.add(t);
        reload();
    }

    /**
     * Removes an item from the list and calls the onRemove callback.
     * If onRemove is null or t is null, this is a no-op.
     * @param t the item to remove
     */
    public void remove(T t) {
        if (onRemove != null && t != null) {
            onRemove.accept(t);
            fltrmodel.srcmodel.remove(t);
            selected.remove(t);
            reload();
        }
    }

    /**
     * Refreshes the list display to show all items from the source model.
     */
    public void reload() {
        fltrmodel.doFilter("");
    }

    /**
     * Opens a popup dialog to rename the given tag. Calls onUpdate if the name
     * actually changed, then refreshes the list. If onUpdate is null this is a no-op.
     * @param tag the tag to rename
     */
    public void rename(T tag) {
        if (onUpdate == null) {
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
        JTextField addField = new javax.swing.JTextField(28);

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
                addBtn.addActionListener(anyting -> onAddT(addField.getText()));
                tbar.add(addBtn);
            }
            tbar.setLayout(new BoxLayout(tbar, BoxLayout.LINE_AXIS));
            setLayout(new java.awt.BorderLayout());
            add(tbar, BorderLayout.CENTER);
            setSize(300, 40);
            setLocation(0, 0);
        }

        private JToolBar getToolbar() {
            JToolBar tbar = new javax.swing.JToolBar();
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
            setLayout(new java.awt.BorderLayout());
            JScrollPane sp = new javax.swing.JScrollPane();
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
         * Mouse handler on the JList. Routes clicks based on X coordinate.
         * Zones: [checkbox 0-25] [label 25 ~ width-60] [rename width-60 ~ width-30] [delete width-30 ~ end]
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
                    // Click is in empty space, not on a cell
                    return;
                }
                @SuppressWarnings("unchecked")
                T item = (T) list.getModel().getElementAt(idx);

                int relativeX = (int) (e.getX() - cellBounds.getX());
                int cellWidth = (int) cellBounds.getWidth();

                int renameBtnX = Math.max(cellWidth - BUTTON_WIDTH * 2, 0);
                int deleteBtnX = Math.max(cellWidth - BUTTON_WIDTH, 0);

                if (e.getClickCount() == 2) {
                    // Double-click on label area → popup rename dialog
                    if (relativeX > CHECKBOX_WIDTH && relativeX < renameBtnX) {
                        rename(item);
                        return;
                    }
                } else {
                    if (relativeX < CHECKBOX_WIDTH) {
                        toggleSelection(item);
                        return;
                    } else if (relativeX >= renameBtnX && relativeX < deleteBtnX) {
                        rename(item);
                        return;
                    } else if (relativeX >= deleteBtnX) {
                        // Confirm delete
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
                    // Clicks on label area do nothing (only checkbox toggles)
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
                setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                setFocusable(false);
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

                // Left: checkbox (disabled, visual only)
                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(checked);
                checkBox.setOpaque(false);
                checkBox.setEnabled(false);
                add(checkBox, BorderLayout.WEST);

                // Label
                JLabel nameLabel = new JLabel(mapper.apply(value));
                nameLabel.setFont(checked ? fontSel : font);
                nameLabel.setOpaque(false);
                nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
                add(nameLabel, BorderLayout.CENTER);

                // Right: rename ✎ and delete ✕ labels (visual only, click handled by mouse handler)
                JPanel rightPanel = new JPanel(new BorderLayout());
                rightPanel.setOpaque(false);

                JLabel renameLabel = new JLabel("✎");
                renameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                renameLabel.setHorizontalAlignment(JLabel.CENTER);
                renameLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                renameLabel.setOpaque(false);
                renameLabel.setToolTipText("Rename");

                JLabel deleteLabel = new JLabel("✕");
                deleteLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
                deleteLabel.setHorizontalAlignment(JLabel.CENTER);
                deleteLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                deleteLabel.setOpaque(false);
                deleteLabel.setToolTipText("Delete");

                rightPanel.add(renameLabel, BorderLayout.WEST);
                rightPanel.add(deleteLabel, BorderLayout.EAST);
                add(rightPanel, BorderLayout.EAST);

                if (cellHasFocus || isSelected) {
                    Color selBg = javax.swing.UIManager.getColor("ing.selectionBackground");
                    if (selBg == null) {
                        selBg = new Color(216, 191, 255); // fallback purple
                    }
                    setBackground(selBg);
                } else {
                    setBackground(Color.WHITE);
                }
                setOpaque(true);

                return this;
            }
        }
    }
}
