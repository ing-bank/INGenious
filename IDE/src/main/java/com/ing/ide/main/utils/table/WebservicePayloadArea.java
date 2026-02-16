
package com.ing.ide.main.utils.table;

import com.ing.datalib.component.TestStep;
import com.ing.ide.main.Main;
import com.ing.ide.main.utils.CodeFormatter;
import com.ing.ide.main.fx.INGIcons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 *
 * 
 */
public class WebservicePayloadArea extends javax.swing.JDialog {

    // ===== Dracula Theme (Dark Mode) =====
    private static final Color DARK_BG = new Color(40, 42, 54);           // #282A36
    private static final Color DARK_FG = new Color(248, 248, 242);        // #F8F8F2
    private static final Color DARK_CURRENT_LINE = new Color(68, 71, 90); // #44475A
    private static final Color DARK_SELECTION = new Color(68, 71, 90);    // #44475A
    private static final Color DARK_TOOLBAR_BG = new Color(33, 34, 44);   // #21222C
    private static final Color DARK_LINE_NUM_BG = new Color(33, 34, 44);  // #21222C
    private static final Color DARK_LINE_NUM_FG = new Color(98, 114, 164);// #6272A4 (purple-gray)
    
    // ===== Light Theme =====
    private static final Color LIGHT_BG = new Color(255, 255, 255);       // #FFFFFF
    private static final Color LIGHT_FG = new Color(30, 30, 30);          // #1E1E1E
    private static final Color LIGHT_CURRENT_LINE = new Color(245, 245, 245); // #F5F5F5
    private static final Color LIGHT_SELECTION = new Color(173, 214, 255);// #ADD6FF
    private static final Color LIGHT_TOOLBAR_BG = new Color(243, 243, 243); // #F3F3F3
    private static final Color LIGHT_LINE_NUM_BG = new Color(243, 243, 243); // #F3F3F3
    private static final Color LIGHT_LINE_NUM_FG = new Color(133, 133, 133); // #858585
    
    // Accent colors (same for both themes)
    private static final Color ACCENT_BLUE = new Color(33, 136, 255);     // #2188FF
    private static final Color CLOSE_RED = new Color(220, 53, 69);        // #DC3545
    
    TestStep currentStep;
    DefaultCompletionProvider provider;
    private String payloadType; // "SOAP" for XML, "REST" for JSON
    private RTextScrollPane scrollPane;

    public WebservicePayloadArea(java.awt.Frame parent, TestStep step, String stepType, List<String> searchStr) {
        super(parent);
        this.currentStep = step;
        this.payloadType = stepType;
        initComponents();
        if(stepType.equals("SOAP"))
        {
        installAutoComplete(searchStr, SyntaxConstants.SYNTAX_STYLE_XML);
        setTitle("XML Payload Editor");
        }
        if(stepType.equals("REST"))
        {
        installAutoComplete(searchStr, SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
        setTitle("JSON Payload Editor");
        }
        addToolbar();
        jTextArea1.setText(step.getInput());
        setLocationRelativeTo(parent);
        initCloseListener();
        setVisible(true);
    }
    
    private void addToolbar() {
        boolean dark = Main.isDarkMode();
        Color toolbarBg = dark ? DARK_TOOLBAR_BG : LIGHT_TOOLBAR_BG;
        Color panelBg = dark ? DARK_BG : LIGHT_BG;
        
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new EmptyBorder(6, 10, 6, 10));
        toolbar.setBackground(toolbarBg);
        toolbar.setOpaque(true);
        
        JButton beautifyBtn = createToolbarButton("Beautify", "format", ACCENT_BLUE, this::beautifyCode);
        toolbar.add(beautifyBtn);
        
        toolbar.add(javax.swing.Box.createHorizontalGlue()); // Push close to right
        
        JButton closeBtn = createToolbarButton("Close", "close", CLOSE_RED, this::closeAndSave);
        toolbar.add(closeBtn);
        
        // Add toolbar at the top
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(panelBg);
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPanel.add(toolbar, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        getContentPane().removeAll();
        getContentPane().setBackground(panelBg);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        pack();
        setSize(1100, 700);
    }
    
    private JButton createToolbarButton(String text, String iconKey, Color bgColor, Runnable action) {
        JButton button = new JButton(text);
        button.setIcon(INGIcons.swing(iconKey, 16, Color.WHITE));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("format".equals(iconKey) ? text + " (Ctrl+Shift+F)" : text);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        button.addActionListener(e -> action.run());
        
        // Hover effect
        final Color originalBg = bgColor;
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(originalBg.brighter());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalBg);
            }
        });
        
        // Add keyboard shortcut for beautify only
        if ("format".equals(iconKey)) {
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), 
                    "beautify");
            getRootPane().getActionMap().put("beautify", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
        }
        
        return button;
    }
    
    private void closeAndSave() {
        Optional.ofNullable(jTextArea1.getText())
                .filter((val) -> !val.trim().isEmpty())
                .map((val) -> val.startsWith("@") ? val : val)
                .ifPresent(currentStep::setInput);
        dispose();
    }
    
    private void beautifyCode() {
        String text = jTextArea1.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        String formatted;
        if ("SOAP".equals(payloadType)) {
            // XML formatting
            formatted = CodeFormatter.beautifyXml(text);
        } else {
            // JSON formatting
            formatted = CodeFormatter.beautifyJson(text);
        }
        
        jTextArea1.setText(formatted);
    }

    private void initCloseListener() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Optional.ofNullable(jTextArea1.getText())
                        .filter((val) -> !val.trim().isEmpty())
                        .map((val) -> val.startsWith("@") ? val : val)
                        .ifPresent(currentStep::setInput);
                dispose();
            }
        });
    }

    private void installAutoComplete(List<String> searchStr, String syntaxConstants) {
        RSyntaxTextArea textArea = (RSyntaxTextArea) jTextArea1;
        boolean dark = Main.isDarkMode();
        
        // Apply theme-aware colors
        Color editorBg = dark ? DARK_BG : LIGHT_BG;
        Color editorFg = dark ? DARK_FG : LIGHT_FG;
        Color currentLine = dark ? DARK_CURRENT_LINE : LIGHT_CURRENT_LINE;
        Color selection = dark ? DARK_SELECTION : LIGHT_SELECTION;
        Color gutterBg = dark ? DARK_LINE_NUM_BG : LIGHT_LINE_NUM_BG;
        Color gutterFg = dark ? DARK_LINE_NUM_FG : LIGHT_LINE_NUM_FG;
        
        textArea.setBackground(editorBg);
        textArea.setForeground(editorFg);
        textArea.setCaretColor(editorFg);
        textArea.setCurrentLineHighlightColor(currentLine);
        textArea.setSelectionColor(selection);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setAntiAliasingEnabled(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setTabSize(4);
        textArea.setMarginLineEnabled(false);
        textArea.setFadeCurrentLineHighlight(true);
        textArea.setHighlightCurrentLine(true);
        textArea.setRoundedSelectionEdges(true);
        textArea.setLineWrap(false); // Don't wrap lines for JSON/XML
        textArea.setSyntaxEditingStyle(syntaxConstants);
        
        // Create scroll pane with line numbers
        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setLineNumbersEnabled(true);
        scrollPane.getGutter().setBackground(gutterBg);
        scrollPane.getGutter().setLineNumberColor(gutterFg);
        scrollPane.getGutter().setLineNumberFont(new Font("Consolas", Font.PLAIN, 12));
        scrollPane.getGutter().setBorderColor(dark ? DARK_CURRENT_LINE : LIGHT_CURRENT_LINE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        provider = new DefaultCompletionProvider();
        setSearchString(searchStr);
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(jTextArea1);
    }

    private void setSearchString(List<String> searchString) {
        searchString.stream().forEach((string) -> {
            provider.addCompletion(new ShorthandCompletion(provider, string, "{" + string + "}"));
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new RSyntaxTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setModal(true);
        setUndecorated(true);

        jTextArea1.setBackground(new java.awt.Color(255, 255, 204));
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setDragEnabled(true);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
