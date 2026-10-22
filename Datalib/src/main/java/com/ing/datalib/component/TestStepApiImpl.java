package com.ing.datalib.component;

import com.ing.ingenious.api.contract.project.TestStepApi;

/**
 * Delegates the {@link TestStepApi} plugin contract to a specific
 * {@link ExecutionStep} instance.
 */
public final class TestStepApiImpl implements TestStepApi {
    private final ExecutionStep step;

    public TestStepApiImpl(ExecutionStep step) {
        this.step = step;
    }

    @Override
    public String getTestScenarioName() {
        return step.getTestScenarioName();
    }

    @Override
    public String getTestCaseName() {
        return step.getTestCaseName();
    }

    @Override
    public String getIteration() {
        return step.getIteration();
    }

    @Override
    public String getStatus() {
        return step.getStatus();
    }

    @Override
    public String getExecute() {
        return step.getExecute();
    }

    @Override
    public String getPlatform() {
        return step.getPlatform();
    }

    @Override
    public String getBrowser() {
        return step.getBrowser();
    }
}
