
package com.ing.ide.main.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * 
 */
public class LoaderScreen extends JPanel {

    private final JLabel loadLabel;

    private JFrame frame;

    Component prevoiusGlassPane;

    private Timer tick;

    public LoaderScreen() {
        loadLabel = new JLabel();
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        loadLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        loadLabel.setVerticalTextPosition(SwingConstants.BOTTOM);

        loadLabel.setBackground(Color.WHITE);
        add(loadLabel, BorderLayout.CENTER);

        tick = new Timer(500, (ActionEvent ae) -> {
            repaint();
        });
        tick.setCoalesce(true);
        tick.setRepeats(true);
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    private void setIcon(String iconLoc) {
        ImageIcon icon = new ImageIcon(
                LoaderScreen.class.getResource(iconLoc));
        loadLabel.setIcon(icon);
        icon.setImageObserver(loadLabel);
    }

    public void showFor(final Runnable runnable, final String text) {
        setIcon("/ui/resources/gears.gif");
        loadLabel.setText(text);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    tick.start();
                    showLoader();
                    runnable.run();
                } finally {
                    hideLoader();
                    tick.stop();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    public void showFor(final Runnable runnable,
            final String text,
            String icon) {
        setIcon(icon);
        loadLabel.setText(text);
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    tick.start();
                    showLoader();
                    runnable.run();
                } finally {
                    hideLoader();
                    tick.stop();
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    public void showIDontCare() {
        showFor(() -> {
            
        }, "loading", "/ui/resources/gears.gif");
    }

    private void showLoader() {
        SwingUtilities.invokeLater(() -> {
            prevoiusGlassPane = frame.getGlassPane();
            frame.setGlassPane(LoaderScreen.this);
            frame.getGlassPane().setVisible(true);
        });
    }

    private void hideLoader() {
        frame.getGlassPane().setVisible(false);
        frame.setGlassPane(prevoiusGlassPane);
    }

}
