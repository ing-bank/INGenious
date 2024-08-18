
package com.ing.ide.main.utils.fileoperation;

import com.ing.ide.util.logging.UILogger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 *
 * 
 */
public class FileOptions {

    private static final org.slf4j.Logger LOG = UILogger.getLogger(FileOptions.class.getName());

    public static void createDirectory(String path) {
        File newDir = new File(path);
        newDir.mkdirs();
    }

    public static void createFile(String path, String content) {
        File newFile = new File(path);
        if (!newFile.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(newFile))) {
                writer.write(content);
            } catch (IOException ex) {
                Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void deleteFile(String path) {
        LOG.warn("DELETING: " + path);
        try {
            if (new File(path).exists()) {
                FileUtils.forceDelete(new File(path));
            }
        } catch (IOException ex) {
            Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void renameFile(String path, String newName) {
        File oldLocation = new File(path);
        if (oldLocation.exists()) {
            try {
                Path source = oldLocation.toPath();
                Files.move(source, source.resolveSibling(newName));
            } catch (IOException ex) {
                Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void copyFile(String source, String destination) {
        if (source != null && source.trim().length() > 0 && destination != null && destination.trim().length() > 0) {
            File sourceFile = new File(source);
            File destFile = new File(destination);
            try {
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
                FileUtils.copyFileToDirectory(sourceFile, destFile);
            } catch (IOException ex) {
                Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void copyFileAs(String source, String destination) {
        if (source != null && source.trim().length() > 0 && destination != null && destination.trim().length() > 0) {
            File sourceFile = new File(source);
            File destFile = new File(destination);
            try {
                if (sourceFile.exists()) {
                    FileUtils.copyFile(sourceFile, destFile);
                }
            } catch (IOException ex) {
                Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void moveDirectory(String source, String destination) {
        File sourceFile = new File(source);
        File destFile = new File(destination);
        try {
            if (sourceFile.exists()) {
                FileUtils.copyDirectoryToDirectory(sourceFile, destFile.getParentFile());
                FileUtils.deleteDirectory(sourceFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void copyDirectory(String source, String destination) {
        File sourceFile = new File(source);
        File destFile = new File(destination);
        try {
            if (sourceFile.exists()) {
                FileUtils.copyDirectoryToDirectory(sourceFile, destFile);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileOptions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
