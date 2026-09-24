package com.ing.ide.main.mainui.components.dbworkbench.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        /** Base table behind the result, when every column comes from one table. */
        public final String tableName;
        /** Schema of {@link #tableName}, may be empty. */
        public final String schemaName;
        /** Real column names (not labels), aligned with {@link #columns}. */
        public final List<String> columnNames;
        /** Primary-key column names of {@link #tableName}, empty when unknown. */
        public final List<String> primaryKey;

        QueryResult(
            List<String> columns,
            List<List<Object>> rows,
            boolean truncated,
            long elapsedMillis,
            String tableName,
            String schemaName,
            List<String> columnNames,
            List<String> primaryKey
        ) {
            this.columns = columns;
            this.rows = rows;
            this.truncated = truncated;
            this.elapsedMillis = elapsedMillis;
            this.tableName = tableName;
            this.schemaName = schemaName;
            this.columnNames = columnNames;
            this.primaryKey = primaryKey;
        }

        /**
         * @return true when edits made in the grid can be written back, which
         *         needs a single base table and a primary key inside the result
         */
        public boolean isUpdatable() {
            return (
                tableName != null &&
                !tableName.isEmpty() &&
                !primaryKey.isEmpty() &&
                columnNames.containsAll(primaryKey)
            );
        }

        /** @return the base table qualified with its schema when there is one */
        public String qualifiedTable() {
            return (schemaName == null || schemaName.isEmpty())
                ? tableName
                : schemaName + "." + tableName;
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

    /** One executed statement of a script and whatever it produced. */
    public static class StatementOutcome {
        public final String sql;
        public final SqlScript.Kind kind;
        public final QueryResult queryResult;
        public final DmlResult dmlResult;

        StatementOutcome(
            String sql,
            SqlScript.Kind kind,
            QueryResult queryResult,
            DmlResult dmlResult
        ) {
            this.sql = sql;
            this.kind = kind;
            this.queryResult = queryResult;
            this.dmlResult = dmlResult;
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
     * A cached connection has its auto-commit mode re-applied from {@code props},
     * so editing the connection's Auto-commit setting takes effect immediately.
     */
    public Connection getConnection(String alias, Properties props)
        throws SQLException, ClassNotFoundException {
        Connection existing = liveConnections.get(alias);
        if (existing != null && !existing.isClosed()) {
            applyAutoCommit(existing, props);
            return existing;
        }
        loadDriver(props);
        Connection c = open(props);
        liveConnections.put(alias, c);
        return c;
    }

    private void applyAutoCommit(Connection c, Properties props) throws SQLException {
        boolean wanted = Boolean.parseBoolean(props.getProperty(COMMIT, "false"));
        if (c.getAutoCommit() == wanted) {
            return;
        }
        // Leaving an open transaction behind would silently discard the work.
        if (!c.getAutoCommit()) {
            c.rollback();
        }
        c.setAutoCommit(wanted);
    }

    /**
     * Runs every statement in the script in order against one connection.
     * <p>
     * Each statement is guarded by a savepoint when the connection is in a
     * transaction, so a failure rolls back only that statement and leaves the
     * session usable — PostgreSQL otherwise aborts the whole transaction and
     * rejects every following statement. Statements that PostgreSQL forbids
     * inside a transaction block are run with auto-commit temporarily enabled.
     *
     * @param c the connection to run against
     * @param script raw SQL, possibly several {@code ;}-separated statements
     * @param timeoutSeconds per-statement query timeout, 0 for none
     * @param readOnly when true, any statement that writes is rejected
     * @return one outcome per executed statement
     * @throws SQLException if a statement fails
     */
    public List<StatementOutcome> executeScript(
        Connection c,
        String script,
        int timeoutSeconds,
        boolean readOnly
    )
        throws SQLException {
        List<String> statements = SqlScript.split(script);
        if (statements.isEmpty()) {
            throw new SQLException("No SQL statement to execute.");
        }
        List<StatementOutcome> outcomes = new ArrayList<>();
        for (String statement : statements) {
            outcomes.add(executeOne(c, statement, timeoutSeconds, readOnly));
        }
        return outcomes;
    }

    private StatementOutcome executeOne(
        Connection c,
        String statement,
        int timeoutSeconds,
        boolean readOnly
    )
        throws SQLException {
        String sql = statement;
        String expanded = SqlScript.expandPsqlShorthand(statement);
        if (expanded != null) {
            sql = expanded;
        } else if (statement.startsWith("\\")) {
            throw new SQLException("Unsupported shorthand command: " + statement);
        }

        SqlScript.Kind kind = SqlScript.classify(sql);
        if (readOnly && kind.isWrite()) {
            throw new SQLException(
                "The connection is marked read-only; " +
                kind +
                " statements are blocked: " +
                shorten(statement)
            );
        }

        if (SqlScript.requiresAutoCommit(sql) && !c.getAutoCommit()) {
            return runOutsideTransaction(c, sql, kind, timeoutSeconds);
        }
        return runGuarded(c, sql, kind, timeoutSeconds);
    }

    /**
     * Ends the open transaction, runs the statement in auto-commit mode and then
     * restores the connection's transactional mode.
     */
    private StatementOutcome runOutsideTransaction(
        Connection c,
        String sql,
        SqlScript.Kind kind,
        int timeoutSeconds
    )
        throws SQLException {
        c.commit();
        c.setAutoCommit(true);
        try {
            return run(c, sql, kind, timeoutSeconds);
        } finally {
            c.setAutoCommit(false);
        }
    }

    /** Runs the statement, undoing just this statement if it fails. */
    private StatementOutcome runGuarded(
        Connection c,
        String sql,
        SqlScript.Kind kind,
        int timeoutSeconds
    )
        throws SQLException {
        if (c.getAutoCommit()) {
            return run(c, sql, kind, timeoutSeconds);
        }
        Savepoint savepoint = null;
        try {
            savepoint = c.setSavepoint("ing_db_workbench");
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Savepoints unsupported on this connection", e);
        }
        try {
            StatementOutcome outcome = run(c, sql, kind, timeoutSeconds);
            releaseQuietly(c, savepoint);
            return outcome;
        } catch (SQLException failure) {
            rollbackQuietly(c, savepoint);
            throw failure;
        }
    }

    private StatementOutcome run(Connection c, String sql, SqlScript.Kind kind, int timeoutSeconds)
        throws SQLException {
        long start = System.currentTimeMillis();
        try (Statement st = c.createStatement()) {
            if (timeoutSeconds > 0) {
                st.setQueryTimeout(timeoutSeconds);
            }
            st.setMaxRows(DEFAULT_MAX_ROWS + 1);
            boolean hasResultSet = st.execute(sql);
            if (hasResultSet) {
                try (ResultSet rs = st.getResultSet()) {
                    return new StatementOutcome(sql, kind, readResultSet(c, rs, start), null);
                }
            }
            return new StatementOutcome(
                sql,
                kind,
                null,
                new DmlResult(st.getUpdateCount(), System.currentTimeMillis() - start)
            );
        }
    }

    private void releaseQuietly(Connection c, Savepoint savepoint) {
        if (savepoint == null) return;
        try {
            c.releaseSavepoint(savepoint);
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Could not release savepoint", e);
        }
    }

    private void rollbackQuietly(Connection c, Savepoint savepoint) {
        try {
            if (savepoint != null) {
                c.rollback(savepoint);
            } else {
                c.rollback();
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Could not roll back after a failed statement", e);
        }
    }

    private static String shorten(String sql) {
        String oneLine = sql.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 80 ? oneLine : oneLine.substring(0, 77) + "...";
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
                return readResultSet(c, rs, start);
            }
        }
    }

    private QueryResult readResultSet(Connection c, ResultSet rs, long start) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        List<String> columns = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
            columns.add(md.getColumnLabel(i));
            columnNames.add(md.getColumnName(i));
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
        String table = singleBaseTable(md, colCount);
        String schema = table == null ? null : safeSchema(md, colCount);
        List<String> primaryKey = table == null
            ? new ArrayList<>()
            : primaryKeyColumns(c, schema, table);
        return new QueryResult(
            columns,
            rows,
            truncated,
            System.currentTimeMillis() - start,
            table,
            schema,
            columnNames,
            primaryKey
        );
    }

    /** @return the base table when every column comes from it, otherwise {@code null} */
    private String singleBaseTable(ResultSetMetaData md, int colCount) {
        String table = null;
        for (int i = 1; i <= colCount; i++) {
            String current;
            try {
                current = md.getTableName(i);
            } catch (SQLException e) {
                return null; // driver does not expose base tables
            }
            if (current == null || current.isEmpty()) {
                return null;
            }
            if (table == null) {
                table = current;
            } else if (!table.equals(current)) {
                return null;
            }
        }
        return table;
    }

    private String safeSchema(ResultSetMetaData md, int colCount) {
        for (int i = 1; i <= colCount; i++) {
            try {
                String schema = md.getSchemaName(i);
                if (schema != null && !schema.isEmpty()) {
                    return schema;
                }
            } catch (SQLException e) {
                return null;
            }
        }
        return null;
    }

    private List<String> primaryKeyColumns(Connection c, String schema, String table) {
        List<String> keys = new ArrayList<>();
        try (ResultSet rs = c.getMetaData().getPrimaryKeys(c.getCatalog(), schema, table)) {
            while (rs.next()) {
                keys.add(rs.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            LOG.log(Level.FINE, "Could not read primary key for " + table, e);
        }
        return keys;
    }

    /**
     * Writes a single edited grid cell back to its base table.
     *
     * @param c the connection to run against
     * @param result the result the grid was built from
     * @param column the column being changed
     * @param newValue the new value, {@code null} for SQL NULL
     * @param keyValues primary-key column to value pairs identifying the row
     * @return the number of rows changed
     * @throws SQLException if the update fails or would touch more than one row
     */
    public int updateCell(
        Connection c,
        QueryResult result,
        String column,
        Object newValue,
        Map<String, Object> keyValues
    )
        throws SQLException {
        if (!result.isUpdatable()) {
            throw new SQLException("This result set cannot be edited in the grid.");
        }
        if (keyValues.isEmpty()) {
            throw new SQLException("The row has no primary key values to match on.");
        }
        StringBuilder sql = new StringBuilder("UPDATE ")
            .append(quoteIdentifier(c, result.qualifiedTable()))
            .append(" SET ")
            .append(quoteIdentifier(c, column))
            .append(" = ? WHERE ");
        List<Object> params = new ArrayList<>();
        params.add(newValue);
        boolean first = true;
        for (Map.Entry<String, Object> key : keyValues.entrySet()) {
            if (!first) sql.append(" AND ");
            sql.append(quoteIdentifier(c, key.getKey())).append(" = ?");
            params.add(key.getValue());
            first = false;
        }
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        }
    }

    private String quoteIdentifier(Connection c, String identifier) throws SQLException {
        String quote = c.getMetaData().getIdentifierQuoteString();
        if (quote == null || quote.trim().isEmpty()) {
            return identifier;
        }
        StringBuilder sb = new StringBuilder();
        for (String part : identifier.split("\\.")) {
            if (sb.length() > 0) sb.append('.');
            sb.append(quote).append(part.replace(quote, quote + quote)).append(quote);
        }
        return sb.toString();
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
        return listTables(c, null);
    }

    /**
     * Lists tables and views in one schema, or in every visible schema when
     * {@code schema} is {@code null}.
     *
     * @param c the connection to inspect
     * @param schema the schema to restrict to, or {@code null} for all
     * @return the tables and views found, capped at 500
     * @throws SQLException if the metadata cannot be read
     */
    public List<TableInfo> listTables(Connection c, String schema) throws SQLException {
        List<TableInfo> tables = new ArrayList<>();
        DatabaseMetaData md = c.getMetaData();
        try (
            ResultSet rs = md.getTables(
                c.getCatalog(),
                schema,
                "%",
                new String[] { "TABLE", "VIEW" }
            )
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

    /**
     * Lists the schemas visible to the connection, skipping the vendor's internal
     * ones so the browser only shows what a tester cares about.
     *
     * @param c the connection to inspect
     * @return the schema names, in catalog order
     * @throws SQLException if the metadata cannot be read
     */
    public List<String> listSchemas(Connection c) throws SQLException {
        List<String> schemas = new ArrayList<>();
        try (ResultSet rs = c.getMetaData().getSchemas()) {
            while (rs.next()) {
                String name = rs.getString("TABLE_SCHEM");
                if (name != null && !isInternalSchema(name)) {
                    schemas.add(name);
                }
            }
        }
        return schemas;
    }

    private static boolean isInternalSchema(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.startsWith("pg_") || n.equals("information_schema");
    }

    /**
     * Lists the databases (catalogs) reachable from the connection.
     *
     * @param c the connection to inspect
     * @return the catalog names
     * @throws SQLException if the metadata cannot be read
     */
    public List<String> listCatalogs(Connection c) throws SQLException {
        List<String> catalogs = new ArrayList<>();
        try (ResultSet rs = c.getMetaData().getCatalogs()) {
            while (rs.next()) {
                catalogs.add(rs.getString("TABLE_CAT"));
            }
        }
        return catalogs;
    }

    /**
     * Switches the connection to another database/schema without reconnecting.
     * Falls back to {@code SET search_path} style behaviour through the JDBC
     * schema API, which is what PostgreSQL and DB2 honour.
     *
     * @param c the connection to switch
     * @param catalog the database to use, or {@code null} to leave it alone
     * @param schema the schema to use, or {@code null} to leave it alone
     * @throws SQLException if the driver rejects the switch
     */
    public void switchNamespace(Connection c, String catalog, String schema) throws SQLException {
        if (catalog != null && !catalog.isEmpty()) {
            c.setCatalog(catalog);
        }
        if (schema != null && !schema.isEmpty()) {
            c.setSchema(schema);
        }
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
        if (c == null || c.isClosed()) {
            throw new SQLException("No open connection for '" + alias + "'.");
        }
        if (c.getAutoCommit()) {
            throw new SQLException(
                "Connection '" +
                alias +
                "' has Auto-commit enabled, so every statement is already committed."
            );
        }
        c.commit();
    }

    /** Rolls back the current transaction on the alias's live connection. */
    public void rollback(String alias) throws SQLException {
        Connection c = liveConnections.get(alias);
        if (c == null || c.isClosed()) {
            throw new SQLException("No open connection for '" + alias + "'.");
        }
        if (c.getAutoCommit()) {
            throw new SQLException(
                "Connection '" +
                alias +
                "' has Auto-commit enabled, so committed statements cannot be rolled back. " +
                "Turn Auto-commit off in the connection settings to use transactions."
            );
        }
        c.rollback();
    }

    /** @return true when the alias has an open connection running in a transaction */
    public boolean isTransactional(String alias) {
        Connection c = liveConnections.get(alias);
        try {
            return c != null && !c.isClosed() && !c.getAutoCommit();
        } catch (SQLException e) {
            return false;
        }
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
