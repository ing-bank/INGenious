
package com.ing.datalib.component.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 *
 *
 */
public class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    private static final JFileChooser SAVE_AS_LOC = new JFileChooser();

    static {
        SAVE_AS_LOC.setApproveButtonText("Save");
    }
    public static final FilenameFilter DIR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory();
        }
    };

    public static final FilenameFilter CSV_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.matches(".*\\.csv");
        }
    };

    public static void loadFileinTable(File file, JTable table) {
        if (file.exists()) {
            try (Reader in = new FileReader(file)) {
                CSVParser parser = CSVFormat.EXCEL.withHeader().withSkipHeaderRecord().withIgnoreEmptyLines().parse(in);
                if (!parser.getHeaderMap().isEmpty()) {
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    for (String columnHeader : parser.getHeaderMap().keySet()) {
                        if (!columnHeader.trim().isEmpty()) {
                            model.addColumn(columnHeader);
                        }
                    }
                    List<CSVRecord> records = parser.getRecords();
                    for (CSVRecord record : records) {
                        Object[] row = new Object[record.size()];
                        for (int i = 0; i < record.size(); i++) {
                            row[i] = record.get(i);
                        }
                        model.addRow(row);
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.log(Level.SEVERE, "File [{0}] doesn''t exist", file.getAbsolutePath());
        }
    }

    public static List<CSVRecord> getRecords(File file) {
        if (file.exists()) {
            try (Reader in = new FileReader(file)) {
                CSVParser parser = CSVFormat.EXCEL.withHeader().withSkipHeaderRecord().withIgnoreEmptyLines().parse(in);
                if (!parser.getHeaderMap().isEmpty()) {
                    return parser.getRecords();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } else {
            LOGGER.log(Level.SEVERE, "File [{0}] doesn''t exist", file.getAbsolutePath());
        }
        return new ArrayList<>();
    }

    public static CSVHParser getCSVHParser(File file) {
        if (file.exists()) {
            try (Reader in = new FileReader(file)) {
                CSVParser parser = CSVFormat.EXCEL.withHeader().withSkipHeaderRecord().withIgnoreEmptyLines().parse(in);
                if (!parser.getHeaderMap().isEmpty()) {
                    return new CSVHParser(parser.getHeaderMap(), parser.getRecords());
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        } else {
            //LOGGER.log(Level.SEVERE, "File [{0}] doesn''t exist", file.getAbsolutePath());
        }
        return null;
    }

    public static File openProject() {
        JFileChooser fc = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter("Open Project", "project");
        fc.setFileFilter(filter);
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile().getParentFile();
        }
        return null;
    }

    public static File saveAsLocation(String title) {
        SAVE_AS_LOC.setDialogTitle(title);
        SAVE_AS_LOC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = SAVE_AS_LOC.showSaveDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return SAVE_AS_LOC.getSelectedFile();
        }
        return null;
    }

    private static String sanitizePathTraversal(String filename) {
        Path p = Paths.get(filename);
        return p.getFileName().toString();
    }
    
    public static Boolean renameFile(String fromFile, String toName) {
        try {
            File src = new File(fromFile);
            if (src.exists()) {
                File target = new File(src.getParent(), sanitizePathTraversal(toName));
                if (target.exists()) {
                    LOGGER.log(Level.INFO, "A File with Name '{1}' already exists, failed to rename '{0}'",
                            new Object[]{fromFile, toName});
                    return false;
                }
                Files.move(src.toPath(), target.toPath());
            }
        } catch (IOException ex) {
            Logger.getLogger(FileUtils.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        LOGGER.log(Level.INFO, "{0} Renamed to {1}", new Object[]{fromFile, toName});
        return true;
    }

    public static Boolean deleteFile(String location) {
        File file = new File(location);
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File subFile : file.listFiles()) {
                    if (subFile.isDirectory()) {
                        deleteFile(subFile.getAbsolutePath());
                    }
                    subFile.delete();
                }
            }
            LOGGER.log(Level.INFO, "Deleting {0}", new Object[]{location});    
            return file.delete();
        }
       return true;
    }
}
