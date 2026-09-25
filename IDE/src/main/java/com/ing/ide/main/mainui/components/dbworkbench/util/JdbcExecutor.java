package com.ing.ide.main.mainui.components.dbworkbench.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IDE-side JDBC helper for the Database Workbench.
 * <p>
 * Owns the live connections held while the workbench is open (one per alias) and
 * runs connection tests and queries. All calls are synchronous and expected to be
 * invoked off the EDT (the UI wraps them in {@code SwingWorker}s).
 */
public class JdbcExecutor {
    private static final Logger LOG = Logger.getLogger(JdbcExecutor.class.getName());

    /** Property keys, matching {@code DBProperties}. */
    public static final String DRIVER = "driver";
    public static final String CONN_STR = "connectionString";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String TIMEOUT = "timeout";
    public static final String COMMIT = "commit";

    private static final int DEFAULT_MAX_ROWS = 1000;

    private final Map<String, Connection> liveConnections = new HashMap<>();

    /** Immutable snapshot of a SELECT result set for the grid. */
    public static class QueryResult {
        public final List<String> columns;
        public final List<List<Object>> rows;
        public final boolean truncated;
        public final long elapsedMillis;

        QueryResult(
            List<String> columns,
            List<List<Object>> rows,
            boolean truncated,
            long elapsedMillis
        ) {
            this.columns = columns;
            this.rows = rows;
            this.truncated = truncated;
            this.elapsedMillis = elapsedMillis;
        }
    }

    /** Result of a DML statement. */
    public static class DmlResult {
        public final int affectedRows;
        public final long elapsedMillis;

        DmlResult(int affectedRows, long elapsedMillis) {
            this.affectedRows = affectedRows;
            this.elapsedMillis = elapsedMillis;
        }
    }

    /**
     * Verifies connectivity for the given connection properties and returns a
     * human-readable metadata summary. Throws on any failure so the caller can
     * surface the exact reason.
     */
    public String testConnection(Properties props) throws SQLException, ClassNotFoundException {
        loadDriver(props);
        try (Connection c = open(props)) {
            DatabaseMetaData md = c.getMetaData();
            return (
                "Connected to " +
                md.getDatabaseProductName() +
                " " +
                md.getDatabaseProductVersion() +
                "\nDriver: " +
                md.getDriverName() +
                " " +
                md.getDriverVersion()
            );
        }
    }

    /**
     * Returns a live connection for the alias, opening (and caching) one if needed.
     */
    public Connection getConnection(String alias, Properties props)
        throws SQLException, ClassNotFoundException {
        Connection existing = liveConnections.get(alias);
        if (existing != null && !existing.isClosed()) {
            return existing;
        }
        loadDriver(props);
        Connection c = open(props);
        liveConnections.put(alias, c);
        return c;
    }

    /** Runs a SELECT and returns a capped snapshot for the grid. */
    public QueryResult executeQuery(Connection c, String sql, int timeoutSeconds)
        throws SQLException {
        long start = System.currentTimeMillis();
        // Forward-only cursor: the grid is built by a single forward pass, and some
        // drivers (e.g. SQLite) only support TYPE_FORWARD_ONLY.
        try (Statement st = c.createStatement()) {
            if (timeoutSeconds > 0) {
                st.setQueryTimeout(timeoutSeconds);
            }
            st.setMaxRows(DEFAULT_MAX_ROWS + 1);
            try (ResultSet rs = st.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    columns.add(md.getColumnLabel(i));
                }
                List<List<Object>> rows = new ArrayList<>();
                boolean truncated = false;
                while (rs.next()) {
                    if (rows.size() >= DEFAULT_MAX_ROWS) {
                        truncated = true;
                        break;
                    }
                    List<Object> row = new ArrayList<>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    rows.add(row);
                }
                return new QueryResult(
                    columns,
                    rows,
                    truncated,
                    System.currentTimeMillis() - start
                );
            }
        }
    }

    /** Runs an INSERT/UPDATE/DELETE and returns the affected-row count. */
    public DmlResult executeUpdate(Connection c, String sql, int timeoutSeconds)
        throws SQLException {
        long start = System.currentTimeMillis();
        try (Statement st = c.createStatement()) {
            if (timeoutSeconds > 0) {
                st.setQueryTimeout(timeoutSeconds);
            }
            int affected = st.executeUpdate(sql);
            return new DmlResult(affected, System.currentTimeMillis() - start);
        }
    }

    /** A single table/view in the schema browser. */
    public static class TableInfo {
        public final String schema;
        public final String name;
        public final String type;

        TableInfo(String schema, String name, String type) {
            this.schema = schema;
            this.name = name;
            this.type = type;
        }

        /** Qualified name for use in SQL, e.g. {@code schema.table}. */
        public String qualified() {
            return (schema == null || schema.isEmpty()) ? name : schema + "." + name;
        }
    }

    /** Lists tables and views visible to the connection (capped for responsiveness). */
    public List<TableInfo> listTables(Connection c) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        DatabaseMetaData md = c.getMetaData();
        try (
            ResultSet rs = md.getTables(c.getCatalog(), null, "%", new String[] { "TABLE", "VIEW" })
        ) {
            while (rs.next() && tables.size() < 500) {
                tables.add(
                    new TableInfo(
                        rs.getString("TABLE_SCHEM"),
                        rs.getString("TABLE_NAME"),
                        rs.getString("TABLE_TYPE")
                    )
                );
            }
        }
        return tables;
    }

    /** Lists column names for a given table. */
    public List<String> listColumns(Connection c, String schema, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getColumns(c.getCatalog(), schema, table, "%")) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    /** Commits the current transaction on the alias's live connection. */
    public void commit(String alias) throws SQLException {
        Connection c = liveConnections.get(alias);
        if (c != null && !c.isClosed()) c.commit();
    }

    /** Rolls back the current transaction on the alias's live connection. */
    public void rollback(String alias) throws SQLException {
        Connection c = liveConnections.get(alias);
        if (c != null && !c.isClosed()) c.rollback();
    }

    /** Closes and forgets the connection for a single alias. */
    public void closeConnection(String alias) {
        Connection c = liveConnections.remove(alias);
        if (c != null) {
            try {
                c.close();
            } catch (SQLException e) {
                LOG.log(Level.FINE, "Error closing connection for " + alias, e);
            }
        }
    }

    /** Closes every live connection (workbench close / project switch). */
    public void closeAll() {
        for (Connection c : liveConnections.values()) {
            try {
                if (c != null) c.close();
            } catch (SQLException e) {
                LOG.log(Level.FINE, "Error closing connection", e);
            }
        }
        liveConnections.clear();
    }

    private Connection open(Properties props) throws SQLException {
        String url = props.getProperty(CONN_STR);
        String user = props.getProperty(USER);
        String pass = props.getProperty(PASSWORD);
        DriverManager.setLoginTimeout(parseInt(props.getProperty(TIMEOUT), 30));
        Connection c;
        if (user != null && !user.isEmpty()) {
            c = DriverManager.getConnection(url, user, pass);
        } else {
            c = DriverManager.getConnection(url);
        }
        c.setAutoCommit(Boolean.parseBoolean(props.getProperty(COMMIT, "false")));
        return c;
    }

    private void loadDriver(Properties props) throws ClassNotFoundException {
        String driver = props.getProperty(DRIVER);
        if (driver != null && !driver.trim().isEmpty()) {
            Class.forName(driver.trim());
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
