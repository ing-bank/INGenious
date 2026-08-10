package com.ing.datalib.or.sap;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapOR.ORScope;

/**
 * Represents a resolved SAP object within the Object Repository, including its scope,
 * page name, object name, and resolved object group.
 */
public class ResolvedSapObject {
    private final ORScope scope;
    private final String pageName;
    private final String objectName;
    private final ObjectGroup<SapORObject> group;

    public ResolvedSapObject(
        ORScope scope,
        String pageName,
        String objectName,
        ObjectGroup<SapORObject> group
    ) {
        this.scope = scope;
        this.pageName = pageName;
        this.objectName = objectName;
        this.group = group;
    }

    public ORScope getScope() {
        return scope;
    }

    public String getPageName() {
        return pageName;
    }

    public String getObjectName() {
        return objectName;
    }

    public ObjectGroup<SapORObject> getGroup() {
        return group;
    }

    /**
     * Returns the first resolved SapORObject from the group, or null if none exist.
     */
    public SapORObject getObject() {
        return (group != null && !group.getObjects().isEmpty()) ? group.getObjects().get(0) : null;
    }

    public boolean isFromProject() {
        return scope == ORScope.PROJECT;
    }

    public boolean isFromShared() {
        return scope == ORScope.SHARED;
    }

    public boolean isPresent() {
        return group != null && !group.getObjects().isEmpty();
    }

    public String debugString() {
        return (
            "ResolvedSapObject{scope=" +
            scope +
            ", page='" +
            pageName +
            '\'' +
            ", object='" +
            objectName +
            '\'' +
            ", objectCount=" +
            (group == null ? 0 : group.getObjects().size()) +
            '}'
        );
    }

    /**
     * Represents a reference to a page along with its OR scope, and provides utilities
     * for formatting and parsing scoped page tokens.
     */
    public static final class PageRef {
        public final String name;
        public final ORScope scope;

        public PageRef(String name, ORScope scope) {
            this.name = name;
            this.scope = scope;
        }

        public String qualified() {
            if (scope == null) return name;
            switch (scope) {
                case PROJECT:
                    return "[Project] " + name;
                case SHARED:
                    return "[Shared] " + name;
                default:
                    return name;
            }
        }

        /**
         * Parses a scoped page token (e.g., "[Shared] SAPScreen") into a PageRef.
         * Defaults to PROJECT scope when missing or unrecognized.
         *
         * @param token raw page reference text
         * @return parsed PageRef instance
         */
        public static PageRef parse(String token) {
            String s = token == null ? "" : token.trim();
            if (s.isEmpty()) return new PageRef("", ORScope.PROJECT);

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
}
