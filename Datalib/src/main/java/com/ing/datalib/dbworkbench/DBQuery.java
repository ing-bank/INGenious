package com.ing.datalib.dbworkbench;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A saved Database Workbench query: a named SQL statement bound to a connection
 * alias, plus the validations/store rules built from its result grid.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String connectionAlias; // maps to a Settings/Databases/<alias>.properties
    private String sql;
    private boolean dml; // true = INSERT/UPDATE/DELETE, false = SELECT
    private List<DBValidation> validations;
    private long createdAt;
    private long updatedAt;

    public DBQuery() {
        this.id = UUID.randomUUID().toString();
        this.validations = new ArrayList<>();
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public DBQuery(String name) {
        this();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        touch();
    }

    public String getConnectionAlias() {
        return connectionAlias;
    }

    public void setConnectionAlias(String connectionAlias) {
        this.connectionAlias = connectionAlias;
        touch();
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
        touch();
    }

    public boolean isDml() {
        return dml;
    }

    public void setDml(boolean dml) {
        this.dml = dml;
        touch();
    }

    public List<DBValidation> getValidations() {
        return validations;
    }

    public void setValidations(List<DBValidation> validations) {
        this.validations = validations;
        touch();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    @Override
    public String toString() {
        return name != null ? name : "Untitled Query";
    }
}
