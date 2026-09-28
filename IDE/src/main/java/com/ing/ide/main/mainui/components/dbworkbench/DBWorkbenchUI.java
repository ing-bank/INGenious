package com.ing.ide.main.mainui.components.dbworkbench;

import com.ing.ide.main.mainui.components.dbworkbench.connections.ConnectionTree;
import com.ing.ide.main.mainui.components.dbworkbench.query.QueryEditorPanel;
import com.ing.ide.main.mainui.components.dbworkbench.result.ResultGridPanel;
import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import java.awt.BorderLayout;
import java.sql.Connection;
import java.util.Properties;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

/**
 * Main UI panel for the Database Workbench.
 * Layout: left connection list | right (SQL editor over results grid).
 */
public class DBWorkbenchUI extends JPanel {
    private final DBWorkbench controller;
    private final ConnectionTree connectionTree;
    private final QueryEditorPanel queryEditor;
    private final ResultGridPanel resultPanel;
    private final JSplitPane mainSplit;
    private int lastConnectionsDivider = 240;
    private static final int COLLAPSED_WIDTH = 34;

    public DBWorkbenchUI(DBWorkbench controller) {
        this.controller = controller;
        setLayout(new BorderLayout());

        connectionTree = new ConnectionTree(controller);
        queryEditor = new QueryEditorPanel(this);
        resultPanel = new ResultGridPanel();

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryEditor, resultPanel);
        rightSplit.setResizeWeight(0.45);
        rightSplit.setDividerLocation(260);

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, connectionTree, rightSplit);
        mainSplit.setResizeWeight(0.2);
        mainSplit.setDividerLocation(240);

        add(mainSplit, BorderLayout.CENTER);

        refresh();
    }

    /** Collapses the connections panel to a thin strip, or restores its width. */
    public void setConnectionsCollapsed(boolean collapsed) {
        if (collapsed) {
            lastConnectionsDivider = Math.max(mainSplit.getDividerLocation(), 120);
            connectionTree.setMinimumSize(new java.awt.Dimension(COLLAPSED_WIDTH, 0));
            mainSplit.setDividerLocation(COLLAPSED_WIDTH);
        } else {
            connectionTree.setMinimumSize(null);
            mainSplit.setDividerLocation(lastConnectionsDivider);
        }
        mainSplit.revalidate();
    }

    public DBWorkbench getController() {
        return controller;
    }

    public ResultGridPanel getResultPanel() {
        return resultPanel;
    }

    public QueryEditorPanel getQueryEditor() {
        return queryEditor;
    }

    /** Refreshes the connection list and the editor's connection selector. */
    public final void refresh() {
        connectionTree.refresh();
        queryEditor.refreshConnections(controller.getConnectionAliases());
    }

    /** Executes the SQL currently in the editor against the selected connection. */
    public void runCurrentQuery() {
        final String alias = queryEditor.getSelectedAlias();
        final String sql = queryEditor.getSql();
        final boolean dml = queryEditor.isDml();
        if (alias == null) {
            resultPanel.showError("Select a database connection first.");
            return;
        }
        if (sql.isEmpty()) {
            resultPanel.showError("Enter a SQL statement.");
            return;
        }

        final Properties props = controller.resolveConnectionProps(alias);
        if (props == null) {
            resultPanel.showError("No connection details found for '" + alias + "'.");
            return;
        }
        if (dml && Boolean.parseBoolean(props.getProperty("readOnly", "false"))) {
            resultPanel.showError(
                "Connection '" + alias + "' is marked read-only; DML statements are blocked."
            );
            return;
        }
        final int timeout = parseTimeout(props);

        new SwingWorker<Object, Void>() {

            @Override
            protected Object doInBackground() throws Exception {
                JdbcExecutor exec = controller.getExecutor();
                Connection c = exec.getConnection(alias, props);
                if (dml) {
                    return exec.executeUpdate(c, sql, timeout);
                }
                return exec.executeQuery(c, sql, timeout);
            }

            @Override
            protected void done() {
                try {
                    Object result = get();
                    if (result instanceof JdbcExecutor.QueryResult) {
                        resultPanel.showResult((JdbcExecutor.QueryResult) result);
                    } else if (result instanceof JdbcExecutor.DmlResult) {
                        resultPanel.showDml((JdbcExecutor.DmlResult) result);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    resultPanel.showError(cause.getMessage());
                }
            }
        }
        .execute();
    }

    private int parseTimeout(Properties props) {
        try {
            return Integer.parseInt(props.getProperty(JdbcExecutor.TIMEOUT, "30").trim());
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    /** Commits the transaction on the editor's selected connection. */
    public void commitCurrent() {
        transaction(true);
    }

    /** Rolls back the transaction on the editor's selected connection. */
    public void rollbackCurrent() {
        transaction(false);
    }

    /** Saves the current editor query into the project. */
    public void saveCurrentQuery() {
        if (controller.getMainFrame().getProject() == null) {
            resultPanel.showError("Open a project first.");
            return;
        }
        controller.saveQuery(queryEditor.buildQuery());
        resultPanel.showMessage("Saved query '" + queryEditor.getQueryName() + "'.");
    }

    /** Opens a saved query into the editor via a picker dialog. */
    public void openSavedQuery() {
        java.util.List<com.ing.datalib.dbworkbench.DBQuery> saved = controller.getAllSavedQueries();
        if (saved.isEmpty()) {
            resultPanel.showMessage("No saved queries yet.");
            return;
        }
        com.ing.datalib.dbworkbench.DBQuery choice = (com.ing.datalib.dbworkbench.DBQuery) javax.swing.JOptionPane.showInputDialog(
            this,
            "Select a saved query:",
            "Open Query",
            javax.swing.JOptionPane.PLAIN_MESSAGE,
            null,
            saved.toArray(),
            saved.get(0)
        );
        if (choice != null) {
            queryEditor.loadQuery(choice);
            resultPanel.clearValidations();
        }
    }

    private void transaction(boolean commit) {
        String alias = queryEditor.getSelectedAlias();
        if (alias == null) {
            resultPanel.showError("Select a database connection first.");
            return;
        }
        try {
            if (commit) {
                controller.getExecutor().commit(alias);
                resultPanel.showMessage("Committed transaction on '" + alias + "'.");
            } else {
                controller.getExecutor().rollback(alias);
                resultPanel.showMessage("Rolled back transaction on '" + alias + "'.");
            }
        } catch (Exception ex) {
            resultPanel.showError(ex.getMessage());
        }
    }
}
