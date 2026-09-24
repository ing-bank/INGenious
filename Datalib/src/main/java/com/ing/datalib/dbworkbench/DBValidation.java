package com.ing.datalib.dbworkbench;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.UUID;

/**
 * A single validation/store rule built from a Database Workbench result grid.
 * <p>
 * Each rule maps to one INGenious {@code Database} test step when the query is
 * converted to automation (see {@code DBWorkbench.buildStepsForQuery}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBValidation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The kind of rule; determines the Engine action emitted during conversion.
     */
    public enum Operator {
        /** Assert an exact value exists in a column (optionally at a row). */
        EQUALS,
        /** Assert a value exists anywhere in a column. */
        CONTAINS,
        /** Assert the column contains the expected value. */
        EXISTS,
        /** Assert the value at a specific column+row equals the expected value exactly. */
        CELL_EQUALS,
        /** Assert the value at a specific column+row is SQL NULL. */
        IS_NULL,
        /** Assert the value at a specific column+row is not SQL NULL. */
        IS_NOT_NULL,
        /** Assert the query returned exactly the expected number of rows. */
        ROW_COUNT,
        /** Store a cell value into a runtime variable ({@code %var%}). */
        STORE_VAR,
        /** Store a cell value into a global variable ({@code %var%}). */
        STORE_GLOBAL,
        /** Store a cell value into a data sheet ({@code Sheet:Column}). */
        STORE_SHEET
    }

    private String id;
    private String column;
    private int row; // 1-based; 0 or 1 means first row
    private Operator operator;
    private String expectedValue; // expected value, or target var/sheet ref for STORE_*
    private boolean enabled;

    public DBValidation() {
        this.id = UUID.randomUUID().toString();
        this.operator = Operator.EQUALS;
        this.row = 1;
        this.enabled = true;
    }

    public DBValidation(String column, int row, Operator operator, String expectedValue) {
        this();
        this.column = column;
        this.row = row;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
