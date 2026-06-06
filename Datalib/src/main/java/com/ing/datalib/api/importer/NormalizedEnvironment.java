package com.ing.datalib.api.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An environment (named set of variables) in the normalized import model.
 */
public class NormalizedEnvironment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private List<NormalizedVariable> variables = new ArrayList<>();

    public NormalizedEnvironment() {}

    public NormalizedEnvironment(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<NormalizedVariable> getVariables() { return variables; }
    public void setVariables(List<NormalizedVariable> variables) { this.variables = variables; }
}
