
package com.ing.datalib.util.data;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class FileScanner {

    public static synchronized void writeFile(File f, String s) {
        try (PrintWriter out = new PrintWriter(f);) {
            out.write(s);
        } catch (Exception ex) {
            Logger.getLogger(FileScanner.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * reads a file and returns the contents as string
     *
     * @param path file path to read
     * @return file content
     * @throws Exception
     */
    public static synchronized String readFile(final File path) throws Exception {
        try (Scanner s = new Scanner(path);) {
            if (s.hasNext()) {
                return s.useDelimiter("\\A").next();
            }
            return "";
        }
    }

    /**
     * reads a file and returns the contents as string
     *
     * @param is
     * @return file content
     */
    public static synchronized String readStream(final InputStream is) {
        try (Scanner s = new Scanner(is);) {
            return s.useDelimiter("\\A").next();
        }
    }

    public static String getResourceString(String resource) {
        try {
            return readStream(FileScanner.class.getClassLoader().getResourceAsStream(resource));
        } catch (Exception ex) {
            Logger.getLogger(FileScanner.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
