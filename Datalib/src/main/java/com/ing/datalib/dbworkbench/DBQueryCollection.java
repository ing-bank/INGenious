package com.ing.datalib.dbworkbench;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A named folder of {@link DBQuery} objects, persisted as one JSON file under the
 * project's {@code db-workbench/collections/} folder. Mirrors {@code APICollection}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DBQueryCollection implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private List<DBQuery> queries;
    private long createdAt;
    private long updatedAt;

    public DBQueryCollection() {
        this.id = UUID.randomUUID().toString();
        this.queries = new ArrayList<>();
        this.createdAt = Instant.now().toEpochMilli();
        this.updatedAt = this.createdAt;
    }

    public DBQueryCollection(String name) {
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
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public List<DBQuery> getQueries() {
        return queries;
    }

    public void setQueries(List<DBQuery> queries) {
        this.queries = queries;
        this.updatedAt = Instant.now().toEpochMilli();
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

    @Override
    public String toString() {
        return name != null ? name : "Untitled Collection";
    }
}
