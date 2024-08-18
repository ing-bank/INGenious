
package com.ing.ide.main.utils.toasterNotification;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class Toaster {

    // Width of the toster
    private int toasterWidth = 300;

    // Height of the toster
    private int toasterHeight = 80;

    // Step for the toaster
    private int step = 20;

    // Step time
    private int stepTime = 20;

    // Show time
    private int displayTime = 3000;

    // Current number of toaster...
    private int currentNumberOfToaster = 0;

    // Last opened toaster
    private int maxToaster = 0;

    // Max number of toasters for the sceen
    private int maxToasterInSceen;

    // Background image
    private Image backgroundImage;

    // Font used to display message
    private Font font;

    // Color for border
    private Color borderColor;

    // Color for toaster
    private Color toasterColor;

    // Set message color
    private Color messageColor;

    // Set the margin
    int margin;

    // Flag that indicate if use alwaysOnTop or not.
    // method always on top start only SINCE JDK 5 !
    boolean useAlwaysOnTop = true;

    private static final long serialVersionUID = 1L;

    public void showToaster(Component parent, String msg) {
        ToasterDialog singleToaster = new ToasterDialog();
        singleToaster.pack();
        singleToaster.setLocationRelativeTo(parent);
        singleToaster.message.setText(msg);
        (new Animation(singleToaster)).start();
    }

    /**
     * @return Returns the font
     */
    public Font getToasterMessageFont() {
        // TODO Auto-generated method stub
        return font;
    }

    /**
     * Set the font for the message
     *
     * @param f
     */
    public void setToasterMessageFont(Font f) {
        font = f;
    }

    /**
     * @return Returns the borderColor.
     */
    public Color getBorderColor() {
        return borderColor;
    }

    /**
     * @param borderColor The borderColor to set.
     */
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    /**
     * @return Returns the displayTime.
     */
    public int getDisplayTime() {
        return displayTime;
    }

    /**
     * @param displayTime The displayTime to set.
     */
    public void setDisplayTime(int displayTime) {
        this.displayTime = displayTime;
    }

    /**
     * @return Returns the margin.
     */
    public int getMargin() {
        return margin;
    }

    /**
     * @param margin The margin to set.
     */
    public void setMargin(int margin) {
        this.margin = margin;
    }

    /**
     * @return Returns the messageColor.
     */
    public Color getMessageColor() {
        return messageColor;
    }

    /**
     * @param messageColor The messageColor to set.
     */
    public void setMessageColor(Color messageColor) {
        this.messageColor = messageColor;
    }

    /**
     * @return Returns the step.
     */
    public int getStep() {
        return step;
    }

    /**
     * @param step The step to set.
     */
    public void setStep(int step) {
        this.step = step;
    }

    /**
     * @return Returns the stepTime.
     */
    public int getStepTime() {
        return stepTime;
    }

    /**
     * @param stepTime The stepTime to set.
     */
    public void setStepTime(int stepTime) {
        this.stepTime = stepTime;
    }

    /**
     * @return Returns the toasterColor.
     */
    public Color getToasterColor() {
        return toasterColor;
    }

    /**
     * @param toasterColor The toasterColor to set.
     */
    public void setToasterColor(Color toasterColor) {
        this.toasterColor = toasterColor;
    }

    /**
     * @return Returns the toasterHeight.
     */
    public int getToasterHeight() {
        return toasterHeight;
    }

    /**
     * @param toasterHeight The toasterHeight to set.
     */
    public void setToasterHeight(int toasterHeight) {
        this.toasterHeight = toasterHeight;
    }

    /**
     * @return Returns the toasterWidth.
     */
    public int getToasterWidth() {
        return toasterWidth;
    }

    /**
     * @param toasterWidth The toasterWidth to set.
     */
    public void setToasterWidth(int toasterWidth) {
        this.toasterWidth = toasterWidth;
    }

    public Image getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    class Animation extends Thread {

        ToasterDialog toaster;

        public Animation(ToasterDialog toaster) {
            this.toaster = toaster;
        }

        /**
         * Animate vertically the toaster. The toaster could be moved from
         * bottom to upper or to upper to bottom
         *
         * @param posx
         * @param fromy
         * @param toy
         * @throws InterruptedException
         */
        protected void animateVertically(int posx, int fromY, int toY) throws InterruptedException {
            toaster.setLocation(posx, fromY);
            if (toY < fromY) {
                for (int i = fromY; i > toY; i -= step) {
                    toaster.setLocation(posx, i);
                    Thread.sleep(stepTime);
                }
            } else {
                for (int i = fromY; i < toY; i += step) {
                    toaster.setLocation(posx, i);
                    Thread.sleep(stepTime);
                }
            }
            toaster.setLocation(posx, toY);
        }

        @Override
        public void run() {
            try {
                boolean animateFromBottom = true;
                GraphicsEnvironment ge = GraphicsEnvironment
                        .getLocalGraphicsEnvironment();
                Rectangle screenRect = ge.getMaximumWindowBounds();

                int screenHeight = (int) screenRect.height;

                int startYPosition;
                int stopYPosition;

                if (screenRect.y > 0) {
                    animateFromBottom = false; // Animate from top!
                }

                maxToasterInSceen = screenHeight / toasterHeight;

                int posx = (int) screenRect.width - toasterWidth - 1;

                toaster.setLocation(posx, screenHeight);
                toaster.setVisible(true);
                if (useAlwaysOnTop) {
                    toaster.setAlwaysOnTop(true);
                }

                if (animateFromBottom) {
                    startYPosition = screenHeight;
                    stopYPosition = startYPosition - toasterHeight - 1;
                    if (currentNumberOfToaster > 0) {
                        stopYPosition = stopYPosition - (maxToaster % maxToasterInSceen * toasterHeight);
                    } else {
                        maxToaster = 0;
                    }
                } else {
                    startYPosition = screenRect.y - toasterHeight;
                    stopYPosition = screenRect.y;

                    if (currentNumberOfToaster > 0) {
                        stopYPosition = stopYPosition + (maxToaster % maxToasterInSceen * toasterHeight);
                    } else {
                        maxToaster = 0;
                    }
                }
                currentNumberOfToaster++;
                maxToaster++;

                animateVertically(posx, startYPosition, stopYPosition);
                Thread.sleep(displayTime);
                animateVertically(posx, stopYPosition, startYPosition);

                currentNumberOfToaster--;
                toaster.setVisible(false);
                toaster.dispose();
            } catch (Exception e) {
                Logger.getLogger(Toaster.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }
}
