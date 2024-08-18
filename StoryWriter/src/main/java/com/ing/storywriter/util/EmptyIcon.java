
package com.ing.storywriter.util;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 *
 */
public class EmptyIcon implements Icon {
    private final int width = 10;
    private final int height = 10;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
    
}
