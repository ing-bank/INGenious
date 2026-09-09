package com.ing.ide.main.mainui.components.dbworkbench.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parsing helpers for the SQL typed into the Database Workbench editor.
 * <p>
 * Splits a script into individual statements, classifies each one so the
 * workbench knows whether to run a query or an update (and whether a read-only
 * connection must block it), and expands the PostgreSQL {@code psql} shorthand
 * commands ({@code \d}, {@code \l}, ...) into portable catalog queries.
 */
public final class SqlScript {

    /** What a single statement does, which drives execution and read-only checks. */
    public enum Kind {
        /** Returns a result set. */
        QUERY,
        /** Changes rows: INSERT / UPDATE / DELETE / MERGE. */
        DML,
        /** Changes structure: CREATE / ALTER / DROP / TRUNCATE / GRANT ... */
        DDL,
        /** Transaction control: COMMIT / ROLLBACK / SAVEPOINT / BEGIN. */
        TRANSACTION,
        /** Anything not recognised; executed as a generic statement. */
        OTHER;

        /** @return true when the statement modifies data or structure. */
        public boolean isWrite() {
            return this == DML || this == DDL;
        }
    }

    private SqlScript() {}

    /**
     * Splits a SQL script into executable statements on unquoted semicolons.
     * Quoted literals, identifiers, dollar-quoted bodies and comments are
     * preserved verbatim, so a semicolon inside them does not split.
     *
     * @param script raw editor content
     * @return the non-empty statements, in order
     */
    public static List<String> split(String script) {
        List<String> statements = new ArrayList<>();
        if (script == null || script.trim().isEmpty()) {
            return statements;
        }
        StringBuilder current = new StringBuilder();
        int i = 0;
        final int length = script.length();
        while (i < length) {
            char c = script.charAt(i);
            if (c == '\'' || c == '"' || c == '`') {
                i = consumeQuoted(script, i, c, current);
            } else if (c == '-' && next(script, i) == '-') {
                i = consumeLineComment(script, i, current);
            } else if (c == '/' && next(script, i) == '*') {
                i = consumeBlockComment(script, i, current);
            } else if (c == '$') {
                int tagEnd = dollarTagEnd(script, i);
                if (tagEnd > 0) {
                    i = consumeDollarQuoted(script, i, script.substring(i, tagEnd), current);
                } else {
                    current.append(c);
                    i++;
                }
            } else if (c == ';') {
                addIfNotBlank(statements, current);
                current.setLength(0);
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder current) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }

    private static char next(String s, int i) {
        return i + 1 < s.length() ? s.charAt(i + 1) : '\0';
    }

    private static int consumeQuoted(String s, int start, char quote, StringBuilder out) {
        out.append(quote);
        int i = start + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            out.append(c);
            if (c == quote) {
                // A doubled quote is an escaped quote, not the end of the literal.
                if (i + 1 < s.length() && s.charAt(i + 1) == quote) {
                    out.append(quote);
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            if (c == '\\' && i + 1 < s.length()) {
                out.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            i++;
        }
        return i;
    }

    private static int consumeLineComment(String s, int start, StringBuilder out) {
        int end = s.indexOf('\n', start);
        if (end < 0) {
            out.append(s, start, s.length());
            return s.length();
        }
        out.append(s, start, end + 1);
        return end + 1;
    }

    private static int consumeBlockComment(String s, int start, StringBuilder out) {
        int end = s.indexOf("*/", start + 2);
        if (end < 0) {
            out.append(s, start, s.length());
            return s.length();
        }
        out.append(s, start, end + 2);
        return end + 2;
    }

    /**
     * @return the index just past a {@code $tag$} opener starting at {@code start},
     *         or {@code -1} when this {@code $} does not open a dollar-quoted body
     */
    private static int dollarTagEnd(String s, int start) {
        int i = start + 1;
        while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
            i++;
        }
        return (i < s.length() && s.charAt(i) == '$') ? i + 1 : -1;
    }

    private static int consumeDollarQuoted(String s, int start, String tag, StringBuilder out) {
        int end = s.indexOf(tag, start + tag.length());
        if (end < 0) {
            out.append(s, start, s.length());
            return s.length();
        }
        out.append(s, start, end + tag.length());
        return end + tag.length();
    }

    /**
     * Classifies a single statement by its leading keyword.
     *
     * @param statement one statement, without a trailing semicolon
     * @return the statement kind
     */
    public static Kind classify(String statement) {
        String keyword = leadingKeyword(statement);
        switch (keyword) {
            case "SELECT":
            case "WITH":
            case "SHOW":
            case "DESC":
            case "DESCRIBE":
            case "EXPLAIN":
            case "VALUES":
            case "TABLE":
                return Kind.QUERY;
            case "INSERT":
            case "UPDATE":
            case "DELETE":
            case "MERGE":
            case "UPSERT":
            case "REPLACE":
            case "COPY":
            case "CALL":
                return Kind.DML;
            case "CREATE":
            case "ALTER":
            case "DROP":
            case "TRUNCATE":
            case "RENAME":
            case "GRANT":
            case "REVOKE":
            case "COMMENT":
            case "VACUUM":
            case "ANALYZE":
            case "REINDEX":
            case "REFRESH":
                return Kind.DDL;
            case "COMMIT":
            case "ROLLBACK":
            case "BEGIN":
            case "START":
            case "SAVEPOINT":
            case "END":
            case "SET":
                return Kind.TRANSACTION;
            default:
                return Kind.OTHER;
        }
    }

    /**
     * PostgreSQL refuses {@code CREATE/DROP/ALTER DATABASE} and a few other
     * statements inside an open transaction block, which is exactly what an
     * earlier SELECT leaves behind when auto-commit is off.
     *
     * @param statement one statement, without a trailing semicolon
     * @return true when the statement has to run outside a transaction
     */
    public static boolean requiresAutoCommit(String statement) {
        String normalized = stripLeadingNoise(statement).toUpperCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\s+", " ");
        return (
            normalized.startsWith("CREATE DATABASE") ||
            normalized.startsWith("DROP DATABASE") ||
            normalized.startsWith("ALTER DATABASE") ||
            normalized.startsWith("CREATE TABLESPACE") ||
            normalized.startsWith("DROP TABLESPACE") ||
            normalized.startsWith("VACUUM") ||
            normalized.startsWith("CREATE INDEX CONCURRENTLY") ||
            normalized.startsWith("DROP INDEX CONCURRENTLY")
        );
    }

    /** @return the first SQL keyword in upper case, or an empty string */
    private static String leadingKeyword(String statement) {
        String stripped = stripLeadingNoise(statement);
        int end = 0;
        while (end < stripped.length() && Character.isLetter(stripped.charAt(end))) {
            end++;
        }
        return stripped.substring(0, end).toUpperCase(Locale.ROOT);
    }

    /** Removes leading whitespace, comments and opening parentheses. */
    private static String stripLeadingNoise(String statement) {
        String s = statement == null ? "" : statement;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == '(') {
                i++;
            } else if (c == '-' && next(s, i) == '-') {
                int end = s.indexOf('\n', i);
                if (end < 0) return "";
                i = end + 1;
            } else if (c == '/' && next(s, i) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end < 0) return "";
                i = end + 2;
            } else {
                break;
            }
        }
        return s.substring(i);
    }

    /**
     * Expands a {@code psql} shorthand command into a portable catalog query.
     *
     * @param statement the statement typed by the user
     * @return the SQL to run instead, or {@code null} when this is not shorthand
     */
    public static String expandPsqlShorthand(String statement) {
        String s = statement == null ? "" : statement.trim();
        if (!s.startsWith("\\")) {
            return null;
        }
        String[] parts = s.split("\\s+", 2);
        String command = parts[0].substring(1);
        String argument = parts.length > 1 ? parts[1].trim() : "";
        boolean verbose = command.endsWith("+");
        if (verbose) {
            command = command.substring(0, command.length() - 1);
        }
        switch (command) {
            case "l":
            case "list":
                return (
                    "SELECT d.datname AS \"Name\", pg_catalog.pg_get_userbyid(d.datdba) AS " +
                    "\"Owner\", pg_catalog.pg_encoding_to_char(d.encoding) AS \"Encoding\" " +
                    "FROM pg_catalog.pg_database d ORDER BY 1"
                );
            case "dn":
                return (
                    "SELECT n.nspname AS \"Name\", pg_catalog.pg_get_userbyid(n.nspowner) AS " +
                    "\"Owner\" FROM pg_catalog.pg_namespace n WHERE n.nspname !~ '^pg_' " +
                    "AND n.nspname <> 'information_schema' ORDER BY 1"
                );
            case "du":
            case "dg":
                return (
                    "SELECT r.rolname AS \"Role name\", r.rolsuper AS \"Superuser\", " +
                    "r.rolcreatedb AS \"Create DB\", r.rolcanlogin AS \"Login\" " +
                    "FROM pg_catalog.pg_roles r WHERE r.rolname !~ '^pg_' ORDER BY 1"
                );
            case "conninfo":
                return (
                    "SELECT current_database() AS \"Database\", current_user AS \"User\", " +
                    "current_schema() AS \"Schema\", version() AS \"Version\""
                );
            case "d":
                return argument.isEmpty() ? relationsQuery("rvmSf") : describeRelation(argument);
            case "dt":
                return relationsQuery("r");
            case "dv":
                return relationsQuery("v");
            case "dm":
                return relationsQuery("m");
            case "ds":
                return relationsQuery("S");
            case "di":
                return relationsQuery("i");
            case "df":
                return (
                    "SELECT n.nspname AS \"Schema\", p.proname AS \"Name\", " +
                    "pg_catalog.pg_get_function_result(p.oid) AS \"Result data type\", " +
                    "pg_catalog.pg_get_function_arguments(p.oid) AS \"Argument data types\" " +
                    "FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n " +
                    "ON n.oid = p.pronamespace WHERE n.nspname NOT IN " +
                    "('pg_catalog', 'information_schema') ORDER BY 1, 2"
                );
            default:
                return null;
        }
    }

    private static String relationsQuery(String relKinds) {
        StringBuilder kinds = new StringBuilder();
        for (char k : relKinds.toCharArray()) {
            if (kinds.length() > 0) kinds.append(", ");
            kinds.append('\'').append(k).append('\'');
        }
        return (
            "SELECT n.nspname AS \"Schema\", c.relname AS \"Name\", CASE c.relkind " +
            "WHEN 'r' THEN 'table' WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized view' " +
            "WHEN 'i' THEN 'index' WHEN 'S' THEN 'sequence' WHEN 'f' THEN 'foreign table' " +
            "WHEN 'p' THEN 'partitioned table' ELSE c.relkind::text END AS \"Type\", " +
            "pg_catalog.pg_get_userbyid(c.relowner) AS \"Owner\" " +
            "FROM pg_catalog.pg_class c LEFT JOIN pg_catalog.pg_namespace n " +
            "ON n.oid = c.relnamespace WHERE c.relkind IN (" +
            kinds +
            ") AND n.nspname NOT IN ('pg_catalog', 'information_schema') " +
            "AND n.nspname !~ '^pg_toast' ORDER BY 1, 2"
        );
    }

    /** Column listing for {@code \d <relation>}; the name is passed as a literal. */
    private static String describeRelation(String relation) {
        String schema = null;
        String name = relation;
        int dot = relation.indexOf('.');
        if (dot > 0) {
            schema = unquoteIdentifier(relation.substring(0, dot));
            name = relation.substring(dot + 1);
        }
        name = unquoteIdentifier(name);
        StringBuilder sql = new StringBuilder(
            "SELECT c.column_name AS \"Column\", c.data_type AS \"Type\", " +
            "c.is_nullable AS \"Nullable\", c.column_default AS \"Default\" " +
            "FROM information_schema.columns c WHERE c.table_name = " +
            quoteLiteral(name)
        );
        if (schema != null) {
            sql.append(" AND c.table_schema = ").append(quoteLiteral(schema));
        }
        sql.append(" ORDER BY c.ordinal_position");
        return sql.toString();
    }

    private static String unquoteIdentifier(String identifier) {
        String id = identifier.trim();
        if (id.length() > 1 && id.startsWith("\"") && id.endsWith("\"")) {
            return id.substring(1, id.length() - 1).replace("\"\"", "\"");
        }
        return id.toLowerCase(Locale.ROOT);
    }

    private static String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
