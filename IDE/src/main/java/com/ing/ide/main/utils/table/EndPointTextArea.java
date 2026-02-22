
package com.ing.ide.main.utils.table;

import com.ing.datalib.component.TestStep;
import com.ing.ide.main.Main;
import com.ing.ide.util.WindowMover;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 *
 * 
 */
public class EndPointTextArea extends javax.swing.JDialog {

    // Theme colors
    private static final Color DARK_BG = new Color(40, 42, 54);
    private static final Color DARK_FG = new Color(248, 248, 242);
    private static final Color DARK_TITLEBAR_BG = new Color(33, 34, 44);
    private static final Color LIGHT_BG = new Color(255, 255, 255);
    private static final Color LIGHT_FG = new Color(30, 30, 30);
    private static final Color LIGHT_TITLEBAR_BG = new Color(243, 243, 243);
    private static final Color CLOSE_RED = new Color(220, 53, 69);
    
    TestStep currentStep;
    DefaultCompletionProvider provider;

    public EndPointTextArea(java.awt.Frame parent, TestStep step, List<String> searchStr) {
        super(parent);
        this.currentStep = step;
        initComponents();
        installAutoComplete(searchStr);
        addTitleBar();
        jTextArea1.setText(step.getInput());
        setLocationRelativeTo(parent);
        initCloseListener();
        setVisible(true);
    }

    private void initCloseListener() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Optional.ofNullable(jTextArea1.getText())
                        .filter((val) -> !val.trim().isEmpty())
                        .map((val) -> val.startsWith("@") ? val : "@" + val)
                        .ifPresent(currentStep::setInput);
                dispose();
            }
        });
    }

    private void installAutoComplete(List<String> searchStr) {
        ((RSyntaxTextArea) jTextArea1).setCodeFoldingEnabled(true);
        ((RSyntaxTextArea) jTextArea1).setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
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
    
    private void addTitleBar() {
        boolean dark = Main.isDarkMode();
        Color titleBarBg = dark ? DARK_TITLEBAR_BG : LIGHT_TITLEBAR_BG;
        Color titleBarFg = dark ? DARK_FG : LIGHT_FG;
        Color panelBg = dark ? DARK_BG : LIGHT_BG;
        
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(titleBarBg);
        titleBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        // Make the dialog draggable by the title bar
        WindowMover.register(this, titleBar, WindowMover.MOVE_BOTH);
        
        JLabel titleLabel = new JLabel("End Point Editor");
        titleLabel.setForeground(titleBarFg);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleBar.add(titleLabel, BorderLayout.WEST);
        
        JButton closeBtn = new JButton("×");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(CLOSE_RED);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setOpaque(true);
        closeBtn.setBorderPainted(false);
        closeBtn.addActionListener(e -> {
            Optional.ofNullable(jTextArea1.getText())
                    .filter((val) -> !val.trim().isEmpty())
                    .map((val) -> val.startsWith("@") ? val : "@" + val)
                    .ifPresent(currentStep::setInput);
            dispose();
        });
        titleBar.add(closeBtn, BorderLayout.EAST);
        
        // Update layout to add title bar
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(panelBg);
        contentPanel.add(titleBar, BorderLayout.NORTH);
        contentPanel.add(jScrollPane2, BorderLayout.CENTER);
        
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(contentPanel, BorderLayout.CENTER);
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

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 860, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
