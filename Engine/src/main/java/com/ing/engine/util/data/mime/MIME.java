
package com.ing.engine.util.data.mime;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * 
 */
public class MIME {

    private final Properties map = new Properties();

    private static MIME mime;

    private MIME() throws IOException {
        map.load(MIME.class.
                getResourceAsStream(
                        "/util/mime/mime.types.properties"));

    }

    public static String getType(File f) {
        try {
            return getType(f.getName());
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    public static String getType(String fileName) {
        try {
            fileName = fileName.replace(":","_");
            return getTypeFor(FilenameUtils.getExtension(fileName));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

    private static String getTypeFor(String ext) {
        try {
            if (mime == null) {
                mime = new MIME();
            }
            return mime.map.getProperty(ext);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }
    private static final Logger LOG = Logger.getLogger(MIME.class.getName());

}
