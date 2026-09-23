package com.ing.engine.drivers.sap;

import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import java.util.ArrayList;
import java.util.List;

/** JACOB-backed {@link SapElement} wrapping a {@code Dispatch}. */
final class JacobSapElement implements SapElement {
    private final Dispatch dispatch;

    JacobSapElement(Dispatch dispatch) {
        this.dispatch = dispatch;
    }

    @Override
    public SapElement findById(String id) {
        try {
            Dispatch d = Dispatch.call(dispatch, "FindById", id).toDispatch();
            return d == null ? null : new JacobSapElement(d);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public List<SapElement> children() {
        List<SapElement> out = new ArrayList<>();
        int n = childCount();
        for (int i = 0; i < n; i++) {
            out.add(child(i));
        }
        return out;
    }

    @Override
    public int childCount() {
        Dispatch kids = Dispatch.call(dispatch, "Children").toDispatch();
        return Dispatch.get(kids, "Count").getInt();
    }

    @Override
    public SapElement child(int index) {
        Dispatch kids = Dispatch.call(dispatch, "Children").toDispatch();
        return new JacobSapElement(Dispatch.call(kids, "Item", index).toDispatch());
    }

    @Override
    public String text() {
        return Dispatch.get(dispatch, "Text").toString();
    }

    @Override
    public String getProperty(String name) {
        Variant v = Dispatch.get(dispatch, name);
        return v == null ? null : v.toString();
    }

    @Override
    public void setProperty(String name, Object value) {
        Dispatch.put(dispatch, name, value);
    }

    @Override
    public void invoke(String method, Object... args) {
        Dispatch.call(dispatch, method, args);
    }

    @Override
    public Object get(String property) {
        return Dispatch.get(dispatch, property);
    }

    @Override
    public Object raw() {
        return dispatch;
    }
}
