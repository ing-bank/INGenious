
package com.ing.engine.support;

import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import eu.infomas.annotation.AnnotationDetector;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class AnnontationUtil {

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
}
