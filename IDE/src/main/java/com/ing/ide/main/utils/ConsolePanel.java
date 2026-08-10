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
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ConsolePanel extends JPanel {
    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    private final JTextComponent consoleView;
    private final StyledDocument document;
    private final SimpleAttributeSet defaultAttributes;
    private final SimpleAttributeSet errorAttributes;

    public ConsolePanel() {
        setLayout(new BorderLayout());
        consoleView = new JTextPane();
        document = ((JTextPane) consoleView).getStyledDocument();
        defaultAttributes = new SimpleAttributeSet();
        errorAttributes = new SimpleAttributeSet();
        StyleConstants.setForeground(errorAttributes, Color.RED);

        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        consoleView
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        consoleView
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        consoleView
            .getInputMap()
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        consoleView
            .getActionMap()
            .put(
                "selectAll",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        consoleView.selectAll();
                    }
                }
            );
        consoleView.setEditable(false);
        consoleView.setFont(FONT);
        add(new JScrollPane(consoleView), BorderLayout.CENTER);
    }

    public void start() {
        clear();
        MessageConsole messageConsole = new MessageConsole(consoleView, true);
        messageConsole.redirectOut();
        messageConsole.redirectErr(Color.RED);
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> consoleView.setText(""));
    }

    public void appendLine(String text) {
        append(text, defaultAttributes);
    }

    public void appendErrorLine(String text) {
        append(text, errorAttributes);
    }

    private void append(String text, SimpleAttributeSet attributes) {
        String line = text == null ? "null" : text;
        SwingUtilities.invokeLater(
            () -> {
                try {
                    document.insertString(
                        document.getLength(),
                        line + System.lineSeparator(),
                        attributes
                    );
                    consoleView.setCaretPosition(document.getLength());
                } catch (BadLocationException ex) {
                    throw new IllegalStateException("Unable to append to console", ex);
                }
            }
        );
    }
}
