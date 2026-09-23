package com.ing.engine.drivers.sap;

import java.util.List;

/**
 * A single SAP GUI element (a {@code GuiComponent}). The seam that keeps
 * {@code com.jacob.*} out of the action layer. The real implementation
 * ({@code JacobSapElement}) wraps a {@code com.jacob.com.Dispatch}; test fakes
 * back it with an in-memory tree.
 */
public interface SapElement {
    /** Resolve a child by id relative to this element; {@code null} when absent. */
    SapElement findById(String id);

    List<SapElement> children();

    int childCount();

    SapElement child(int index);

    String text();

    String getProperty(String name);

    void setProperty(String name, Object value);

    void invoke(String method, Object... args);

    Object get(String property);

    /** The underlying {@code com.jacob.com.Dispatch}, or {@code null} for a fake. */
    Object raw();
}
