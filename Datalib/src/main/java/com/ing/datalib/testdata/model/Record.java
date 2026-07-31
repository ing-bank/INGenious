package com.ing.datalib.testdata.model;

import java.util.ArrayList;

/**
 *
 *
 */
public class Record extends ArrayList<String> {
    public static final String[] HEADERS = new String[] {
        "Scenario",
        "Flow",
        "Scope",
        "Iteration",
        "SubIteration"
    };

    public Record() {
        super();
        // initialize with empty strings for each header to avoid index errors when CSVs are missing columns
        for (int i = 0; i < HEADERS.length; i++) {
            super.add("");
        }
    }

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
        return get(3);
    }

    public String getSubIteration() {
        return get(4);
    }

    public void setScenario(String scenario) {
        set(0, scenario);
    }

    public void setTestcase(String testCase) {
        set(1, testCase);
    }

    public void setIteration(String iteration) {
        set(3, iteration);
    }

    public void setSubIteration(String subIteration) {
        set(4, subIteration);
    }

    public String getScope() {
        return get(2);
    }

    public void setScope(String scope) {
        set(2, scope);
    }

    @Override
    public String set(int i, String e) {
        switch (i) {
            case 3:
            case 4:
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
