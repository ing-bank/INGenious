
package com.ing.ide.main.utils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Credit to
 * http://java-swing-tips.blogspot.in/2008/09/double-click-on-each-tab-and-change-its.html
 *
 */
public class TabTitleEditListener extends MouseAdapter implements ChangeListener {

    private final JTabbedPane tabbedPane;
    private final RenamePopup rPopup;

    private int editingIdx = -1;

    private final Action onRenameAction;

    private Action onMiddleClickAction;

    private final int[] dontEdit;

    public TabTitleEditListener(final JTabbedPane tabbedPane, Action onrenameAction, int... dontEdit) {
        super();
        this.tabbedPane = tabbedPane;
        rPopup = new RenamePopup();
        this.dontEdit = dontEdit;
        this.onRenameAction = onrenameAction;
        tabbedPane.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "start-editing");
        tabbedPane.getActionMap().put("start-editing", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkIndexContains()) {
                    startEditing();
                }
            }
        });
    }

    private Boolean checkIndexContains() {
        if (dontEdit != null) {
            for (int index : dontEdit) {
                if (tabbedPane.getSelectedIndex() == index) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        renameTabTitle();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (tabbedPane.getSelectedIndex() != -1) {
            Rectangle rect = tabbedPane.getUI().getTabBounds(tabbedPane, tabbedPane.getSelectedIndex());
            if (rect.contains(e.getPoint())) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    if (checkIndexContains()) {
                        startEditing();
                    }
                } else if (SwingUtilities.isMiddleMouseButton(e)) {
                    if (onMiddleClickAction != null) {
                        onMiddleClickAction.actionPerformed(null);
                    }
                }

            } else {
                renameTabTitle();
            }
        }
    }

    public void setOnMiddleClickAction(Action onMiddleClickAction) {
        this.onMiddleClickAction = onMiddleClickAction;
    }

    private void startEditing() {
        editingIdx = tabbedPane.getSelectedIndex();
        rPopup.showPopup();
    }

    private void cancelEditing() {
        if (editingIdx >= 0) {
            editingIdx = -1;
            rPopup.hidePopup();
            tabbedPane.requestFocusInWindow();
        }
    }

    private void renameTabTitle() {
        String title = rPopup.editor.getText().trim();
        if (editingIdx >= 0 && !title.isEmpty()) {
            String prevTitle = tabbedPane.getTitleAt(editingIdx);
            if (!prevTitle.equals(title)) {
                onRenameAction.putValue("oldValue", prevTitle);
                onRenameAction.putValue("newValue", title);
                onRenameAction.actionPerformed(null);
                if (onRenameAction.getValue("rename").equals(true)) {
                    tabbedPane.setTitleAt(editingIdx, title);
                }
            }
        }
        cancelEditing();
    }

    class RenamePopup extends JPopupMenu {

        JTextField editor = new JTextField();

        public RenamePopup() {
            add(editor);
            init();
        }

        private void init() {
            setBorder(BorderFactory.createEmptyBorder());
            editor.setBorder(BorderFactory.createEmptyBorder());
            editor.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    hidePopup();
                    renameTabTitle();
                }
            });
            editor.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    hidePopup();
                    renameTabTitle();
                }
            });
            editor.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ESCAPE:
                            cancelEditing();
                            break;
                    }
                }
            });
        }

        public void showPopup() {
            editor.setText(tabbedPane.getTitleAt(editingIdx));

            Rectangle labelRectangle = tabbedPane.getBoundsAt(editingIdx);
            setPreferredSize(new Dimension(labelRectangle.width, labelRectangle.height));
            show(tabbedPane, labelRectangle.x, labelRectangle.y);
            editor.requestFocusInWindow();
            editor.selectAll();
        }

        public void hidePopup() {
            setVisible(false);
        }

    }
}
