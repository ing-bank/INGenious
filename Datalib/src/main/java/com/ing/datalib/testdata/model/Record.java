
package com.ing.datalib.testdata.model;

import java.util.ArrayList;

/**
 *
 * 
 */
public class Record extends ArrayList<String> {

    public static final String[] HEADERS = new String[]{"Scenario", "Flow", "Iteration", "SubIteration"};

    @Override
    public String remove(int i) {
        if (i >= HEADERS.length) {
            return String.valueOf(super.remove(i));
        }
        return String.valueOf(get(i));
    }

    public String getScenario() {
        return get(0);
    }

    public String getTestcase() {
        return get(1);
    }

    public String getIteration() {
        return get(2);
    }

    public String getSubIteration() {
        return get(3);
    }

    public void setScenario(String scenario) {
        set(0, scenario);
    }

    public void setTestcase(String testCase) {
        set(1, testCase);
    }

    public void setIteration(String iteration) {
        set(2, iteration);
    }

    public void setSubIteration(String subIteration) {
        set(3, subIteration);
    }

    @Override
    public String set(int i, String e) {
        switch (i) {
            case 2:
            case 3:
                if (!validIterRSubIteration(e)) {
                    if (!validIterRSubIteration(get(i))) {
                        e = "1";
                    } else {
                        return get(i);
                    }
                }
                break;

        }
        return super.set(i, e);
    }

    private Boolean validIterRSubIteration(String value) {
        return value.isEmpty() || value.matches("[1-9][0-9]*");
    }

}
