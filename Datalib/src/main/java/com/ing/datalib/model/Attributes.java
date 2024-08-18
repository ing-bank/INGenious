
package com.ing.datalib.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * 
 */
public class Attributes extends ArrayList<Attribute> {

    @Override
    public boolean add(Attribute a) {
        if (!contains(a.getName())) {
            return super.add(a);
        }
        return false;
    }

    public boolean add(Object name, Object value) {
        return add(Attribute.create(Objects.toString(name), Objects.toString(value)));
    }

    public boolean addAll(List<Attribute> a) {
        if (Objects.nonNull(a)) {
            return super.addAll(a);
        }
        return false;
    }

    public Attribute get(String name) {
        return find(name).get();
    }

    public boolean contains(String name) {
        return find(name).isPresent();
    }

    public boolean contains(String name, String value) {
        return find(Attribute.create(name, value)).isPresent();
    }

    public Optional<Attribute> find(Attribute attr) {
        return filter(attr).findFirst();
    }

    private Stream<Attribute> filter(Attribute attr) {
        return stream().filter(a -> a.equals(attr));
    }

    public Optional<Attribute> find(String name) {
        return filter(name).findFirst();
    }

    private Stream<Attribute> filter(String name) {
        return stream().filter(attr -> equals(attr::getName, name));
    }

    public static boolean equals(Supplier<Object> supplier, Object val) {
        return Objects.equals(supplier.get(), val);
    }

    public void update(Attribute a) {
        if (Objects.nonNull(a)) {
            Optional<Attribute> attRes = find(a.getName());
            if (attRes.isPresent()) {
                attRes.get().setValue(a.getValue());
            } else {
                this.add(a);
            }
        }
    }
}
