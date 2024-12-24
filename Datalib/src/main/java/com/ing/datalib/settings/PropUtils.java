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
            System.out.println(file.getName() + properties.entrySet());
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
        System.out.println("++++++++++++++++" + file.getAbsolutePath());
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "ISO_8859_1"))) {
            synchronized (prop) {
                for (Map.Entry<Object, Object> e : prop.entrySet()) {
                    String key = (String) e.getKey();
                    String val = (String) e.getValue();
                    key = escapeUnicode(key);
                    val = escapeUnicode(val);
                    bw.write(key + "=" + val);
                    bw.newLine();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(PropUtils.class.getName()).log(Level.SEVERE, filename, ex);
        }

    }
    
    private static String escapeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c > 127) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
