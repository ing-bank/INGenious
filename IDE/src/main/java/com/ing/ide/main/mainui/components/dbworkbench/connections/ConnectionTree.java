package com.ing.ide.main.mainui.components.dbworkbench.connections;

import com.ing.datalib.settings.DBProperties;
import com.ing.ide.main.mainui.components.dbworkbench.DBWorkbench;
import com.ing.ide.main.mainui.components.dbworkbench.util.DBWorkbenchColors;
import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Left-rail schema browser: a tree of connection aliases, optionally grouped,
 * where expanding a connection lazily loads its databases, schemas and tables.
 * Double-clicking a table generates a {@code SELECT} into the editor against
 * that connection. Reads and writes the project's {@link DBProperties} store.
 */
public class ConnectionTree extends JPanel {
    private final DBWorkbench controller;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Connections");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);

    private final JPanel body = new JPanel();
    private final JLabel titleLabel = new JLabel("Database Connections");
    private final JButton toggleBtn = new JButton("\u25BE"); // ▾ expanded / ▸ collapsed
    private boolean collapsed = false;

    private boolean sortAscending = true;
    private boolean groupByFolder = true;

    /** Marker node used to make a container appear expandable before it loads. */
    private static final String LOADING = "Loading…";
    private static final String DATABASES_FOLDER = "Databases";
    private static final String UNGROUPED = "";

    public ConnectionTree(DBWorkbench controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        // Discontiguous selection so several connections can be deleted at once.
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addTreeWillExpandListener(
            new javax.swing.event.TreeWillExpandListener() {

                @Override
                public void treeWillExpand(javax.swing.event.TreeExpansionEvent e) {
                    lazyLoad((DefaultMutableTreeNode) e.getPath().getLastPathComponent());
                }

                @Override
                public void treeWillCollapse(javax.swing.event.TreeExpansionEvent e) {}
            }
        );
        tree.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        onNodeDoubleClick();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShowContextMenu(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShowContextMenu(e);
                }
            }
        );
        add(header(), BorderLayout.NORTH);

        body.setLayout(new BorderLayout());
        body.add(new JScrollPane(tree), BorderLayout.CENTER);

        // GridLayout keeps all four buttons visible at the panel's default width.
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 4, 4, 4));
        buttons.setBorder(new EmptyBorder(6, 0, 0, 0));
        JButton addBtn = new JButton("New");
        JButton editBtn = new JButton("Edit");
        JButton delBtn = new JButton("Delete");
        JButton testBtn = new JButton("Test");

        // Make "New" the prominent primary action: solid theme-color fill, white text.
        addBtn.setBackground(DBWorkbenchColors.ACCENT);
        addBtn.setForeground(java.awt.Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setFont(addBtn.getFont().deriveFont(java.awt.Font.BOLD));
        addBtn.setToolTipText("Add a new database connection");
        delBtn.setToolTipText("Delete the selected connection(s)");

        addBtn.addActionListener(e -> onAdd());
        editBtn.addActionListener(e -> onEdit());
        delBtn.addActionListener(e -> onDelete());
        testBtn.addActionListener(e -> onTest());
        buttons.add(addBtn);
        buttons.add(editBtn);
        buttons.add(delBtn);
        buttons.add(testBtn);
        body.add(buttons, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        refresh();
    }

    /** Title bar with a chevron toggle that collapses/expands the panel on demand. */
    private JPanel header() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBorder(new EmptyBorder(0, 0, 6, 0));

        toggleBtn.setBorderPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setFocusPainted(false);
        toggleBtn.setMargin(new java.awt.Insets(0, 2, 0, 2));
        toggleBtn.setToolTipText("Collapse / expand the Connections panel");
        toggleBtn.addActionListener(e -> toggleCollapsed());

        titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD));

        JButton viewBtn = new JButton("\u2261"); // ≡
        viewBtn.setBorderPainted(false);
        viewBtn.setContentAreaFilled(false);
        viewBtn.setFocusPainted(false);
        viewBtn.setToolTipText("Sorting and grouping");
        viewBtn.addActionListener(e -> viewMenu().show(viewBtn, 0, viewBtn.getHeight()));

        h.add(toggleBtn, BorderLayout.WEST);
        h.add(titleLabel, BorderLayout.CENTER);
        h.add(viewBtn, BorderLayout.EAST);
        return h;
    }

    private JPopupMenu viewMenu() {
        JPopupMenu menu = new JPopupMenu();
        JCheckBoxMenuItem asc = new JCheckBoxMenuItem("Sort A → Z", sortAscending);
        asc.addActionListener(
            e -> {
                sortAscending = true;
                refresh();
            }
        );
        JCheckBoxMenuItem desc = new JCheckBoxMenuItem("Sort Z → A", !sortAscending);
        desc.addActionListener(
            e -> {
                sortAscending = false;
                refresh();
            }
        );
        JCheckBoxMenuItem group = new JCheckBoxMenuItem("Group by group name", groupByFolder);
        group.addActionListener(
            e -> {
                groupByFolder = !groupByFolder;
                refresh();
            }
        );
        menu.add(asc);
        menu.add(desc);
        menu.addSeparator();
        menu.add(group);
        return menu;
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        body.setVisible(!collapsed);
        titleLabel.setVisible(!collapsed);
        toggleBtn.setText(collapsed ? "\u25B8" : "\u25BE"); // ▸ collapsed / ▾ expanded
        toggleBtn.setToolTipText(collapsed ? "Show Connections" : "Hide Connections");
        controller.getDBWorkbenchUI().setConnectionsCollapsed(collapsed);
        revalidate();
        repaint();
    }

    public final void refresh() {
        String previouslySelected = getSelectedAlias();
        root.removeAllChildren();
        DBProperties dbp = controller.getDatabaseSettings();
        if (dbp != null) {
            for (Map.Entry<String, List<String>> group : groupedAliases(dbp).entrySet()) {
                DefaultMutableTreeNode parent = group.getKey().isEmpty()
                    ? root
                    : new DefaultMutableTreeNode(new GroupNode(group.getKey()));
                if (parent != root) {
                    root.add(parent);
                }
                for (String alias : group.getValue()) {
                    DefaultMutableTreeNode conn = new DefaultMutableTreeNode(new ConnNode(alias));
                    conn.add(new DefaultMutableTreeNode(LOADING));
                    parent.add(conn);
                }
            }
        }
        treeModel.reload();
        // A full reload clears the selection; restore it (or default to the first
        // connection) so Edit/Delete/Test remain usable without an extra click.
        reselect(previouslySelected);
    }

    /** @return group name to sorted aliases; the empty key holds ungrouped connections */
    private Map<String, List<String>> groupedAliases(DBProperties dbp) {
        List<String> aliases = new ArrayList<>(dbp.getDbList());
        Collections.sort(aliases, String.CASE_INSENSITIVE_ORDER);
        if (!sortAscending) {
            Collections.reverse(aliases);
        }
        if (!groupByFolder) {
            Map<String, List<String>> single = new LinkedHashMap<>();
            single.put(UNGROUPED, aliases);
            return single;
        }
        Map<String, List<String>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String alias : aliases) {
            Properties p = dbp.getDBPropertiesFor(alias);
            String group = p == null ? "" : p.getProperty(ConnectionDialog.GROUP, "").trim();
            grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(alias);
        }
        // Ungrouped connections stay at the top level, above the group folders.
        Map<String, List<String>> ordered = new LinkedHashMap<>();
        if (grouped.containsKey(UNGROUPED)) {
            ordered.put(UNGROUPED, grouped.remove(UNGROUPED));
        }
        ordered.putAll(grouped);
        return ordered;
    }

    private void reselect(String alias) {
        DefaultMutableTreeNode target = alias == null ? null : findConnNode(alias);
        if (target == null) {
            target = firstConnNode(root);
        }
        if (target != null) {
            tree.setSelectionPath(new TreePath(target.getPath()));
        }
    }

    private DefaultMutableTreeNode findConnNode(String alias) {
        java.util.Enumeration<?> nodes = root.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            Object uo = node.getUserObject();
            if (uo instanceof ConnNode && ((ConnNode) uo).alias.equals(alias)) {
                return node;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode firstConnNode(DefaultMutableTreeNode from) {
        java.util.Enumeration<?> nodes = from.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof ConnNode) {
                return node;
            }
        }
        return null;
    }

    private void showNoSelectionWarning() {
        JOptionPane.showMessageDialog(
            this,
            "Select a connection first.",
            "No Connection Selected",
            JOptionPane.WARNING_MESSAGE
        );
    }

    /** Alias of the selected node (a connection, or an ancestor of a table node). */
    public String getSelectedAlias() {
        return aliasOf(selectedNode());
    }

    /** @return the aliases of every selected connection, table or schema node */
    private List<String> getSelectedAliases() {
        List<String> aliases = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return aliases;
        for (TreePath path : paths) {
            String alias = aliasOf((DefaultMutableTreeNode) path.getLastPathComponent());
            if (alias != null && !aliases.contains(alias)) {
                aliases.add(alias);
            }
        }
        return aliases;
    }

    /** Walks up from any node to the connection it belongs to. */
    private String aliasOf(DefaultMutableTreeNode node) {
        while (node != null) {
            Object uo = node.getUserObject();
            if (uo instanceof ConnNode) return ((ConnNode) uo).alias;
            if (uo instanceof TableNode) return ((TableNode) uo).alias;
            if (uo instanceof SchemaNode) return ((SchemaNode) uo).alias;
            if (uo instanceof CatalogNode) return ((CatalogNode) uo).alias;
            node = (DefaultMutableTreeNode) node.getParent();
        }
        return null;
    }

    private DefaultMutableTreeNode selectedNode() {
        TreePath path = tree.getSelectionPath();
        return path == null ? null : (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Lazy loading
    // ═══════════════════════════════════════════════════════════════════

    private void lazyLoad(DefaultMutableTreeNode node) {
        if (!isPlaceholderOnly(node)) return;
        Object uo = node.getUserObject();
        if (uo instanceof ConnNode) {
            loadConnectionChildren(node, ((ConnNode) uo).alias);
        } else if (uo instanceof SchemaNode) {
            SchemaNode sn = (SchemaNode) uo;
            loadTables(node, sn.alias, sn.schema);
        } else if (uo instanceof CatalogFolderNode) {
            loadCatalogs(node, ((CatalogFolderNode) uo).alias);
        }
    }

    private boolean isPlaceholderOnly(DefaultMutableTreeNode node) {
        if (node.getChildCount() != 1) return false;
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) node.getChildAt(0);
        return LOADING.equals(first.getUserObject());
    }

    /** Loads the databases folder plus either the schemas or the tables directly. */
    private void loadConnectionChildren(DefaultMutableTreeNode connNode, String alias) {
        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) return;

        new SwingWorker<List<String>, Void>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(alias, props);
                return exec.listSchemas(c);
            }

            @Override
            protected void done() {
                connNode.removeAllChildren();
                try {
                    DefaultMutableTreeNode catalogs = new DefaultMutableTreeNode(
                        new CatalogFolderNode(alias)
                    );
                    catalogs.add(new DefaultMutableTreeNode(LOADING));
                    connNode.add(catalogs);

                    List<String> schemas = get();
                    if (schemas.isEmpty()) {
                        // No schema concept (e.g. MySQL/SQLite): list tables directly.
                        loadTables(connNode, alias, null);
                    } else {
                        for (String schema : schemas) {
                            DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(
                                new SchemaNode(alias, schema)
                            );
                            schemaNode.add(new DefaultMutableTreeNode(LOADING));
                            connNode.add(schemaNode);
                        }
                    }
                } catch (Exception ex) {
                    connNode.add(new DefaultMutableTreeNode("Error: " + rootMessage(ex)));
                }
                treeModel.reload(connNode);
                tree.expandPath(new TreePath(connNode.getPath()));
            }
        }
        .execute();
    }

    private void loadCatalogs(DefaultMutableTreeNode folderNode, String alias) {
        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) return;

        new SwingWorker<List<String>, Void>() {

            @Override
            protected List<String> doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(alias, props);
                return exec.listCatalogs(c);
            }

            @Override
            protected void done() {
                folderNode.removeAllChildren();
                try {
                    List<String> catalogs = get();
                    for (String catalog : catalogs) {
                        folderNode.add(new DefaultMutableTreeNode(new CatalogNode(alias, catalog)));
                    }
                    if (catalogs.isEmpty()) {
                        folderNode.add(new DefaultMutableTreeNode("(no databases)"));
                    }
                } catch (Exception ex) {
                    folderNode.add(new DefaultMutableTreeNode("Error: " + rootMessage(ex)));
                }
                treeModel.reload(folderNode);
                tree.expandPath(new TreePath(folderNode.getPath()));
            }
        }
        .execute();
    }

    private void loadTables(DefaultMutableTreeNode parentNode, String alias, String schema) {
        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) return;

        new SwingWorker<List<JdbcExecutor.TableInfo>, Void>() {

            @Override
            protected List<JdbcExecutor.TableInfo> doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(alias, props);
                return exec.listTables(c, schema);
            }

            @Override
            protected void done() {
                if (schema != null) {
                    parentNode.removeAllChildren();
                }
                try {
                    List<JdbcExecutor.TableInfo> tables = get();
                    List<String> completions = new ArrayList<>();
                    for (JdbcExecutor.TableInfo t : tables) {
                        parentNode.add(new DefaultMutableTreeNode(new TableNode(alias, t)));
                        completions.add(t.name);
                    }
                    if (tables.isEmpty()) {
                        parentNode.add(new DefaultMutableTreeNode("(no tables)"));
                    }
                    controller
                        .getDBWorkbenchUI()
                        .getQueryEditor()
                        .addSchemaCompletions(completions);
                } catch (Exception ex) {
                    parentNode.add(new DefaultMutableTreeNode("Error: " + rootMessage(ex)));
                }
                treeModel.reload(parentNode);
                tree.expandPath(new TreePath(parentNode.getPath()));
            }
        }
        .execute();
    }

    private static String rootMessage(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Interactions
    // ═══════════════════════════════════════════════════════════════════

    private void onNodeDoubleClick() {
        DefaultMutableTreeNode node = selectedNode();
        if (node == null) return;
        Object uo = node.getUserObject();
        if (uo instanceof TableNode) {
            selectTable((TableNode) uo);
        } else if (uo instanceof CatalogNode) {
            switchTo((CatalogNode) uo);
        }
    }

    /** Puts a SELECT for the table in the editor and points the editor at its connection. */
    private void selectTable(TableNode tn) {
        controller.getDBWorkbenchUI().getQueryEditor().setSelectedAlias(tn.alias);
        controller
            .getDBWorkbenchUI()
            .getQueryEditor()
            .setSql("SELECT * FROM " + tn.info.qualified());
    }

    /** Switches the live connection to another database and reloads its subtree. */
    private void switchTo(CatalogNode cn) {
        final Properties props = controller.resolveConnectionProps(cn.alias);
        if (props == null) return;
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(cn.alias, props);
                exec.switchNamespace(c, cn.catalog, null);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    controller.getDBWorkbenchUI().getQueryEditor().setSelectedAlias(cn.alias);
                    controller
                        .getDBWorkbenchUI()
                        .getResultPanel()
                        .showMessage(
                            "Switched '" + cn.alias + "' to database '" + cn.catalog + "'."
                        );
                    reloadConnection(cn.alias);
                } catch (Exception ex) {
                    controller
                        .getDBWorkbenchUI()
                        .getResultPanel()
                        .showError("Could not switch database: " + rootMessage(ex));
                }
            }
        }
        .execute();
    }

    /** Drops the cached children of a connection so the next expand reloads them. */
    private void reloadConnection(String alias) {
        DefaultMutableTreeNode connNode = findConnNode(alias);
        if (connNode == null) return;
        connNode.removeAllChildren();
        connNode.add(new DefaultMutableTreeNode(LOADING));
        treeModel.reload(connNode);
    }

    private void maybeShowContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path != null && !tree.isPathSelected(path)) {
            tree.setSelectionPath(path);
        }
        JPopupMenu menu = buildContextMenu();
        if (menu.getComponentCount() > 0) {
            menu.show(tree, e.getX(), e.getY());
        }
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        DefaultMutableTreeNode node = selectedNode();
        Object uo = node == null ? null : node.getUserObject();

        if (uo instanceof TableNode) {
            TableNode tn = (TableNode) uo;
            addItem(menu, "Select all rows", () -> selectTable(tn));
            addItem(
                menu,
                "Count rows",
                () -> {
                    controller.getDBWorkbenchUI().getQueryEditor().setSelectedAlias(tn.alias);
                    controller
                        .getDBWorkbenchUI()
                        .getQueryEditor()
                        .setSql("SELECT COUNT(*) FROM " + tn.info.qualified());
                    controller.getDBWorkbenchUI().runCurrentQuery();
                }
            );
            addItem(
                menu,
                "Show columns",
                () -> {
                    controller.getDBWorkbenchUI().getQueryEditor().setSelectedAlias(tn.alias);
                    controller
                        .getDBWorkbenchUI()
                        .getQueryEditor()
                        .setSql("SELECT * FROM " + tn.info.qualified() + " WHERE 1 = 0");
                    controller.getDBWorkbenchUI().runCurrentQuery();
                }
            );
            addItem(
                menu,
                "Copy qualified name",
                () ->
                    java
                        .awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(
                            new java.awt.datatransfer.StringSelection(tn.info.qualified()),
                            null
                        )
            );
            menu.addSeparator();
        } else if (uo instanceof SchemaNode) {
            SchemaNode sn = (SchemaNode) uo;
            addItem(menu, "Use this schema", () -> useSchema(sn));
            addItem(menu, "Refresh", () -> reloadConnection(sn.alias));
            menu.addSeparator();
        } else if (uo instanceof CatalogNode) {
            addItem(menu, "Use this database", () -> switchTo((CatalogNode) uo));
            menu.addSeparator();
        }

        if (getSelectedAlias() != null) {
            addItem(menu, "New Connection…", this::onAdd);
            addItem(menu, "Edit Connection…", this::onEdit);
            addItem(menu, "Rename Connection…", this::onRename);
            addItem(menu, "Duplicate Connection", this::onDuplicate);
            addItem(menu, "Test Connection", this::onTest);
            addItem(menu, "Refresh", () -> reloadConnection(getSelectedAlias()));
            menu.addSeparator();
            int count = getSelectedAliases().size();
            addItem(
                menu,
                count > 1 ? "Delete " + count + " Connections" : "Delete Connection",
                this::onDelete
            );
        } else {
            addItem(menu, "New Connection…", this::onAdd);
        }
        return menu;
    }

    private void addItem(JPopupMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    private void useSchema(SchemaNode sn) {
        final Properties props = controller.resolveConnectionProps(sn.alias);
        if (props == null) return;
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(sn.alias, props);
                exec.switchNamespace(c, null, sn.schema);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    controller
                        .getDBWorkbenchUI()
                        .getResultPanel()
                        .showMessage("Switched '" + sn.alias + "' to schema '" + sn.schema + "'.");
                } catch (Exception ex) {
                    controller
                        .getDBWorkbenchUI()
                        .getResultPanel()
                        .showError("Could not switch schema: " + rootMessage(ex));
                }
            }
        }
        .execute();
    }

    private void onAdd() {
        DBProperties dbp = controller.getDatabaseSettings();
        if (dbp == null) {
            JOptionPane.showMessageDialog(
                this,
                "Open a project first.",
                "No Project",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        ConnectionDialog dialog = new ConnectionDialog(
            controller.getMainFrame(),
            controller.getExecutor(),
            null,
            null,
            dbp.getDbList()
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            // A previously deleted alias may still have a cached connection.
            controller.getExecutor().closeConnection(dialog.getAlias());
            dbp.addDB(dialog.getAlias(), dialog.toPersistedProps());
            dbp.save(dialog.getAlias());
            controller.onConnectionsChanged();
        }
    }

    private void onEdit() {
        String alias = getSelectedAlias();
        if (alias == null) {
            showNoSelectionWarning();
            return;
        }
        DBProperties dbp = controller.getDatabaseSettings();
        Properties existing = dbp.getDBPropertiesFor(alias);
        ConnectionDialog dialog = new ConnectionDialog(
            controller.getMainFrame(),
            controller.getExecutor(),
            alias,
            existing,
            dbp.getDbList()
        );
        dialog.setVisible(true);
        if (!dialog.isSaved()) return;

        controller.getExecutor().closeConnection(alias); // force reconnect with new details
        if (dialog.isRenamed()) {
            dbp.delete(alias);
        }
        dbp.addDB(dialog.getAlias(), dialog.toPersistedProps());
        dbp.save(dialog.getAlias());
        controller.onConnectionsChanged();
    }

    private void onRename() {
        String alias = getSelectedAlias();
        if (alias == null) {
            showNoSelectionWarning();
            return;
        }
        if ("default".equals(alias)) {
            JOptionPane.showMessageDialog(
                this,
                "The 'default' connection cannot be renamed.",
                "Protected",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        DBProperties dbp = controller.getDatabaseSettings();
        String newAlias = JOptionPane.showInputDialog(this, "New connection name:", alias);
        if (newAlias == null) return;
        newAlias = newAlias.trim();
        if (newAlias.isEmpty() || newAlias.equals(alias)) return;
        if (dbp.getDbList().contains(newAlias)) {
            JOptionPane.showMessageDialog(
                this,
                "A connection named '" + newAlias + "' already exists.",
                "Duplicate Alias",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Properties existing = dbp.getDBPropertiesFor(alias);
        Properties renamed = new Properties();
        if (existing != null) {
            for (String key : existing.stringPropertyNames()) {
                renamed.setProperty(key, existing.getProperty(key));
            }
        }
        renamed.setProperty("db.alias", newAlias);
        controller.getExecutor().closeConnection(alias);
        dbp.delete(alias);
        dbp.addDB(newAlias, renamed);
        dbp.save(newAlias);
        controller.onConnectionsChanged();
    }

    private void onDuplicate() {
        String alias = getSelectedAlias();
        if (alias == null) {
            showNoSelectionWarning();
            return;
        }
        DBProperties dbp = controller.getDatabaseSettings();
        Properties existing = dbp.getDBPropertiesFor(alias);
        ConnectionDialog dialog = new ConnectionDialog(
            controller.getMainFrame(),
            controller.getExecutor(),
            null,
            existing,
            dbp.getDbList()
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            dbp.addDB(dialog.getAlias(), dialog.toPersistedProps());
            dbp.save(dialog.getAlias());
            controller.onConnectionsChanged();
        }
    }

    private void onDelete() {
        List<String> aliases = getSelectedAliases();
        if (aliases.isEmpty()) {
            showNoSelectionWarning();
            return;
        }
        if (aliases.remove("default")) {
            JOptionPane.showMessageDialog(
                this,
                "The 'default' connection cannot be deleted and was skipped.",
                "Protected",
                JOptionPane.WARNING_MESSAGE
            );
            if (aliases.isEmpty()) return;
        }
        String message = aliases.size() == 1
            ? "Delete connection '" + aliases.get(0) + "'?"
            : "Delete these " + aliases.size() + " connections?\n\n" + String.join("\n", aliases);
        int confirm = JOptionPane.showConfirmDialog(
            this,
            message,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return;
        for (String alias : aliases) {
            controller.getExecutor().closeConnection(alias);
            controller.getDatabaseSettings().delete(alias);
        }
        controller.onConnectionsChanged();
    }

    private void onTest() {
        final String alias = getSelectedAlias();
        if (alias == null) {
            showNoSelectionWarning();
            return;
        }
        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) {
            JOptionPane.showMessageDialog(
                this,
                "No connection details found for '" + alias + "'.",
                "Missing Connection",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        final JPanel self = this;
        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return controller.getExecutor().testConnection(props);
            }

            @Override
            protected void done() {
                try {
                    String info = get();
                    JOptionPane.showMessageDialog(
                        self,
                        info,
                        "Connection OK: " + alias,
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(
                        self,
                        ConnectionDialog.describeFailure(
                            cause,
                            props.getProperty(JdbcExecutor.DRIVER, "")
                        ),
                        "Connection Failed: " + alias,
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
        .execute();
    }

    /** Folder holding the connections that share a group name. */
    private static class GroupNode {
        final String name;

        GroupNode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Connection alias node. */
    private static class ConnNode {
        final String alias;

        ConnNode(String alias) {
            this.alias = alias;
        }

        @Override
        public String toString() {
            return alias;
        }
    }

    /** Folder holding the databases reachable from a connection. */
    private static class CatalogFolderNode {
        final String alias;

        CatalogFolderNode(String alias) {
            this.alias = alias;
        }

        @Override
        public String toString() {
            return DATABASES_FOLDER;
        }
    }

    /** A single database (catalog) under a connection. */
    private static class CatalogNode {
        final String alias;
        final String catalog;

        CatalogNode(String alias, String catalog) {
            this.alias = alias;
            this.catalog = catalog;
        }

        @Override
        public String toString() {
            return catalog;
        }
    }

    /** A schema under a connection. */
    private static class SchemaNode {
        final String alias;
        final String schema;

        SchemaNode(String alias, String schema) {
            this.alias = alias;
            this.schema = schema;
        }

        @Override
        public String toString() {
            return schema;
        }
    }

    /** Table/view node under a connection or schema. */
    private static class TableNode {
        final String alias;
        final JdbcExecutor.TableInfo info;

        TableNode(String alias, JdbcExecutor.TableInfo info) {
            this.alias = alias;
            this.info = info;
        }

        @Override
        public String toString() {
            return info.name + ("VIEW".equalsIgnoreCase(info.type) ? "  (view)" : "");
        }
    }
}
