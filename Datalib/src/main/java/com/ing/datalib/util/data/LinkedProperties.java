
package com.ing.datalib.util.data;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Properties;

public class LinkedProperties extends Properties {

    private final HashSet<Object> keys = new LinkedHashSet<>();

    public LinkedProperties() {
    }

    public Iterable<Object> orderedKeys() {
        return Collections.list(keys());
    }

    @Override
    public Enumeration<Object> keys() {
        return Collections.<Object>enumeration(keys);
    }

    @Override
    public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

    public Object update(Object key, Object value) {
        keys.remove(key);
        keys.add(key);
        return super.put(key, value);
    }

    @Override
    public synchronized void clear() {
        super.clear();
        keys.clear();
    }

}
