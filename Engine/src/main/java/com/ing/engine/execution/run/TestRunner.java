package com.ing.engine.execution.run;

/**
 *
 *
 */
public interface TestRunner {
    public Object runEnv();

    /**
     * Environment name to resolve {@code [Shared]}-scoped test data against. Mirrors
     * {@link #runEnv()} - Shared Test Data always runs against the same Environment selected
     * for the Project.
     */
    public Object sharedRunEnv();

    public Object dataProvider();

    public Object getProject();

    public boolean isContinueOnError();
}
