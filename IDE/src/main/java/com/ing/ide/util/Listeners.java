
package com.ing.ide.util;

import static com.ing.ide.util.Border.focusBorder;
import static com.ing.ide.util.Border.thumbPrevOffFocus;
import static com.ing.ide.util.Border.thumbPrevOnFocus;
import static com.ing.ide.util.Border.transBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.JComponent;

/**
 *
 * 
 */
public class Listeners {

    public static MouseListener buttonFocus = new MouseAdapter() {

        @Override
        public void mouseEntered(MouseEvent e) {
            JButton now = (JButton) e.getSource();
            if (!now.isSelected()) {
                now.setBorder(focusBorder);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            JButton now = (JButton) e.getSource();
            if (!now.isSelected()) {
                now.setBorder(transBorder);
            }

        }

    };
    public static MouseListener thumbPrevFocus = new MouseAdapter() {

        @Override
        public void mouseEntered(MouseEvent e) {
            JComponent now = (JComponent) e.getSource();
            if (now.getBorder() != Border.thumbPrevSelected) {
                now.setBorder(thumbPrevOnFocus);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            JComponent now = (JComponent) e.getSource();
            if (now.getBorder() != Border.thumbPrevSelected) {
                now.setBorder(thumbPrevOffFocus);
            }
        }

    };

}
