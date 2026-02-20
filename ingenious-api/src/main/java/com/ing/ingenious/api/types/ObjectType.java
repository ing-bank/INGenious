
package com.ing.ingenious.api.types;

import java.util.Set;
import java.util.HashSet;

/**
 * Defines standard object type constants used throughout the framework.
 * <p>
 * This class provides constant definitions for all built-in object types that the framework
 * supports, including Browser, Mobile, Database, Webservice, and various other automation
 * object types. These constants are used for method routing, validation, and IDE auto-suggest.
 * </p>
 *
 * @see com.ing.engine.support.ObjectTypeUtil
 * @see MethodInfoManager
 */
public class ObjectType {
    /** Browser automation object type */
    public static final String BROWSER = "Browser";
    /** Web element object type */
    public static final String WEB = "Web";
    /** Mobile automation object type */
    public static final String MOBILE = "Mobile";
    /** Image processing object type */
    public static final String IMAGE = "Image";
    /** Playwright browser automation object type */
    public static final String PLAYWRIGHT = "Playwright";
    /** Mobile application object type */
    public static final String APP = "App";
    /** Database operations object type */
    public static final String DATABASE = "Database";
    /** ProtractorJS automation object type */
    public static final String PROTRACTORJS = "ProtractorJS";
    /** Generic object type for universal actions */
    public static final String ANY = "Any";
    /** Web service operations object type */
    public static final String WEBSERVICE = "Webservice";
    /** File operations object type */
    public static final String FILE = "File";
    /** Kafka messaging object type */
    public static final String KAFKA = "Kafka";
    /** Queue messaging object type */
    public static final String QUEUE = "Queue";
    /** Synthetic data generation object type */
    public static final String DATA = "Data";
    /** General utility operations object type */
    public static final String GENERAL = "General";
    /** String manipulation operations object type */
    public static final String STRINGOPERATIONS = "String Operations";

    /**
     * An unmodifiable set containing all standard framework object types.
     * <p>
     * This set is initialized with all predefined object type constants and is used
     * to distinguish between built-in types and custom plugin-registered types.
     * </p>
     */
    public static final Set<String> initialObjectTypes = new HashSet<String>() {{
        add(BROWSER);
        add(WEB);
        add(MOBILE);
        add(IMAGE);
        add(PLAYWRIGHT);
        add(APP);
        add(DATABASE);
        add(PROTRACTORJS);
        add(ANY);
        add(WEBSERVICE);
        add(FILE);
        add(KAFKA);
        add(QUEUE);
        add(DATA);
        add(GENERAL);
        add(STRINGOPERATIONS);
        }};

    /**
     * Returns a copy of all initial object types supported by the framework.
     * <p>
     * This method returns a new HashSet containing all standard object types,
     * protecting the internal set from modification.
     * </p>
     *
     * @return a new set containing all initial object type names
     */
    public static Set<String> getObjectTypes() {
        return new HashSet<>(initialObjectTypes);
    }
    
}
