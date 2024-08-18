
package com.ing.datalib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 *
 * 
 */
public class Data extends ArrayList<DataItem> {

    @Override
    public boolean add(DataItem a) {
        if (!contains(a.getName())) {
            return super.add(a);
        }
        return false;
    }

    public boolean addAll(List<DataItem> a) {
        if (Objects.nonNull(a)) {
            return super.addAll(a);
        }
        return false;
    }

    public DataItem getByName(String name) {
        return find(name).get();
    }

    public boolean contains(String name) {
        return find(name).isPresent();
    }

    public Optional<DataItem> find(String name) {
        return stream().filter(attr -> equals(attr::getName, name))
                .findFirst();
    }

    public Optional<DataItem> find(String name, String aName, String aVal) {
        return stream().filter(data -> equals(data::getName, name)
                && data.getAttributes().contains(aName, aVal))
                .findFirst();
    }

    public Optional<DataItem> find(String name, String scn) {
        return find(name, Meta.Attributes.scenario.name(), scn);
    }

    public DataItem findOrCreate(String name, String scn) {
        Optional<DataItem> find = find(name, scn);
        if (find.isPresent()) {
            return find.get();
        } else {
            DataItem newItem = DataItem.createTestCase(name, scn);
            add(newItem);
            return newItem;
        }
    }

    public static boolean equals(Supplier<Object> supplier, Object val) {
        return Objects.equals(supplier.get(), val);
    }
}
