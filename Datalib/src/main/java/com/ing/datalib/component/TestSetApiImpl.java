package com.ing.datalib.component;

import com.ing.datalib.settings.ExecutionSettingsApiImpl;
import com.ing.ingenious.api.contract.project.TestSetApi;
import com.ing.ingenious.api.contract.project.TestStepApi;
import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delegates the {@link TestSetApi} plugin contract to a specific
 * {@link TestSet} instance.
 */
public final class TestSetApiImpl implements TestSetApi {
    private final TestSet testSet;

    public TestSetApiImpl(TestSet testSet) {
        this.testSet = testSet;
    }

    @Override
    public List<TestStepApi> getTestSteps() {
        return testSet
            .getTestSteps()
            .stream()
            .map(TestStepApiImpl::new)
            .collect(Collectors.toList());
    }

    @Override
    public ExecutionSettingsApi getExecSettings() {
        return new ExecutionSettingsApiImpl(testSet.getExecSettings());
    }
}
