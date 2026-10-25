package com.ing.datalib.component;

import com.ing.ingenious.api.contract.project.ReleaseApi;
import com.ing.ingenious.api.contract.project.TestSetApi;

/**
 * Delegates the {@link ReleaseApi} plugin contract to a specific
 * {@link Release} instance.
 */
public final class ReleaseApiImpl implements ReleaseApi {
    private final Release release;

    public ReleaseApiImpl(Release release) {
        this.release = release;
    }

    @Override
    public TestSetApi getTestSetByName(String name) {
        TestSet testSet = release.getTestSetByName(name);
        return testSet == null ? null : new TestSetApiImpl(testSet);
    }
}
