package com.ing.ide.main.mainui.components.testdesign.or.clipboard;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds OR objects in clipboard along with
 * the action type (copy or cut).
 *
 * This is a UI-level helper class.
 */
public class ORObjectClipboard {

    public enum Type {
        OBJECT,
        PAGE
    }

    private final Object data; // ORObjectInf, List<ORObjectInf>, ORPageInf, or List<ORPageInf>
    private final boolean cut;
    private final Type type;

    public ORObjectClipboard(ORObjectInf object, boolean cut) {
        this.data = object;
        this.cut = cut;
        this.type = Type.OBJECT;
    }

    public ORObjectClipboard(List<ORObjectInf> objects, boolean cut) {
        this.data = new ArrayList<>(objects);
        this.cut = cut;
        this.type = Type.OBJECT;
    }

    public ORObjectClipboard(ORPageInf page, boolean cut) {
        this.data = page;
        this.cut = cut;
        this.type = Type.PAGE;
    }

    // Private constructor for creating multi-page clipboard
    private ORObjectClipboard(List<ORPageInf> pages, boolean cut, boolean isPageList) {
        this.data = new ArrayList<>(pages);
        this.cut = cut;
        this.type = Type.PAGE;
    }

    // Factory method for multiple pages
    public static ORObjectClipboard forPages(List<ORPageInf> pages, boolean cut) {
        return new ORObjectClipboard(pages, cut, true);
    }

    public boolean isCut() {
        return cut;
    }

    public Type getType() {
        return type;
    }

    public ORObjectInf getObject() {
        if (data instanceof List) {
            List<ORObjectInf> list = (List<ORObjectInf>) data;
            return list.isEmpty() ? null : list.get(0);
        }
        return (ORObjectInf) data;
    }

    public List<ORObjectInf> getObjects() {
        if (data instanceof List) {
            return Collections.unmodifiableList((List<ORObjectInf>) data);
        }
        return Collections.singletonList((ORObjectInf) data);
    }

    public boolean hasMultipleObjects() {
        return data instanceof List && ((List<?>) data).size() > 1;
    }

    public ORPageInf getPage() {
        if (data instanceof List) {
            List<ORPageInf> list = (List<ORPageInf>) data;
            return list.isEmpty() ? null : list.get(0);
        }
        return (ORPageInf) data;
    }

    public List<ORPageInf> getPages() {
        if (type == Type.PAGE && data instanceof List) {
            return Collections.unmodifiableList((List<ORPageInf>) data);
        }
        if (type == Type.PAGE && data instanceof ORPageInf) {
            return Collections.singletonList((ORPageInf) data);
        }
        return Collections.emptyList();
    }

    public boolean hasMultiplePages() {
        return type == Type.PAGE && data instanceof List && ((List<?>) data).size() > 1;
    }
}
