
package com.ing.datalib.component.utils;

import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * 
 */
public class CSVHParser {

    private final Map<String, Integer> headerMap;
    private final List<CSVRecord> records;

    public CSVHParser(Map<String, Integer> headerMap, List<CSVRecord> records) {
        this.headerMap = headerMap;
        this.records = records;
    }

    public Map<String, Integer> getHeaderMap() {
        return headerMap;
    }

    public List<CSVRecord> getRecords() {
        return records;
    }
}
