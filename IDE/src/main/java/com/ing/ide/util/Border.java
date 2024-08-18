
package com.ing.ide.util;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.border.LineBorder;

/**
 *
 * 
 */
public class Border {

    public static  javax.swing.border.Border transBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(255, 255, 255, 0)),
            focusBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, Color.decode("#13BEFF")),
            selectedBorder = BorderFactory.createMatteBorder(0, 0, 0, 0, Color.decode("#13BEFF")),
            thumbPrevOnFocus=new LineBorder(Color.GRAY, 3),
            thumbPrevOffFocus=new LineBorder(Color.LIGHT_GRAY, 3),
            thumbPrevSelected=new LineBorder(Color.DARK_GRAY, 3);
    
}
