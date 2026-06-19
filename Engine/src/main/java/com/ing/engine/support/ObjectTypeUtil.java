package com.ing.engine.support;

import com.ing.ingenious.api.types.ObjectType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for managing and querying object types at runtime.
 */
public final class ObjectTypeUtil {
    private static final Set<String> pluginObjectTypes = new HashSet<>();
    private static final List<String> objectTypesforIDEDropdown = new ArrayList<>();

    private ObjectTypeUtil() {}

    /**
     * Registers a custom object type from a plugin.
     * <p>
     * This method allows plugins to register their own object types that will be available
     * in the IDE alongside standard framework types. The type is only registered if it is
     * not null, not empty, not already present in the initial object types, and not already
     * registered by another plugin.
     * </p>
     *
     * @param type the custom object type name to register (e.g., "CustomAPI", "MyService")
     * @see #getAllTypesForIDE()
     */
    public static void registerObjectTypefromPlugin(String type) {
        if (
            type != null &&
            !type.isEmpty() &&
            !ObjectType.initialObjectTypes.contains(type) &&
            !pluginObjectTypes.contains(type)
        ) {
            pluginObjectTypes.add(type);
            System.out.println("Registered new object type: " + type);
        }
    }

    /**
     * Checks if the given type is registered (case-insensitive).
     * <p>
     * This method performs a case-insensitive comparison to allow flexibility
     * in object type naming (e.g., "Browser", "browser", "BROWSER" are all valid).
     * </p>
     *
     * @param type the object type to check
     * @return true if registered (ignoring case), false otherwise
     */
    public static boolean isKnownType(String type) {
        if (type == null) {
            return false;
        }
        return objectTypesforIDEDropdown.stream().anyMatch(t -> t.equalsIgnoreCase(type));
    }

    /**
     * Returns a list of all object types available for display in the IDE.
     * <p>
     * This method aggregates all standard framework object types (Browser, Mobile, Database, etc.)
     * along with any custom object types registered by plugins. The list is used to populate
     * object type dropdowns and auto-suggest components in the IDE.
     * </p>
     *
     * @return an unmodifiable list of all available object type names for IDE components
     * @see #registerObjectTypefromPlugin(String)
     * @see #isKnownType(String)
     */
    public static List<String> getAllTypesForIDE() {
        objectTypesforIDEDropdown.clear();
        objectTypesforIDEDropdown.add(ObjectType.BROWSER);
        objectTypesforIDEDropdown.add(ObjectType.MOBILE);
        objectTypesforIDEDropdown.add(ObjectType.WEBSERVICE);
        objectTypesforIDEDropdown.add(ObjectType.DATABASE);
        objectTypesforIDEDropdown.add(ObjectType.KAFKA);
        objectTypesforIDEDropdown.add(ObjectType.QUEUE);
        objectTypesforIDEDropdown.add("Synthetic Data");
        objectTypesforIDEDropdown.add(ObjectType.FILE);
        objectTypesforIDEDropdown.add(ObjectType.GENERAL);
        objectTypesforIDEDropdown.add("EXECUTE");
        objectTypesforIDEDropdown.add(ObjectType.STRINGOPERATIONS);
        objectTypesforIDEDropdown.addAll(pluginObjectTypes);
        return Collections.unmodifiableList(objectTypesforIDEDropdown);
    }
}
