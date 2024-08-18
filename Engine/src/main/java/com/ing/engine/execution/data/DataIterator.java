
package com.ing.engine.execution.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * 
 */
public class DataIterator {

    private final Map<String, Integer> dataIter;
    private int maxIter = -1;

    public DataIterator() {
        dataIter = new HashMap<>();
    }

    public void setMaxIter(int n) {
        maxIter = Math.max(1, n);
    }

    public boolean isIterResolved(String sheet) {
        return dataIter.containsKey(sheet);
    }

    public synchronized void setIter(String sheet, Set<String> iter) {
        dataIter.put(sheet, iter.size());
        maxIter = (maxIter <= 1) ? iter.size() : Math.min(iter.size(), maxIter);
    }

    public Integer getMaxIter() {
        return Math.max(1, maxIter);
    }


    @Override
    public String toString() {
        return String.format("MaxIter:%s", maxIter);
    }
}
