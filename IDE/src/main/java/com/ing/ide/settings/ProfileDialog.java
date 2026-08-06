package com.ing.ide.main.ui;

import com.ing.ide.settings.ProfileConfig;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * Dialog for editing the user profile.
 * Stores System Under Test (SUT) and PCode in Configuration/profile.properties.
 */
public class ProfileDialog extends JDialog {
    private final JTextField sutField = new JTextField();
    private final JTextField pcodeField = new JTextField();
    private static final String PCODE_FORMAT = "^p\\d{5}$";

    private static final Color MODERN_BG = new Color(255, 255, 255);
    private static final Color MODERN_PANEL_BG = new Color(248, 250, 252);
    private static final Color MODERN_BORDER = new Color(226, 232, 240);
    private static final Color MODERN_TEXT = new Color(30, 41, 59);
    private static final Color MODERN_ACCENT = new Color(59, 130, 246);
    private static final Color MODERN_INPUT_BG = Color.WHITE;
    private static final Color VALIDATION_BORDER = new Color(220, 38, 38);

    public ProfileDialog(Frame owner) {
        super(owner, "Profile", true);
        initUI();
        loadValues();
    }

    private void initUI() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(MODERN_BG);

        // ── Form panel ──
        JPanel form = new JPanel(new BorderLayout(0, 14));
        form.setBackground(MODERN_BG);
        form.setBorder(new EmptyBorder(20, 24, 12, 24));

        JLabel title = new JLabel("User Profile");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(MODERN_TEXT);
        form.add(title, BorderLayout.NORTH);

        JPanel fields = new JPanel(new BorderLayout(0, 12));
        fields.setBackground(MODERN_BG);

        fields.add(createField("System Under Test (SUT)", sutField), BorderLayout.NORTH);
        fields.add(createField("PCode", pcodeField), BorderLayout.CENTER);

        form.add(fields, BorderLayout.CENTER);
        add(form, BorderLayout.CENTER);

        // ── Button panel ──
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        buttons.setBackground(MODERN_PANEL_BG);
        buttons.setBorder(new MatteBorder(1, 0, 0, 0, MODERN_BORDER));

        JButton cancel = new JButton("Cancel");
        styleSecondary(cancel);
        cancel.addActionListener(e -> dispose());

        JButton save = new JButton("Save");
        stylePrimary(save);
        save.addActionListener(e -> saveProfile());

        buttons.add(cancel);
        buttons.add(save);
        add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(save);

        // ── ESC closes ──
        getRootPane()
            .registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );

        pack();
        setSize(420, getHeight());
        setResizable(false);
        setLocationRelativeTo(getOwner());
    }

    private JPanel createField(String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(MODERN_TEXT);
        label.setBorder(new EmptyBorder(0, 0, 4, 0));

        field.setBackground(MODERN_INPUT_BG);
        field.setForeground(MODERN_TEXT);
        field.setCaretColor(MODERN_ACCENT);
        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MODERN_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
            )
        );
        field.putClientProperty("JComponent.roundRect", true);
        field.setColumns(24);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(MODERN_BG);
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    public void highlightMissingRequiredFields(boolean sutMissing, boolean pcodeMissing) {
        highlightField(sutField, sutMissing, "System Under Test");
        highlightField(pcodeField, pcodeMissing, "PCode");

        if (sutMissing || pcodeMissing) {
            JTextField focusField = sutMissing ? sutField : pcodeField;
            SwingUtilities.invokeLater(() -> {
                focusField.requestFocusInWindow();
                focusField.selectAll();
            });
        }
    }

    private void highlightField(JTextField field, boolean isMissing, String fieldLabel) {
        if (isMissing) {
            field.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(VALIDATION_BORDER, 2),
                    new EmptyBorder(7, 11, 7, 11)
                )
            );
            field.setToolTipText(fieldLabel + " is required");
        } else {
            field.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MODERN_BORDER, 1),
                    new EmptyBorder(8, 12, 8, 12)
                )
            );
            field.setToolTipText(null);
        }
    }

    private void stylePrimary(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(MODERN_ACCENT);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(8, 24, 8, 24));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JButton.arc", 8);
    }

    private void styleSecondary(JButton button) {
        button.setFocusPainted(false);
        button.setBackground(MODERN_INPUT_BG);
        button.setForeground(MODERN_TEXT);
        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MODERN_BORDER, 1),
                new EmptyBorder(7, 23, 7, 23)
            )
        );
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("JButton.arc", 8);
    }

    private void loadValues() {
        sutField.setText(ProfileConfig.getSut());
        pcodeField.setText(ProfileConfig.getPcode());
    }

    private void saveProfile() {
        String sut = sutField.getText();
        String pcode = pcodeField.getText();
        boolean sutMissing = sut == null || sut.trim().isEmpty();
        boolean pcodeMissing = pcode == null || pcode.trim().isEmpty();

        if (sutMissing || pcodeMissing) {
            highlightMissingRequiredFields(sutMissing, pcodeMissing);
            return;
        }

        String normalizedPcode = pcode.trim().toLowerCase();
        if (!normalizedPcode.matches(PCODE_FORMAT)) {
            highlightMissingRequiredFields(false, true);
            pcodeField.setToolTipText("PCode format must be p followed by 5 digits (example: p33148)");
            return;
        }

        ProfileConfig.save(sut.trim(), normalizedPcode);
        dispose();
    }
}
