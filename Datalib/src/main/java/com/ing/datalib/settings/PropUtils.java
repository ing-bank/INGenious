package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
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
        saveProperties(prop, filename);
    }

    private static void saveProperties(Properties prop, String filename) {
        File file = new File(filename);
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "ISO_8859_1"))) {
            synchronized (prop) {
                for (Map.Entry<Object, Object> e : prop.entrySet()) {
                    String key = (String) e.getKey();
                    String val = (String) e.getValue();
                    bw.write(key + "=" + val);
                    bw.newLine();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PropUtils.class.getName()).log(Level.SEVERE, filename, ex);
        }

    }
}
