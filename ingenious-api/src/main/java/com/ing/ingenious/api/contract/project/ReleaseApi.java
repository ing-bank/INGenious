package com.ing.ingenious.api.contract.project;

/**
 * Contract for a project release, backed by the framework's {@code Release}.
 */
public interface ReleaseApi {

    /**
     * @param name the test set name
     * @return the test set with the given name, or {@code null} if not found
     */
    TestSetApi getTestSetByName(String name);
}
