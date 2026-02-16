package com.ing.ide.main.mainui.components.apitester.collections;

import com.ing.datalib.api.APICollection;
import com.ing.datalib.api.APIRequest;
import com.ing.ide.main.mainui.components.apitester.APITester;
import com.ing.ide.main.mainui.components.apitester.APITesterUI;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Tree panel for displaying and managing API collections.
 */
public class CollectionTree extends JPanel {

    private final APITesterUI parentUI;
    private final APITester controller;
    
    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    
    private JPopupMenu collectionMenu;
    private JPopupMenu folderMenu;
    private JPopupMenu requestMenu;
    
    public CollectionTree(APITesterUI parentUI, APITester controller) {
        this.parentUI = parentUI;
        this.controller = controller;
        initComponents();
        createContextMenus();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(10, 10, 5, 10));
        
        JLabel titleLabel = new JLabel("Collections");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        
        JButton addBtn = new JButton("+");
        addBtn.setFont(addBtn.getFont().deriveFont(Font.BOLD, 14f));
        addBtn.setMargin(new Insets(0, 5, 0, 5));
        addBtn.setToolTipText("Create New Collection");
        addBtn.addActionListener(e -> createNewCollection());
        
        header.add(titleLabel, BorderLayout.WEST);
        header.add(addBtn, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);
        
        // Tree
        rootNode = new DefaultMutableTreeNode("Collections");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new CollectionTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        // Double-click to open request
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleDoubleClick();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void createContextMenus() {
        // Collection context menu
        collectionMenu = new JPopupMenu();
        
        JMenuItem addRequest = new JMenuItem("Add Request");
        addRequest.addActionListener(e -> addRequestToCollection());
        collectionMenu.add(addRequest);
        
        JMenuItem addFolder = new JMenuItem("Add Folder");
        addFolder.addActionListener(e -> addFolderToCollection());
        collectionMenu.add(addFolder);
        
        collectionMenu.addSeparator();
        
        JMenuItem renameCollection = new JMenuItem("Rename");
        renameCollection.addActionListener(e -> renameSelected());
        collectionMenu.add(renameCollection);
        
        JMenuItem exportCollection = new JMenuItem("Export");
        exportCollection.addActionListener(e -> exportCollection());
        collectionMenu.add(exportCollection);
        
        collectionMenu.addSeparator();
        
        JMenuItem deleteCollection = new JMenuItem("Delete");
        deleteCollection.addActionListener(e -> deleteSelected());
        collectionMenu.add(deleteCollection);
        
        // Folder context menu
        folderMenu = new JPopupMenu();
        
        JMenuItem addRequestToFolder = new JMenuItem("Add Request");
        addRequestToFolder.addActionListener(e -> addRequestToFolder());
        folderMenu.add(addRequestToFolder);
        
        folderMenu.addSeparator();
        
        JMenuItem renameFolder = new JMenuItem("Rename");
        renameFolder.addActionListener(e -> renameSelected());
        folderMenu.add(renameFolder);
        
        folderMenu.addSeparator();
        
        JMenuItem deleteFolder = new JMenuItem("Delete");
        deleteFolder.addActionListener(e -> deleteSelected());
        folderMenu.add(deleteFolder);
        
        // Request context menu
        requestMenu = new JPopupMenu();
        
        JMenuItem openRequest = new JMenuItem("Open");
        openRequest.addActionListener(e -> openSelectedRequest());
        requestMenu.add(openRequest);
        
        JMenuItem duplicateRequest = new JMenuItem("Duplicate");
        duplicateRequest.addActionListener(e -> duplicateRequest());
        requestMenu.add(duplicateRequest);
        
        requestMenu.addSeparator();
        
        JMenuItem renameRequest = new JMenuItem("Rename");
        renameRequest.addActionListener(e -> renameSelected());
        requestMenu.add(renameRequest);
        
        JMenuItem moveRequest = new JMenuItem("Move...");
        moveRequest.addActionListener(e -> moveRequest());
        requestMenu.add(moveRequest);
        
        requestMenu.addSeparator();
        
        JMenuItem deleteRequest = new JMenuItem("Delete");
        deleteRequest.addActionListener(e -> deleteSelected());
        requestMenu.add(deleteRequest);
    }
    
    private void showContextMenu(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;
        
        tree.setSelectionPath(path);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        
        if (userObject instanceof CollectionNode) {
            collectionMenu.show(tree, e.getX(), e.getY());
        } else if (userObject instanceof FolderNode) {
            folderMenu.show(tree, e.getX(), e.getY());
        } else if (userObject instanceof RequestNode) {
            requestMenu.show(tree, e.getX(), e.getY());
        }
    }
    
    private void handleDoubleClick() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return;
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        
        if (userObject instanceof RequestNode) {
            openSelectedRequest();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Collection Operations
    // ═══════════════════════════════════════════════════════════════════
    
    private void createNewCollection() {
        String name = JOptionPane.showInputDialog(this, 
            "Enter collection name:", "New Collection", JOptionPane.PLAIN_MESSAGE);
        
        if (name != null && !name.trim().isEmpty()) {
            APICollection collection = new APICollection(name.trim());
            controller.addCollection(collection);
            refreshTree();
        }
    }
    
    private void addRequestToCollection() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        CollectionNode colNode = (CollectionNode) node.getUserObject();
        
        String name = JOptionPane.showInputDialog(this,
            "Enter request name:", "New Request", JOptionPane.PLAIN_MESSAGE);
        
        if (name != null && !name.trim().isEmpty()) {
            APIRequest request = new APIRequest(name.trim());
            request.setUrl("https://api.example.com");
            colNode.collection.addRequest(request);
            controller.saveCollection(colNode.collection);
            refreshTree();
        }
    }
    
    private void addFolderToCollection() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        CollectionNode colNode = (CollectionNode) node.getUserObject();
        
        String name = JOptionPane.showInputDialog(this,
            "Enter folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
        
        if (name != null && !name.trim().isEmpty()) {
            APICollection folder = new APICollection(name.trim());
            colNode.collection.addFolder(folder);
            controller.saveCollection(colNode.collection);
            refreshTree();
        }
    }
    
    private void addRequestToFolder() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        FolderNode folderNode = (FolderNode) node.getUserObject();
        
        String name = JOptionPane.showInputDialog(this,
            "Enter request name:", "New Request", JOptionPane.PLAIN_MESSAGE);
        
        if (name != null && !name.trim().isEmpty()) {
            APIRequest request = new APIRequest(name.trim());
            request.setUrl("https://api.example.com");
            folderNode.folder.addRequest(request);
            controller.saveCollection(folderNode.parentCollection);
            refreshTree();
        }
    }
    
    private void openSelectedRequest() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        RequestNode reqNode = (RequestNode) node.getUserObject();
        parentUI.loadRequest(reqNode.request, reqNode.parentCollection);
    }
    
    private void duplicateRequest() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        RequestNode reqNode = (RequestNode) node.getUserObject();
        APIRequest copy = reqNode.request.copy();
        copy.setName(copy.getName() + " (Copy)");
        copy.setId(java.util.UUID.randomUUID().toString());
        
        reqNode.parentCollection.addRequest(copy);
        controller.saveCollection(reqNode.parentCollection);
        refreshTree();
    }
    
    private void moveRequest() {
        // Simple move dialog - show list of collections
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        RequestNode reqNode = (RequestNode) node.getUserObject();
        List<APICollection> collections = controller.getCollections();
        
        if (collections.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No other collections available.");
            return;
        }
        
        String[] names = collections.stream()
            .map(APICollection::getName)
            .toArray(String[]::new);
        
        String selected = (String) JOptionPane.showInputDialog(this,
            "Move to collection:", "Move Request",
            JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        
        if (selected != null) {
            APICollection target = collections.stream()
                .filter(c -> c.getName().equals(selected))
                .findFirst().orElse(null);
            
            if (target != null && target != reqNode.parentCollection) {
                // Remove from current collection
                reqNode.parentCollection.getRequests().remove(reqNode.request);
                controller.saveCollection(reqNode.parentCollection);
                
                // Add to target
                target.addRequest(reqNode.request);
                controller.saveCollection(target);
                refreshTree();
            }
        }
    }
    
    private void renameSelected() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        Object userObject = node.getUserObject();
        String currentName = "";
        
        if (userObject instanceof CollectionNode) {
            currentName = ((CollectionNode) userObject).collection.getName();
        } else if (userObject instanceof FolderNode) {
            currentName = ((FolderNode) userObject).folder.getName();
        } else if (userObject instanceof RequestNode) {
            currentName = ((RequestNode) userObject).request.getName();
        }
        
        String newName = JOptionPane.showInputDialog(this,
            "Enter new name:", currentName);
        
        if (newName != null && !newName.trim().isEmpty()) {
            if (userObject instanceof CollectionNode) {
                CollectionNode cn = (CollectionNode) userObject;
                cn.collection.setName(newName.trim());
                controller.saveCollection(cn.collection);
            } else if (userObject instanceof FolderNode) {
                FolderNode fn = (FolderNode) userObject;
                fn.folder.setName(newName.trim());
                controller.saveCollection(fn.parentCollection);
            } else if (userObject instanceof RequestNode) {
                RequestNode rn = (RequestNode) userObject;
                rn.request.setName(newName.trim());
                controller.saveCollection(rn.parentCollection);
            }
            refreshTree();
        }
    }
    
    private void deleteSelected() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        Object userObject = node.getUserObject();
        String name = "";
        String type = "";
        
        if (userObject instanceof CollectionNode) {
            name = ((CollectionNode) userObject).collection.getName();
            type = "collection";
        } else if (userObject instanceof FolderNode) {
            name = ((FolderNode) userObject).folder.getName();
            type = "folder";
        } else if (userObject instanceof RequestNode) {
            name = ((RequestNode) userObject).request.getName();
            type = "request";
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete " + type + " '" + name + "'?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (userObject instanceof CollectionNode) {
                CollectionNode cn = (CollectionNode) userObject;
                controller.deleteCollection(cn.collection);
            } else if (userObject instanceof FolderNode) {
                FolderNode fn = (FolderNode) userObject;
                fn.parentCollection.getFolders().remove(fn.folder);
                controller.saveCollection(fn.parentCollection);
            } else if (userObject instanceof RequestNode) {
                RequestNode rn = (RequestNode) userObject;
                rn.parentCollection.getRequests().remove(rn.request);
                // Also check folders
                for (APICollection f : rn.parentCollection.getFolders()) {
                    f.getRequests().remove(rn.request);
                }
                controller.saveCollection(rn.parentCollection);
            }
            refreshTree();
        }
    }
    
    private void exportCollection() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        
        CollectionNode colNode = (CollectionNode) node.getUserObject();
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Collection");
        chooser.setSelectedFile(new java.io.File(colNode.collection.getName() + ".json"));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                controller.exportCollection(colNode.collection, chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Collection exported successfully.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, 
                    "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private DefaultMutableTreeNode getSelectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;
        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Refreshes the tree with current collections.
     */
    public void refreshTree() {
        rootNode.removeAllChildren();
        
        for (APICollection collection : controller.getCollections()) {
            DefaultMutableTreeNode colNode = new DefaultMutableTreeNode(
                new CollectionNode(collection));
            
            // Add folders
            for (APICollection folder : collection.getFolders()) {
                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(
                    new FolderNode(folder, collection));
                
                // Add requests in folder
                for (APIRequest request : folder.getRequests()) {
                    folderNode.add(new DefaultMutableTreeNode(
                        new RequestNode(request, collection)));
                }
                
                colNode.add(folderNode);
            }
            
            // Add root-level requests
            for (APIRequest request : collection.getRequests()) {
                colNode.add(new DefaultMutableTreeNode(
                    new RequestNode(request, collection)));
            }
            
            rootNode.add(colNode);
        }
        
        treeModel.reload();
        
        // Expand all collections
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Node Wrapper Classes
    // ═══════════════════════════════════════════════════════════════════
    
    static class CollectionNode {
        final APICollection collection;
        
        CollectionNode(APICollection collection) {
            this.collection = collection;
        }
        
        @Override
        public String toString() {
            return collection.getName();
        }
    }
    
    static class FolderNode {
        final APICollection folder;
        final APICollection parentCollection;
        
        FolderNode(APICollection folder, APICollection parentCollection) {
            this.folder = folder;
            this.parentCollection = parentCollection;
        }
        
        @Override
        public String toString() {
            return folder.getName();
        }
    }
    
    static class RequestNode {
        final APIRequest request;
        final APICollection parentCollection;
        
        RequestNode(APIRequest request, APICollection parentCollection) {
            this.request = request;
            this.parentCollection = parentCollection;
        }
        
        @Override
        public String toString() {
            return request.getName();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Tree Cell Renderer
    // ═══════════════════════════════════════════════════════════════════
    
    private static class CollectionTreeCellRenderer extends DefaultTreeCellRenderer {
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            
            if (userObject instanceof CollectionNode) {
                setIcon(UIManager.getIcon("FileView.directoryIcon"));
                setText(((CollectionNode) userObject).collection.getName());
            } else if (userObject instanceof FolderNode) {
                setIcon(UIManager.getIcon("FileView.directoryIcon"));
                setText(((FolderNode) userObject).folder.getName());
            } else if (userObject instanceof RequestNode) {
                RequestNode reqNode = (RequestNode) userObject;
                APIRequest.HttpMethod method = reqNode.request.getMethod();
                
                // Create method badge
                String methodStr = method.name();
                setIcon(null);
                setText("<html><span style='color:" + getMethodColorHex(method) + 
                       ";font-size:9px;font-weight:bold;'>" + methodStr + 
                       "</span> " + reqNode.request.getName() + "</html>");
            }
            
            return this;
        }
        
        private String getMethodColorHex(APIRequest.HttpMethod method) {
            Color c;
            switch (method) {
                case GET:
                    c = APITesterColors.methodGet();
                    break;
                case POST:
                    c = APITesterColors.methodPost();
                    break;
                case PUT:
                    c = APITesterColors.methodPut();
                    break;
                case PATCH:
                    c = APITesterColors.methodPatch();
                    break;
                case DELETE:
                    c = APITesterColors.methodDelete();
                    break;
                default:
                    c = APITesterColors.statusNeutral();
            }
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
    }
    
    /**
     * Refresh theme colors when theme changes.
     */
    public void refreshThemeColors() {
        // Refresh panel background
        setBackground(UIManager.getColor("Panel.background"));
        
        // Refresh tree colors
        if (tree != null) {
            tree.setBackground(UIManager.getColor("Tree.background"));
            tree.setForeground(UIManager.getColor("Tree.foreground"));
            // Force tree to repaint with new colors
            tree.repaint();
        }
        
        // Refresh all child components
        refreshComponentColors(this);
        repaint();
    }
    
    /**
     * Recursively refresh colors on child components.
     */
    private void refreshComponentColors(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(UIManager.getColor("Panel.background"));
            } else if (c instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) c;
                sp.getViewport().setBackground(UIManager.getColor("Tree.background"));
            }
            if (c instanceof Container) {
                refreshComponentColors((Container) c);
            }
        }
    }
}
