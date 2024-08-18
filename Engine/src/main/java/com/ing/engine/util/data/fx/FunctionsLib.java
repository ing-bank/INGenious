
package com.ing.engine.util.data.fx;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

/**
 *
 * 
 */
public class FunctionsLib {

    public Object getDate(int dx) {
        return getDate(dx, "dd/MM/yyyy");
    }

    public Object getDate(int dx, String format) {
        SimpleDateFormat date = new SimpleDateFormat(format);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, dx);
        return date.format(cal.getTime());
    }

    public Object getRound(Double val) {
        return Math.round(val);
    }

    public Object getRandom(Double from, Double to) {
        Random rn = new Random(System.currentTimeMillis());
        return from + (rn.nextDouble() * (to - from));
    }

    public Object getRandom(Double len) {
        return getRandom(Math.pow(10d, len - 1), Math.pow(10d, len) - 1);
    }

    public Object getPow(Double a, Double b) {
        return Math.pow(a, b);
    }

    public Object getMin(Double a, Double b) {
        return Math.min(a, b);
    }

    public Object getMax(Double a, Double b) {
        return Math.max(a, b);
    }

}
