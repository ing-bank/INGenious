
package com.ing.engine.support.methodInf;

import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.FilePath;
import com.ing.engine.support.AnnontationUtil;
import com.ing.engine.support.reflect.Discovery;
import com.ing.engine.support.reflect.MethodExecutor;
import eu.infomas.annotation.AnnotationDetector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
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
     * Map of method names to their associated {@link Action} annotation metadata.
     */
    public static Map<String, Action> methodInfoMap = new HashMap<>();
    
    private static final AnnotationDetector.MethodReporter METHOD_REPORTER = new AnnotationDetector.MethodReporter() {
        
        @SuppressWarnings("unchecked")
        @Override
        public Class<? extends Annotation>[] annotations() {
            return new Class[]{Action.class};
        }
        
        @Override
        public void reportMethodAnnotation(Class<? extends Annotation> annotation,
                String className, String methodName) {
            loadMethod(className, methodName);
        }
        
    };
    
    private static final AnnotationDetector ANNOTATION_DETECTOR = new AnnotationDetector(METHOD_REPORTER);
    
    /**
     * Loads all methods annotated with {@link Action} from the main application and plugin JARs.
     * <p>
     * This method clears the current method info map, initializes method executors, and scans
     * both the action packages and all plugin JARs for annotated methods.
     */
    public static void load() {
        MethodExecutor.init();
        methodInfoMap.clear();
        AnnontationUtil.detect(ANNOTATION_DETECTOR, "com.ing.engine.commands");

        File basePluginDir = new File(FilePath.getAppRoot() + "/plugins");
        String[] jarPaths = getPluginJarPaths(basePluginDir);
        AnnontationUtil.detectFromPluginPaths(ANNOTATION_DETECTOR, jarPaths);
    }
    
    /**
     * Loads the method with the given class and method name, and stores its {@link Action} annotation in the map.
     *
     * @param className the fully qualified class name
     * @param methodName the method name to load
     */
    private static void loadMethod(String className, String methodName) {
        try {
            Method method = getClass(className).getMethod(methodName);
            Action mInfo = method.getAnnotation(Action.class);
            methodInfoMap.put(methodName, mInfo);
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
    public static List<String> getMethodListFor(ObjectType type, ObjectType... others) {
        List<String> methodList = new ArrayList<>();
        for (Map.Entry<String, Action> entry : methodInfoMap.entrySet()) {
            String methodName = entry.getKey();
            Action mInfo = entry.getValue();
            if (mInfo.object().equals(type)
                    || (others != null
                    && Arrays.asList(others).contains(mInfo.object()))) {
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
            return methodInfoMap.get(action).desc();
        }
        return "";
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
        return Arrays.stream(pluginDirs) // Stream each plugin directory
            .flatMap(dir -> Arrays.stream( // For each directory, stream its files
                dir.listFiles(file ->      // Only include files that:
                    file.isFile() &&       // - are regular files
                    file.getName().endsWith(".jar") // - have a .jar extension
                )
            ))
            .map(File::getAbsolutePath)    // Map each File to its absolute path
            .toArray(String[]::new);       // Collect results into a String array
    }
    
}
