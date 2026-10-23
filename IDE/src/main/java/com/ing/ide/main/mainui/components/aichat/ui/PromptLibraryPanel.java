package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.model.PromptTemplate;
import com.ing.ide.main.mainui.components.aichat.skills.PromptLibrary;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Collapsible accordion of prompt chips built from {@link PromptLibrary}.
 * Clicking a chip substitutes live IDE tokens into the prompt template and
 * hands the result to the supplied consumer (which fills the chat input).
 */
public class PromptLibraryPanel extends JPanel {
    private final Consumer<String> onPromptChosen;
    private final UnaryOperator<String> tokenResolver;

    /**
     * @param onPromptChosen receives the fully-substituted prompt text
     * @param tokenResolver  resolves a token name (e.g. {@code currentProject})
     *                       to its live value; may return {@code null}
     */
    public PromptLibraryPanel(
        Consumer<String> onPromptChosen,
        UnaryOperator<String> tokenResolver
    ) {
        this.onPromptChosen = onPromptChosen;
        this.tokenResolver = tokenResolver;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        build();
    }

    private void build() {
        Map<String, List<PromptTemplate>> sections = PromptLibrary.bySection();
        boolean first = true;
        for (Map.Entry<String, List<PromptTemplate>> entry : sections.entrySet()) {
            addSection(entry.getKey(), entry.getValue(), first);
            first = false;
        }
    }

    private void addSection(String title, List<PromptTemplate> prompts, boolean expanded) {
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        chips.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (PromptTemplate p : prompts) {
            chips.add(createChip(p));
        }
        chips.setVisible(expanded);

        JButton header = new JButton((expanded ? "▾ " : "▸ ") + title);
        header.setHorizontalAlignment(SwingConstants.LEFT);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 11f));
        header.setFocusPainted(false);
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        header.setContentAreaFilled(false);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        header.addActionListener(
            e -> {
                boolean vis = !chips.isVisible();
                chips.setVisible(vis);
                header.setText((vis ? "▾ " : "▸ ") + title);
                revalidate();
                repaint();
            }
        );

        add(header);
        add(chips);
    }

    private JButton createChip(PromptTemplate template) {
        JButton chip = new JButton(template.getLabel());
        chip.setFont(chip.getFont().deriveFont(11f));
        chip.setFocusPainted(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setToolTipText(previewTooltip(template));
        chip.addActionListener(e -> onPromptChosen.accept(substitute(template.getTemplate())));
        return chip;
    }

    private String previewTooltip(PromptTemplate template) {
        String text = substitute(template.getTemplate());
        String oneLine = text.replaceAll("\\s+", " ").trim();
        if (oneLine.length() > 140) {
            oneLine = oneLine.substring(0, 137) + "…";
        }
        return "<html><body style='width:320px'>" + escape(oneLine) + "</body></html>";
    }

    /** Substitutes {@code ${token}} placeholders using the resolver, with
     *  guide-friendly defaults when no live value is available. */
    private String substitute(String template) {
        String out = template;
        out = replaceToken(out, "currentProject", "CLIDemo");
        out = replaceToken(out, "currentScenario", "APIBasics");
        out = replaceToken(out, "currentTestCase", "GetUsers");
        return out;
    }

    private String replaceToken(String text, String token, String fallback) {
        String placeholder = "${" + token + "}";
        if (!text.contains(placeholder)) {
            return text;
        }
        String value = null;
        try {
            value = tokenResolver.apply(token);
        } catch (Exception ignore) {
            // fall through to fallback
        }
        if (value == null || value.isEmpty()) {
            value = fallback;
        }
        return text.replace(placeholder, value);
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** A compact label used by the toggle in the top bar. */
    public static final String TITLE = "Prompts";

    // Keep a stable preferred width so the accordion wraps nicely in the sidebar.
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.min(d.width, 340), d.height);
    }
}
