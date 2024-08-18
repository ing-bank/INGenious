
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel;
import com.ing.ide.main.utils.Utils;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;

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
        add(switchToolBar, BorderLayout.NORTH);
        add(repos, BorderLayout.CENTER);
        initRepos();
    }

    private void initRepos() {
        repos.setLayout(new CardLayout());
        repos.add(webOR, "Web");
       // repos.add(mobileOR, "Mobile");
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
      //  mobileOR.load();
    }

    public void adjustUI() {
        webOR.adjustUI();
     //   mobileOR.adjustUI();
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
        private JToggleButton imageButton;
        private JToggleButton mobileButton;

        public SwitchToolBar() {
            init();
        }

        private void init() {
            setFloatable(false);
            bgroup = new ButtonGroup();
            JLabel label = new JLabel("Object Repository");
            
            try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                //register the font
                ge.registerFont(customFont);
            } catch (IOException | FontFormatException e) {
               // e.printStackTrace();
            }
            
            label.setFont(new Font("ING Me", Font.BOLD, 12));
            label.setForeground(UIManager.getColor("text"));
            add(new javax.swing.Box.Filler(new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 0),
                    new java.awt.Dimension(10, 32767)));
            add(label);
            add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767)));
            add(webButton = create("Web"));
            add(imageButton = create("Image"));
          //  add(mobileButton = create("Mobile"));
        }

        private JToggleButton create(String text) {
            JToggleButton togg = new JToggleButton();
            togg.setIcon(Utils.getIconByResourceName("/ui/resources/or/" + text.toLowerCase()));
            togg.setToolTipText(text);
            togg.setActionCommand(text);
            togg.addItemListener(ObjectRepo.this);
            bgroup.add(togg);
            return togg;
        }
    }
}
