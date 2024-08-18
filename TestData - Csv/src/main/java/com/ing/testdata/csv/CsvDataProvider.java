
package com.ing.testdata.csv;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import java.io.File;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.DataProvider;
import com.ing.datalib.testdata.model.Record;

@DataProvider(type = "csv")
public class CsvDataProvider extends TestData {

    public CsvDataProvider(Project sProject, String enviroment) {
        super(sProject, enviroment);
    }

    @Override
    public void load() {
        File file = new File(getLocation());
        if (file.exists()) {
            for (File tData : file.listFiles(FileUtils.CSV_FILTER)) {
                if (!tData.getName().equals("GlobalData.csv")) {
                    addTestData(new CsvTestData(tData.getAbsolutePath()));
                }
            }
        }
        loadGlobalData();
    }

    private void loadGlobalData() {
        File file = new File(getLocation() + File.separator + "GlobalData.csv");
        setGlobalData(new CsvGlobalData(file.getAbsolutePath()));
    }

    @Override
    public CsvTestData getNewTestData(String name) {
        CsvTestData csvData = new CsvTestData(getLocation() + File.separator + name + ".csv");
        csvData.setColumns(Record.HEADERS);
        csvData.addColumn("Data1");
        csvData.addColumn("Data2");
        return csvData;
    }

    @Override
    public CsvTestData importTestData(File file) {
        CsvTestData csvData = new CsvTestData(file.getAbsolutePath());
        csvData.loadTableModel();
        csvData.setLocation(getLocation() + File.separator + file.getName());
        csvData.saveChanges();
        return csvData;
    }

}
