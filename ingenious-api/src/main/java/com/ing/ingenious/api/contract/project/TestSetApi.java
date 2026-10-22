package com.ing.ingenious.api.contract.project;

import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;
import java.util.List;

/**
 * Contract for a test set, backed by the framework's {@code TestSet}.
 */
public interface TestSetApi {

    /** @return the test steps in this test set */
    List<TestStepApi> getTestSteps();

    /** @return the execution settings configured for this test set */
    ExecutionSettingsApi getExecSettings();
}
