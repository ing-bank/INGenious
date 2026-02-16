
package com.ing.ide.util;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

/**
 * Centralized border definitions using theme-aware colors.
 */
public class Border {

    private static Color borderColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    public static  javax.swing.border.Border transBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(255, 255, 255, 0)),
            focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, Color.decode("#13BEFF")),
            selectedBorder = BorderFactory.createMatteBorder(0, 0, 0, 0, Color.decode("#13BEFF")),
            thumbPrevOnFocus=new LineBorder(Color.decode("#9E9E9E"), 3),
            thumbPrevOffFocus=new LineBorder(Color.decode("#BDBDBD"), 3),
            thumbPrevSelected=new LineBorder(Color.decode("#616161"), 3);
    
}
