
package com.ing.engine.galenWrapper;

import com.galenframework.parser.ExpectRange;
import com.galenframework.parser.Expectations;
import com.galenframework.parser.StringCharReader;
import com.galenframework.specs.Location;
import com.galenframework.specs.Range;
import com.galenframework.specs.colors.ColorRange;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class Parser {

    public static Range parseRange(String Data) {
        return Data == null || Data.trim().isEmpty() ? Range.greaterThan(-1)
                : Expectations.range().read(new StringCharReader(Data));
    }

    public static Range parseRangePercent(String Data) {
        return Data == null || Data.trim().isEmpty() ? Range.greaterThan(-1)
                : getRange(Data);
    }

    private static Range getRange(String Data) {
        ExpectRange expectRange = new ExpectRange();
        expectRange.setEndingWord("%");
        return expectRange.read(new StringCharReader(Data));
    }

    public static List<Location> parseLocation(String Data) {
        return Data == null || Data.trim().isEmpty() ? new ArrayList<Location>() : Expectations.locations().read(new StringCharReader(Data));
    }

    public static List<ColorRange> parseColorRanges(String Data) {
        return Data == null || Data.trim().isEmpty() ? new ArrayList<ColorRange>() : Expectations.colorRanges().read(new StringCharReader(Data));
    }

    public static int parseInt(String Data) {
        return Data == null || Data.trim().isEmpty() ? 0 : parseInteger(Data);
    }

    public static int parseInt(Object Data) {
        return Data == null ? 0 : parseInt(Data.toString());
    }

    private static int parseInteger(String Data) {
        if (Data.matches("[0-9]+")) {
            return Integer.parseInt(Data);
        } else {
            return 0;
        }
    }

}
