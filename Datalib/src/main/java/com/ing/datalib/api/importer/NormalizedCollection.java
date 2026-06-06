package com.ing.datalib.api.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Root of a parsed collection.
 */
public class NormalizedCollection implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private ImportSource source;
    private List<NormalizedVariable> variables = new ArrayList<>();
    private List<NormalizedRequest> requests = new ArrayList<>();
    private List<NormalizedEnvironment> environments = new ArrayList<>();

    public NormalizedCollection() {}

    public NormalizedCollection(String name, ImportSource source) {
        this.name = name;
        this.source = source;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ImportSource getSource() { return source; }
    public void setSource(ImportSource source) { this.source = source; }

    public List<NormalizedVariable> getVariables() { return variables; }
    public void setVariables(List<NormalizedVariable> variables) { this.variables = variables; }

    public List<NormalizedRequest> getRequests() { return requests; }
    public void setRequests(List<NormalizedRequest> requests) { this.requests = requests; }

    public List<NormalizedEnvironment> getEnvironments() { return environments; }
    public void setEnvironments(List<NormalizedEnvironment> environments) { this.environments = environments; }
}
