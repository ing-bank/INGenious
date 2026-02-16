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
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxUI;

/**
 *
 * @author Julie Ann Ayap
 */
public class InputMainAutoSuggest extends JComboBox<String> {

    private final List<String> searchList = new ArrayList<>();

    JTextField textField;

    DefaultComboBoxModel model;

    AutoSuggestKeyHandler handler;

    private Action onHide;
    
    JTable table;

    public InputMainAutoSuggest() {
        setEditable(true);
        textField = (JTextField) getEditor().getEditorComponent();

        alterDefaultKeyBindings();

        textField.setText("");
        handler = new AutoSuggestKeyHandler();
        textField.addKeyListener(handler);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent fe) {
                boolean isStringOpsEditor = isStringOpsEditor();
                if (!isStringOpsEditor()) {
                    beforeShow();
                    if (!searchList.isEmpty()) {
                        showPopup();
                    }
                } 
            }

            @Override
            public void focusLost(FocusEvent fe) {
                handler.shouldHide = true;
                if (onHide != null) {
                    onHide.actionPerformed(null);
                }
            }

        });
        setSelectedIndex(-1);
    }
    
    public void setTable(JTable table){
        this.table = table;
    }
    
    private boolean isStringOpsEditor(){
        int row = this.table.getSelectedRow();
        String value = "";
        if(row >= 0)
            value = this.table.getModel().getValueAt(row, 1).toString();
        if(!value.matches("String Operations"))
            return false;
        
        return true;     
    }

    /**
     * Help from http://stackoverflow.com/a/38913548/3122133
     */
    @Override
    public void updateUI() {
        setUI(new BasicComboBoxUI() {
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
            public void configureArrowButton() {
            }
        });
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

    public final InputMainAutoSuggest withSearchList(List<String> items) {
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

    public InputMainAutoSuggest withOnHide(Action onHide) {
        this.onHide = onHide;
        return this;
    }

    public void beforeShow() {

    }

    public void afterReset() {
    }

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

    public void beforeSearch(String text) {

    }

    class AutoSuggestKeyHandler extends KeyAdapter {

        private boolean shouldHide = true;

        protected AutoSuggestKeyHandler() {
            super();
        }

        @Override
        public void keyTyped(final KeyEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    String text = textField.getText();
                    beforeSearch(text);
                    boolean isStringOpsEditor = isStringOpsEditor();
                    if (!searchList.isEmpty()) {
                        if (shouldHide) {
                            hidePopup();
                        } else {
                            if(!isStringOpsEditor) {
                                setSuggestionModel(getSuggestedModel(), text);
                                if (isShowing()) {
                                    showPopup();
                                }
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

    private ComboBoxModel<String> getSuggestedModel() {
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (String s : searchList) {
            if (s.toLowerCase().contains(getSearchString().toLowerCase())) {
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
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "none");

        // Add Cmd key bindings
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        textField.getActionMap().put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textField.selectAll();
            }
        });

    }
    
}
