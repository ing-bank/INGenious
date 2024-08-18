
package com.ing.ide.main.shr.mobile;

import com.ing.datalib.or.mobile.MobileORPage;
import java.io.File;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;

/**
 *
 * 
 */
public interface MobileUtils {

    public void setValues(JTree jTree1, JLabel jLabel1, JTable jTable1);

    public Rect loadValuesToTable();

    public void highlightOnMouseMove(int x, int y);

    public void switchHighlightOnMouseMove();

    public void setScreenShotImageToLabelWResize();

    public void setScreenShotImageToLabelWResize(File file);

    public void setScreenShotImageToLabelWResize(String base64Image);

    public void saveImageTo(MobileORPage mObject);

    public String[] getResourcesFor(MobileORPage mObject);
    
}
