
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
public class MetaList extends ArrayList<Meta> {

    @Override
    public boolean add(Meta a) {
        if (!contains(a.getType(), a.getName())) {
            return super.add(a);
        }
        return false;
    }

    public boolean add(Object name, Object value) {
        return add(Meta.create(Objects.toString(name), Objects.toString(value)));
    }

    public boolean addAll(List<Meta> a) {
        if (Objects.nonNull(a)) {
            return super.addAll(a);
        }
        return false;
    }

    public Meta get(String type, String name) {
        return find(type, name).get();
    }

    public boolean contains(String type, String name) {
        return find(type, name).isPresent();
    }

    public Optional<Meta> find(Meta attr) {
        return filter(attr).findFirst();
    }

    private Stream<Meta> filter(Meta attr) {
        return stream().filter(a -> a.equals(attr));
    }

    public Optional<Meta> find(String type, String name) {
        return filter(type, name).findFirst();
    }

    private Stream<Meta> filter(String type, String name) {
        return stream().filter(m -> equals(m::getType, type) && equals(m::getName, name));
    }

    public static boolean equals(Supplier<Object> supplier, Object val) {
        return Objects.equals(supplier.get(), val);
    }

    public void update(Meta a) {
        if (Objects.nonNull(a)) {
            Optional<Meta> attRes = find(a.getType(), a.getName());
            if (attRes.isPresent()) {
                attRes.get().setName(a.getName());
                attRes.get().setName(a.getName());
                attRes.get().setRef(a.getRef());
                attRes.get().setTags(a.getTags());
                attRes.get().setAttributes(a.getAttributes());
            } else {
                this.add(a);
            }
        }
    }

    public void remove(String type, String name) {
        find(type, name).ifPresent(this::remove);
    }

    public void remove(Tag t) {
        remove(Meta.Props.tag.name(), t.getValue());
    }
}
