
package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.settings.IconSettings;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;


public class RecorderDialog extends JFrame {

    private final TestDesign testDesign;

    public RecorderDialog(TestDesign testDesign) {
        this.testDesign = testDesign;
        setAlwaysOnTop(true);
        setPreferredSize(new Dimension(500, 300));
        setIconImage(IconSettings.getIconSettings().getRecorderLarge().getImage());
        setLayout(new BorderLayout());
        setTitle("Recorder");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                hideRecorder();
            }
        });
        pack();
    }

    public void toggleRecorder() {
        if (isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    RecorderDialog.this.dispatchEvent(new WindowEvent(RecorderDialog.this, WindowEvent.WINDOW_CLOSING));
                }
            });
        } else {
            showRecorder();
        }
    }

    private void showRecorder() {
        testDesign.getsMainFrame().setVisible(false);
        add(testDesign.getTestCaseComponent());
        pack();
        setSize(800, 300);
        setLocation();
        setVisible(true);
//        testDesign.getsMainFrame().getSpyHealReco().startRecorder();
    }

    private void setLocation() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        setLocation((int) rect.getCenterX() - getWidth() / 2, (int) rect.getHeight() - getHeight() - 40);
    }

    private void hideRecorder() {
        testDesign.getsMainFrame().getTestDesign().getTestDesignUI().resetAfterRecorder();
        testDesign.getsMainFrame().setVisible(true);
//       testDesign.getsMainFrame().getSpyHealReco().stopRecorder();
    }

}
