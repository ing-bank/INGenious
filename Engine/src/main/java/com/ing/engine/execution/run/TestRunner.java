package com.ing.engine.execution.run;

/**
 *
 *
 */
public interface TestRunner {
    public Object runEnv();

    /**
     * Environment name to resolve {@code [Shared]}-scoped test data against. Distinct from
     * {@link #runEnv()} so Shared Test Data can be targeted at an environment the project does
     * not define (and vice versa).
     */
    public Object sharedRunEnv();

    public Object dataProvider();

    public Object getProject();

    public boolean isContinueOnError();
}
