
package com.ing.ide.main.mainui.components.testdesign.testcase.validation;

import com.ing.datalib.component.TestStep;
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 *
 * 
 */
public class StepRenderer extends AbstractRenderer {

    public StepRenderer() {
        super(null);
    }

    @Override
    public void render(JComponent comp, TestStep step, Object value) {
        comp.setForeground(getColor(step));
        
//        try {
//            //create the font to use. Specify the size!
//            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            //register the font
//            ge.registerFont(customFont);
//        } catch (IOException | FontFormatException e) {
//            e.printStackTrace();
//        }
//        
//        comp.setFont(new Font("ING Me", Font.BOLD, 11));
        
       // if (step.isCommented() || step.hasBreakPoint()) {
       //     comp.setFont(new Font("Default", Font.BOLD, 11));
       // }
    }

    private Color getColor(TestStep step) {
        if (step.isCommented()) {
            return Color.lightGray;
        } else if (step.hasBreakPoint()) {
            return Color.BLUE;
        }
        return UIManager.getColor("text");
    }

}
