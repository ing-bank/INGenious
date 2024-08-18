
package com.ing.ide.main.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;

public class ConsolePanel extends JPanel {

    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private final JTextComponent consoleView;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        consoleView = new JTextPane();
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
