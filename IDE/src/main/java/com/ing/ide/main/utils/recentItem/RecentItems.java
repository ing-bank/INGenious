package com.ing.ide.main.utils.recentItem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.component.Project;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.util.Notification;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class RecentItems extends JMenu implements ActionListener {
    private static final Logger LOG = Logger.getLogger(RecentItems.class.getName());
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final File RECENT_FILE = new File("recent.items");

    private List<RecentItem> RECENT_ITEMS = new ArrayList<>();

    AppMainFrame sMainFrame;

    public RecentItems(AppMainFrame sMainFrame) {
        super("Recent Projects");
        this.sMainFrame = sMainFrame;
        load();
    }

    private void load() {
        try {
            if (RECENT_FILE.exists()) {
                RECENT_ITEMS =
                    MAPPER.readValue(RECENT_FILE, new TypeReference<List<RecentItem>>() {});
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error loading recent items", ex);
        }
        checkIfExists();
        loadToMenus();
    }

    private void checkIfExists() {
        for (int i = RECENT_ITEMS.size() - 1; i >= 0; i--) {
            if (!new File(RECENT_ITEMS.get(i).getLocation()).exists()) {
                RECENT_ITEMS.remove(i);
            }
        }
    }

    private void loadToMenus() {
        for (RecentItem recentItem : RECENT_ITEMS) {
            add(getMenu(recentItem));
        }
    }

    private JMenuItem getMenu(RecentItem recentItem) {
        JMenuItem menuItem = new JMenuItem(recentItem.getProjectName());
        menuItem.addActionListener(this);
        menuItem.setToolTipText(recentItem.getLocation());
        return menuItem;
    }

    public void addItem(Project sProject) {
        removeIfExists(sProject);
        RecentItem item = new RecentItem();
        item.setProjectName(sProject.getName());
        item.setLocation(sProject.getLocation());
        RECENT_ITEMS.add(0, item);
        insert(getMenu(item), 0);
    }

    private void removeIfExists(Project sProject) {
        for (RecentItem recentItem : RECENT_ITEMS) {
            if (recentItem.getLocation().equals(sProject.getLocation())) {
                remove(RECENT_ITEMS.indexOf(recentItem));
                RECENT_ITEMS.remove(recentItem);
                break;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        JMenuItem menuItem = (JMenuItem) ae.getSource();
        String location = menuItem.getToolTipText();

        // Validate that the project path exists
        if (!new File(location).exists()) {
            Notification.show("Project path no longer exists: " + location);
            removeItemByLocation(location);
            save();
            return;
        }

        sMainFrame.loadProject(location);
    }

    public void save() {
        try {
            MAPPER.writeValue(RECENT_FILE, RECENT_ITEMS);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error saving recent items", ex);
        }
    }

    public Boolean isEmpty() {
        return RECENT_ITEMS.isEmpty();
    }

    public String getRecentProjectLocation() {
        return RECENT_ITEMS.get(0).getLocation();
    }

    public List<RecentItem> getRECENT_ITEMS() {
        return RECENT_ITEMS;
    }

    /**
     * Removes a recent item by its location path.
     * Used when a project path no longer exists.
     *
     * @param location The project location path to remove
     */
    public void removeItemByLocation(String location) {
        for (int i = RECENT_ITEMS.size() - 1; i >= 0; i--) {
            if (RECENT_ITEMS.get(i).getLocation().equals(location)) {
                remove(i);
                RECENT_ITEMS.remove(i);
                break;
            }
        }
    }
}
