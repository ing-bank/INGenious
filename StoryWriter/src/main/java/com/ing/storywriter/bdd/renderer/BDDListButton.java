
package com.ing.storywriter.bdd.renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 *
 */
public class BDDListButton extends JButton {
    
    public static String Template = "<html><div align=left width=200px>"
            + "<font size=-2>[-NAME-]</font><br>"
            + "<em>[-DESC-]</em>"
            + "</div></html>";
    public int pixels = 2;
    
    public BDDListButton() {
        initDropShadow();    
        this.setHorizontalAlignment(LEFT);
    }
    
    public final void initDropShadow() {
        Border border = BorderFactory.createEmptyBorder(pixels, 1, pixels, 1);
        this.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(1, 1, 1, 0), border));
        this.setLayout(new BorderLayout());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int shade = 0;
        int topOpacity = 80;
        for (int i = 0; i < pixels; i++) {
            g.setColor(new Color(shade, shade, shade, ((topOpacity / pixels) * i)));
            g.drawRect(i, i, this.getWidth() - ((i * 2) + 1), this.getHeight() - ((i * 2) + 1));
        }
    }
}
