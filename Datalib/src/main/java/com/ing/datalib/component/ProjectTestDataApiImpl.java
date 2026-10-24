package com.ing.datalib.component;

import com.ing.ingenious.api.contract.project.ProjectTestDataApi;
import com.ing.ingenious.api.contract.project.TestDataSetApi;

/**
 * Delegates the {@link ProjectTestDataApi} plugin contract to a specific
 * {@link EnvTestData} instance.
 */
public final class ProjectTestDataApiImpl implements ProjectTestDataApi {
    private final EnvTestData testData;

    public ProjectTestDataApiImpl(EnvTestData testData) {
        this.testData = testData;
    }

    @Override
    public String defEnv() {
        return testData.defEnv();
    }

    @Override
    public TestDataSetApi getTestDataFor(String environment) {
        TestData data = testData.getTestDataFor(environment);
        return data == null ? null : new TestDataSetApiImpl(data);
    }
}
