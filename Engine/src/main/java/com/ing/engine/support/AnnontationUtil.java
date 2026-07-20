package com.ing.engine.support;

import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import eu.infomas.annotation.AnnotationDetector;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for detecting annotated classes and methods in the main application, user-defined directories,
 * and plugin JARs using the AnnotationDetector library.
 */
public class AnnontationUtil {

    /**
     * Detects annotated classes and methods in the specified packages, external command JARs, engine JAR,
     * and user-defined directory using the provided AnnotationDetector.
     *
     * @param ANNOTATION_DETECTOR the annotation detector to use
     * @param packageNames the package names to scan for annotations
     */
    public static void detect(AnnotationDetector ANNOTATION_DETECTOR, String... packageNames) {
        try {
            String libLocation = "lib" + File.separator;
            File[] externalCommands = new File(libLocation + "commands").listFiles();
            if (externalCommands != null) {
                ANNOTATION_DETECTOR.detect(externalCommands);
            }
            if (SystemDefaults.getClassesFromJar.get()) {
                ANNOTATION_DETECTOR.detect(new File(FilePath.getEngineJarPath()));
            } else {
                ANNOTATION_DETECTOR.detect(packageNames);
            }
            ANNOTATION_DETECTOR.detect(new File(FilePath.getAppRoot(), "userdefined"));
        } catch (IOException ex) {
            Logger.getLogger(AnnontationUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Detects annotated classes and methods from the given plugin JAR file paths using the provided AnnotationDetector.
     * Only valid JAR files are scanned; invalid files are logged as warnings.
     *
     * @param annotationDetector the annotation detector to use
     * @param jarPaths array of plugin JAR file paths to scan
     */
    public static void detectFromPluginPaths(
        AnnotationDetector annotationDetector,
        String... jarPaths
    ) {
        for (String jarPath : jarPaths) {
            File jarFile = new File(jarPath);
            try {
                if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                    annotationDetector.detect(jarFile);
                } else {
                    Logger
                        .getLogger(AnnontationUtil.class.getName())
                        .log(Level.WARNING, "Invalid JAR file: " + jarFile.getAbsolutePath());
                }
            } catch (IOException ex) {
                Logger.getLogger(AnnontationUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
