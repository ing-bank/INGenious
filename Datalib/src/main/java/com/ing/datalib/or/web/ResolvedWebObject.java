
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
            if (null == scope) {
                return name;
            } 
            else switch (scope) {
                case PROJECT:
                    return "[Project] " + name;
                case SHARED:
                    return "[Shared] " + name;
                default:
                    return name;
            }
        }

        public static PageRef parse(String token) {
            String s = token == null ? "" : token.trim();
            if (s.isEmpty()) {
                return new PageRef("", ORScope.PROJECT);
            }
            if (s.startsWith("[") && s.contains("]")) {
                int end = s.indexOf(']');
                String scopeText = s.substring(1, end).trim().toUpperCase();
                String base = s.substring(end + 1).trim();
                ORScope sc;
                switch (scopeText) {
                    case "PROJECT":
                        sc = ORScope.PROJECT;
                        break;
                    case "SHARED":
                        sc = ORScope.SHARED;
                        break;
                    default:
                        sc = ORScope.PROJECT;
                }

                return new PageRef(base, sc);
            }
            return new PageRef(s, ORScope.PROJECT);
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