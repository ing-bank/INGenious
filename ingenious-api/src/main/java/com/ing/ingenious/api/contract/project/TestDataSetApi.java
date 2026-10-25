package com.ing.ingenious.api.contract.project;

import com.ing.ingenious.api.contract.data.TestDataViewApi;
import java.util.List;

/**
 * Contract for a single environment's test data set, backed by the
 * framework's {@code TestData}.
 */
public interface TestDataSetApi {

    /** @return the names of the data sheets in this test data set */
    List<String> getTestDataNames();

    /**
     * @param name the data sheet name
     * @return a view over the named data sheet, or {@code null} if not found
     */
    TestDataViewApi getByName(String name);
}
