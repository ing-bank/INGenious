package com.ing.datalib.api.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of a collection import. Pure data holder.
 */
public class ImportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private int requestsRead;
    private int reusablesCreated;
    private int reusablesSkipped;
    private int environmentsCreated;
    private final List<String> createdScenarios = new ArrayList<>();
    private final List<String> createdReusables = new ArrayList<>();
    private final List<ImportWarning> warnings = new ArrayList<>();

    public int getRequestsRead() {
        return requestsRead;
    }

    public void setRequestsRead(int requestsRead) {
        this.requestsRead = requestsRead;
    }

    public int getReusablesCreated() {
        return reusablesCreated;
    }

    public void incReusablesCreated() {
        this.reusablesCreated++;
    }

    public int getReusablesSkipped() {
        return reusablesSkipped;
    }

    public void incReusablesSkipped() {
        this.reusablesSkipped++;
    }

    public int getEnvironmentsCreated() {
        return environmentsCreated;
    }

    public void incEnvironmentsCreated() {
        this.environmentsCreated++;
    }

    public List<String> getCreatedScenarios() {
        return createdScenarios;
    }

    public List<String> getCreatedReusables() {
        return createdReusables;
    }

    public List<ImportWarning> getWarnings() {
        return warnings;
    }
}
