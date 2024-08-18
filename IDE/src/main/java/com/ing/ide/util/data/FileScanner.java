
package com.ing.ide.util.data;

import com.ing.ide.util.logging.UILogger;
import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 *
 * 
 */
public class FileScanner {

    private static final org.slf4j.Logger LOG = UILogger.getLogger(FileScanner.class.getName());

    public  static synchronized void writeFile(File f, String s) {
        try (PrintWriter out = new PrintWriter(f);) {
            out.write(s);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    public  static synchronized String readFile(File path) throws Exception {
        try(Scanner s=new Scanner(path).useDelimiter("\\A")){
           return s.next();
        }
    }

    private FileScanner() {

    }
}
