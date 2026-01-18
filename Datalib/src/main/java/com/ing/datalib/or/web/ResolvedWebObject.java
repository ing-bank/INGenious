
package com.ing.datalib.or.web;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR.ORScope;

public class ResolvedWebObject {
    private final ORScope scope;
    private final String pageName;
    private final String objectName;
    private final ObjectGroup<WebORObject> group;

    public ResolvedWebObject(ORScope scope, String pageName, String objectName, ObjectGroup<WebORObject> group) {
        this.scope = scope;
        this.pageName = pageName;
        this.objectName = objectName;
        this.group = group;
    }

    public static final class PageRef {
        public final String name;
        public final ORScope scope;

        public PageRef(String name, ORScope scope) {
            this.name = name;
            this.scope = scope;
        }

        public String qualified() {
            return name + "@" + scope.name();
        }

        public static PageRef parse(String token) {
            String s = token == null ? "" : token.trim();
            int at = s.lastIndexOf('@');
            if (at <= 0 || at == s.length() - 1) {
                return new PageRef(s, ORScope.PROJECT);
            }
            String base = s.substring(0, at).trim();
            String suf  = s.substring(at + 1).trim().toUpperCase();
            ORScope sc = "SHARED".equals(suf) ? ORScope.SHARED : ORScope.PROJECT;
            return new PageRef(base, sc);
        }
    }

    public ORScope getScope() { return scope; }
    public String getPageName() { return pageName; }
    public String getObjectName() { return objectName; }
    public ObjectGroup<WebORObject> getGroup() { return group; }

    public WebORObject getObject() {
        return (group != null && !group.getObjects().isEmpty()) ? group.getObjects().get(0) : null;
    }

    public boolean isFromProject() { return scope == ORScope.PROJECT; }
    public boolean isFromShared()  { return scope == ORScope.SHARED; }
    public boolean isPresent()     { return group != null && !group.getObjects().isEmpty(); }

    public String debugString() {
        return "ResolvedWebObject{scope=" + scope +
               ", page='" + pageName + '\'' +
               ", object='" + objectName + '\'' +
               ", objectCount=" + (group == null ? 0 : group.getObjects().size()) +
               '}';
    }
}