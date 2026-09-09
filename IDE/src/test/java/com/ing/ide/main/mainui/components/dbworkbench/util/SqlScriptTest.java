package com.ing.ide.main.mainui.components.dbworkbench.util;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import org.testng.annotations.Test;

/**
 * Tests for the Database Workbench SQL parsing helpers: statement splitting,
 * statement classification and psql shorthand expansion.
 */
public class SqlScriptTest {

    @Test
    public void splitsOnUnquotedSemicolons() {
        List<String> statements = SqlScript.split("SELECT 1; SELECT 2;");
        assertEquals(statements.size(), 2);
        assertEquals(statements.get(0), "SELECT 1");
        assertEquals(statements.get(1), "SELECT 2");
    }

    @Test
    public void keepsSemicolonsInsideStringLiterals() {
        List<String> statements = SqlScript.split("SELECT 'a;b' FROM t");
        assertEquals(statements.size(), 1);
        assertEquals(statements.get(0), "SELECT 'a;b' FROM t");
    }

    @Test
    public void keepsSemicolonsInsideQuotedIdentifiers() {
        List<String> statements = SqlScript.split("SELECT \"we;ird\" FROM t");
        assertEquals(statements.size(), 1);
    }

    @Test
    public void keepsSemicolonsInsideComments() {
        List<String> statements = SqlScript.split("SELECT 1 -- a; b\n; SELECT 2");
        assertEquals(statements.size(), 2);
    }

    @Test
    public void keepsSemicolonsInsideDollarQuotedBodies() {
        String script =
            "CREATE FUNCTION f() RETURNS int AS $$ BEGIN RETURN 1; END; $$ LANGUAGE plpgsql; SELECT 1";
        List<String> statements = SqlScript.split(script);
        assertEquals(statements.size(), 2);
        assertEquals(statements.get(1), "SELECT 1");
    }

    @Test
    public void ignoresTrailingAndEmptyStatements() {
        assertEquals(SqlScript.split("  ;; SELECT 1 ; ; ").size(), 1);
        assertTrue(SqlScript.split("   ").isEmpty());
        assertTrue(SqlScript.split(null).isEmpty());
    }

    @Test
    public void classifiesReadsAndWrites() {
        assertEquals(SqlScript.classify("SELECT * FROM t"), SqlScript.Kind.QUERY);
        assertEquals(
            SqlScript.classify("WITH x AS (SELECT 1) SELECT * FROM x"),
            SqlScript.Kind.QUERY
        );
        assertEquals(SqlScript.classify("insert into t values (1)"), SqlScript.Kind.DML);
        assertEquals(SqlScript.classify("CREATE TABLE t (a int)"), SqlScript.Kind.DDL);
        assertEquals(SqlScript.classify("DROP TABLE t"), SqlScript.Kind.DDL);
        assertEquals(SqlScript.classify("CREATE DATABASE db"), SqlScript.Kind.DDL);
        assertEquals(SqlScript.classify("COMMIT"), SqlScript.Kind.TRANSACTION);
    }

    @Test
    public void ddlAndDmlCountAsWrites() {
        assertTrue(SqlScript.classify("CREATE TABLE t (a int)").isWrite());
        assertTrue(SqlScript.classify("DROP TABLE t").isWrite());
        assertTrue(SqlScript.classify("UPDATE t SET a = 1").isWrite());
        assertFalse(SqlScript.classify("SELECT 1").isWrite());
    }

    @Test
    public void classifiesThroughLeadingComments() {
        assertEquals(SqlScript.classify("-- load the rows\nSELECT * FROM t"), SqlScript.Kind.QUERY);
        assertEquals(SqlScript.classify("/* setup */ CREATE TABLE t (a int)"), SqlScript.Kind.DDL);
    }

    @Test
    public void detectsStatementsThatCannotRunInATransaction() {
        assertTrue(SqlScript.requiresAutoCommit("CREATE DATABASE demo"));
        assertTrue(SqlScript.requiresAutoCommit("drop   database demo"));
        assertTrue(SqlScript.requiresAutoCommit("ALTER DATABASE demo RENAME TO other"));
        assertFalse(SqlScript.requiresAutoCommit("CREATE TABLE t (a int)"));
        assertFalse(SqlScript.requiresAutoCommit("SELECT 1"));
    }

    @Test
    public void expandsPsqlShorthand() {
        assertTrue(SqlScript.expandPsqlShorthand("\\l").contains("pg_database"));
        assertTrue(SqlScript.expandPsqlShorthand("\\dt").contains("pg_class"));
        assertTrue(SqlScript.expandPsqlShorthand("\\dn").contains("pg_namespace"));
        assertTrue(SqlScript.expandPsqlShorthand("\\du").contains("pg_roles"));
        assertTrue(SqlScript.expandPsqlShorthand("\\d").contains("pg_class"));
        assertTrue(SqlScript.expandPsqlShorthand("\\conninfo").contains("current_database"));
    }

    @Test
    public void expandsDescribeWithRelationName() {
        String sql = SqlScript.expandPsqlShorthand("\\d public.users");
        assertTrue(sql.contains("information_schema.columns"));
        assertTrue(sql.contains("'users'"));
        assertTrue(sql.contains("'public'"));
    }

    @Test
    public void escapesQuotesInRelationNames() {
        String sql = SqlScript.expandPsqlShorthand("\\d it's");
        assertTrue(sql.contains("'it''s'"));
    }

    @Test
    public void returnsNullForNonShorthand() {
        assertNull(SqlScript.expandPsqlShorthand("SELECT 1"));
        assertNull(SqlScript.expandPsqlShorthand("\\unknown"));
    }
}
