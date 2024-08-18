
package com.ing.testdata.csv;

import com.ing.datalib.component.utils.CSVHParser;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public class CSVUtils {

    public static void load(File location, AbstractDataModel sAbstractData) {
        CSVHParser parser = FileUtils.getCSVHParser(location);
        if (parser != null) {
            for (CSVRecord crecord : parser.getRecords()) {
                List record = sAbstractData.getNewRecord();
                for (int i = 0; i < crecord.size(); i++) {
                    record.add(crecord.get(i));
                }
                sAbstractData.addRecord(record);
            }
        }
    }

    public static Set<String> loadColumns(File location) {
        CSVHParser parser = FileUtils.getCSVHParser(location);
        if (parser != null) {
            return parser.getHeaderMap().keySet();
        }
        return new HashSet<>();
    }

    private static void createIfNotExists(String fileLoc) {
        File file = new File(fileLoc);
        file.getParentFile().mkdirs();
    }

    public static void saveChanges(GlobalDataModel globalData) {
        createIfNotExists(globalData.getLocation());
        try (FileWriter out = new FileWriter(new File(globalData.getLocation()));
                CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
            for (String header : globalData.getColumns()) {
                printer.print(header);
            }
            printer.println();
            globalData.removeEmptyRecords();
            for (List<String> record : globalData.getRecords()) {
                for (String value : record) {
                    printer.print(value);
                }
                printer.println();
            }
        } catch (Exception ex) {
            Logger.getLogger(CSVUtils.class.getName()).log(Level.SEVERE, "Error while saving", ex);
        }
    }

    public static void saveChanges(TestDataModel testData) {
        createIfNotExists(testData.getLocation());
        try (FileWriter out = new FileWriter(new File(testData.getLocation()));
                CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
            for (String header : testData.getColumns()) {
                printer.print(header);
            }
            printer.println();
            testData.removeEmptyRecords();
            for (Record record : testData.getRecords()) {
                for (String value : record) {
                    printer.print(value);
                }
                printer.println();
            }
        } catch (Exception ex) {
            Logger.getLogger(CSVUtils.class.getName()).log(Level.SEVERE, "Error while saving", ex);
        }
    }
}
