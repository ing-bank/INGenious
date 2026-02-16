
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.ide.main.fx.FXPanelHeader;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**
 *
 * 
 */
public class ObjectRepo extends JPanel implements ItemListener {

    private final TestDesign testDesign;

    private final SwitchToolBar switchToolBar;

    private final JPanel repos;

    private final WebORPanel webOR;


    private final MobileORPanel mobileOR;

    public ObjectRepo(TestDesign testDesign) {
        this.testDesign = testDesign;
        switchToolBar = new SwitchToolBar();
        repos = new JPanel();
        webOR = new WebORPanel(testDesign);
        mobileOR = new MobileORPanel(testDesign);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        // Create header panel with FXPanelHeader + SwitchToolBar
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        FXPanelHeader header = new FXPanelHeader("Object Repository");
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
        switchToolBar.bgroup.getElements().nextElement().setSelected(true);
    }

    @Override
    public void itemStateChanged(ItemEvent ie) {
        if (ie.getStateChange() == ItemEvent.SELECTED) {
            CardLayout layout = (CardLayout) repos.getLayout();
            layout.show(repos, ((JToggleButton) ie.getSource()).getActionCommand());
        }
    }

    public void load() {
        webOR.load();
        mobileOR.load();
    }

    public void adjustUI() {
        webOR.adjustUI();
        mobileOR.adjustUI();
    }

    public WebORPanel getWebOR() {
        return webOR;
    }


    public MobileORPanel getMobileOR() {
        return mobileOR;
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        if (webOR.navigateToObject(objectName, pageName)) {
            switchToolBar.webButton.setSelected(true);
            return true;
        }  else if (mobileOR.navigateToObject(objectName, pageName)) {
            switchToolBar.mobileButton.setSelected(true);
            return true;
        }
        return false;
    }

    class SwitchToolBar extends JToolBar {

        private ButtonGroup bgroup;

        private JToggleButton webButton;
        //private JToggleButton imageButton;
        private JToggleButton mobileButton;

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
        }

        private JToggleButton create(String text, String iconKey) {
            JToggleButton togg = new JToggleButton();
            togg.setIcon(INGIcons.swingColored(iconKey, 18));
            togg.setToolTipText(text + " Object Repository");
            togg.setActionCommand(text);
            togg.addItemListener(ObjectRepo.this);
            // Remove hover/focus background effect
            togg.setContentAreaFilled(false);
            togg.setFocusPainted(false);
            togg.setBorderPainted(false);
            bgroup.add(togg);
            return togg;
        }
    }
}
