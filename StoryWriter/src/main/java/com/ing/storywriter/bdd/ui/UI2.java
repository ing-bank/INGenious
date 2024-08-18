
package com.ing.storywriter.bdd.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.ing.storywriter.bdd.data.Story;
import com.ing.storywriter.bdd.editor.StyledEditor;
import com.ing.storywriter.util.Notification.Msg;
import com.ing.storywriter.util.Validator;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.UIManager;

/**
 *
 */
public final class UI2 extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;

    DefaultListModel<Story> storydata;
    UIControl uic;
    JPopupMenu addPop, remPopup;
    StyledEditor textArea;
    private static final Logger LOGGER = Logger.getLogger(UI2.class.getName());

    /**
     * Creates new form UI2
     *
     * @param uic
     */
    public UI2(UIControl uic) {

        this.uic = uic;
        storydata = new DefaultListModel();
        initComponents();
        Image img = new ImageIcon(getClass().getResource("/favicon.png")).getImage();
        setIconImage(img);
        addStory.setIconImage(img);
        newProj.setIconImage(img);
        addPop = new JPopupMenu();
        remPopup = new JPopupMenu();
        textArea = new StyledEditor() {
            @Override
            public void onSave() {
                uic.saveFeature(textArea.getText());
            }
        };
        editorPanel.add(textArea.setup().getScrollView());
        npName.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                validate(npName.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                validate(npName.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
            }
        });
        featureList.setTransferHandler(uic.featureTransferHandler);
        this.setLocationRelativeTo(null);
        eTools(false);
    }

    /**
     * initialize story data
     *
     * @param stories
     */
    public void initStoryData(List<Story> stories) {
        try {
            storydata.clear();
            stories.stream().forEach(storydata::addElement);
        } catch (Exception ex) {
            Logger.getLogger(UI2.class.getName()).log(Level.SEVERE, null, ex);
        }
        refreshStoryList();
    }

    /**
     * refresh story list
     */
    public void refreshStoryList() {
        featureList.setModel(storydata);
        if (!storydata.isEmpty()) {
            featureList.setSelectedIndex(0);
            uic.storyChanged();
        }
        featureList.requestFocus();
        sScroll.revalidate();
        sScroll.repaint();
    }

    /**
     * initialize add/update story
     */
    void initiateAddStory() {
        if (!uic.editStory) {
            storyName.setText("");
            upStory.setText("Add");
        } else {
            upStory.setText("Update");
        }
    }

    /**
     * enable / disable tools
     *
     * @param b
     */
    public void eTools(boolean b) {
        eComponents(jToolBar1, b);
        importStoryBtn.setEnabled(b);
        saveProjBtn.setEnabled(b);
    }

    /**
     * enable / disable components
     *
     * @param t
     * @param s
     */
    private void eComponents(JToolBar t, boolean s) {
        for (Component c : t.getComponents()) {
            c.setEnabled(s);
        }
    }

    void setStoryText(String data) {
        textArea.setText(data);
    }

    void updateAutoComplete() {
        textArea.updateAutoComplete();
    }

    public String getStoryText() {
        return textArea.getText();
    }
    
    private static String sanitizePathTraversal(String filename) {
        Path p = Paths.get(filename);
        return p.getFileName().toString();
    }

    void validate(String text) {
        if (Validator.isValidName(text)) {
            File f = new File("projects", sanitizePathTraversal(text) + ".json");
            if (f.exists()) {
                notify(Msg.PROJ_EXIST, false);
                LOGGER.log(Level.WARNING, Msg.PROJ_EXIST + "{0}", f.getAbsolutePath());
            } else {
                notify(null, true);
            }
        } else {
            notify(Msg.INVALID_PROJ, false);
            LOGGER.warning(Msg.INVALID_PROJ);
        }
    }

    void notify(String tip, boolean valid) {
        npName.setBorder(new LineBorder(valid ? Color.GREEN : Color.RED));
        npName.setToolTipText(tip);
        createP.setEnabled(valid);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addStory = new javax.swing.JDialog(){

            public void setVisible(boolean b){
                if(b){
                    initiateAddStory();
                }
                super.setVisible(b);
            }
        };
        addFeature_controls = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        storyName = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        storyDesc = new javax.swing.JTextArea();
        addFeature_inputs = new javax.swing.JPanel();
        upStory = new javax.swing.JButton();
        newProj = new javax.swing.JDialog();
        jLabel5 = new javax.swing.JLabel();
        npName = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        npDesc = new javax.swing.JTextArea();
        createP = new javax.swing.JButton();
        metaCheckStory = new javax.swing.JCheckBox();
        contentPanel = new javax.swing.JPanel();
        contentSplit = new javax.swing.JSplitPane();
        featurePanel = new javax.swing.JPanel();
        editorHeader = new javax.swing.JPanel();
        name = new javax.swing.JLabel();
        try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
        editorPanel = new javax.swing.JPanel();
        featuresPanel = new javax.swing.JPanel();
        storytoolPanel = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        addStoryBtn = new javax.swing.JButton();
        remStortBtn = new javax.swing.JButton();
        jToolBar5 = new javax.swing.JToolBar();
        jLabel1 = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 110));
        newProjBtn = new javax.swing.JButton();
        openProjBtn = new javax.swing.JButton();
        saveProjBtn = new javax.swing.JButton();
        importStoryBtn = new javax.swing.JButton();
        exportStoryBtn = new javax.swing.JButton();
        storyPanel = new javax.swing.JPanel();
        sScroll = new javax.swing.JScrollPane();
        featureList = new javax.swing.JList();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        newProjMenu = new javax.swing.JMenuItem();
        openProjMenu = new javax.swing.JMenuItem();
        saveProjMenu = new javax.swing.JMenuItem();
        recentsMenu = new javax.swing.JMenu();

        addStory.setTitle("Feature");
        addStory.setAlwaysOnTop(true);
        addStory.setMinimumSize(new java.awt.Dimension(400, 200));
        addStory.setResizable(false);
        addStory.setLocationRelativeTo(null);

        jLabel3.setText("Description :");

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        storyName.setColumns(20);
        storyName.setRows(5);
        jScrollPane1.setViewportView(storyName);

        jLabel2.setText("Feature Name");

        storyDesc.setColumns(20);
        storyDesc.setRows(5);
        jScrollPane2.setViewportView(storyDesc);

        javax.swing.GroupLayout addFeature_controlsLayout = new javax.swing.GroupLayout(addFeature_controls);
        addFeature_controls.setLayout(addFeature_controlsLayout);
        addFeature_controlsLayout.setHorizontalGroup(
            addFeature_controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addFeature_controlsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(addFeature_controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 325, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap(65, Short.MAX_VALUE))
        );
        addFeature_controlsLayout.setVerticalGroup(
            addFeature_controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addFeature_controlsLayout.createSequentialGroup()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        addStory.getContentPane().add(addFeature_controls, java.awt.BorderLayout.NORTH);

        upStory.setText("Add");
        upStory.setSelected(true);
        upStory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upStoryActionPerformed(evt);
            }
        });
        upStory.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                upStoryKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout addFeature_inputsLayout = new javax.swing.GroupLayout(addFeature_inputs);
        addFeature_inputs.setLayout(addFeature_inputsLayout);
        addFeature_inputsLayout.setHorizontalGroup(
            addFeature_inputsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, addFeature_inputsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(upStory)
                .addContainerGap())
        );
        addFeature_inputsLayout.setVerticalGroup(
            addFeature_inputsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addFeature_inputsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(upStory)
                .addContainerGap())
        );

        addStory.getContentPane().add(addFeature_inputs, java.awt.BorderLayout.SOUTH);

        newProj.setTitle("New Project");
        newProj.setAlwaysOnTop(true);
        newProj.setMinimumSize(new java.awt.Dimension(348, 205));
        newProj.setResizable(false);

        jLabel5.setText("Project Name :");

        jLabel6.setText("Description :");

        npDesc.setColumns(20);
        npDesc.setRows(5);
        jScrollPane3.setViewportView(npDesc);

        createP.setText("Create");
        createP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createPActionPerformed(evt);
            }
        });
        createP.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                createPKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout newProjLayout = new javax.swing.GroupLayout(newProj.getContentPane());
        newProj.getContentPane().setLayout(newProjLayout);
        newProjLayout.setHorizontalGroup(
            newProjLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(newProjLayout.createSequentialGroup()
                .addGroup(newProjLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(createP)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 264, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(newProjLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(newProjLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jLabel5))
                        .addGroup(newProjLayout.createSequentialGroup()
                            .addGap(47, 47, 47)
                            .addComponent(npName, javax.swing.GroupLayout.PREFERRED_SIZE, 263, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(newProjLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jLabel6))))
                .addContainerGap(38, Short.MAX_VALUE))
        );
        newProjLayout.setVerticalGroup(
            newProjLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(newProjLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(npName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(createP)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        newProj.setLocationRelativeTo(null);

        metaCheckStory.setText("Given Stories");
        metaCheckStory.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                metaCheckStoryStateChanged(evt);
            }
        });
        metaCheckStory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                metaCheckStoryActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FeatureEditor");
        setMinimumSize(new java.awt.Dimension(782, 494));
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        featurePanel.setBackground(new java.awt.Color(224, 207, 156));

        editorHeader.setBackground(new java.awt.Color(224, 207, 156));

        name.setBackground(new java.awt.Color(224, 207, 156));
        name.setText("Feature");

        javax.swing.GroupLayout editorHeaderLayout = new javax.swing.GroupLayout(editorHeader);
        editorHeader.setLayout(editorHeaderLayout);
        editorHeaderLayout.setHorizontalGroup(
            editorHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editorHeaderLayout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addComponent(name, javax.swing.GroupLayout.PREFERRED_SIZE, 516, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34))
        );
        editorHeaderLayout.setVerticalGroup(
            editorHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editorHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(name)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        editorPanel.setBackground(new java.awt.Color(224, 207, 156));
        editorPanel.setLayout(new java.awt.BorderLayout());
        editorPanel.setFont(new Font("ING Me", Font.BOLD, 12));

        javax.swing.GroupLayout featurePanelLayout = new javax.swing.GroupLayout(featurePanel);
        featurePanel.setLayout(featurePanelLayout);
        featurePanelLayout.setHorizontalGroup(
            featurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editorHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(editorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        featurePanelLayout.setVerticalGroup(
            featurePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(featurePanelLayout.createSequentialGroup()
                .addComponent(editorHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE))
        );

        contentSplit.setRightComponent(featurePanel);

        featuresPanel.setLayout(new java.awt.BorderLayout());

        storytoolPanel.setLayout(new java.awt.BorderLayout());

        jToolBar1.setBackground(new java.awt.Color(224, 207, 156));
        jToolBar1.setRollover(true);
        jToolBar1.setBorderPainted(false);

        addStoryBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/add.png"))); // NOI18N
        addStoryBtn.setToolTipText("Add a Story");
        addStoryBtn.setFocusable(false);
        addStoryBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addStoryBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addStoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addStoryBtnActionPerformed(evt);
            }
        });
        jToolBar1.add(addStoryBtn);

        remStortBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/rem.png"))); // NOI18N
        remStortBtn.setToolTipText("Remove Story");
        remStortBtn.setFocusable(false);
        remStortBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        remStortBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        remStortBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remStortBtnActionPerformed(evt);
            }
        });
        jToolBar1.add(remStortBtn);

        storytoolPanel.add(jToolBar1, java.awt.BorderLayout.EAST);

        jToolBar5.setBackground(new java.awt.Color(224, 207, 156));
        jToolBar5.setRollover(true);
        jToolBar5.setBorderPainted(false);

        jLabel1.setFont(UIManager.getFont("Table.font"));
        jLabel1.setText("Stories");
        jToolBar5.add(jLabel1);
        jToolBar5.add(filler1);

        newProjBtn.setForeground(new java.awt.Color(204, 204, 255));
        newProjBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/new-project.png"))); // NOI18N
        newProjBtn.setToolTipText("New Project");
        newProjBtn.setFocusable(false);
        newProjBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        newProjBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        newProjBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProjBtnActionPerformed(evt);
            }
        });
        jToolBar5.add(newProjBtn);

        openProjBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/open-project.png"))); // NOI18N
        openProjBtn.setToolTipText("Open Project");
        openProjBtn.setFocusable(false);
        openProjBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openProjBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        openProjBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openProjBtnActionPerformed(evt);
            }
        });
        jToolBar5.add(openProjBtn);

        saveProjBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/saveall.png"))); // NOI18N
        saveProjBtn.setToolTipText("Save Project");
        saveProjBtn.setFocusable(false);
        saveProjBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveProjBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveProjBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjBtnActionPerformed(evt);
            }
        });
        jToolBar5.add(saveProjBtn);

        importStoryBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/001-import.png"))); // NOI18N
        importStoryBtn.setToolTipText("Import  Feature");
        importStoryBtn.setFocusPainted(false);
        importStoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importStoryBtnActionPerformed(evt);
            }
        });
        jToolBar5.add(importStoryBtn);

        exportStoryBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/002-export.png"))); // NOI18N
        exportStoryBtn.setToolTipText("Export as Feature File");
        exportStoryBtn.setFocusable(false);
        exportStoryBtn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        exportStoryBtn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        exportStoryBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportStoryBtnActionPerformed(evt);
            }
        });
        jToolBar5.add(exportStoryBtn);

        storytoolPanel.add(jToolBar5, java.awt.BorderLayout.WEST);

        featuresPanel.add(storytoolPanel, java.awt.BorderLayout.PAGE_START);

        storyPanel.setBackground(new java.awt.Color(255, 255, 255));
        storyPanel.setMaximumSize(new java.awt.Dimension(275, 394));
        storyPanel.setPreferredSize(new java.awt.Dimension(275, 394));
        storyPanel.setLayout(new java.awt.BorderLayout());

        sScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        featureList.setModel(new DefaultListModel<Story>());
        featureList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                featureListMousePressed(evt);
            }
        });
        featureList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                featureListKeyPressed(evt);
            }
        });
        sScroll.setViewportView(featureList);
        featureList.setCellRenderer(new com.ing.storywriter.bdd.renderer.StoryListRenderer());

        storyPanel.add(sScroll, java.awt.BorderLayout.CENTER);

        featuresPanel.add(storyPanel, java.awt.BorderLayout.CENTER);

        contentSplit.setLeftComponent(featuresPanel);

        javax.swing.GroupLayout contentPanelLayout = new javax.swing.GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
            contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 829, Short.MAX_VALUE)
            .addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(contentSplit, javax.swing.GroupLayout.DEFAULT_SIZE, 829, Short.MAX_VALUE))
        );
        contentPanelLayout.setVerticalGroup(
            contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 484, Short.MAX_VALUE)
            .addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(contentSplit))
        );

        getContentPane().add(contentPanel);

        jMenu3.setText("File");
        jMenu3.setFont(UIManager.getFont("Table.font"));

        newProjMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        newProjMenu.setMnemonic('N');
        newProjMenu.setText("New Project");
        newProjMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newProjMenuActionPerformed(evt);
            }
        });
        jMenu3.add(newProjMenu);

        openProjMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openProjMenu.setMnemonic('O');
        openProjMenu.setText("Open Project");
        openProjMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openProjMenuActionPerformed(evt);
            }
        });
        jMenu3.add(openProjMenu);

        saveProjMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        saveProjMenu.setMnemonic('S');
        saveProjMenu.setText("Save");
        saveProjMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjMenuActionPerformed(evt);
            }
        });
        jMenu3.add(saveProjMenu);

        recentsMenu.setText("Recent Projects");
        jMenu3.add(recentsMenu);

        jMenuBar2.add(jMenu3);

        setJMenuBar(jMenuBar2);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked


    }//GEN-LAST:event_formMouseClicked

    private void remStortBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remStortBtnActionPerformed
        uic.removeStory();
    }//GEN-LAST:event_remStortBtnActionPerformed

    private void saveProjBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjBtnActionPerformed

        uic.saveAll();
    }//GEN-LAST:event_saveProjBtnActionPerformed

    private void exportStoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportStoryBtnActionPerformed
        uic.export();
    }//GEN-LAST:event_exportStoryBtnActionPerformed

    private void openProjBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjBtnActionPerformed
        uic.openProj();
    }//GEN-LAST:event_openProjBtnActionPerformed

    private void metaCheckStoryStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_metaCheckStoryStateChanged

// addStory.pack();
    }//GEN-LAST:event_metaCheckStoryStateChanged

    private void upStoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upStoryActionPerformed
        uic.addStory();
        addStory.setVisible(false);
    }//GEN-LAST:event_upStoryActionPerformed

    private void addStoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addStoryBtnActionPerformed
        uic.editStory = false;
        addStory.setVisible(true);
    }//GEN-LAST:event_addStoryBtnActionPerformed

    private void newProjBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProjBtnActionPerformed
        npName.setText("");
        npDesc.setText("");
        newProj.setVisible(true);
    }//GEN-LAST:event_newProjBtnActionPerformed

    private void createPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createPActionPerformed
        uic.createNewProject();
        newProj.setVisible(false);
    }//GEN-LAST:event_createPActionPerformed

    private void metaCheckStoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_metaCheckStoryActionPerformed

    }//GEN-LAST:event_metaCheckStoryActionPerformed

    private void featureListMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_featureListMousePressed
        if (evt.isAltDown()) {
            uic.startEdit((Story) featureList.getSelectedValue());
        }
    }//GEN-LAST:event_featureListMousePressed

    private void importStoryBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importStoryBtnActionPerformed
        uic.importFeature();
    }//GEN-LAST:event_importStoryBtnActionPerformed

    private void featureListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_featureListKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_F2) {
            if (featureList.getSelectedValue() != null) {
                uic.startEdit((Story) featureList.getSelectedValue());
            }
        }
    }//GEN-LAST:event_featureListKeyPressed

    private void upStoryKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_upStoryKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            uic.addStory();
            addStory.setVisible(false);
        }
    }//GEN-LAST:event_upStoryKeyPressed

    private void createPKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_createPKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            uic.createNewProject();
            newProj.setVisible(false);
        }
    }//GEN-LAST:event_createPKeyPressed

    private void saveProjMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjMenuActionPerformed
        uic.saveAll();
    }//GEN-LAST:event_saveProjMenuActionPerformed

    private void openProjMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjMenuActionPerformed
        uic.openProj();
    }//GEN-LAST:event_openProjMenuActionPerformed

    private void newProjMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProjMenuActionPerformed
        npName.setText("");
        npDesc.setText("");
        newProj.setVisible(true);
    }//GEN-LAST:event_newProjMenuActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel addFeature_controls;
    private javax.swing.JPanel addFeature_inputs;
    public javax.swing.JDialog addStory;
    private javax.swing.JButton addStoryBtn;
    private javax.swing.JPanel contentPanel;
    private javax.swing.JSplitPane contentSplit;
    private javax.swing.JButton createP;
    private javax.swing.JPanel editorHeader;
    private javax.swing.JPanel editorPanel;
    private javax.swing.JButton exportStoryBtn;
    public javax.swing.JList featureList;
    private javax.swing.JPanel featurePanel;
    private javax.swing.JPanel featuresPanel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JButton importStoryBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar5;
    private javax.swing.JCheckBox metaCheckStory;
    private javax.swing.JLabel name;
    private javax.swing.JDialog newProj;
    private javax.swing.JButton newProjBtn;
    private javax.swing.JMenuItem newProjMenu;
    public javax.swing.JTextArea npDesc;
    public javax.swing.JTextField npName;
    private javax.swing.JButton openProjBtn;
    private javax.swing.JMenuItem openProjMenu;
    public javax.swing.JMenu recentsMenu;
    private javax.swing.JButton remStortBtn;
    private javax.swing.JScrollPane sScroll;
    private javax.swing.JButton saveProjBtn;
    private javax.swing.JMenuItem saveProjMenu;
    public javax.swing.JTextArea storyDesc;
    public javax.swing.JTextArea storyName;
    private javax.swing.JPanel storyPanel;
    private javax.swing.JPanel storytoolPanel;
    public javax.swing.JButton upStory;
    // End of variables declaration//GEN-END:variables

}
