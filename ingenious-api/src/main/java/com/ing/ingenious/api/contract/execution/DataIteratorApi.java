package com.ing.ingenious.api.contract.execution;

/**
 * Contract for a resolved data-iteration bound for a single scenario/test
 * case, backed by the framework's {@code DataIterator}.
 */
public interface DataIteratorApi {

    /** @return whether any data sheet has registered its iteration count yet */
    boolean isResolved();

    /** @return the highest resolved iteration bound, at least 1 */
    int getMaxIter();

    /**
     * @param sheet the data sheet name
     * @return whether the given sheet has registered its iteration count
     */
    boolean isIterResolved(String sheet);
}
