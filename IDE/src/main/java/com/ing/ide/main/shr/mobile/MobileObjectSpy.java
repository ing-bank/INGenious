
package com.ing.ide.main.shr.mobile;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import com.ing.ide.main.settings.PropUtils;
import com.ing.ide.main.shr.mobile.android.AndroidAdbCLI;
import com.ing.ide.main.shr.mobile.android.AndroidTree;
import com.ing.ide.main.shr.mobile.android.AndroidUtil;
import com.ing.ide.main.shr.mobile.ios.IOSTree;
import com.ing.ide.main.shr.mobile.ios.IOSUtil;
//import com.ing.ide.main.shr.mobile.ios.IOSpy;
import com.ing.ide.main.utils.table.JtableUtils;
import com.ing.ide.settings.IconSettings;
import com.ing.ide.util.Notification;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * 
 */
public class MobileObjectSpy extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(MobileObjectSpy.class.getName());

    private final transient Icon spy = new ImageIcon(getClass().getResource("/mobilespy/spy.png"));
    private final transient Icon heal = new ImageIcon(getClass().getResource("/mobilespy/heal.png"));
    private final transient Icon android = new ImageIcon(getClass().getResource("/mobilespy/android.png"));
    private final transient Icon ios = new ImageIcon(getClass().getResource("/mobilespy/apple.png"));

    private final transient AndroidAdbCLI screenshotAction = new AndroidAdbCLI();
    private transient Rect selectedRect;
    private final transient FileFilter xmlDumpFilter = new FileNameExtensionFilter("Screen XML Dump", "uix", "xml");
    private final transient FileFilter screenShotFilter = new FileNameExtensionFilter("ScreenShot", "png");

    private transient MobileUtils mobileUtils;
    private transient MobileTree mobileTree;

    private final AppMainFrame sMainFrame;

    private MobileOR mobileOR;

    private ObjectTree objectTree;

    public MobileObjectSpy(AppMainFrame sMainFrame) {
        initComponents();
        setIconImage(IconSettings.getIconSettings().getMobileObjectGrabb().getImage());
        this.sMainFrame = sMainFrame;
        initVars();
        loadDefaultAppiumCaps();
    }

    private void initVars() {
        mobileTree = AndroidTree.get();
        mobileUtils = AndroidUtil.get();
        mobileUtils.setValues(jTree1, screenShotLabel, jTable1);
        mobileTree.setTree(jTree1);
        setJMenuBar(jMenuBar1);
        JtableUtils.addlisteners(jTable1, true);
        JtableUtils.addlisteners(mobilePropTable, true);
        JtableUtils.addlisteners(jTable3, true);
        objectTree = new ObjectTree() {
            @Override
            public void loadTableModelForSelection() {
                Object obj = objectTree.getSelectedObject();
                if (obj != null && obj instanceof MobileORObject) {
                    mobilePropTable.setModel((MobileORObject) obj);
                }
            }

            @Override
            public void showImpactedTestCases(List<TestCase> testcases, String pageName, String objectName) {
//                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Project getProject() {
                return sMainFrame.getProject();
            }

            @Override
            public ORRootInf getOR() {
                return mobileOR;
            }
        };
        treePanel.add(new JScrollPane(objectTree.getTree()), BorderLayout.CENTER);
    }

    public void load() {
        mobileOR = sMainFrame.getProject().getObjectRepository().getMobileOR();
        objectTree.load();
    }

    public void reloadEmulators() {
        andEmulatorCombo.setModel(new DefaultComboBoxModel(sMainFrame.getProject().getProjectSettings()
                .getEmulators().getAppiumEmulatorNames().toArray()));
        iosEmulatorCombo.setModel(andEmulatorCombo.getModel());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        mapToPage = new javax.swing.JMenuItem();
        loadFromPage = new javax.swing.JMenuItem();
        jFileChooser1 = new javax.swing.JFileChooser();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel2 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        loadScreen = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        spyrHeal = new javax.swing.JToggleButton();
        jToggleButton1 = new javax.swing.JToggleButton();
        settings = new javax.swing.JToggleButton();
        mainPanel = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        imageAndObjectRepo = new javax.swing.JSplitPane();
        screenShotLabel = new javax.swing.JLabel(){
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if(selectedRect!=null)
                {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    g2d.setStroke(new BasicStroke(1.8f));
                    g2d.setColor(Color.RED);
                    g2d.drawRect(selectedRect.getX(), selectedRect.getY(), selectedRect.getWidth(), selectedRect.getHeight());
                    g2d.dispose();
                }
            }
        };
        jSplitPane3 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        mobilePropTable = new javax.swing.JTable();
        treePanel = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        addToSelectedPage = new javax.swing.JToggleButton();
        loadResourceFromPage = new javax.swing.JButton();
        mapCurrentToPage = new javax.swing.JButton();
        settingsPane = new javax.swing.JTabbedPane();
        androidSettings = new javax.swing.JPanel();
        jToolBar4 = new javax.swing.JToolBar();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        loadAndroidDevices = new javax.swing.JButton();
        loadPackageAndActivity = new javax.swing.JButton();
        addAsAndroidemulator = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        andEmulatorCombo = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        androidDevices = new javax.swing.JComboBox();
        jLabel7 = new javax.swing.JLabel();
        packageName = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        activityName = new javax.swing.JTextField();
        iosSettings = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTable3 = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        appiumServerLoc = new javax.swing.JTextField();
        jCheckBox1 = new javax.swing.JCheckBox();
        iosEmulatorCombo = new javax.swing.JComboBox();
        jToolBar2 = new javax.swing.JToolBar();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        loadDefault = new javax.swing.JButton();
        addRow = new javax.swing.JButton();
        removeRow = new javax.swing.JButton();
        updateEmulator = new javax.swing.JButton();

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_DOWN_MASK));
        jMenuItem3.setText("Exit");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Tools");

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        jMenuItem4.setText("Load Screen");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem4);

        mapToPage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        mapToPage.setText("Map To Page");
        mapToPage.setToolTipText("Map the cuurent ScreenShot and XML to the selected Page");
        mapToPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mapToPageActionPerformed(evt);
            }
        });
        jMenu2.add(mapToPage);

        loadFromPage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        loadFromPage.setText("Load From Page");
        loadFromPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromPageActionPerformed(evt);
            }
        });
        jMenu2.add(loadFromPage);

        jMenuBar1.add(jMenu2);

        jPanel2.setLayout(new java.awt.BorderLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Mobile Object Grabber");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jToolBar1.setOrientation(javax.swing.SwingConstants.VERTICAL);
        jToolBar1.setRollover(true);

        loadScreen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/loadIcon.png"))); // NOI18N
        loadScreen.setToolTipText("Load From Device [Ctrl+L]");
        loadScreen.setFocusable(false);
        loadScreen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadScreen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        loadScreen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadScreenActionPerformed(evt);
            }
        });
        jToolBar1.add(loadScreen);

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/loadFromFileIcon.png"))); // NOI18N
        jButton1.setToolTipText("Load Data from file [Ctrl+O]");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        spyrHeal.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/spy.png"))); // NOI18N
        spyrHeal.setToolTipText("Spy Mode");
        spyrHeal.setFocusable(false);
        spyrHeal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        spyrHeal.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        spyrHeal.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                spyrHealItemStateChanged(evt);
            }
        });
        jToolBar1.add(spyrHeal);

        jToggleButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/android.png"))); // NOI18N
        jToggleButton1.setFocusable(false);
        jToggleButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jToggleButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jToggleButton1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jToggleButton1ItemStateChanged(evt);
            }
        });
        jToolBar1.add(jToggleButton1);

        settings.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/Settings.png"))); // NOI18N
        settings.setToolTipText("Settings");
        settings.setFocusable(false);
        settings.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        settings.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        settings.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                settingsItemStateChanged(evt);
            }
        });
        jToolBar1.add(settings);

        getContentPane().add(jToolBar1, java.awt.BorderLayout.WEST);

        mainPanel.setLayout(new java.awt.CardLayout());

        jSplitPane2.setDividerLocation(0);
        jSplitPane2.setResizeWeight(0.2);
        jSplitPane2.setOneTouchExpandable(true);

        jSplitPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("App OR"));
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.5);
        jSplitPane1.setOneTouchExpandable(true);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Property", "Value"
            }
        ));
        jTable1.setColumnSelectionAllowed(true);
        jScrollPane2.setViewportView(jTable1);
        jTable1.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        jSplitPane1.setRightComponent(jScrollPane2);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("View");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("Node");
        javax.swing.tree.DefaultMutableTreeNode treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("Value");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jTree1);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jSplitPane2.setLeftComponent(jSplitPane1);

        imageAndObjectRepo.setResizeWeight(0.7);
        imageAndObjectRepo.setOneTouchExpandable(true);

        screenShotLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/screenshot.png"))); // NOI18N
        screenShotLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        screenShotLabel.setMaximumSize(new java.awt.Dimension(78676, 778786));
        screenShotLabel.setMinimumSize(new java.awt.Dimension(346, 680));
        screenShotLabel.setPreferredSize(new java.awt.Dimension(250, 680));
        screenShotLabel.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        screenShotLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                screenShotLabelMouseMoved(evt);
            }
        });
        screenShotLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                screenShotLabelMouseClicked(evt);
            }
        });
        screenShotLabel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                screenShotLabelComponentResized(evt);
            }
        });
        imageAndObjectRepo.setLeftComponent(screenShotLabel);

        jSplitPane3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane3.setResizeWeight(0.5);
        jSplitPane3.setMinimumSize(new java.awt.Dimension(100, 94));
        jSplitPane3.setPreferredSize(new java.awt.Dimension(300, 836));

        mobilePropTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Property", "Value"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane3.setViewportView(mobilePropTable);

        jSplitPane3.setRightComponent(jScrollPane3);

        treePanel.setLayout(new java.awt.BorderLayout());

        jToolBar3.setRollover(true);
        jToolBar3.add(filler2);

        addToSelectedPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/pageOpen.png"))); // NOI18N
        addToSelectedPage.setToolTipText("Add Objects to Selected Page/Object Group");
        addToSelectedPage.setFocusable(false);
        addToSelectedPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addToSelectedPage.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/mobilespy/pageLock.png"))); // NOI18N
        addToSelectedPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addToSelectedPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addToSelectedPageActionPerformed(evt);
            }
        });
        jToolBar3.add(addToSelectedPage);

        loadResourceFromPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/Inject.png"))); // NOI18N
        loadResourceFromPage.setToolTipText("Load From Page [Ctrl+Alt+ L]");
        loadResourceFromPage.setFocusable(false);
        loadResourceFromPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadResourceFromPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        loadResourceFromPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadResourceFromPageActionPerformed(evt);
            }
        });
        jToolBar3.add(loadResourceFromPage);

        mapCurrentToPage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/Inject.png"))); // NOI18N
        mapCurrentToPage.setToolTipText("Map Current view to Page [Ctrl+Alt+M]");
        mapCurrentToPage.setFocusable(false);
        mapCurrentToPage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        mapCurrentToPage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        mapCurrentToPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mapCurrentToPageActionPerformed(evt);
            }
        });
        jToolBar3.add(mapCurrentToPage);

        treePanel.add(jToolBar3, java.awt.BorderLayout.PAGE_START);

        jSplitPane3.setTopComponent(treePanel);

        imageAndObjectRepo.setRightComponent(jSplitPane3);

        jSplitPane2.setRightComponent(imageAndObjectRepo);

        mainPanel.add(jSplitPane2, "Spy");

        androidSettings.setLayout(new java.awt.BorderLayout());

        jToolBar4.setRollover(true);
        jToolBar4.add(filler3);

        loadAndroidDevices.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/ref.png"))); // NOI18N
        loadAndroidDevices.setToolTipText("Load UDID");
        loadAndroidDevices.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadAndroidDevicesActionPerformed(evt);
            }
        });
        jToolBar4.add(loadAndroidDevices);

        loadPackageAndActivity.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/objects.png"))); // NOI18N
        loadPackageAndActivity.setToolTipText("Fetch current Acitivity");
        loadPackageAndActivity.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPackageAndActivityActionPerformed(evt);
            }
        });
        jToolBar4.add(loadPackageAndActivity);

        addAsAndroidemulator.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/saveproj.png"))); // NOI18N
        addAsAndroidemulator.setToolTipText("Save/Update Device");
        addAsAndroidemulator.setFocusable(false);
        addAsAndroidemulator.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addAsAndroidemulator.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addAsAndroidemulator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAsAndroidemulatorActionPerformed(evt);
            }
        });
        jToolBar4.add(addAsAndroidemulator);

        androidSettings.add(jToolBar4, java.awt.BorderLayout.NORTH);

        jLabel5.setText("AddTo/Update");

        andEmulatorCombo.setEditable(true);
        andEmulatorCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel4.setText("UDID");

        jLabel7.setText("Package Name");

        jLabel1.setText("Activity Name");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(androidDevices, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(andEmulatorCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(packageName)
                    .addComponent(activityName, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(684, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(andEmulatorCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(androidDevices, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(packageName, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(activityName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        androidSettings.add(jPanel3, java.awt.BorderLayout.CENTER);

        settingsPane.addTab("Android", androidSettings);

        iosSettings.setLayout(new java.awt.GridBagLayout());

        jTable3.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"UDID", null},
                {"appium-version", "1.0"},
                {"platformName", "iOS"},
                {"platformVersion", "8.2"},
                {"deviceName", "iPhone 5s"},
                {"app", "/Users/Apple/Library/Developer/Xcode/DerivedData/UICatalog-awyrhprjhfypbofjkcjkujlkngty/Build/Products/Debug-iphonesimulator/UICatalog.app"}
            },
            new String [] {
                "Capability", "Value"
            }
        ));
        jTable3.setColumnSelectionAllowed(true);
        jTable3.getTableHeader().setReorderingAllowed(false);
        jScrollPane5.setViewportView(jTable3);
        jTable3.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 405;
        gridBagConstraints.ipady = 229;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 15, 1, 0);
        iosSettings.add(jScrollPane5, gridBagConstraints);

        jLabel6.setText("Appium Server");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(28, 19, 0, 0);
        iosSettings.add(jLabel6, gridBagConstraints);

        appiumServerLoc.setText("http://127.0.0.1:4723/wd/hub");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 322;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(22, 12, 0, 0);
        iosSettings.add(appiumServerLoc, gridBagConstraints);

        jCheckBox1.setText("Load Settings For");
        jCheckBox1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBox1ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(25, 12, 0, 0);
        iosSettings.add(jCheckBox1, gridBagConstraints);

        iosEmulatorCombo.setEditable(true);
        iosEmulatorCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        iosEmulatorCombo.setEnabled(false);
        iosEmulatorCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                iosEmulatorComboItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(21, 12, 0, 0);
        iosSettings.add(iosEmulatorCombo, gridBagConstraints);

        jToolBar2.setRollover(true);
        jToolBar2.add(filler1);

        loadDefault.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/refresh.png"))); // NOI18N
        loadDefault.setToolTipText("Load Default Value");
        loadDefault.setFocusable(false);
        loadDefault.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        loadDefault.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        loadDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadDefaultActionPerformed(evt);
            }
        });
        jToolBar2.add(loadDefault);

        addRow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/add.png"))); // NOI18N
        addRow.setToolTipText("Add Row");
        addRow.setFocusable(false);
        addRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRowActionPerformed(evt);
            }
        });
        jToolBar2.add(addRow);

        removeRow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/rem.png"))); // NOI18N
        removeRow.setToolTipText("Remove Rows");
        removeRow.setFocusable(false);
        removeRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        removeRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeRowActionPerformed(evt);
            }
        });
        jToolBar2.add(removeRow);

        updateEmulator.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/saveproj.png"))); // NOI18N
        updateEmulator.setToolTipText("Update Selected Emulator");
        updateEmulator.setEnabled(false);
        updateEmulator.setFocusable(false);
        updateEmulator.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        updateEmulator.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        updateEmulator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateEmulatorActionPerformed(evt);
            }
        });
        jToolBar2.add(updateEmulator);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 334;
        gridBagConstraints.ipady = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 15, 0, 0);
        iosSettings.add(jToolBar2, gridBagConstraints);

        settingsPane.addTab("IOS", iosSettings);

        mainPanel.add(settingsPane, "Settings");

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loadScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadScreenActionPerformed
        if (jToggleButton1.isSelected()) {
            loadIOSXMLAndScreenShot();
        } else {
            loadAndroidXMLAndScreenShot();
        }
    }//GEN-LAST:event_loadScreenActionPerformed

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        selectedRect = mobileUtils.loadValuesToTable();
        if (selectedRect != null) {
            screenShotLabel.paintImmediately(screenShotLabel.getBounds());
        }
    }//GEN-LAST:event_jTree1ValueChanged

    private void screenShotLabelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_screenShotLabelMouseMoved
        mobileUtils.highlightOnMouseMove(evt.getX(), evt.getY());
    }//GEN-LAST:event_screenShotLabelMouseMoved

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        showOpenDialog();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void screenShotLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_screenShotLabelMouseClicked
        if (evt.getButton() == 3) {
            if (spyrHeal.isSelected()) {
                updateObject(mobileTree.getSelectedNode());
            } else {
                saveObject(mobileTree.getSelectedNode());
            }
        } else {
            mobileUtils.switchHighlightOnMouseMove();
        }
    }//GEN-LAST:event_screenShotLabelMouseClicked

    private void updateObject(MobileTreeNode mobileNode) {
        if (objectTree.getSelectedObject() != null) {
            getDummyObject(mobileNode).clone((MobileORObject) objectTree.getSelectedObject());
            mobilePropTable.repaint();
        } else {
            Notification.show("Please select an object from OR to update");
        }
    }

    private void saveObject(MobileTreeNode mobileNode) {
        if (addToSelectedPage.isSelected()) {
            saveObjectToSelected(mobileNode);
        } else {
            saveObjectToRep(mobileNode);
        }
    }

    private void saveObjectToRep(MobileTreeNode mobileNode) {
        MobileORPage page = mobileOR.getPageByName(mobileNode.getPageName());
        if (page == null) {
            page = mobileOR.addPage(mobileNode.getPageName());
            page.setPackageName(mobileNode.getPageName());
            ((DefaultTreeModel) objectTree.getTree().getModel()).nodesWereInserted(mobileOR, new int[]{mobileOR.getChildCount() - 1});
        }
        saveObjectToPage(page, mobileNode);
    }

    private MobileORObject getDummyObject(MobileTreeNode mobileNode) {
        MobileORObject moj = new MobileORObject();
        moj.setName(mobileNode.getValidName());
        moj.setId(mobileNode.getId());
        moj.setClassName(mobileNode.getClassName());
        moj.setXpath(XpathGenerator.getFullXpath(mobileNode));
        return moj;
    }

    private void saveObjectToPage(MobileORPage page, MobileTreeNode mobileNode) {
        MobileORObject dummyObject = getDummyObject(mobileNode);
        MobileORObject dupObj = findDuplicate(page, dummyObject);
        if (dupObj == null) {
            String objName = dummyObject.getName();
            int i = 1;
            while (page.getObjectGroupByName(objName) != null) {
                objName = dummyObject.getName() + i++;
            }
            MobileORObject newObj = page.addObject(objName);
            dummyObject.clone(newObj);
            Notification.show("Object Added : " + objName);
            ((DefaultTreeModel) objectTree.getTree().getModel()).nodesWereInserted(page, new int[]{page.getChildCount() - 1});
        } else {
            Notification.show("Object Already Present\nPage : " + page.getName() + "\nObject : " + dupObj.getName());
        }
    }

    private void saveObjectToGroup(ObjectGroup<MobileORObject> group, MobileTreeNode mobileNode) {
        MobileORObject dupObj = null;
        MobileORObject dummyObject = getDummyObject(mobileNode);
        for (MobileORObject object : group.getObjects()) {
            if (object.isEqualOf(dummyObject)) {
                dupObj = object;
            }
        }
        if (dupObj == null) {
            String objName = dummyObject.getName();
            int i = 1;
            while (group.getObjectByName(objName) != null) {
                objName = dummyObject.getName() + i++;
            }
            MobileORObject newObj = group.addObject(objName);
            dummyObject.clone(newObj);
            LOG.log(Level.INFO, "Object Added : {0}", objName);
            ((DefaultTreeModel) objectTree.getTree().getModel()).nodesWereInserted(group, new int[]{group.getChildCount() - 1});
            if (group.getChildCount() == 2) {
                ((DefaultTreeModel) objectTree.getTree().getModel()).reload(group.getParent());
            }
        } else {
            LOG.log(Level.WARNING, "Object Similar To\nObject : {0}", dupObj.getName());
        }

    }

    private void saveObjectToSelected(MobileTreeNode mobileNode) {
        if (objectTree.getTree().getSelectionPath() != null) {
            Object obj = objectTree.getTree().getSelectionPath().getLastPathComponent();
            if (obj instanceof MobileORPage) {
                saveObjectToPage((MobileORPage) obj, mobileNode);
            } else if (obj instanceof ObjectGroup) {
                saveObjectToGroup((ObjectGroup) obj, mobileNode);
            } else if (obj instanceof MobileORObject) {
                saveObjectToGroup(((MobileORObject) obj).getParent(), mobileNode);
            }
        } else {
            saveObjectToRep(mobileNode);
        }
    }

    private MobileORPage getPageFromSelection() {
        if (objectTree.getTree().getSelectionPath() != null) {
            Object obj = objectTree.getTree().getSelectionPath().getLastPathComponent();
            if (obj instanceof MobileORPage) {
                return (MobileORPage) obj;
            } else if (obj instanceof ObjectGroup) {
                return (MobileORPage) ((ObjectGroup) obj).getParent();
            } else if (obj instanceof MobileORObject) {
                return ((MobileORObject) obj).getPage();
            }
        }
        return null;
    }

    private MobileORObject findDuplicate(MobileORPage page, MobileORObject dummyObject) {
        for (ObjectGroup<MobileORObject> objectGroup : page.getObjectGroups()) {
            for (MobileORObject object : objectGroup.getObjects()) {
                if (object.isEqualOf(dummyObject)) {
                    return object;
                }
            }
        }
        return null;
    }


    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        showOpenDialog();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        mobileOR.getObjectRepository().save();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        loadAndroidXMLAndScreenShot();
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void spyrHealItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_spyrHealItemStateChanged
        if (spyrHeal.isSelected()) {
            spyrHeal.setIcon(heal);
            spyrHeal.setToolTipText("Update values of the Selected Object in the OR");
        } else {
            spyrHeal.setIcon(spy);
            spyrHeal.setToolTipText("Add a new Object to OR");
        }
    }//GEN-LAST:event_spyrHealItemStateChanged

    private void jToggleButton1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jToggleButton1ItemStateChanged
        switchTo(jToggleButton1.isSelected());
    }//GEN-LAST:event_jToggleButton1ItemStateChanged

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    //    IOSpy.quit();
        addToSelectedPage.setSelected(false);
        addToSelectedPage.setToolTipText("Add Objects to Selected Page");
        sMainFrame.getTestDesign().getObjectRepo().getMobileOR().load();
    }//GEN-LAST:event_formWindowClosing

    private void settingsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_settingsItemStateChanged
        CardLayout layout = (CardLayout) mainPanel.getLayout();
        if (settings.isSelected()) {
            setSize(500, getHeight());
            settingsPane.setSelectedIndex(jToggleButton1.isSelected() ? 1 : 0);
            layout.show(mainPanel, "Settings");
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            layout.show(mainPanel, "Spy");
        }
    }//GEN-LAST:event_settingsItemStateChanged

    private void screenShotLabelComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_screenShotLabelComponentResized
        if (mobileUtils != null) {
            mobileUtils.setScreenShotImageToLabelWResize();
        }
    }//GEN-LAST:event_screenShotLabelComponentResized

    private void mapToPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mapToPageActionPerformed
        mapToPage();
    }//GEN-LAST:event_mapToPageActionPerformed

    private void loadFromPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromPageActionPerformed
        loadFromPage();
    }//GEN-LAST:event_loadFromPageActionPerformed

    private void iosEmulatorComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_iosEmulatorComboItemStateChanged
        if (evt.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            loadEmulator(evt.getItem().toString());
        }
    }//GEN-LAST:event_iosEmulatorComboItemStateChanged

    private void jCheckBox1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBox1ItemStateChanged
        iosEmulatorCombo.setEnabled(jCheckBox1.isSelected());
        updateEmulator.setEnabled(jCheckBox1.isSelected());
        if (jCheckBox1.isSelected() && iosEmulatorCombo.getItemCount() > 0) {
            loadEmulator(iosEmulatorCombo.getSelectedItem().toString());
        }
    }//GEN-LAST:event_jCheckBox1ItemStateChanged

    private void addRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRowActionPerformed
        JtableUtils.addrow(jTable3);
    }//GEN-LAST:event_addRowActionPerformed

    private void removeRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeRowActionPerformed
        JtableUtils.deleterow(jTable3);
    }//GEN-LAST:event_removeRowActionPerformed

    private void updateEmulatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateEmulatorActionPerformed
        addRUpdateIOSEmulator();
    }//GEN-LAST:event_updateEmulatorActionPerformed

    private void loadDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadDefaultActionPerformed
        loadDefaultAppiumCaps();
    }//GEN-LAST:event_loadDefaultActionPerformed

    private void loadAndroidDevicesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadAndroidDevicesActionPerformed
        androidDevices.setModel(new DefaultComboBoxModel(screenshotAction.getDevices().toArray()));
    }//GEN-LAST:event_loadAndroidDevicesActionPerformed

    private void loadResourceFromPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadResourceFromPageActionPerformed
        loadFromPage();
    }//GEN-LAST:event_loadResourceFromPageActionPerformed

    private void mapCurrentToPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mapCurrentToPageActionPerformed
        mapToPage();
    }//GEN-LAST:event_mapCurrentToPageActionPerformed

    private void addToSelectedPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addToSelectedPageActionPerformed
//        selectedPageNode = null;
//        if (addToSelectedPage.isSelected()) {
//            if (mobileObjectTree.getSelectedNode() != null) {
//                MobileObjectNode node = mobileObjectTree.getSelectedNode();
//                if (node.isPage()) {
//                    selectedPageNode = node;
//                } else if (node.isObject()) {
//                    selectedPageNode = node.getParent();
//                } else {
//                    Notification.show("Please select a Page");
//                    addToSelectedPage.setSelected(false);
//                }
//                addToSelectedPage.setToolTipText("Add Objects to Selected Page -{{ " + selectedPageNode.getText() + " }}");
//            } else {
//                Notification.show("Please select a Page");
//                addToSelectedPage.setSelected(false);
//            }
//        } else {
//            addToSelectedPage.setToolTipText("Add Objects to Selected Page");
//        }
    }//GEN-LAST:event_addToSelectedPageActionPerformed

    private void loadPackageAndActivityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadPackageAndActivityActionPerformed
        screenshotAction.setSerial(androidDevices.getSelectedItem());
        String[] details = screenshotAction.getPackageNameAndActivityName();
        if (details.length > 0) {
            packageName.setText(details[0]);
            activityName.setText(details[1]);
        } else {
            Notification.show("Couldn't fetch the details");
        }
    }//GEN-LAST:event_loadPackageAndActivityActionPerformed

    private void addAsAndroidemulatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAsAndroidemulatorActionPerformed
        addRUpdateAndroidEmulator();
    }//GEN-LAST:event_addAsAndroidemulatorActionPerformed

    private void addRUpdateIOSEmulator() {
        Object val = iosEmulatorCombo.getEditor().getItem();
        if (addNewEmulator(val)) {
            LinkedProperties prop = new LinkedProperties();
            for (int i = 0; i < jTable3.getRowCount(); i++) {
                String attr = Objects.toString(jTable3.getValueAt(i, 0), "").trim();
                String value = Objects.toString(jTable3.getValueAt(i, 1), "");
                if (!attr.isEmpty()) {
                    prop.put(attr, value);
                }
            }
            sMainFrame.getProject().getProjectSettings().getCapabilities().addCapability(val.toString(), prop);
        }
    }

    private Boolean addNewEmulator(Object emulatorName) {
        String val = Objects.toString(emulatorName, "");
        if (!val.trim().isEmpty()) {
            if (((DefaultComboBoxModel) iosEmulatorCombo.getModel()).getIndexOf(val) == -1) {
                sMainFrame.getProject().getProjectSettings()
                        .getEmulators().addAppiumEmulator(val, appiumServerLoc.getText());
                ((DefaultComboBoxModel) iosEmulatorCombo.getModel()).addElement(val);
                iosEmulatorCombo.setSelectedItem(emulatorName);
                andEmulatorCombo.setSelectedItem(emulatorName);
                sMainFrame.reloadBrowsers();
            }
            return true;
        }
        return false;
    }

    private void addRUpdateAndroidEmulator() {
        Object val = andEmulatorCombo.getEditor().getItem();
        if (addNewEmulator(val)) {
            String udid = Objects.toString(androidDevices.getSelectedItem(), "");
            sMainFrame.getProject().getProjectSettings().getCapabilities().
                    addDefaultAppiumCapability(val.toString(), udid, packageName.getText(), activityName.getText());
        }
    }

    private void loadFromPage() {
        String[] paths = mobileUtils.getResourcesFor(getPageFromSelection());
        if (paths.length > 0) {
            mobileTree.loadTree(paths[0]);
            mobileUtils.setScreenShotImageToLabelWResize(new File(paths[1]));
        } else {
            Notification.show("File Not found/not mapped for the selected Page");
        }
    }

    private void mapToPage() {
        MobileORPage page = getPageFromSelection();
        if (mobileTree.saveAsXML(page)) {
            mobileUtils.saveImageTo(page);
        }
    }

    private void loadDefaultAppiumCaps() {
        DefaultTableModel model = (DefaultTableModel) jTable3.getModel();
        model.setRowCount(0);
        model.addRow(new Object[]{"udid", null});       
        model.addRow(new Object[]{"deviceName", "iPhone 5s"});
        model.addRow(new Object[]{"platformName", "iOS"});
        model.addRow(new Object[]{"platformVersion", "8.2"});
        model.addRow(new Object[]{"automationName", "XCUITest"});
        model.addRow(new Object[]{"bundleId", ""});
        model.addRow(new Object[]{"xcodeOrgId", "<Team ID>"});
        model.addRow(new Object[]{"xcodeSigningId", "iPhone Developer"});
        model.addRow(new Object[]{"noReset", "true"});
        model.addRow(new Object[]{"app", "application.app"});
    }

    private void loadEmulator(String emulatorName) {
        Emulator emul = sMainFrame.getProject().getProjectSettings()
                .getEmulators().getEmulator(emulatorName);
        if (emul != null) {
            appiumServerLoc.setText(emul.getRemoteUrl());
            PropUtils.loadPropertiesInTable(
                    sMainFrame.getProject().getProjectSettings().getCapabilities().getCapabiltiesFor(emulatorName), jTable3);
        }
    }

    private void switchTo(Boolean isIOs) {
        if (isIOs) {
            mobileUtils = IOSUtil.get();
            mobileTree = IOSTree.get();
            jToggleButton1.setIcon(ios);
        } else {
            mobileUtils = AndroidUtil.get();
            mobileTree = AndroidTree.get();
            jToggleButton1.setIcon(android);
        }
        mobileUtils.setValues(jTree1, screenShotLabel, jTable1);
        mobileTree.setTree(jTree1);
    }

    private void showOpenDialog() {
     //   fileChooserDialog.pack();
     //   fileChooserDialog.setLocationRelativeTo(null);
     //   fileChooserDialog.setVisible(true);
    }

    private String showFileChooser(FileFilter ff) {
        jFileChooser1.setFileFilter(ff);
        int option = jFileChooser1.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            return jFileChooser1.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private void setLoadingCursor() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void setNormalCursor() {
        this.setCursor(Cursor.getDefaultCursor());
    }

    private void loadAndroidXMLAndScreenShot() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                screenshotAction.setSerial(androidDevices.getSelectedItem());
                File xml = screenshotAction.takeXML();
                File screenshot = screenshotAction.takeScreenshot();
                loadAndroidXMLScreenshot(xml, screenshot);
                setNormalCursor();
            }
        };
        setLoadingCursor();
        SwingUtilities.invokeLater(r);
    }

    private void loadIOSXMLAndScreenShot() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String remoteServer = "";
                if(appiumServerLoc.getText().equals("http://127.0.0.1:4723/wd/hub"))
                    remoteServer = "http://127.0.0.1:4723/wd/hub";
                else{
                    Matcher matcher = Pattern.compile("^((http[s]?):\\/)?\\/?([^:\\/\\s]+)(:([^\\/]*))?((\\/\\w+)*\\/)([\\w\\-\\.]+[^#?\\s]+)(\\?([^#]*))?(#(.*))?$").matcher(appiumServerLoc.getText()); 
                    if(matcher.matches()){
                         remoteServer = matcher.group(0); 
                    }else{
                        Notification.show("URL is not safe!");
                    }
                }
                if(remoteServer.startsWith("http") && remoteServer.endsWith("/wd/hub"))
            //    IOSpy.setSettings(remoteServer, jTable3);
            //    String xml = IOSpy.getXML();
            //    String screenshot = IOSpy.getScreenShot();
            //    if (xml != null && screenshot != null) {
            //        mobileTree.loadTree(xml);
            //        mobileUtils.setScreenShotImageToLabelWResize(screenshot);
            //    }
                setNormalCursor();
            }
        };
        setLoadingCursor();
        SwingUtilities.invokeLater(r);
    }

    private void loadAndroidXMLScreenshot(File xml, File screenshot) {
        if (xml != null && screenshot != null) {
            mobileTree.loadTree(xml.getAbsolutePath());
            AndroidUtil.get().rotateScreenshot(screenshot);
            mobileUtils.setScreenShotImageToLabelWResize(screenshot);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField activityName;
    private javax.swing.JButton addAsAndroidemulator;
    private javax.swing.JButton addRow;
    private javax.swing.JToggleButton addToSelectedPage;
    private javax.swing.JComboBox andEmulatorCombo;
    private javax.swing.JComboBox androidDevices;
    private javax.swing.JPanel androidSettings;
    private javax.swing.JTextField appiumServerLoc;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JSplitPane imageAndObjectRepo;
    private javax.swing.JComboBox iosEmulatorCombo;
    private javax.swing.JPanel iosSettings;
    private javax.swing.JButton jButton1;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable3;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JTree jTree1;
    private javax.swing.JButton loadAndroidDevices;
    private javax.swing.JButton loadDefault;
    private javax.swing.JMenuItem loadFromPage;
    private javax.swing.JButton loadPackageAndActivity;
    private javax.swing.JButton loadResourceFromPage;
    private javax.swing.JButton loadScreen;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton mapCurrentToPage;
    private javax.swing.JMenuItem mapToPage;
    private javax.swing.JTable mobilePropTable;
    private javax.swing.JTextField packageName;
    private javax.swing.JButton removeRow;
    private javax.swing.JLabel screenShotLabel;
    private javax.swing.JToggleButton settings;
    private javax.swing.JTabbedPane settingsPane;
    private javax.swing.JToggleButton spyrHeal;
    private javax.swing.JPanel treePanel;
    private javax.swing.JButton updateEmulator;
    // End of variables declaration//GEN-END:variables
}
