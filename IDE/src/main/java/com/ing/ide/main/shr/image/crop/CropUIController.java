
package com.ing.ide.main.shr.image.crop;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JFrame;

/**
 *
 *
 */
public class CropUIController {

    static CropUIController ctrlr;

    public Cropper cropper;
    private Robot robot;
    private Dimension dim;

    Image img;
    private JFrame parent;

    Action onCrop, onHide;

    public static CropUIController getInstance() {
        if (ctrlr == null) {
            ctrlr = new CropUIController();
        }
        ctrlr.reset();
        return ctrlr;
    }

    public void setRightClickDisArm(boolean disArmEnable) {
        cropper.setRCDisArm(disArmEnable);
    }

    public CropUIController() {
        init();
    }

    public void setOnCrop(Action onCrop) {
        if (onCrop != null) {
            this.onCrop = onCrop;
        }
    }

    public void setOnHide(Action onHide) {
        if (onHide != null) {
            this.onHide = onHide;
        }
    }

    void done() {
        if (onHide != null) {
            this.onHide.actionPerformed(null);
        }
    }

    private void reset() {
        cropper.setRCDisArm(true);
    }

    public void setFrame(JFrame parent) {
        this.parent = parent;
    }

    public JFrame getFrame() {
        return parent;
    }

    private void init() {
        try {
            cropper = new Cropper(this);
            robot = new Robot();
            dim = Toolkit.getDefaultToolkit().getScreenSize();
        } catch (AWTException ex) {
            Logger.getLogger(CropUIController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void doCrop() {
        cropper.showCropper(getScreen());
    }

    private void cropVisible(Boolean flag) {
        parent.setVisible(flag);
    }

    public Boolean doCBCrop() {
        Image screen = getScreenfromClipB();
        if (screen != null) {
            cropper.showCropper(toBufferedImage(screen));
            return true;
        }
        return false;
    }

    public BufferedImage getScreen() {
        cropVisible(false);
        BufferedImage currimg = robot.createScreenCapture(new Rectangle(0, 0, dim.width, dim.height));
        cropVisible(true);
        return currimg;
    }

    Image getScreenfromClipB() {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            try {
                return (Image) clipboard.getData(DataFlavor.imageFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                Logger.getLogger(CropUIController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }

    public Image getCroppedImage() {
        return img;
    }

    public BufferedImage getBackGroundImage() {
        return cropper.screen;
    }

    public void resetImages() {
        img = null;
        cropper.screen = null;
    }

    public void stop() {
        cropper.hideCropper();
        resetImages();
    }

    public void previewImage(Image src) {
        if (src != null) {
            img = src;
            if (onCrop != null) {
                onCrop.putValue("img", src);
                onCrop.actionPerformed(null);
            }
        }
    }

    public void refresh() {
        cropper.repaint();
    }

}
