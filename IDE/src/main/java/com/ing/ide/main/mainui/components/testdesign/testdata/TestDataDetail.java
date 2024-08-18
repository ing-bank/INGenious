
package com.ing.ide.main.mainui.components.testdesign.testdata;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class TestDataDetail {

    private String sheetName;
    private List<String> columnNames = new ArrayList<>();

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }
}
