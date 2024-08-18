
package com.ing.ide.main.explorer;

import com.ing.ide.main.explorer.settings.Settings;
import com.ing.ide.util.Border;
import com.ing.ide.util.Listeners;
import com.ing.ide.util.Notification;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import static javax.swing.SwingConstants.CENTER;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * 
 */
public class ImageGallery extends javax.swing.JPanel {

    /**
     * Creates new form ImageGallery
     */
    private final FileFilter imagefilter;
    private final List<File> fList = new ArrayList<>();
    private final List<String> selflist = new ArrayList<>();
    private final List<JPanel> thumbList = new ArrayList<>();
    private final Map<String, Image> thumbs = new HashMap<>();
    private static final Dimension THUMB_SIZE = new Dimension(100, 100),
            PREVIEW_SIZE = new Dimension(650, 350),
            CB_SIZE = new Dimension(16, 16),
            CL_SIZE = new Dimension(18, 18);
    private JPanel selected;
    private MouseListener thumbselected;
    private MouseListener previewClicked;
    private String editor = "mspaint ";
    private Icon close, closesel;

    public ImageGallery(final String ext) {
        close = new ImageIcon(ClassLoader.getSystemClassLoader().getResource("ui/resources/close16.png"));
        closesel = new ImageIcon(ClassLoader.getSystemClassLoader().getResource("ui/resources/close16sel.png"));
        thumbselected = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                thumbSelected(e.getSource());
            }

        };
        previewClicked = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) && previewObj.isEnabled()) {
                    previewObj.setEnabled(false);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            openEditor();
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    previewObj.setEnabled(true);
                                }
                            });
                        }
                    });

                }

            }

        };
        initComponents();
        previewObj.setSize(previewpanel.getSize());
        previewpanel.addMouseListener(previewClicked);

        this.setName("ScreenShots");
        imagefilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getAbsolutePath().endsWith("." + ext);
            }
        };
    }

    /**
     * return the list of selected images
     *
     * @return
     */
    public List<?> getSelectedImages() {
        return selflist;
    }

    /**
     * set up the command to open the configured image editor with preview Image
     *
     * @return
     */
    public String[] getCommand() {
        String curreditor = Settings.getImageEditor();
        if (curreditor == null || "".equals(curreditor)) {
            curreditor = this.editor;
        }
        String arg = Settings.getImageEditorArgs();
        if (!arg.contains(Settings.FILE_ARGS)) {
            arg = Settings.FILE_ARGS + " " + arg;
        } else {
            arg = arg.replace(Settings.FILE_ARGS, Settings.FILE_ARGS + " ");
        }
        String[] args = arg.split(" ");
        int len = args.length;
        for (int i = 0; i < len; i++) {
            if (Settings.FILE_ARGS.equals(args[i])) {
                args[i] = selected.getName();
            }
        }
        return ArrayUtils.addAll(new String[]{curreditor}, args);

    }

    /**
     * refresh the gallery UI
     */
    void refresh() {
        setThumbImage(selected.getName());
        setPreview(selected.getName());
        repaintLater(selected);
    }

    /**
     * performs the set up actions when a thumb image is selected
     *
     * @param source
     */
    private void thumbSelected(Object source) {
        if (selected != null) {
            selected.setBorder(Border.thumbPrevOffFocus);
        }
        selected = (JPanel) source;
        selected.setBorder(Border.thumbPrevSelected);
        String path = ((JComponent) source).getName();
        setPreview(path);
    }

    /**
     * loads the images from the file system to the UI
     *
     * @param loc
     * @return
     */
    public boolean load(String loc) {
        clear();
        File dir = new File(loc);
        fList.addAll(Arrays.asList(dir.listFiles(imagefilter)));
        if (fList.isEmpty()) {
            Notification.show("No Images to Display!!!");
            return false;
        } else {
            load(fList);
            thumbSelected(thumbList.get(0));
            return true;
        }
    }

    /**
     * clear all the list,thumbs, preview images
     */
    private void clear() {
        fList.clear();
        selflist.clear();
        thumbs.clear();
    }

    /**
     * loads the given list of files to the UI
     *
     * @param files
     */
    void load(List<File> files) {
        clearThumbs();
        for (final File f : files) {
            addThumb(f);
        }

    }

    /**
     * creates the thumb image for the given file
     *
     * @param f
     */
    private void addThumb(File f) {
        JPanel thumb = getPanel(f.getAbsolutePath());
        thumbPanel.add(thumb);

    }

    /**
     * set up the thumb image UI
     *
     * @param f
     * @return
     */
    JPanel getPanel(final String f) {
        setThumbImage(f);
        JPanel p = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                g.drawImage(thumbs.get(f), 0, 0, null);
            }
        };
        p.setPreferredSize(THUMB_SIZE);
        p.setBorder(new LineBorder(Color.LIGHT_GRAY, 3));
        JComponent c = getThumbSelector(f);
        JComponent cl = setupThumbClose(p);
        setupAlignment(p, c, cl);
        p.setName(f);
        p.addMouseListener(thumbselected);
        p.addMouseListener(Listeners.thumbPrevFocus);
        thumbList.add(p);
        return p;
    }

    /**
     * create returns the thumb selector component
     *
     * @param f
     * @return
     */
    private JComponent getThumbSelector(final String f) {
        final JCheckBox cb = new JCheckBox();
        cb.setText("");
        cb.setSelected(false);
        cb.setName(f);
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (cb.isSelected()) {
                    selflist.add(f);
                } else {
                    selflist.remove(f);
                }
            }
        });
        cb.setPreferredSize(CB_SIZE);
        return cb;

    }

    /**
     * create returns the thumb close component
     *
     * @param p
     * @return
     */
    private JComponent setupThumbClose(final JPanel p) {
        JCheckBox cb = new JCheckBox();
        cb.setIcon(close);
        cb.setSelectedIcon(closesel);
        cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeThumbPanel(p);
            }
        });
        cb.setPreferredSize(CL_SIZE);
        cb.setMaximumSize(CL_SIZE);
        return cb;
    }

    /**
     * removes the thumb image/panel from UI
     *
     * @param p
     */
    void removeThumbPanel(JPanel p) {
        int index = thumbList.indexOf(p);
        thumbList.remove(p);
        thumbPanel.remove(p);
        if (index > (thumbList.size() - 1)) {
            index--;
        }
        if (index < 0) {
            onEmpty();
        } else {
            thumbSelected(thumbList.get(index));
            repaintLater();
        }
        removeImage(p.getName());
    }

    /**
     * delete the image from the file system
     *
     * @param file
     */
    void removeImage(String file) {
        FileUtils.deleteQuietly(new File(file));

    }

    /**
     * refresh the UI after change
     */
    private void repaintLater() {
        repaintLater(this);
    }

    /**
     * refresh the UI after change
     */
    void repaintLater(final JComponent c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                c.repaint();
            }
        });
    }

    /**
     * defines the component alignment
     *
     * @param p
     * @param c1
     * @param c2
     */
    void setupAlignment(JPanel p, JComponent c1, JComponent c2) {
        javax.swing.GroupLayout pLayout = new javax.swing.GroupLayout(p);
        p.setLayout(pLayout);
        pLayout.setHorizontalGroup(
                pLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pLayout.createSequentialGroup()
                        .addComponent(c1, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(85, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(c2, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        pLayout.setVerticalGroup(
                pLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(pLayout.createSequentialGroup()
                        .addComponent(c2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                        .addComponent(c1, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

    }

    /**
     * resize and create the thumb image
     *
     * @param f
     * @return
     */
    Image setThumbImage(String f) {
        ImageIcon icon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(f));
        Image img = icon.getImage().getScaledInstance(THUMB_SIZE.width, THUMB_SIZE.width, Image.SCALE_SMOOTH);
        thumbs.put(f, img);
        return img;
    }

    /**
     * resize and create the preview image
     *
     * @param path
     */
    private void setPreview(String path) {

        ImageIcon icon = new ImageIcon(path);
        int w = icon.getIconWidth() > PREVIEW_SIZE.width ? PREVIEW_SIZE.width : icon.getIconWidth();
        int h = icon.getIconHeight() > PREVIEW_SIZE.height ? PREVIEW_SIZE.height : icon.getIconHeight();
        Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        previewObj.setIcon(new ImageIcon(img));
        previewObj.setHorizontalAlignment(CENTER);
        previewObj.setVerticalAlignment(CENTER);
        previewObj.repaint();
    }

    /**
     * clear the thumb panels to free the memory
     */
    private void clearThumbs() {
        thumbPanel.removeAll();
        for (JPanel l : thumbList) {
            l = null;
        }
        thumbList.clear();
    }

    /**
     * opens the configured image editor with the preview image to edit
     */
    private void openEditor() {
        try {
            Hide();
            Process p;
            p = Runtime.getRuntime().exec(getCommand());
            p.waitFor();
            refresh();
            Show();
        } catch (Exception ex) {
            Logger.getLogger(ImageGallery.class.getName()).log(Level.SEVERE, null, ex);
            Notification.show("Please, Configure Image Editor in Settings!!");
            Show();
        }
    }

    public void onEmpty() {

    }

    public void Hide() {

    }

    public void Show() {

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        previewpanel = new javax.swing.JPanel();
        previewObj = new javax.swing.JLabel();
        thumbnailpanel = new javax.swing.JScrollPane();
        thumbPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153), 2));
        setMaximumSize(new java.awt.Dimension(653, 481));
        setMinimumSize(new java.awt.Dimension(653, 481));
        setName(""); // NOI18N

        jSplitPane1.setDividerLocation(340);
        jSplitPane1.setDividerSize(2);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.8);
        jSplitPane1.setToolTipText("");

        previewpanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(204, 204, 204), 1, true));
        previewpanel.setLayout(new java.awt.GridLayout(1, 0));
        previewpanel.add(previewObj);

        jSplitPane1.setTopComponent(previewpanel);

        thumbnailpanel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        thumbnailpanel.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        thumbnailpanel.setMaximumSize(new java.awt.Dimension(550, 130));
        thumbnailpanel.setMinimumSize(new java.awt.Dimension(550, 130));
        thumbnailpanel.setPreferredSize(new java.awt.Dimension(550, 130));
        thumbnailpanel.setRequestFocusEnabled(false);

        thumbPanel.setMaximumSize(new java.awt.Dimension(2147483647, 128));
        thumbPanel.setMinimumSize(new java.awt.Dimension(546, 128));
        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 25, 5);
        flowLayout1.setAlignOnBaseline(true);
        thumbPanel.setLayout(flowLayout1);
        thumbnailpanel.setViewportView(thumbPanel);

        jSplitPane1.setBottomComponent(thumbnailpanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 653, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 481, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel previewObj;
    private javax.swing.JPanel previewpanel;
    private javax.swing.JPanel thumbPanel;
    private javax.swing.JScrollPane thumbnailpanel;
    // End of variables declaration//GEN-END:variables

}
