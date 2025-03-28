
package com.ing.ide.main.mainui;

import com.ing.ide.main.utils.Utils;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Component;

/**
 *
 * 
 */
public class AppToolBar extends JToolBar {

    AppActionListener sActionListener;
    private JToggleButton toggleSwitch;

    public AppToolBar(AppActionListener sActionListener) {
        this.sActionListener = sActionListener;
        init();
    }

    private void init() {
        setFloatable(false);
        setBorder(BorderFactory.createEtchedBorder());
        add(createButton("New Project"));
        add(createButton("Open Project"));
        addSeparator();
        add(createButton("Save Project"));
        addSeparator();
        add(createLabel("Auto Save  "));
        add(createToggleButton("Auto Save"));
        //addSeparator();
        //add(createButton("Mobile Spy")); /**** This is disabled to ensure that the mobile capabilities are captured from Appium Inspector ****/
        addSeparator();
        add(createButton("Run Settings"));
        add(createButton("Browser Configuration"));
        addSeparator();
        add(new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767)));
      //  add(createButton("Refresh"));
    }

    private JButton createButton(String action) {
        JButton btn = new JButton();
        btn.setActionCommand(action);
        btn.setToolTipText(action);
        btn.setIcon(Utils.getIconByResourceName("/ui/resources/main/" + action.replace(" ", "")));
        btn.addActionListener(sActionListener);
        return btn;
    }
    
    private JToggleButton createToggleButton(String action) {
        toggleSwitch = new JToggleButton("OFF");
        toggleSwitch.setOpaque(true);
        toggleSwitch.setRolloverEnabled(false);
        toggleSwitch.setContentAreaFilled(true);
        toggleSwitch.setBackground(UIManager.getColor("text"));
        toggleSwitch.setForeground(Color.WHITE);
        toggleSwitch.setUI(new BasicButtonUI() {
            @Override
            protected void paintButtonPressed(Graphics g, AbstractButton b) {
                // Do nothing to prevent the pressed look
            }
        });
        toggleSwitch.setActionCommand(action);
        toggleSwitch.addActionListener(sActionListener);
        return toggleSwitch;
    }
    
    private JLabel createLabel(String text) {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
         
        }
        JLabel label = new JLabel(text);
        label.setFont(new Font("ING Me", Font.BOLD, 12));
        label.setForeground(UIManager.getColor("text"));  
        
        return label;
    }
    
    public JToggleButton getToggleSwitch() {
        return toggleSwitch;
    }
    
    public void setActionListener(AppActionListener sActionListener) {
        this.sActionListener = sActionListener;
        for (Component comp : getComponents()) {
        if (comp instanceof AbstractButton) {
            ((AbstractButton) comp).addActionListener(sActionListener);
        }
    }
    }

}
