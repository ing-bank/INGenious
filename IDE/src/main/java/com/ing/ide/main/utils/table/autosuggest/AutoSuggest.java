
package com.ing.ide.main.utils.table.autosuggest;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;

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
        textField.setText("");
        handler = new AutoSuggestKeyHandler();
        textField.addKeyListener(handler);
        textField.addFocusListener(new FocusAdapter() {
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

        });
        setSelectedIndex(-1);
    }

    /**
     * Help from http://stackoverflow.com/a/38913548/3122133
     */
    @Override
    public void updateUI() {
        //super.updateUI();
        setUI(new javax.swing.plaf.synth.SynthComboBoxUI() {
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
        UIDefaults d = new UIDefaults();

        Painter<JComponent> emptyPainter = new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                /* Empty painter */
            }
        };
        d.put("TextField.borderPainter", emptyPainter);
        d.put("TextField[Enabled].borderPainter", emptyPainter);
        d.put("TextField[Focused].borderPainter", emptyPainter);
        d.put("ComboBox:\"ComboBox.textField\"[Enabled].backgroundPainter", emptyPainter);
        d.put("ComboBox:\"ComboBox.textField\"[Selected].backgroundPainter", emptyPainter);
        d.put("ComboBox[Editable+Focused].backgroundPainter", emptyPainter);
        putClientProperty("Nimbus.Overrides", d);
        JComponent c = (JComponent) getEditor().getEditorComponent();
        c.putClientProperty("Nimbus.Overrides", d);
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
        Object old=getSelectedItem();
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
}
