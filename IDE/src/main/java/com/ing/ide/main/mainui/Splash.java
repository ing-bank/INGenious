
package com.ing.ide.main.mainui;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Shows Splash Screen with Logo and Progress bar until the UI loads
 *
 */
public class Splash extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;
    javax.swing.JProgressBar  pbar;
    /**
     * creates a Splash Screen instance with <code> splasScreen</code> image and
     * starts progress bar in a separate thread
     *
     */
    public Splash() {
        super("loading");
        Border border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE),
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        );
        
        javax.swing.ImageIcon icon = new javax.swing.ImageIcon(getClass().getResource("/ui/resources/favicon.png"));
        javax.swing.ImageIcon splasScreen = new javax.swing.ImageIcon(getClass().getResource("/ui/resources/splash.png"));
        int topbar = 0;
        setDefaultCloseOperation(javax.swing.JFrame.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setIconImage(icon.getImage());
        setSize(splasScreen.getIconWidth(), splasScreen.getIconHeight() + progreessBarHeight() + topbar);
        setLayout(null);
        setLocationRelativeTo(null);
        javax.swing.JLabel imglabel = new javax.swing.JLabel(splasScreen);
        pbar = new javax.swing.JProgressBar(0, 100);
        pbar.setStringPainted(false);
        pbar.setBorderPainted(false);
        //pbar.setBorder(null);
        
        pbar.setBorder(border);
        imglabel.setBounds(0, topbar, splasScreen.getIconWidth(), splasScreen.getIconHeight() - topbar);
        pbar.setForeground(java.awt.Color.decode("#FF6200"));
        pbar.setBackground(java.awt.Color.BLACK);
        pbar.setPreferredSize(new java.awt.Dimension(splasScreen.getIconWidth(), progreessBarHeight()));
        pbar.setBounds(0, splasScreen.getIconHeight() + topbar, splasScreen.getIconWidth(), progreessBarHeight());
        add(pbar);
        add(imglabel);
    }

    private static int progreessBarHeight() {
        return 10;
    }

    public void progressed(int val) {
        pbar.setValue(val);
    }

}
