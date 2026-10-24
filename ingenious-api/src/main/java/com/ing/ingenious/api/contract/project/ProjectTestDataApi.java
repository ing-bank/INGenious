package com.ing.ingenious.api.contract.project;

/**
 * Contract for a project's environment-scoped test data, backed by the
 * framework's {@code EnvTestData}.
 * <p>
 * This is distinct from
 * {@link com.ing.ingenious.api.contract.data.UserDataAccessApi#getTestData(String)},
 * which implicitly reaches into the currently executing scenario/testcase/
 * iteration context. Use this interface to explicitly reach into a
 * different environment's data, and {@code UserDataAccessApi} for the
 * currently executing context.
 * </p>
 */
public interface ProjectTestDataApi {

    /** @return the name of the project's default environment */
    String defEnv();

    /**
     * @param environment the environment name
     * @return the test data set for the given environment
     */
    TestDataSetApi getTestDataFor(String environment);
}
