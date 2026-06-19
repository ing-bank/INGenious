package com.ing.engine.support.methodInf;

import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.FilePath;
import com.ing.engine.support.AnnontationUtil;
import com.ing.engine.support.ObjectTypeUtil;
import com.ing.engine.support.reflect.Discovery;
import com.ing.engine.support.reflect.MethodExecutor;
import com.ing.exceptions.DuplicateMethodException;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.ObjectType;
import eu.infomas.annotation.AnnotationDetector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages discovery and lookup of methods annotated with {@link Action} in the application and plugins.
 * <p>
 * This class scans the main packages for actions and plugin JARs for methods annotated with {@code @Action},
 * builds a map of method names to their {@code Action} metadata, and provides lookup utilities for
 * descriptions and method lists by object type.
 */
public class MethodInfoManager {

    /**
     * Wrapper class to store Action annotation, class name, method name, and plugin location.
     */
    private static class MethodInfo {
        private final Action action;
        private final String className;
        private final String methodName;
        private final String pluginLocation;

        public MethodInfo(
            Action action,
            String className,
            String methodName,
            String pluginLocation
        ) {
            this.action = action;
            this.className = className;
            this.methodName = methodName;
            this.pluginLocation = pluginLocation;
        }

        public Action getAction() {
            return action;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getPluginLocation() {
            return pluginLocation;
        }
    }

    /**
     * Map of method names to their associated {@link MethodInfo} containing Action annotation and location metadata.
     */
    public static Map<String, MethodInfo> methodInfoMap = new HashMap<>();
    private static boolean isDuplicateMethodDetected = false;
    private static Map<String, Set<String>> objectTypeMethodMap = new HashMap<>();

    private static final AnnotationDetector.MethodReporter METHOD_REPORTER = new AnnotationDetector.MethodReporter() {

        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Annotation>[] annotations() {
            return new Class[] { Action.class };
        }

        @Override
        public void reportMethodAnnotation(
            Class<? extends Annotation> annotation,
            String className,
            String methodName
        ) {
            loadMethodAndRegisterType(className, methodName);
        }
    };

    private static final AnnotationDetector ANNOTATION_DETECTOR = new AnnotationDetector(
        METHOD_REPORTER
    );

    /**
     * Loads all methods annotated with {@link Action} from the application and plugins.
     * <p>
     * This method performs a complete scan and initialization of the action framework:
     * <ul>
     *   <li>Initializes method executors</li>
     *   <li>Clears any previously loaded method information</li>
     *   <li>Scans the main application package (com.ing.engine.commands) for @Action methods</li>
     *   <li>Scans all plugin JARs in the plugins directory for @Action methods</li>
     *   <li>Detects and reports duplicate method names for the same object type</li>
     * </ul>
     * This method must be called during application startup before any action execution.
     * </p>
     *
     * @throws DuplicateMethodException if duplicate action methods are detected across plugins or core
     * @see Action
     * @see #loadMethodAndRegisterType(String, String)
     */
    public static void load() throws DuplicateMethodException {
        MethodExecutor.init();
        methodInfoMap.clear();
        AnnontationUtil.detect(ANNOTATION_DETECTOR, "com.ing.engine.commands");

        File basePluginDir = new File(FilePath.getAppRoot() + "/plugins");
        String[] jarPaths = getPluginJarPaths(basePluginDir);
        AnnontationUtil.detectFromPluginPaths(ANNOTATION_DETECTOR, jarPaths);

        if (isDuplicateMethodDetected) {
            System.out.println(
                "\u001B[31mDuplicate method names detected in the loaded actions. Please resolve the conflicts.\u001B[0m"
            );
            throw new DuplicateMethodException(
                "Duplicate method names detected in the loaded actions. Please resolve the conflicts."
            );
        }
    }

    /**
     * Loads a method's {@link Action} annotation and registers its object type.
     * <p>
     * This method performs the following operations:
     * <ul>
     *   <li>Retrieves the method from the specified class</li>
     *   <li>Extracts the {@link Action} annotation metadata</li>
     *   <li>Registers the object type from the annotation with {@link ObjectTypeUtil}</li>
     *   <li>Checks for duplicate method names within the same object type</li>
     *   <li>Stores the method info in the map if no duplicate is detected</li>
     * </ul>
     * If a duplicate is detected, the method is not registered and the duplicate flag is set.
     * </p>
     *
     * @param className the fully qualified class name containing the method
     * @param methodName the name of the method to load (must be a no-arg method)
     */
    private static void loadMethodAndRegisterType(String className, String methodName) {
        try {
            Method method = getClass(className).getMethod(methodName);
            Action mInfo = method.getAnnotation(Action.class);
            String currentLocation = getPluginFolderName(method);
            ObjectTypeUtil.registerObjectTypefromPlugin(mInfo.object());
            if (isDuplicateMethodForObjectType(methodName, mInfo.object())) {
                MethodInfo originalMethodInfo = methodInfoMap.get(methodName);
                String originalClassName = originalMethodInfo.getClassName();
                String originalLocation = originalMethodInfo.getPluginLocation();

                System.out.println(
                    "\u001B[31m" +
                    "Duplicate action '" +
                    methodName +
                    "' for object type '" +
                    mInfo.object() +
                    "' detected:\n" +
                    "  - Original found in: " +
                    originalLocation +
                    " (class: " +
                    originalClassName +
                    ")\n" +
                    "  - Duplicate found in: " +
                    currentLocation +
                    " (class: " +
                    className +
                    ")\u001B[0m"
                );
                isDuplicateMethodDetected = true; // Set flag and return early
                return; // Don't register the duplicate
            }
            methodInfoMap.put(
                methodName,
                new MethodInfo(mInfo, className, methodName, currentLocation)
            );
            registerMethodToObjectType(methodName, mInfo.object());
        } catch (NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(MethodInfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Attempts to retrieve a {@link Class} object for the specified class name.
     * <p>
     * This method first tries to obtain the class using {@link Discovery#getClassByName(String)}.
     * If unsuccessful, it falls back to using {@link Class#forName(String)}.
     * If the class cannot be found, it logs the exception and returns {@code null}.
     * </p>
     *
     * @param className the fully qualified name of the desired class
     * @return the {@link Class} object representing the class, or {@code null} if not found
     */
    private static Class<?> getClass(String className) {
        try {
            Class<?> class_ = Discovery.getClassByName(className);
            if (class_ != null) {
                return class_;
            }
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MethodInfoManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Retrieves a sorted list of method names associated with the specified {@link ObjectType}
     * and any additional {@link ObjectType}s provided.
     *
     * <p>
     * The method iterates through the {@code methodInfoMap} and collects the names of methods
     * whose associated object type matches the given {@code type} or any of the {@code others}.
     * The resulting list of method names is sorted in natural order before being returned.
     * </p>
     *
     * @param type   the primary {@link ObjectType} to filter methods by
     * @param others additional {@link ObjectType}s to include in the filter (optional)
     * @return a sorted {@link List} of method names matching the specified object types
     */
    public static List<String> getMethodListFor(String type, String... others) {
        List<String> methodList = new ArrayList<>();
        for (Map.Entry<String, MethodInfo> entry : methodInfoMap.entrySet()) {
            String methodName = entry.getKey();
            Action mInfo = entry.getValue().getAction();
            if (
                mInfo.object().equals(type) ||
                (others != null && Arrays.asList(others).contains(mInfo.object()))
            ) {
                methodList.add(methodName);
            }
        }
        Collections.sort(methodList);
        return methodList;
    }

    /**
     * Returns the description for the given action method, if available.
     *
     * @param action the method name
     * @return the description from the {@link Action} annotation, or an empty string if not found
     */
    public static String getDescriptionFor(String action) {
        if (methodInfoMap.containsKey(action)) {
            return methodInfoMap.get(action).getAction().desc();
        }
        return "";
    }

    /**
     * Returns the MethodInfo for the given action method, if available.
     *
     * @param action the method name
     * @return the MethodInfo object, or null if not found
     */
    public static MethodInfo getMethodInfo(String action) {
        return methodInfoMap.get(action);
    }

    /**
     * Returns the Action annotation for the given action method, if available.
     *
     * @param action the method name
     * @return the Action annotation, or null if not found
     */
    public static Action getActionFor(String action) {
        MethodInfo info = methodInfoMap.get(action);
        return info != null ? info.getAction() : null;
    }

    /**
     * Checks if an action method exists in the registry.
     *
     * @param action the method name
     * @return true if the action exists, false otherwise
     */
    public static boolean containsAction(String action) {
        return methodInfoMap.containsKey(action);
    }

    /**
     * Returns the resolved description for a test step, replacing placeholders with actual values.
     *
     * @param step the test step
     * @return the resolved description string
     */
    public static String getResolvedDescriptionFor(TestStep step) {
        return getDescriptionFor(step.getAction())
            .replace("[<Object>]", step.getObject())
            .replace("[<Object2>]", step.getCondition())
            .replace("[<Data>]", step.getInput());
    }

    /**
     * Returns an array of absolute paths to all plugin JAR files found in subdirectories of the given base plugin directory.
     *
     * @param basePluginDir the root plugin directory
     * @return array of absolute paths to plugin JAR files, or empty array if none found
     */
    private static String[] getPluginJarPaths(File basePluginDir) {
        if (basePluginDir == null || !basePluginDir.isDirectory()) {
            return new String[0];
        }
        File[] pluginDirs = basePluginDir.listFiles(File::isDirectory);
        if (pluginDirs == null) {
            return new String[0];
        }
        return Arrays
            .stream(pluginDirs) // Stream each plugin directory
            .flatMap(
                dir ->
                    Arrays.stream( // For each directory, stream its files
                        dir.listFiles(
                            file -> // Only include files that:
                                file.isFile() && // - are regular files
                                file.getName().endsWith(".jar") // - have a .jar extension
                        )
                    )
            )
            .map(File::getAbsolutePath) // Map each File to its absolute path
            .toArray(String[]::new); // Collect results into a String array
    }

    /**
     * Registers a method name to its associated object type.
     * <p>
     * This method maintains a mapping of object types to their registered method names,
     * allowing efficient lookup and duplicate detection.
     * </p>
     *
     * @param methodName the name of the method to register
     * @param objectType the object type associated with the method
     */
    private static void registerMethodToObjectType(String methodName, String objectType) {
        objectTypeMethodMap.computeIfAbsent(objectType, k -> new HashSet<>()).add(methodName);
    }

    /**
     * Checks if a method is already registered for the specified object type (duplicate detection).
     * <p>
     * This method is used for duplicate detection to ensure that each method name
     * is unique within its object type context.
     * </p>
     *
     * @param methodName the name of the method to check
     * @param objectType the object type to check against
     * @return {@code true} if the method is already registered for this object type (duplicate), {@code false} otherwise
     */
    private static boolean isDuplicateMethodForObjectType(String methodName, String objectType) {
        return (
            objectTypeMethodMap.containsKey(objectType) &&
            objectTypeMethodMap.get(objectType).contains(methodName)
        );
    }

    /**
     * Extracts the plugin folder name from the method's class location.
     * <p>
     * For plugin classes, returns the name of the folder containing the plugin JAR.
     * For core application classes, returns "core".
     * </p>
     *
     * @param method the method to get the plugin folder for
     * @return the plugin folder name or "core" for application classes
     */
    private static String getPluginFolderName(Method method) {
        try {
            java.net.URL location = method
                .getDeclaringClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
            if (location != null) {
                String path = location.getPath();
                // Extract plugin folder name from path like: /path/to/plugins/plugin-name/plugin.jar
                if (path.contains("/plugins/")) {
                    int pluginsIndex = path.indexOf("/plugins/");
                    String afterPlugins = path.substring(pluginsIndex + "/plugins/".length());
                    int nextSlash = afterPlugins.indexOf("/");
                    if (nextSlash > 0) {
                        return afterPlugins.substring(0, nextSlash);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return default
        }
        return "core";
    }
}
