
package com.ing.datalib.or.web;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR.ORScope;


/**
 * Represents a resolved web object within the Object Repository, including its scope,
 * page name, object name, and resolved object group.
 */

public class ResolvedWebObject {
    private final ORScope scope;
    private final String pageName;
    private final String objectName;
    private final ObjectGroup<WebORObject> group;
    
    /**
    * Creates a resolved web object record tying together scope, page name, object name,
    * and the matched object group.
    *
    * @param scope      OR scope (Project or Shared)
    * @param pageName   logical page name
    * @param objectName name of the web object
    * @param group      group of matching WebORObject instances
    */

    public ResolvedWebObject(ORScope scope, String pageName, String objectName, ObjectGroup<WebORObject> group) {
        this.scope = scope;
        this.pageName = pageName;
        this.objectName = objectName;
        this.group = group;
    }

    /**
     * Represents a reference to a page along with its OR scope, and provides utilities
     * for formatting and parsing scoped page tokens.
     */
    
    public static final class PageRef {
        public final String name;
        public final ORScope scope;

        /**
         * Creates a page reference with the given name and scope.
         *
         * @param name  page name without prefix
         * @param scope OR scope of the page
         */

        public PageRef(String name, ORScope scope) {
            this.name = name;
            this.scope = scope;
        }
        
        /**
         * Returns the page name prefixed with its scope (e.g., "[Project] Login"),
         * or the raw name if scope is null.
         *
         * @return fully qualified scoped page name
         */
        
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

        /**
         * Parses a scoped page token (e.g., "[Shared] Home") into a PageRef.
         * Defaults to PROJECT scope when missing or unrecognized.
         *
         * @param token raw page reference text
         * @return parsed PageRef instance
         */
        
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

    /**
     * Returns the first resolved WebORObject from the group, or null if none exist.
     *
     * @return a resolved WebORObject or null
     */

    public WebORObject getObject() {
        return (group != null && !group.getObjects().isEmpty()) ? group.getObjects().get(0) : null;
    }

    public boolean isFromProject() { return scope == ORScope.PROJECT; }
    public boolean isFromShared()  { return scope == ORScope.SHARED; }
    public boolean isPresent()     { return group != null && !group.getObjects().isEmpty(); }

    /**
     * Returns a debug-friendly string summarizing the scope, page, object name,
     * and number of resolved objects.
     *
     * @return formatted debug information
     */

    public String debugString() {
        return "ResolvedWebObject{scope=" + scope +
               ", page='" + pageName + '\'' +
               ", object='" + objectName + '\'' +
               ", objectCount=" + (group == null ? 0 : group.getObjects().size()) +
               '}';
    }
}