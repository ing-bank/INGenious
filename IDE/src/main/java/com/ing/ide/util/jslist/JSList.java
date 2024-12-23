
package com.ing.ide.util.jslist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.toList;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 *
 * 
 * @param <T>
 */
public class JSList<T> extends JPanel {

    private static final ImageIcon ADD_NEW_ICON = new ImageIcon(JSList.class.getResource("/ui/resources/addNew.png"));
    private ListPanel listPanel;
    private TopBar topBar;
    private Consumer<List<T>> onSelect;
    private Consumer<T> onRemove;
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

    public JSList(List srcmodel, Function<T, String> mapper,
            Function<String, T> onAdd, BiPredicate<String, String> predicate) {
        this.onAdd = onAdd;
        this.mapper = mapper;
        fltrmodel = new FilterModel(srcmodel, mapper, predicate);
        setLayout(new java.awt.BorderLayout());
        topBar = new TopBar(fltrmodel::doFilter);
        add(topBar, java.awt.BorderLayout.NORTH);
        listPanel = new ListPanel(fltrmodel, mapper);
        add(listPanel, java.awt.BorderLayout.CENTER);
        setSize(300, 380);
    }

    public void setOnSelect(Consumer<List<T>> onSelect) {
        this.onSelect = onSelect;
    }

    public JSList withOnRemove(Consumer<T> onRemove) {
        this.onRemove = onRemove;
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
        fltrmodel.doFilter(mapper.apply(t));
    }

    public void remove(T t) {
        if (onRemove != null) {
            onRemove.accept(t);
            fltrmodel.srcmodel.remove(t);
            selected.remove(t);
            reload();
        }
    }

    public void reload() {
        fltrmodel.doFilter(topBar.searchBox.getText());
    }

    class FilterModel extends DefaultListModel {

        List<T> srcmodel;
        BiPredicate<T, Supplier<String>> predicate;

        public FilterModel(List srcmodel, Function<T, String> mapper, BiPredicate<String, String> predicate) {
            this.srcmodel = srcmodel;
            this.predicate = (entry, key) -> predicate.test(mapper.apply(entry), key.get());
            srcmodel.stream().forEach(this::addElement);
        }

        private List<Object> items() {
            return Arrays.asList(toArray());
        }

        public void doFilter(Object keyword) {
            List<T> cselected = listPanel.getSelected();
            items().stream().filter(item -> !cselected.contains((T) item)).forEach(selected::remove);
            this.clear();
            srcmodel.stream().filter(by(keyword::toString)).forEach(this::addElement);
            listPanel.reselect();
        }

        public Predicate<T> by(Supplier<String> provider) {
            return entry -> predicate.test(entry, provider);
        }
    }

    class TopBar extends JPanel {

        JTextField searchBox = new javax.swing.JTextField(28);

        public TopBar(Consumer<Object> onUpdate) {
            JToolBar tbar = getToolbar();
            searchBox.getDocument().addDocumentListener(new SearchFieldListener(onUpdate));
            searchBox.addKeyListener(onEscape());
            JPanel textfieldWithButton = new JPanel(new BorderLayout());
            textfieldWithButton.add(withToolbar(searchBox, getClearButton(searchBox)));
            textfieldWithButton.setBorder(searchBox.getBorder());
            tbar.add(textfieldWithButton);
            if (onAdd != null) {
                JButton add = new JButton();
                add.setIcon(ADD_NEW_ICON);
                add.addActionListener(anyting -> onAddT(searchBox.getText()));
                tbar.add(add);
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

        private JButton getClearButton(JTextComponent parent) {
            JButton clear = new JButton(" x ");
            clear.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            clear.addActionListener((ActionEvent e) -> {
                parent.setText("");
            });
            return clear;
        }

        private KeyAdapter onEscape() {
            return new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        ((JTextComponent) ke.getSource()).setText("");
                    }
                }
            };
        }

        private void onAddT(String txt) {
            if (!txt.trim().isEmpty()) {
                JSList.this.add(onAdd.apply(txt));
            }
        }

        public class SearchFieldListener implements DocumentListener {

            Consumer<Object> onUpdate;

            public SearchFieldListener(Consumer<Object> fltrC) {
                this.onUpdate = fltrC;
            }

            @Override
            public void insertUpdate(DocumentEvent de) {
                updateFilter(de.getDocument());
            }

            protected void updateFilter(Document doc) {
                try {
                    onUpdate.accept(Objects.toString(doc.getText(0, doc.getLength()), ""));
                } catch (BadLocationException ex) {
                    Logger.getLogger(JSList.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                updateFilter(de.getDocument());
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                updateFilter(de.getDocument());
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
            list.setSelectionModel(new MultiSelectionModel(this::onSelect));
            list.addKeyListener(onDelete());
            int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, SHORTCUT), "SelectAll");
            list.getActionMap().put("SelectAll", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    list.setSelectionInterval(0, list.getModel().getSize() - 1);
                }
            });
        }

        private KeyAdapter onDelete() {
            return new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent ke) {
                    if (ke.getKeyCode() == KeyEvent.VK_DELETE) {
                        onRemove(((CheckBoxListRenderer) list.getCellRenderer()).getFocused());
                    }
                }
            };
        }

        private void onRemove(T t) {
            JSList.this.remove(t);
        }

        private void onSelect() {
            if (onSelect != null) {
                onSelect.accept(new ArrayList(selected));
            }
        }

        public List<T> getSelected() {
            return list.getSelectedValuesList();
        }

        private void reselect() {
            isAdjusting = true;
            items().stream().filter(selected::contains).forEach(this::selectIt);
            isAdjusting = false;
        }

        private void selectIt(Object t) {
            if (!list.isSelectedIndex(items().indexOf(t))) {
                list.setSelectedValue(t, false);
            }
        }

        private List<Object> items() {
            return ((FilterModel) list.getModel()).items();
        }

        class MultiSelectionModel extends DefaultListSelectionModel {

            Runnable r;

            public MultiSelectionModel(Runnable r) {
                this.r = r;
            }

            @Override
            public void setSelectionInterval(int index0, int index1) {
                T item = (T) items().get(index0);
                if (super.isSelectedIndex(index0)) {
                    if (selected.contains(item)) {
                        if (!isAdjusting) {
                            selected.remove(item);
                        }
                    }
                    super.removeSelectionInterval(index0, index1);
                } else {
                    selected.add(item);
                    super.addSelectionInterval(index0, index1);
                }

                r.run();
            }
        }

        class CheckBoxListRenderer extends JCheckBox implements ListCellRenderer<T> {

            private final Function<T, String> mapper;
            private final int pixels = 1;
            Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            Font fontSel = new Font(Font.SANS_SERIF, Font.BOLD, 14);
            private T focused;

            public CheckBoxListRenderer(Function<T, String> mapper) {
                super();
                this.mapper = mapper;
                init();
            }

            public T getFocused() {
                return focused;
            }

            public boolean contains(T v) {
                return selected.stream().anyMatch(v::equals);
            }

            @Override
            public Component getListCellRendererComponent(JList<? extends T> list,
                    T value, int index, boolean isSelected, boolean cellHasFocus) {
                setText(mapper.apply(value));
                if (isSelected) {
                    if (!contains(value)) {
                        selected.add(value);
                    }
                    setSelected(true);
                } else {
                    setSelected(contains(value));
                }
                if (cellHasFocus) {
                    focused = value;
                }
                this.setForeground(cellHasFocus ? Color.BLUE : Color.BLACK);
                return this;
            }

            @Override
            public void setSelected(boolean state) {
                super.setSelected(state);
                setFont(state ? fontSel : font);
            }

            public final void init() {
                Border border = BorderFactory.createEmptyBorder(pixels, 1, pixels, 1);
                this.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(1, 1, 1, 0), border));
                this.setLayout(new BorderLayout());
                setSize(this.getWidth(), 40);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                this.setFocusPainted(true);
            }

        }

    }

}
