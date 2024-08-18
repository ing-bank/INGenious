
package com.ing.testdata.csv;

import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CsvGlobalData extends GlobalDataModel {

    public CsvGlobalData(String location) {
        super(location, true);
    }

    @Override
    public List<String> getNewRecord() {
        return new ArrayList<>();
    }

    @Override
    public Boolean rename(String newName) {
        //Not Supported
        return false;
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
    public CsvGlobalData loadRecords(File location) {
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

}
