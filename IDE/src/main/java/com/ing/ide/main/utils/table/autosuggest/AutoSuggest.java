package com.ing.ide.main.utils.table.autosuggest;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 *
 *
 */
public class AutoSuggest extends JComboBox<String> {
    private final List<String> searchList = new ArrayList<>();

    JTextField textField;

    DefaultComboBoxModel model;

    AutoSuggestKeyHandler handler;

    private Action onHide;

    public AutoSuggest() {
        setEditable(true);
        textField = (JTextField) getEditor().getEditorComponent();

        alterDefaultKeyBindings();

        textField.setText("");
        handler = new AutoSuggestKeyHandler();
        textField.addKeyListener(handler);
        textField.addFocusListener(
            new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent fe) {
                    beforeShow();
                    if (!searchList.isEmpty()) {
                        showPopup();
                    }
                }

                @Override
                public void focusLost(FocusEvent fe) {
                    handler.shouldHide = true;
                    if (onHide != null) {
                        onHide.actionPerformed(null);
                    }
                }
            }
        );
        setSelectedIndex(-1);
    }

    /**
     * Help from http://stackoverflow.com/a/38913548/3122133
     */
    @Override
    public void updateUI() {
        setUI(
            new BasicComboBoxUI() {

                @Override
                protected JButton createArrowButton() {
                    JButton button = new JButton() {

                        @Override
                        public int getWidth() {
                            return 0;
                        }
                    };
                    button.setBorder(BorderFactory.createEmptyBorder());
                    button.setVisible(false);
                    return button;
                }

                @Override
                public void configureArrowButton() {}
            }
        );
        setBorder(BorderFactory.createEmptyBorder());
        JComponent c = (JComponent) getEditor().getEditorComponent();
        c.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    @Override
    public boolean contains(int x, int y) {
        Insets i = getInsets();
        int w = getWidth() - i.left - i.right;
        int h = getHeight() - i.top - i.bottom;
        return (x >= i.left) && (x < w) && (y >= i.top) && (y < h);
    }

    public final AutoSuggest withSearchList(List<String> items) {
        setSearchList(items);
        removeAllItems();
        for (String item : items) {
            addItem(item);
        }
        return this;
    }

    public final void setSearchList(List<String> items) {
        searchList.clear();
        searchList.addAll(items);
    }

    public final void clearSearchList() {
        searchList.clear();
    }

    public void reset() {
        Object old = getSelectedItem();
        String val = Objects.toString(old, "");
        if (val.isEmpty()) {
            val = textField.getText();
        }
        removeAllItems();
        setSelectedItem(old);
        beforeSearch(val);
        for (String item : searchList) {
            addItem(item);
        }
        setSelectedItem(preReset(val));
        afterReset();
    }

    public String preReset(String val) {
        return val;
    }

    public AutoSuggest withOnHide(Action onHide) {
        this.onHide = onHide;
        return this;
    }

    public void beforeShow() {}

    public void afterReset() {}

    public final String getText() {
        return textField.getText();
    }

    public String getSearchString() {
        return getText();
    }

    public final JTextField getTextField() {
        return textField;
    }

    public final void updateList() {
        handler.shouldHide = false;
        handler.keyTyped(null);
    }

    public final Boolean isEditing() {
        return !handler.shouldHide;
    }

    public void beforeSearch(String text) {}

    private boolean isHeaderItem(Object o) {
        if (o instanceof String) {
            String s = (String) o;
            return s.startsWith("__HEADER__:");
        }
        return false;
    }

    private int findNextSelectableIndex(int startIndex, int direction) {
        ComboBoxModel<String> mdl = getModel();
        if (mdl == null) return -1;
        int size = mdl.getSize();
        int i = startIndex;
        while (i >= 0 && i < size) {
            Object el = mdl.getElementAt(i);
            if (!isHeaderItem(el)) return i;
            i += direction;
        }
        return -1;
    }

    @Override
    public void setSelectedItem(Object anObject) {
        // When attempting to select a header token, skip it and select the next selectable item
        if (isHeaderItem(anObject)) {
            // try to find next selectable in current model
            ComboBoxModel<String> mdl = getModel();
            if (mdl != null) {
                int idx = -1;
                // prefer next item forward
                for (int i = 0; i < mdl.getSize(); i++) {
                    if (!isHeaderItem(mdl.getElementAt(i))) {
                        idx = i;
                        break;
                    }
                }
                if (idx != -1) {
                    super.setSelectedItem(mdl.getElementAt(idx));
                    return;
                }
            }
            // no selectable item found -> ignore
            return;
        }
        super.setSelectedItem(anObject);
    }

    class AutoSuggestKeyHandler extends KeyAdapter {
        private boolean shouldHide = true;

        protected AutoSuggestKeyHandler() {
            super();
        }

        @Override
        public void keyTyped(final KeyEvent e) {
            SwingUtilities.invokeLater(
                new Runnable() {

                    @Override
                    public void run() {
                        String text = textField.getText();
                        beforeSearch(text);
                        if (!searchList.isEmpty()) {
                            if (shouldHide) {
                                hidePopup();
                            } else {
                                setSuggestionModel(getSuggestedModel(), text);
                                if (isShowing()) {
                                    showPopup();
                                }
                            }
                        } else {
                            hidePopup();
                        }
                    }
                }
            );
        }

        @Override
        public void keyPressed(KeyEvent e) {
            shouldHide = false;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    if (!isPopupVisible() && !searchList.isEmpty()) {
                        setSuggestionModel(getSuggestedModel(), getSearchString());
                        showPopup();
                        e.consume();
                    } else if (isPopupVisible()) {
                        // Allow the combo box to handle navigation when popup is visible
                        int currentIndex = getSelectedIndex();
                        int next = currentIndex + 1;
                        int sel = findNextSelectableIndex(next, 1);
                        if (sel == -1 && currentIndex == -1) {
                            sel = findNextSelectableIndex(0, 1);
                        }
                        if (sel != -1) setSelectedIndex(sel);
                        e.consume();
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (!isPopupVisible() && !searchList.isEmpty()) {
                        setSuggestionModel(getSuggestedModel(), getSearchString());
                        showPopup();
                        e.consume();
                    } else if (isPopupVisible()) {
                        // Allow the combo box to handle navigation when popup is visible and skip headers
                        int currentIndex = getSelectedIndex();
                        int prev = currentIndex - 1;
                        int selPrev = findNextSelectableIndex(prev, -1);
                        if (selPrev == -1 && currentIndex == -1 && getItemCount() > 0) {
                            selPrev = findNextSelectableIndex(getItemCount() - 1, -1);
                        }
                        if (selPrev != -1) setSelectedIndex(selPrev);
                        e.consume();
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    shouldHide = true;
                    break;
                case KeyEvent.VK_ESCAPE:
                    shouldHide = true;
                    break;
                case KeyEvent.VK_TAB:
                    setSelectedItem(getSearchString());
                    shouldHide = true;
                default:
                    break;
            }
        }
    }

    private void setSuggestionModel(ComboBoxModel<String> mdl, String str) {
        setModel(mdl);
        setSelectedIndex(-1);
        textField.setText(str);
    }

    @Override
    public void setSelectedIndex(int index) {
        ComboBoxModel<String> mdl = getModel();
        if (mdl == null) {
            super.setSelectedIndex(index);
            return;
        }
        if (index >= 0 && index < mdl.getSize()) {
            Object el = mdl.getElementAt(index);
            if (isHeaderItem(el)) {
                // ignore selecting headers
                return;
            }
        }
        super.setSelectedIndex(index);
    }

    private ComboBoxModel<String> getSuggestedModel() {
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        String search = getSearchString().toLowerCase();
        for (String s : searchList) {
            // Always include header tokens (special items starting with __HEADER__:) so the dropdown shows section headers
            if (s != null && s.startsWith("__HEADER__:")) {
                m.addElement(s);
                continue;
            }
            if (s != null && s.toLowerCase().contains(search)) {
                m.addElement(s);
            }
        }
        if (m.getSize() == 0) {
            m = new DefaultComboBoxModel<>(searchList.toArray(new String[searchList.size()]));
        }
        return m;
    }

    private void alterDefaultKeyBindings() {
        // Customize key bindings
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Remove default Ctrl key bindings
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "none");

        // Add Cmd key bindings
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        textField
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        textField
            .getActionMap()
            .put(
                "selectAll",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        textField.selectAll();
                    }
                }
            );
    }
}
