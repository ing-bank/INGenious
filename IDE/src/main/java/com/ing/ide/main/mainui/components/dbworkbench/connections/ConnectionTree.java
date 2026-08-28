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
import java.util.List;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Left-rail schema browser: a tree of connection aliases; expanding a connection
 * lazily loads its tables/views, and double-clicking a table generates a
 * {@code SELECT} into the editor. New / Edit / Delete / Test act on the selected
 * connection. Reads and writes the project's {@link DBProperties} store.
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

    /** Marker node used to make a connection appear expandable before its tables load. */
    private static final String LOADING = "Loading…";

    public ConnectionTree(DBWorkbench controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeWillExpandListener(
            new javax.swing.event.TreeWillExpandListener() {

                @Override
                public void treeWillExpand(javax.swing.event.TreeExpansionEvent e) {
                    lazyLoadTables((DefaultMutableTreeNode) e.getPath().getLastPathComponent());
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
                        onTableDoubleClick();
                    }
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

        h.add(toggleBtn, BorderLayout.WEST);
        h.add(titleLabel, BorderLayout.CENTER);
        return h;
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
        root.removeAllChildren();
        DBProperties dbp = controller.getDatabaseSettings();
        if (dbp != null) {
            for (String alias : dbp.getDbList()) {
                DefaultMutableTreeNode conn = new DefaultMutableTreeNode(new ConnNode(alias));
                conn.add(new DefaultMutableTreeNode(LOADING));
                root.add(conn);
            }
        }
        treeModel.reload();
    }

    /** Alias of the selected node (a connection, or the parent of a table node). */
    public String getSelectedAlias() {
        DefaultMutableTreeNode node = selectedNode();
        if (node == null) return null;
        Object uo = node.getUserObject();
        if (uo instanceof ConnNode) return ((ConnNode) uo).alias;
        if (uo instanceof TableNode) return ((TableNode) uo).alias;
        return null;
    }

    private DefaultMutableTreeNode selectedNode() {
        TreePath path = tree.getSelectionPath();
        return path == null ? null : (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    private void lazyLoadTables(DefaultMutableTreeNode connNode) {
        if (!(connNode.getUserObject() instanceof ConnNode)) return;
        // Already loaded when the single child is not the LOADING marker.
        if (connNode.getChildCount() != 1) return;
        DefaultMutableTreeNode first = (DefaultMutableTreeNode) connNode.getChildAt(0);
        if (!LOADING.equals(first.getUserObject())) return;

        final String alias = ((ConnNode) connNode.getUserObject()).alias;
        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) return;

        new SwingWorker<List<JdbcExecutor.TableInfo>, Void>() {

            @Override
            protected List<JdbcExecutor.TableInfo> doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(alias, props);
                return exec.listTables(c);
            }

            @Override
            protected void done() {
                connNode.removeAllChildren();
                try {
                    List<JdbcExecutor.TableInfo> tables = get();
                    List<String> completions = new ArrayList<>();
                    for (JdbcExecutor.TableInfo t : tables) {
                        connNode.add(new DefaultMutableTreeNode(new TableNode(alias, t)));
                        completions.add(t.name);
                    }
                    if (tables.isEmpty()) {
                        connNode.add(new DefaultMutableTreeNode("(no tables)"));
                    }
                    controller
                        .getDBWorkbenchUI()
                        .getQueryEditor()
                        .addSchemaCompletions(completions);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    connNode.add(new DefaultMutableTreeNode("Error: " + cause.getMessage()));
                }
                treeModel.reload(connNode);
                tree.expandPath(new TreePath(connNode.getPath()));
            }
        }
        .execute();
    }

    private void onTableDoubleClick() {
        DefaultMutableTreeNode node = selectedNode();
        if (node == null || !(node.getUserObject() instanceof TableNode)) return;
        TableNode tn = (TableNode) node.getUserObject();
        controller
            .getDBWorkbenchUI()
            .getQueryEditor()
            .setSql("SELECT * FROM " + tn.info.qualified());
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
            null
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            dbp.addDB(dialog.getAlias(), dialog.toPersistedProps());
            dbp.save(dialog.getAlias());
            controller.onConnectionsChanged();
        }
    }

    private void onEdit() {
        String alias = getSelectedAlias();
        if (alias == null) return;
        DBProperties dbp = controller.getDatabaseSettings();
        Properties existing = dbp.getDBPropertiesFor(alias);
        ConnectionDialog dialog = new ConnectionDialog(
            controller.getMainFrame(),
            controller.getExecutor(),
            alias,
            existing
        );
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            dbp.addDB(alias, dialog.toPersistedProps());
            dbp.save(alias);
            controller.getExecutor().closeConnection(alias); // force reconnect with new details
            controller.onConnectionsChanged();
        }
    }

    private void onDelete() {
        String alias = getSelectedAlias();
        if (alias == null) return;
        if ("default".equals(alias)) {
            JOptionPane.showMessageDialog(
                this,
                "The 'default' connection cannot be deleted.",
                "Protected",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete connection '" + alias + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            controller.getExecutor().closeConnection(alias);
            controller.getDatabaseSettings().delete(alias);
            controller.onConnectionsChanged();
        }
    }

    private void onTest() {
        final String alias = getSelectedAlias();
        if (alias == null) return;
        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) return;
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
                        "Connection failed:\n" + cause.getMessage(),
                        "Connection Failed: " + alias,
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
        .execute();
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

    /** Table/view node under a connection. */
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
