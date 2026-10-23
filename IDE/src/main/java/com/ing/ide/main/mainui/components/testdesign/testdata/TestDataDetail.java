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
    private boolean shared;

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    /**
     * Whether the dragged sheet came from the Shared Test Data tab rather than the project's own.
     */
    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }
}
