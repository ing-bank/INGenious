
package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class PropUtils {

    public static LinkedProperties load(File file) {
        return load(new LinkedProperties(), file);
    }

    public static LinkedProperties load(LinkedProperties properties, File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        } catch (IOException ex) {
            Logger.getLogger(PropUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return properties;
    }

    public static void save(Properties prop, String filename) {
        if (!new File(filename).getParentFile().exists()) {
            new File(filename).getParentFile().mkdirs();
        }
        try (FileOutputStream fout = new FileOutputStream(new File(filename))) {
            prop.store(fout, null);
        } catch (Exception ex) {
            Logger.getLogger(PropUtils.class.getName()).log(Level.SEVERE, filename, ex);
        }
    }
}
