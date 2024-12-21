
package com.ing.ide.main.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

public class ConsolePanel extends JPanel {

    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private final JTextComponent consoleView;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        consoleView = new JTextPane();
        
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        
        consoleView.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        consoleView.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        consoleView.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        consoleView.getActionMap().put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                consoleView.selectAll();
            }
        });
        consoleView.setEditable(false);
        consoleView.setFont(FONT);
        add(new JScrollPane(consoleView), BorderLayout.CENTER);
    }

    public void start() {
        consoleView.setText("");
        MessageConsole messageConsole = new MessageConsole(consoleView, true);
        messageConsole.redirectOut();
        messageConsole.redirectErr(Color.RED);
    }

}
