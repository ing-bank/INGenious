
package com.ing.testdata.csv;

import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.util.Set;

/**
 *
 * @author 394173
 */
public class CsvTestData extends TestDataModel {

    public CsvTestData(String location) {
        super(location, true);
    }

    @Override
    public Record getNewRecord() {
        return new Record();
    }

    @Override
    public Boolean rename(String newName) {
        File f = new File(getLocation());
        File newFile = new File(f.getParent() + File.separator + newName + ".csv");
        if (f.exists()) {
            if (f.renameTo(newFile)) {
                setLocation(newFile.getAbsolutePath());
            } else {
                return false;
            }
        } else {
            setLocation(newFile.getAbsolutePath());
        }
        return true;
    }

    @Override
    public void saveChanges() {
        CSVUtils.saveChanges(this);
    }

    @Override
    public String getName() {
        String name = new File(getLocation()).getName();
        return name.substring(0, name.lastIndexOf("."));
    }

    @Override
    public Set<String> loadColumns(File location) {
        return CSVUtils.loadColumns(location);
    }

    @Override
    public CsvTestData loadRecords(File location) {
        CSVUtils.load(location, this);
        return this;
    }

    @Override
    public Set<String> loadColumns() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AbstractDataModel loadRecords() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Boolean delete() {
        File f = new File(getLocation());
        if (f.exists()) {
            return f.delete();
        }
        return true;
    }

}
