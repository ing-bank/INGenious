
package com.ing.ide.main.mainui.components.testdesign.or;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.Highlighter.Highlight;

import com.ing.ide.main.fx.FXPanelHeader;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.sap.SapORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.structureddata.StructuredDataORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel;

/**
 * Main UI container for managing the Object Repository within Test Design.
 * <p>
 * The {@code ObjectRepo} panel provides a unified interface for switching between
 * Web and Mobile Object Repository views. It embeds both {@link WebORPanel} and
 * {@link MobileORPanel} inside a card-based layout and exposes high-level actions
 * such as loading repository data, adjusting UI layout, and navigating directly to
 * specific OR objects.
 * </p>
 *
 * <p>
 * A toggle-based toolbar allows the user to switch between repository types, and
 * the component ensures the correct panel is shown and updated when selections occur.
 * This class acts as the entry point for OR maintenance within the Test Design module.
 * </p>
 */
public class ObjectRepo extends JPanel implements ItemListener {

    private final TestDesign testDesign;

    private final SwitchToolBar switchToolBar;

    private final JPanel repos;

    private final WebORPanel webOR;

    private final MobileORPanel mobileOR;

    private final StructuredDataORPanel structuredDataOR;

    private final SapORPanel sapOR;

    FXPanelHeader header = new FXPanelHeader("Object Repository");

    public ObjectRepo(TestDesign testDesign) {
        this.testDesign = testDesign;
        switchToolBar = new SwitchToolBar();
        repos = new JPanel();
        webOR = new WebORPanel(testDesign);
        mobileOR = new MobileORPanel(testDesign);
        structuredDataOR = new StructuredDataORPanel(testDesign);
        sapOR = new SapORPanel(testDesign);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        // Create header panel with FXPanelHeader + SwitchToolBar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        header = new FXPanelHeader("Object Repository");
        headerPanel.add(header, BorderLayout.NORTH);
        headerPanel.add(switchToolBar, BorderLayout.CENTER);
        
        add(headerPanel, BorderLayout.NORTH);
        add(repos, BorderLayout.CENTER);
        initRepos();
    }

    private void initRepos() {
        repos.setLayout(new CardLayout());
        repos.setOpaque(false);
        repos.add(webOR, "Web");
        repos.add(mobileOR, "Mobile");
        repos.add(structuredDataOR, "Structured Data");
        repos.add(sapOR, "SAP");
        switchToolBar.bgroup.getElements().nextElement().setSelected(true);
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            CardLayout layout = (CardLayout) repos.getLayout();
            String command = ((JToggleButton) ie.getSource()).getActionCommand();
            layout.show(repos, command);
            // Call adjustUI after panel becomes visible to fix split pane divider
            SwingUtilities.invokeLater(() -> {
                switch (command) {
                    case "Web":
                        webOR.adjustUI();
                        break;
                    case "Mobile":
                        mobileOR.adjustUI();
                        break;
                    case "Structured Data":
                        structuredDataOR.adjustUI();
                        break;
                    case "SAP":
                        sapOR.adjustUI();
                        break;
                }
            });
        }
    }

    public void load() {
        webOR.load();
        mobileOR.load();
        structuredDataOR.load();
        sapOR.load();
    }

    public void adjustUI() {
        webOR.adjustUI();
        mobileOR.adjustUI();
        structuredDataOR.adjustUI();
        sapOR.adjustUI();
    }

    public WebORPanel getWebOR() {
        return webOR;
    }

    public MobileORPanel getMobileOR() {
        return mobileOR;
    }

    public StructuredDataORPanel getStructuredDataOR() {
        return structuredDataOR;
    }

    public SapORPanel getSapOR() {
        return sapOR;
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        if (webOR.navigateToObject(objectName, pageName)) {
            switchToolBar.webButton.setSelected(true);
            return true;
        } else if (mobileOR.navigateToObject(objectName, pageName)) {
            switchToolBar.mobileButton.setSelected(true);
            return true;
        } else if (structuredDataOR.navigateToObject(objectName, pageName)) {
            switchToolBar.structuredDataButton.setSelected(true);
            return true;
        } else if (sapOR.navigateToObject(objectName, pageName)) {
            switchToolBar.sapButton.setSelected(true);
            return true;
        }
        return false;
    }

    class SwitchToolBar extends JToolBar {

        private ButtonGroup bgroup;

        private JToggleButton webButton;
        //private JToggleButton imageButton;
        private JToggleButton mobileButton;
        private JToggleButton structuredDataButton;
        private JToggleButton sapButton;

        public SwitchToolBar() {
            init();
        }

        private void init() {
            setFloatable(false);
            setOpaque(false);
            bgroup = new ButtonGroup();
            
            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 32767)));
            add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767)));
            add(webButton = create("Web", "or.Web"));
            //add(imageButton = create("Image"));
            add(mobileButton = create("Mobile", "or.Mobile"));

            add(structuredDataButton = create("Structured Data", "or.StructuredData"));
            add(sapButton = create("SAP", "or.SAP"));
        }

        private JToggleButton create(String text, String iconKey) {
            JToggleButton toggleButton = new JToggleButton();
            toggleButton.setIcon(INGIcons.swingColored(iconKey, 18));
            toggleButton.setToolTipText(text + " Object Repository");
            toggleButton.setActionCommand(text);
            toggleButton.addItemListener(ObjectRepo.this);
            // Remove hover/focus background effect
            toggleButton.setContentAreaFilled(false);
            toggleButton.setFocusPainted(false);
            toggleButton.setBorderPainted(false);
            toggleButton.addItemListener(e -> {
                if (toggleButton.isSelected()) {
                    toggleButton.setIcon(INGIcons.swingColored(iconKey+".selected", 18));
                } else {
                    toggleButton.setIcon(INGIcons.swingColored(iconKey, 18));
                }
            });
            bgroup.add(toggleButton);
            return toggleButton;
        }
    }
}