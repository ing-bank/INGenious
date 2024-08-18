
package com.ing.ide.main.shr.mobile;

import com.ing.datalib.or.mobile.MobileORPage;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * 
 */
public abstract class MobileUtil implements MobileUtils {

    private JTree tree;
    private JLabel label;
    private JTable table;
    public BufferedImage img;

    @Override
    public void setValues(JTree tree, JLabel label, JTable table) {
        this.tree = tree;
        this.label = label;
        this.table = table;
    }

    public double scaleFactor;
    private Boolean highlightOnMouseMove = true;

    @Override
    public void setScreenShotImageToLabelWResize() {
        if (img != null) {
            label.setIcon(getScreenShotImage());
        }
    }

    @Override
    public void setScreenShotImageToLabelWResize(File screenShotFile) {
        label.setIcon(getScreenShotImage(screenShotFile));
        setHighlightOnMouseMove();
    }

    @Override
    public void setScreenShotImageToLabelWResize(String base64Image) {
        label.setIcon(getImageIcon(base64Image));
        setHighlightOnMouseMove();
    }

    private ImageIcon getImageIcon(String base64Image) {
        try {
            byte[] imageByte = Base64.decodeBase64(base64Image);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(imageByte)) {
                img = ImageIO.read(bis);
                return getScreenShotImage();
            } catch (Exception ex) {
                Logger.getLogger(MobileUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (Exception ex) {
            Logger.getLogger(MobileUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private ImageIcon getScreenShotImage(File screenShotFile) {
        try {
            img = ImageIO.read(screenShotFile);
            return getScreenShotImage();
        } catch (IOException ex) {
            Logger.getLogger(MobileUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public ImageIcon getScreenShotImage() {
        if (img.getHeight() > img.getWidth()) {
            scaleFactor = label.getHeight() / (double) img.getHeight();
        } else {
            scaleFactor = label.getWidth() / (double) img.getWidth();
        }
        int width = (int) (img.getWidth() * scaleFactor);
        int height = (int) (img.getHeight() * scaleFactor);
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    @Override
    public Rect loadValuesToTable() {
        if (tree.getSelectionPath() != null) {
            Object selected = tree.getSelectionPath().getLastPathComponent();
            if (selected instanceof MobileTreeNode) {
                MobileTreeNode treenode = (MobileTreeNode) selected;
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                removeAllRows(model);
                for (Object attr : treenode.getAttributes().keySet()) {
                    model.addRow(new Object[]{attr, treenode.getAttributes().get(attr)});
                }
                return treenode.getBounds();
            }
        }
        return null;
    }

    private void removeAllRows(DefaultTableModel model) {
        model.setRowCount(0);
    }

    @Override
    public void switchHighlightOnMouseMove() {
        highlightOnMouseMove = !highlightOnMouseMove;
    }

    public void setHighlightOnMouseMove() {
        highlightOnMouseMove = true;
    }

    @Override
    public void highlightOnMouseMove(int x, int y) {
        if (highlightOnMouseMove) {
            Object root = tree.getModel().getRoot();
            if (root instanceof MobileTreeNode) {
                updateSelectionForCoordinates(x, y);
            }
        }
    }

    /**
     * Based on the x y coordinates find the element in the tree and select the
     * node
     *
     * @param x
     * @param y
     */
    public void updateSelectionForCoordinates(int x, int y) {
        MobileTreeNode mRootNode = (MobileTreeNode) tree.getModel().getRoot();
        MobileTreeNode mSelectedNode = null;
        if (tree.getSelectionPath() != null) {
            mSelectedNode = (MobileTreeNode) tree.getSelectionPath().getLastPathComponent();
        }
        MinAreaFindNodeListener listener = new MinAreaFindNodeListener();
        boolean found = mRootNode.findLeafMostNodesAtPoint(x, y, listener);
        if (found && listener.mNode != null && !listener.mNode.equals(mSelectedNode)) {
            TreePath path = new TreePath(listener.mNode.getPath());
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    }

    @Override
    public void saveImageTo(MobileORPage node) {
        String loc = node.getRepLocation();
        if (loc != null) {
            File file = new File(loc);
            if (!file.exists()) {
                file.mkdirs();
            }
            saveImage(loc + File.separator + "screenshot.png");
        }
    }

    @Override
    public String[] getResourcesFor(MobileORPage mobileObjectNode) {
        String loc = mobileObjectNode.getRepLocation();
        if (loc != null && new File(loc).exists()) {
            return new String[]{loc + File.separator + "dump.xml", loc + File.separator + "screenshot.png"};
        }
        return new String[]{};
    }

    private void saveImage(String loc) {
        try {
            ImageIO.write(img, "png", new File(loc));
        } catch (IOException ex) {
            Logger.getLogger(MobileUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
