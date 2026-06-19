package com.ing.ide.main.mainui.components.apitester.collections;

import com.ing.datalib.api.APICollection;
import com.ing.datalib.api.APIRequest;
import com.ing.ide.main.mainui.components.apitester.APITester;
import com.ing.ide.main.mainui.components.apitester.APITesterUI;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;
import org.checkerframework.checker.guieffect.qual.UI;

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
    // Path used for context actions (right-click) - does not change selection
    private TreePath contextMenuPath;
    private TreePath contextOutlinePath;

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
        tree =
            new JTree(treeModel) {

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintContextOutline(g);
                }

                @Override
                protected void processKeyEvent(KeyEvent e) {
                    if (shouldSuppressTreeTypeAhead(e)) {
                        e.consume();
                        return;
                    }

                    super.processKeyEvent(e);
                }
            };
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new CollectionTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.setRowHeight(24); // Increased row height for better readability
        tree.setFocusable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        disableTreeArrowKeys();

        setTreeMouseListener();

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Registers mouse handling for the collection tree.
     *
     * Left-click delegates row selection and request opening to handleTreeLeftMousePress.
     * Right-click shows the context menu on both press and release for cross-platform support.
     * This method only affects UI mouse behavior.
     */
    private void setTreeMouseListener() {
        tree.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                        handleTreeLeftMousePress(e);
                        return;
                    }

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
            }
        );
    }

    /**
     * Handles left-click behavior for the collection tree.
     *
     * Selects the clicked full-row path, clears any right-click context state,
     * and opens the request when the clicked row represents a request node.
     * If the click is outside a valid row, the current tree selection is cleared.
     *
     * @param e the mouse event triggered by the left-click press
     */
    private void handleTreeLeftMousePress(MouseEvent e) {
        TreePath path = getPathForFullRow(e.getX(), e.getY());

        contextMenuPath = null;
        contextOutlinePath = null;

        if (path == null) {
            tree.clearSelection();
            tree.repaint();
            return;
        }

        tree.setSelectionPath(path);
        tree.setLeadSelectionPath(path);
        tree.requestFocusInWindow();
        tree.repaint();

        if (isRequestPath(path)) {
            SwingUtilities.invokeLater(
                () -> {
                    openRequestAtPath(path);

                    TreePath restoredPath = findPathForRequestFromPath(path);

                    if (restoredPath != null) {
                        tree.setSelectionPath(restoredPath);
                        tree.setLeadSelectionPath(restoredPath);
                        tree.scrollPathToVisible(restoredPath);
                    }
                }
            );
        }
    }

    /**
     * Disables arrow-key navigation and expand/collapse behavior for the collection tree.
     *
     * Maps arrow-key shortcuts to a no-op action so Up, Down, Left, and Right do not
     * change selection or expand/collapse tree nodes. Mouse interaction remains unchanged.
     */
    private void disableTreeArrowKeys() {
        InputMap focusedInputMap = tree.getInputMap(JComponent.WHEN_FOCUSED);
        InputMap ancestorInputMap = tree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = tree.getActionMap();

        String disabledActionKey = "disableArrowKeys";

        Action doNothing = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Intentionally do nothing.
            }
        };

        actionMap.put(disabledActionKey, doNothing);

        int[] arrowKeys = { KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT };

        int[] modifiers = {
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK,
            InputEvent.META_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK
        };

        for (int key : arrowKeys) {
            for (int modifier : modifiers) {
                KeyStroke keyStroke = KeyStroke.getKeyStroke(key, modifier);

                focusedInputMap.put(keyStroke, disabledActionKey);
                ancestorInputMap.put(keyStroke, disabledActionKey);
            }
        }
    }

    /**
     * Determines whether a tree key event should be suppressed to disable type-ahead selection.
     *
     * Returns true only for printable typed characters, such as letters, numbers,
     * punctuation, symbols, and spaces. Control keys are allowed through so normal
     * non-printable keyboard behavior such as Tab, Escape, Enter, and Backspace is not blocked.
     *
     * @param e the key event to inspect
     * @return true if the key event should be consumed, otherwise false
     */
    private boolean shouldSuppressTreeTypeAhead(KeyEvent e) {
        if (e.getID() != KeyEvent.KEY_TYPED) {
            return false;
        }

        char keyChar = e.getKeyChar();

        // Let control characters pass through.
        // This avoids blocking Tab, Escape, Enter, Backspace, etc.
        if (keyChar == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(keyChar)) {
            return false;
        }

        // Suppress printable typing:
        // letters, numbers, punctuation, symbols, spaces.
        return true;
    }

    /**
     * Paints the persistent right-click context outline across the full tree row.
     *
     * Uses contextOutlinePath to find the target row and draws a rounded outline
     * spanning the visible width of the tree. This is visual-only and does not
     * affect tree selection or context-menu action state.
     *
     * @param g the graphics context used to paint the tree
     */
    private void paintContextOutline(Graphics g) {
        if (tree == null || contextOutlinePath == null) {
            return;
        }

        int row = tree.getRowForPath(contextOutlinePath);

        if (row < 0) {
            return;
        }

        Rectangle rowBounds = tree.getRowBounds(row);

        if (rowBounds == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color outlineColor = UIManager.getColor("Tree.selectionBorderColor");

            if (outlineColor == null) {
                outlineColor = Color.GRAY;
            }

            g2.setColor(outlineColor);
            g2.setStroke(new BasicStroke(1.25f));

            int x = 1;
            int y = rowBounds.y + 1;
            int width = tree.getWidth() - 3;
            int height = rowBounds.height - 3;

            g2.drawRoundRect(x, y, width, height, 6, 6);
        } finally {
            g2.dispose();
        }
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

        JMenuItem addSubFolder = new JMenuItem("Add Folder");
        addSubFolder.addActionListener(e -> addFolderToFolder());
        folderMenu.add(addSubFolder);

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

        // Clear context outline when popup closes
        attachContextMenuClearer(collectionMenu);
        attachContextMenuClearer(folderMenu);
        attachContextMenuClearer(requestMenu);
    }

    /**
     * Attaches cleanup behavior to a context menu when it closes or is canceled.
     *
     * Clears the context action path after menu actions can run, but keeps the
     * visual outline stable so the right-clicked row remains visibly marked until
     * the user clicks another row or right-clicks elsewhere.
     *
     * @param menu the popup menu to attach the cleanup listener to
     */
    private void attachContextMenuClearer(JPopupMenu menu) {
        if (menu == null) return;

        menu.addPopupMenuListener(
            new javax.swing.event.PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    // Clear action path after menu actions have had a chance to run.
                    // Keep visual outline stable until the user left-clicks or right-clicks elsewhere.
                    SwingUtilities.invokeLater(
                        () -> {
                            contextMenuPath = null;
                            if (tree != null) tree.repaint();
                        }
                    );
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                    contextMenuPath = null;

                    // Do NOT clear contextOutlinePath here.
                    // This keeps the right-click outline stable even if the popup is dismissed.
                    if (tree != null) tree.repaint();
                }
            }
        );
    }

    /**
     * Shows the appropriate context menu for the row under the mouse event.
     *
     * Uses full-row hit detection to resolve the clicked path, stores that path for
     * context-menu actions and visual outlining, then displays the menu matching
     * the node type. If no valid row is clicked, context state is cleared.
     *
     * @param e the mouse event that triggered the context menu
     */
    private void showContextMenu(MouseEvent e) {
        TreePath path = getPathForFullRow(e.getX(), e.getY());

        if (path == null) {
            contextMenuPath = null;
            contextOutlinePath = null;
            tree.repaint();
            return;
        }

        contextMenuPath = path;
        contextOutlinePath = path;

        tree.repaint();

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

    /**
     * Returns the node that actions should operate on: the context node (right-clicked)
     * if present, otherwise the currently selected node.
     */
    private DefaultMutableTreeNode getActionNode() {
        if (contextMenuPath != null) {
            return (DefaultMutableTreeNode) contextMenuPath.getLastPathComponent();
        }
        return getSelectedNode();
    }

    /**
     * Returns the tree path for the row at the given mouse coordinates.
     *
     * Uses the y-coordinate to resolve the row and intentionally ignores the
     * x-coordinate so the full horizontal row acts as the hitbox. Returns null
     * when the coordinates are outside a valid tree row.
     *
     * @param x the mouse x-coordinate, ignored for full-row hit detection
     * @param y the mouse y-coordinate used to locate the row
     * @return the tree path for the row at the given y-coordinate, or null if none exists
     */
    private TreePath getPathForFullRow(int x, int y) {
        if (tree == null) {
            return null;
        }

        if (y < 0 || y >= tree.getHeight()) {
            return null;
        }

        int row = tree.getClosestRowForLocation(0, y);

        if (row < 0) {
            return null;
        }

        Rectangle rowBounds = tree.getRowBounds(row);

        if (rowBounds == null) {
            return null;
        }

        if (y < rowBounds.y || y >= rowBounds.y + rowBounds.height) {
            return null;
        }

        return tree.getPathForRow(row);
    }

    /**
     * Checks whether the given tree path points to a request node.
     *
     * @param path the tree path to inspect
     * @return true if the path points to a RequestNode, otherwise false
     */
    private boolean isRequestPath(TreePath path) {
        if (path == null) return false;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        return node.getUserObject() instanceof RequestNode;
    }

    /**
     * Opens the request represented by the given tree path.
     *
     * Loads the request with folder context when the request belongs to a folder,
     * otherwise loads it directly under its parent collection. Non-request paths
     * are ignored.
     *
     * @param path the tree path expected to point to a request node
     */
    private void openRequestAtPath(TreePath path) {
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (!(userObject instanceof RequestNode)) {
            return;
        }

        RequestNode reqNode = (RequestNode) userObject;

        if (reqNode.parentFolder != null) {
            parentUI.loadRequest(reqNode.request, reqNode.parentCollection, reqNode.parentFolder);
        } else {
            parentUI.loadRequest(reqNode.request, reqNode.parentCollection);
        }
    }

    /**
     * Finds the current tree path for the request represented by the original path.
     *
     * If the original path does not point to a request node, the original path is
     * returned unchanged. This is used to restore selection after UI updates that
     * may repaint or reload parts of the tree.
     *
     * @param originalPath the original tree path before the request was opened
     * @return the current tree path for the same request, the original path for
     *         non-request nodes, or null if no matching request path is found
     */
    private TreePath findPathForRequestFromPath(TreePath originalPath) {
        if (originalPath == null) return null;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) originalPath.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (!(userObject instanceof RequestNode)) {
            return originalPath;
        }

        RequestNode originalRequestNode = (RequestNode) userObject;
        return findPathForRequest(originalRequestNode.request);
    }

    /**
     * Searches the tree for the path belonging to the given request.
     *
     * Traverses all nodes under the root and returns the first RequestNode whose
     * request matches the target request by ID or object reference.
     *
     * @param targetRequest the request to locate in the tree
     * @return the tree path for the matching request, or null if no match is found
     */
    private TreePath findPathForRequest(APIRequest targetRequest) {
        if (targetRequest == null || rootNode == null) return null;

        java.util.Enumeration<?> nodes = rootNode.depthFirstEnumeration();

        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            Object userObject = node.getUserObject();

            if (userObject instanceof RequestNode) {
                RequestNode requestNode = (RequestNode) userObject;

                if (sameRequest(requestNode.request, targetRequest)) {
                    return new TreePath(node.getPath());
                }
            }
        }

        return null;
    }

    /**
     * Checks whether two request objects represent the same request.
     *
     * Requests are matched by ID when both IDs are available. If either ID is
     * missing, the comparison falls back to object reference equality.
     *
     * @param a the first request to compare
     * @param b the second request to compare
     * @return true if both requests have the same ID or are the same object instance
     */
    private boolean sameRequest(APIRequest a, APIRequest b) {
        if (a == null || b == null) return false;

        String aId = a.getId();
        String bId = b.getId();

        if (aId != null && bId != null) {
            return aId.equals(bId);
        }

        return a == b;
    }

    // ═══════════════════════════════════════════════════════════════════
    // Collection Operations
    // ═══════════════════════════════════════════════════════════════════

    private void createNewCollection() {
        String name = JOptionPane.showInputDialog(
            this,
            "Enter collection name:",
            "New Collection",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            APICollection collection = new APICollection(name.trim());
            controller.addCollection(collection);
            refreshTree();
        }
    }

    private void addRequestToCollection() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        CollectionNode colNode = (CollectionNode) node.getUserObject();

        String name = JOptionPane.showInputDialog(
            this,
            "Enter request name:",
            "New Request",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            APIRequest request = new APIRequest(name.trim());
            request.setUrl("https://api.example.com");
            colNode.collection.addRequest(request);
            controller.saveCollection(colNode.collection);
            refreshTree();

            // Expand the collection node to show the newly added request
            TreePath collectionPath = new TreePath(node.getPath());
            tree.expandPath(collectionPath);

            // Auto-load the new request into the editor
            parentUI.loadRequest(request, colNode.collection);
        }
    }

    private void addFolderToCollection() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        CollectionNode colNode = (CollectionNode) node.getUserObject();

        String name = JOptionPane.showInputDialog(
            this,
            "Enter folder name:",
            "New Folder",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            APICollection folder = new APICollection(name.trim());
            colNode.collection.addFolder(folder);
            controller.saveCollection(colNode.collection);
            refreshTree();

            // Expand the collection node to show the newly added folder
            TreePath collectionPath = new TreePath(node.getPath());
            tree.expandPath(collectionPath);
        }
    }

    private void addRequestToFolder() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        FolderNode folderNode = (FolderNode) node.getUserObject();

        String name = JOptionPane.showInputDialog(
            this,
            "Enter request name:",
            "New Request",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            APIRequest request = new APIRequest(name.trim());
            request.setId(java.util.UUID.randomUUID().toString());
            request.setUrl("https://api.example.com");
            folderNode.folder.addRequest(request);
            controller.saveCollection(folderNode.parentCollection);
            refreshTree();

            // Expand the folder node to show the newly added request
            TreePath folderPath = new TreePath(node.getPath());
            tree.expandPath(folderPath);

            // Auto-load the new request into the editor
            parentUI.loadRequest(request, folderNode.parentCollection, folderNode.folder);
        }
    }

    private void addFolderToFolder() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        FolderNode folderNode = (FolderNode) node.getUserObject();

        String name = JOptionPane.showInputDialog(
            this,
            "Enter folder name:",
            "New Folder",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            APICollection subFolder = new APICollection(name.trim());
            folderNode.folder.addFolder(subFolder);

            controller.saveCollection(folderNode.parentCollection);
            refreshTree();

            TreePath folderPath = new TreePath(node.getPath());
            tree.expandPath(folderPath);
        }
    }

    private void openSelectedRequest() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        RequestNode reqNode = (RequestNode) node.getUserObject();

        // If request is in a folder, pass the folder info
        if (reqNode.parentFolder != null) {
            parentUI.loadRequest(reqNode.request, reqNode.parentCollection, reqNode.parentFolder);
        } else {
            parentUI.loadRequest(reqNode.request, reqNode.parentCollection);
        }
    }

    private void duplicateRequest() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        RequestNode reqNode = (RequestNode) node.getUserObject();

        String originalName = reqNode.request.getName();

        APIRequest copy = reqNode.request.copy();

        copy.setName(originalName + " (Copy)");

        copy.setId(java.util.UUID.randomUUID().toString());

        if (reqNode.parentFolder != null) {
            reqNode.parentFolder.addRequest(copy);
        } else {
            reqNode.parentCollection.addRequest(copy);
        }

        controller.saveCollection(reqNode.parentCollection);
        refreshTree();
    }

    private void moveRequest() {
        DefaultMutableTreeNode node = getActionNode();
        if (node == null) return;

        RequestNode reqNode = (RequestNode) node.getUserObject();

        List<MoveDestination> destinations = buildMoveDestinations(reqNode);

        if (destinations.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No move destinations available.");
            return;
        }

        MoveDestination selected = (MoveDestination) JOptionPane.showInputDialog(
            this,
            "Move request to:",
            "Move Request",
            JOptionPane.PLAIN_MESSAGE,
            null,
            destinations.toArray(),
            destinations.get(0)
        );

        if (selected == null) {
            return;
        }

        boolean sameCollection = selected.collection == reqNode.parentCollection;
        boolean sameFolder = selected.folder == reqNode.parentFolder;

        if (sameCollection && sameFolder) {
            return;
        }

        boolean removed = removeRequestFromCollection(reqNode.parentCollection, reqNode.request);

        if (!removed) {
            JOptionPane.showMessageDialog(
                this,
                "Unable to move request. The request could not be removed from its current location.",
                "Move Request",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (selected.folder != null) {
            selected.folder.addRequest(reqNode.request);
        } else {
            selected.collection.addRequest(reqNode.request);
        }

        controller.saveCollection(reqNode.parentCollection);

        if (selected.collection != reqNode.parentCollection) {
            controller.saveCollection(selected.collection);
        }

        refreshTree();

        if (selected.folder != null) {
            parentUI.loadRequest(reqNode.request, selected.collection, selected.folder);
        } else {
            parentUI.loadRequest(reqNode.request, selected.collection);
        }
    }

    private List<MoveDestination> buildMoveDestinations(RequestNode requestNode) {
        List<MoveDestination> destinations = new java.util.ArrayList<>();

        for (APICollection collection : controller.getCollections()) {
            if (collection == null) {
                continue;
            }

            String collectionName = collection.getName();

            // Collection root destination
            if (!(collection == requestNode.parentCollection && requestNode.parentFolder == null)) {
                destinations.add(new MoveDestination(collection, null, collectionName));
            }

            // Folder destinations, including nested folders
            addFolderMoveDestinations(
                destinations,
                collection,
                collection.getFolders(),
                collectionName,
                requestNode
            );
        }

        return destinations;
    }

    private void addFolderMoveDestinations(
        List<MoveDestination> destinations,
        APICollection parentCollection,
        List<APICollection> folders,
        String parentPath,
        RequestNode requestNode
    ) {
        if (folders == null) {
            return;
        }

        for (APICollection folder : folders) {
            if (folder == null) {
                continue;
            }

            String folderPath = parentPath + "/" + folder.getName();

            boolean isCurrentLocation =
                parentCollection == requestNode.parentCollection &&
                folder == requestNode.parentFolder;

            if (!isCurrentLocation) {
                destinations.add(new MoveDestination(parentCollection, folder, folderPath));
            }

            addFolderMoveDestinations(
                destinations,
                parentCollection,
                folder.getFolders(),
                folderPath,
                requestNode
            );
        }
    }

    private void renameSelected() {
        DefaultMutableTreeNode node = getActionNode();
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

        String newName = JOptionPane.showInputDialog(this, "Enter new name:", currentName);

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
        DefaultMutableTreeNode node = getActionNode();
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

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete " + type + " '" + name + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (userObject instanceof CollectionNode) {
                CollectionNode cn = (CollectionNode) userObject;
                controller.deleteCollection(cn.collection);
            } else if (userObject instanceof FolderNode) {
                FolderNode fn = (FolderNode) userObject;

                boolean removed = removeFolderFromCollection(fn.parentCollection, fn.folder);

                if (removed) {
                    controller.saveCollection(fn.parentCollection);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Unable to delete folder. The folder could not be found.",
                        "Delete Folder",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } else if (userObject instanceof RequestNode) {
                RequestNode rn = (RequestNode) userObject;

                boolean removed = removeRequestFromCollection(rn.parentCollection, rn.request);

                if (removed) {
                    controller.saveCollection(rn.parentCollection);
                    parentUI.notifyRequestDeleted(rn.request);
                }
            }
            refreshTree();
        }
    }

    private boolean removeFolderFromCollection(
        APICollection collection,
        APICollection folderToDelete
    ) {
        if (collection == null || folderToDelete == null) {
            return false;
        }

        return removeFolderFromFolders(collection.getFolders(), folderToDelete);
    }

    private boolean removeFolderFromFolders(
        java.util.List<APICollection> folders,
        APICollection folderToDelete
    ) {
        if (folders == null || folderToDelete == null) {
            return false;
        }

        // First, try removing from the current folder list.
        boolean removed = folders.removeIf(folder -> folder == folderToDelete);

        if (removed) {
            return true;
        }

        // If not found at this level, search nested folders.
        for (APICollection folder : folders) {
            if (folder == null) {
                continue;
            }

            removed = removeFolderFromFolders(folder.getFolders(), folderToDelete);

            if (removed) {
                return true;
            }
        }

        return false;
    }

    private boolean removeRequestFromCollection(
        APICollection collection,
        APIRequest requestToDelete
    ) {
        if (collection == null || requestToDelete == null) {
            return false;
        }

        boolean removed = false;

        removed |= removeRequestFromList(collection.getRequests(), requestToDelete);
        removed |= removeRequestFromFolders(collection.getFolders(), requestToDelete);

        return removed;
    }

    private boolean removeRequestFromFolders(
        java.util.List<APICollection> folders,
        APIRequest requestToDelete
    ) {
        if (folders == null || requestToDelete == null) {
            return false;
        }

        boolean removed = false;

        for (APICollection folder : folders) {
            if (folder == null) {
                continue;
            }

            removed |= removeRequestFromList(folder.getRequests(), requestToDelete);
            removed |= removeRequestFromFolders(folder.getFolders(), requestToDelete);
        }

        return removed;
    }

    private boolean removeRequestFromList(
        java.util.List<APIRequest> requests,
        APIRequest requestToDelete
    ) {
        if (requests == null || requestToDelete == null) {
            return false;
        }

        String targetId = requestToDelete.getId();

        return requests.removeIf(
            request -> {
                if (request == null) {
                    return false;
                }

                String requestId = request.getId();

                if (targetId != null && requestId != null) {
                    return targetId.equals(requestId);
                }

                return request == requestToDelete;
            }
        );
    }

    private void exportCollection() {
        DefaultMutableTreeNode node = getActionNode();
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
                JOptionPane.showMessageDialog(
                    this,
                    "Export failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
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
                new CollectionNode(collection)
            );

            // Add folders recursively
            addFolderNodes(colNode, collection, collection.getFolders());

            // Add root-level requests
            for (APIRequest request : collection.getRequests()) {
                colNode.add(new DefaultMutableTreeNode(new RequestNode(request, collection)));
            }

            rootNode.add(colNode);
        }

        treeModel.reload();

        // Expand all collections
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void addFolderNodes(
        DefaultMutableTreeNode parentTreeNode,
        APICollection parentCollection,
        List<APICollection> folders
    ) {
        if (folders == null) {
            return;
        }

        for (APICollection folder : folders) {
            DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(
                new FolderNode(folder, parentCollection)
            );

            // Add nested folders first
            addFolderNodes(folderNode, parentCollection, folder.getFolders());

            // Add requests in this folder
            if (folder.getRequests() != null) {
                for (APIRequest request : folder.getRequests()) {
                    folderNode.add(
                        new DefaultMutableTreeNode(
                            new RequestNode(request, parentCollection, folder)
                        )
                    );
                }
            }

            parentTreeNode.add(folderNode);
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
        final APICollection parentFolder; // Null if request is directly in collection

        RequestNode(APIRequest request, APICollection parentCollection) {
            this.request = request;
            this.parentCollection = parentCollection;
            this.parentFolder = null;
        }

        RequestNode(
            APIRequest request,
            APICollection parentCollection,
            APICollection parentFolder
        ) {
            this.request = request;
            this.parentCollection = parentCollection;
            this.parentFolder = parentFolder;
        }

        @Override
        public String toString() {
            return request.getName();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tree Cell Renderer
    // ═══════════════════════════════════════════════════════════════════

    private class CollectionTreeCellRenderer extends DefaultTreeCellRenderer {
        private JLabel label;

        public CollectionTreeCellRenderer() {
            label = new JLabel();
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userObject = node.getUserObject();
            String tooltipText = null;

            if (userObject instanceof CollectionNode) {
                setIcon(UIManager.getIcon("FileView.directoryIcon"));
                String collName = ((CollectionNode) userObject).collection.getName();
                setText(collName);
                tooltipText = "Collection: " + collName;
            } else if (userObject instanceof FolderNode) {
                setIcon(UIManager.getIcon("FileView.directoryIcon"));
                String folderName = ((FolderNode) userObject).folder.getName();
                setText(folderName);
                tooltipText = "Folder: " + folderName;
            } else if (userObject instanceof RequestNode) {
                RequestNode reqNode = (RequestNode) userObject;
                APIRequest.HttpMethod method = reqNode.request.getMethod();

                // Create method badge
                String methodStr = method.name();
                String requestName = reqNode.request.getName();
                setIcon(null);
                setText(
                    "<html><span style='color:" +
                    getMethodColorHex(method) +
                    ";font-size:9px;font-weight:bold;'>" +
                    methodStr +
                    "</span> " +
                    requestName +
                    "</html>"
                );
                tooltipText = "Request: " + methodStr + " " + requestName;
            }

            // Set tooltip for long names
            ToolTipManager.sharedInstance().registerComponent(tree);
            setToolTipText(tooltipText);

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

    static class MoveDestination {
        final APICollection collection;
        final APICollection folder; // null means collection root
        final String displayPath;

        MoveDestination(APICollection collection, APICollection folder, String displayPath) {
            this.collection = collection;
            this.folder = folder;
            this.displayPath = displayPath;
        }

        @Override
        public String toString() {
            return displayPath;
        }
    }
}
