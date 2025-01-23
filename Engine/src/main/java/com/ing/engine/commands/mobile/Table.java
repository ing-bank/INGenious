
package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class Table extends Command {

    public Table(CommandControl cc) {
        super(cc);
    }
/*
    @Action(object = ObjectType.MOBILE,desc ="Get data from the desired cell of the web table and store it in a variable", input =InputType.YES)
    public void getCellValue() {
        if (Element != null) {
            String strValue = Data;
            String[] userInput = strValue.split(",");
            String tableDetails = userInput[0];
            String variable = userInput[1];

            String[] splitVal = tableDetails.split(";");
            int RowNo = Integer.parseInt(splitVal[0]);
            int ColNo = Integer.parseInt(splitVal[1]);

            String vCellValue = null;

            List<WebElement> allRows = Element.findElements(By.tagName("tr"));
            if (!allRows.isEmpty()) {
                List<WebElement> Cells = allRows.get(RowNo).findElements(
                        By.tagName("td"));
                List<WebElement> cellHdrs = allRows.get(RowNo).findElements(
                        By.tagName("th"));
                if (!Cells.isEmpty()) {
                    vCellValue = Cells.get(ColNo).getText();
                    addVar(variable, vCellValue);
                    Report.updateTestLog("getCellValue",
                            "Table cell value " + vCellValue
                            + " has been stored into " + variable,
                            Status.PASS);
                } else if (!cellHdrs.isEmpty()) {
                    vCellValue = Cells.get(ColNo).getText();
                    Report.updateTestLog("getCellValue",
                            "Table cell value " + vCellValue
                            + " has been stored into " + variable,
                            Status.PASS);
                } else {
                    Report.updateTestLog("getCellValue",
                            "Table Column size is zero", Status.FAIL);
                    vCellValue = null;
                }
            } else {
                Report.updateTestLog("getCellValue",
                        "Table Row size is zero", Status.FAIL);
                vCellValue = null;
            }
        } else {
            Report.updateTestLog("getCellValue", "Object [" + ObjectName + "] not found",
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE,desc ="Count the number of columns in a row in a web table and store it in a variable",input =InputType.YES)
    public void getColCount() {
        if (Element != null) {
            String inputData = Data;
            String[] userInput = inputData.split(",");
            String vRowNo = userInput[0];
            String variable = userInput[1];

            int RowNo = Integer.parseInt(vRowNo);

            int intColCount = 0;

            List<WebElement> allRows = Element.findElements(By.tagName("tr"));
            if (!allRows.isEmpty()) {
                List<WebElement> Cells = allRows.get(RowNo).findElements(
                        By.tagName("td"));
                List<WebElement> cellHdrs = allRows.get(RowNo).findElements(
                        By.tagName("th"));
                if (!Cells.isEmpty()) {
                    intColCount = Cells.size();
                    addVar(variable, String.valueOf(intColCount));
                    Report.updateTestLog("getColCount", "Table row has '"
                            + intColCount + "' columns, stored in " + variable,
                            Status.PASS);
                } else if (!cellHdrs.isEmpty()) {
                    intColCount = cellHdrs.size();
                    addVar(variable, String.valueOf(intColCount));
                    Report.updateTestLog("getColCount", "Table row has '"
                            + intColCount + "' columns, stored in " + variable,
                            Status.PASS);
                } else {
                    intColCount = 0;
                    Report.updateTestLog("getColCount",
                            "Table Column size is zero", Status.FAIL);
                }
            } else {
                Report.updateTestLog("getColCount",
                        "Table column size is zero", Status.FAIL);
                intColCount = 0;
            }

        } else {
            Report.updateTestLog("getColCount", "Object [" + ObjectName + "] not found",
                    Status.FAIL);
        }

    }

    @Action(object = ObjectType.MOBILE,desc ="Count the number of rows in a web table and store it in variable", input =InputType.YES)
    public void getRowCount() {
        if (Element != null) {
            int intRowCount = 0;
            String variable = Data;

            List<WebElement> allRows = Element.findElements(By.tagName("tr"));
            if (!allRows.isEmpty()) {
                intRowCount = allRows.size();
                addVar(variable, String.valueOf(intRowCount));
                Report.updateTestLog("getRowCount", "Table has '" + intRowCount
                        + "' rows, stored in variable " + variable, Status.PASS);
            } else {
                Report.updateTestLog("getRowCount", "Table Row size is zero",
                        Status.FAIL);
            }
        } else {
            Report.updateTestLog("getRowCount", "Object [" + ObjectName + "] not found",
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc ="Get the number of the row having the desired data [<Data>] in a web table and store it in a variable", input =InputType.YES)
    public void getRowNumber() {
        if (Element != null) {
            String userInput = Data;
            String[] input = userInput.split(",");
            String CellValue = input[0];
            String variable = input[1];

            int rowCount = 0;
            int rtnValue = 0;

            List<WebElement> allRows = Element.findElements(By.tagName("tr"));

            for (WebElement row : allRows) {
                List<WebElement> cells = row.findElements(By.tagName("td"));
                List<WebElement> cellHdrs = row.findElements(By.tagName("th"));
                if (!cells.isEmpty()) {
                    for (WebElement col : cells) {
                        if (col.getText().equals(CellValue.trim())) {
                            rtnValue = rowCount;
                            addVar(variable, String.valueOf(rtnValue));
                            Report.updateTestLog("getRowNumber",
                                    "Desired data is in'" + rtnValue
                                    + "' row, stored in variable "
                                    + variable, Status.PASS);
                            break;
                        }
                    }
                } else if (!cellHdrs.isEmpty()) {
                    for (WebElement col : cellHdrs) {
                        if (col.getText().equals(CellValue.trim())) {
                            rtnValue = rowCount;
                            addVar(variable, String.valueOf(rtnValue));
                            Report.updateTestLog("getRowNumber",
                                    "Desired data is in'" + rtnValue
                                    + "' row, stored in variable "
                                    + variable, Status.PASS);
                            break;
                        }

                    }
                } else {
                    Report.updateTestLog("getRowNumber",
                            "Table doesn't have the desired data ", Status.FAIL);
                }
                if (!(rtnValue == 0)) {
                    break;
                } else {
                    rowCount++;
                }
            }
        } else {
            Report.updateTestLog("getRowNumber", "Object [" + ObjectName + "] not found",
                    Status.FAIL);
        }

    }

    @Action(object = ObjectType.MOBILE,desc ="Get the column number of the column having the [<Data>] in a web table and store it in a variable",input =InputType.YES)
    public void getColNumber() {
        if (Element != null) {
            String userInput = Data;
            String[] input = userInput.split(",");
            String CellValue = input[0];
            String variable = input[1];

            int colCount = 0;
            int rtnValue = 0;
            List<WebElement> allRows = Element.findElements(By.tagName("tr"));

            for (WebElement row : allRows) {
                colCount = 0;
                List<WebElement> cells = row.findElements(By.tagName("td"));
                List<WebElement> cellHdrs = row.findElements(By.tagName("th"));
                if (!cells.isEmpty()) {
                    for (WebElement col : cells) {
                        if (col.getText().equals(CellValue.trim())) {
                            rtnValue = colCount;
                            addVar(variable, String.valueOf(rtnValue));
                            Report.updateTestLog("getColNumber",
                                    "Desired data is in'" + rtnValue
                                    + "' column, stored in variable "
                                    + variable, Status.PASS);
                            break;
                        }
                        colCount++;
                    }
                } else if (!cellHdrs.isEmpty()) {
                    for (WebElement col : cells) {
                        if (col.getText().equals(CellValue.trim())) {
                            rtnValue = colCount;
                            addVar(variable, String.valueOf(rtnValue));
                            Report.updateTestLog("getColNumber",
                                    "Desired data is in'" + rtnValue
                                    + "' column, stored in variable "
                                    + variable, Status.PASS);
                            break;
                        }
                        colCount++;
                    }
                } else {
                    Report.updateTestLog("getColNumber",
                            "Table doesn't have the desired data ", Status.FAIL);
                }
                if (!(rtnValue == 0)) {
                    break;
                }
            }
        } else {
            Report.updateTestLog("getColNumber", "Object [" + ObjectName + "] not found",
                    Status.FAIL);
        }
    }
*/
}
