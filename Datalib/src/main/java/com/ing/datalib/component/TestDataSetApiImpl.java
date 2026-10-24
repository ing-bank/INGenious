package com.ing.datalib.component;

import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.ingenious.api.contract.data.TestDataViewApi;
import com.ing.ingenious.api.contract.project.TestDataSetApi;
import java.util.List;

/**
 * Delegates the {@link TestDataSetApi} plugin contract to a specific
 * {@link TestData} instance.
 */
public final class TestDataSetApiImpl implements TestDataSetApi {
    private final TestData testData;

    public TestDataSetApiImpl(TestData testData) {
        this.testData = testData;
    }

    @Override
    public List<String> getTestDataNames() {
        return testData.getTestDataNames();
    }

    @Override
    public TestDataViewApi getByName(String name) {
        TestDataModel model = testData.getByName(name);
        return model == null ? null : model.view();
    }
}
