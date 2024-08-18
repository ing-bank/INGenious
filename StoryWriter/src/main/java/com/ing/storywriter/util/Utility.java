
package com.ing.storywriter.util;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utility {

    static final SimpleDateFormat DATA_FORMAT = new SimpleDateFormat("MM/dd/yyyy"),
            TIME_FORMAT = new SimpleDateFormat("hh:mm:ss a"),
            DATE_FILE_FORMAT = new SimpleDateFormat("MM-dd-yyyy"),
            TIME_FILE_FORMAT = new SimpleDateFormat("hh-mm-ssa"),
            LIC_DATE_FORMAT = new SimpleDateFormat("ddMMyyyy");

    static Path path;
    static File file;
 
    private Utility() {

    }

  

    public static boolean isEmpty(Object val) {
        return val == null || "".equals(val.toString().trim());
    }

  
    public static String getdatetimeString() {
        Date dat = new Date();
        return DATE_FILE_FORMAT.format(dat) + "_" + TIME_FILE_FORMAT.format(dat);
    }

    public static String getdateString() {
        Date dat = new Date();
        return DATE_FILE_FORMAT.format(dat);
    }

    

   
    public static String getValue(Object value) {
        if (isEmpty(value)) {
            return "";
        }
        return value.toString();
    }

    /**
     * return the Date difference in no of days
     *
     * @param exp
     * @return
     */
    public static int getDays(Date exp) {
        try {
            Date today = new Date();
            if (exp != null) {
                long diff = exp.getTime() - today.getTime();
                return (int) (diff / (24 * 60 * 60 * 1000));
            }
        } catch (Exception ex) {
            Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 30;
    }

}
