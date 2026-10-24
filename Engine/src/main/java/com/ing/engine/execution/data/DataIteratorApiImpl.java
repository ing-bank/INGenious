package com.ing.engine.execution.data;

import com.ing.ingenious.api.contract.execution.DataIteratorApi;

/**
 * Delegates the {@link DataIteratorApi} plugin contract to a specific
 * {@link DataIterator} instance.
 */
public final class DataIteratorApiImpl implements DataIteratorApi {
    private final DataIterator dataIterator;

    public DataIteratorApiImpl(DataIterator dataIterator) {
        this.dataIterator = dataIterator;
    }

    @Override
    public boolean isResolved() {
        return dataIterator.isResolved();
    }

    @Override
    public int getMaxIter() {
        return dataIterator.getMaxIter();
    }

    @Override
    public boolean isIterResolved(String sheet) {
        return dataIterator.isIterResolved(sheet);
    }
}
