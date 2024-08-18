
package com.ing.storywriter.bdd.ui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import com.ing.storywriter.bdd.data.Story;
import com.ing.storywriter.bdd.data.BDDProject;
import com.ing.storywriter.bdd.data.StoryParser;
import com.ing.storywriter.bdd.ui.handlers.FeatureListTransferHandler;
import com.ing.storywriter.util.Notification.Msg;
import com.ing.storywriter.util.SLogger;
import com.ing.storywriter.util.Tools;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 */
public final class UIControl {

    BDDProject project;
    String path = "projects/test.json";
    public UI2 ui;
    public static JFileChooser openProj, newProj, importFeature, exportFeature;
    Story cStory;
    boolean editStory;
    public MouseListener sClick;
    public static UIControl ctrl;
    boolean editScn;
    private static final Logger LOGGER = Logger.getLogger(UIControl.class.getName());
    RecentItems rItems;
    TransferHandler featureTransferHandler;

    public UIControl() throws IOException {
        openProj = new JFileChooser(System.getProperty("user.dir"), null);
        openProj.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
        openProj.setFileFilter(com.ing.storywriter.util.Tools.json);

        newProj = new JFileChooser(System.getProperty("user.dir"), null);
        newProj.setFileSelectionMode(JFileChooser.SAVE_DIALOG);
        newProj.setFileFilter(com.ing.storywriter.util.Tools.json);
        exportFeature = new JFileChooser(System.getProperty("user.dir"), null);

        importFeature = new JFileChooser(System.getProperty("user.dir"), null);
        importFeature.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
        importFeature.setFileFilter(com.ing.storywriter.util.Tools.feature);

        featureTransferHandler = new FeatureListTransferHandler(this::importFeature);
        ui = new UI2(this);
        rItems = new RecentItems();
        File latest = rItems.lastProject();
        if (latest != null) {
            loadProj(latest.getAbsolutePath());
        }
        rItems.updateMenu(ui.recentsMenu);
        initListeners();
        ui.setVisible(true);
    }

    /**
     * loads a projects
     *
     * @param path
     */
    public void loadProj(String path) {
        try {
            clearAll();
            project = BDDProject.load(new File(path));
            ui.eTools(true);
            if (project.hasStories()) {
                ui.initStoryData(project.getStories());
            }
            ui.setTitle(new File(path).getName().replace(".json", ""));
        } catch (Exception ex) {
            SLogger.LOGE(ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {

        try {
            LogManager logManager = LogManager.getLogManager();
            try (InputStream is = UIManager.class.getResourceAsStream("/log.config")) {
                logManager.readConfiguration(is);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Metal")) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        ctrl = new UIControl();

    }

    /**
     * initialize listeners
     */
    private void initListeners() {
        ui.featureList.addListSelectionListener((ListSelectionEvent e) -> {
            storyChanged();
        });
        sClick = (new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    startEdit((Story) ui.featureList.getSelectedValue());
                }
            }
        });

    }

    /**
     * initiate actions on story change
     */
    public void storyChanged() {
        Story s = (Story) ui.featureList.getSelectedValue();
        if (s != null && cStory != s) {
            cStory = s;
            ui.setStoryText(s.getData());
        }
    }

    /**
     * set story edit flag true
     *
     * @param s
     */
    void startEdit(Story s) {
        editStory = true;
        ui.addStory.setVisible(true);
        ui.storyName.setText(s.desc);
    }

    /**
     * removes the story selected
     */
    void removeStory() {
        int[] list = ui.featureList.getSelectedIndices();
        if (list.length > 0) {
            int index = list[0] - 1;
            for (int i : list) {
                Story s = (Story) ui.featureList.getSelectedValue();
                project.getStories().remove(s);
                ui.storydata.removeElement(s);
            }
            ui.refreshStoryList();
            if (index >= 0) {
                ui.featureList.setSelectedIndex(index);
            }
        }

    }

    /**
     * save the project
     */
    void saveAll() {
        try {
            saveFeature(ui.getStoryText());
            project.write();
            SLogger.LOGA(Msg.C_SAVE);
            LOGGER.info(Msg.C_SAVE);
        } catch (Exception ex) {
            SLogger.LOGA(Msg.SOS + ex.getMessage());
            LOGGER.log(Level.SEVERE, Msg.SOS, ex);
        }
    }

    /**
     * export a selected story/feature
     */
    void export() {
        Story s = (Story) ui.featureList.getSelectedValue();
        if (s != null) {
            exportFeature.setSelectedFile(new File(String.format("%s.feature", s.name)));
            int stat = exportFeature.showSaveDialog(ui);
            if (stat == JFileChooser.APPROVE_OPTION) {
                Tools.writeFile(exportFeature.getSelectedFile(), s.getData());
                SLogger.LOGA(Msg.C_EXPORT);
                LOGGER.info(Msg.C_EXPORT);
            }
        } else {
            SLogger.LOGA(Msg.NO_EXPORT);
            LOGGER.warning(Msg.NO_EXPORT);
        }
    }

    /**
     * open a project (json)
     */
    void openProj() {
        int res = openProj.showOpenDialog(ui);
        if (res == JFileChooser.APPROVE_OPTION) {
            loadProj(openProj.getSelectedFile().getAbsolutePath());
            rItems.addentry(openProj.getSelectedFile().getAbsolutePath());
            rItems.updateMenu(ui.recentsMenu);
            SLogger.LOGA(openProj.getSelectedFile().getAbsolutePath());
            LOGGER.info(openProj.getSelectedFile().getAbsolutePath());

        }
    }

    /**
     * add or edit a story
     */
    void addStory() {
        String name = ui.storyName.getText();
        if (editStory) {
             LOGGER.info(String.format("Updating Feature %s", ((Story) ui.featureList.getSelectedValue()).name));
            ((Story) ui.featureList.getSelectedValue()).name = ui.storyName.getText();
           
        } else if (name != null && !"".equals(name)) {            
            Story s = new Story(name, ui.storyDesc.getText());
            LOGGER.info(String.format("Adding Feature %s", s.name));
            addStory(s);
            ui.refreshStoryList();
        }
    }

    /**
     * add story to stories
     *
     * @param s
     */
    void addStory(Story s) {
        ui.storydata.addElement(s);
        project.getStories().add(s);
    }

    /**
     * creates new project
     */
    
    private static String sanitizePathTraversal(String filename) {
        Path p = Paths.get(filename);
        return p.getFileName().toString();
    }
    
    void createNewProject() {
        String name = sanitizePathTraversal(ui.npName.getText());
        if (!name.isEmpty()) {
            File f = new File("projects" + File.separator + name + ".json");
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                BDDProject.create(f, name, ui.npDesc.getText());
                loadProj(f.getAbsolutePath());
                rItems.addentry(f.getAbsolutePath());
                rItems.updateMenu(ui.recentsMenu);
                SLogger.LOGA("New Project created!\n" + f.getAbsolutePath());
                LOGGER.log(Level.INFO, "New Project created! {0}", f.getAbsolutePath());
            } else {
                SLogger.LOGA("Project Exist!\n" + f.getAbsolutePath());
                LOGGER.log(Level.WARNING, "Project Exist! {0}", f.getAbsolutePath());
            }
        }

    }

    /**
     * clear all existing
     */
    private void clearAll() {
        ui.storydata.clear();

    }

    /**
     * Get - Open - Parse and Import a feature file
     */
    void importFeature() {
        int stat = importFeature.showOpenDialog(ui);
        if (stat == JFileChooser.APPROVE_OPTION) {
            importFeature(importFeature.getSelectedFile());
        }
    }

    /**
     * Get - Open - Parse and Import a feature file
     *
     * @param feature
     */
    public void importFeature(File feature) {

        try {
            LOGGER.log(Level.INFO, "Importing {0}", feature.getName());
            StoryParser sp = new StoryParser(feature);
            project.getStories().addAll(sp.stories());
            sp.stories().stream().forEach(ui.storydata::addElement);
            ui.refreshStoryList();
            SLogger.LOGA(String.format("%s Imported!", feature.getName()));
            LOGGER.log(Level.INFO, String.format("%s Imported!", feature.getName()));
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, Msg.SOS, ex);
        }
    }

    void saveFeature(String text) {
        cStory.setData(text);
        ui.updateAutoComplete();
    }

}
