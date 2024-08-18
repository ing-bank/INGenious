
package com.ing.engine.support;

import com.ing.engine.constants.SystemDefaults;
import java.util.Objects;

public class DLogger {

    public static void Log(Object... msgs) {

        if (debug()) {
            for (Object msg : msgs) {
                System.out.print(" " + Objects.toString(msg, "NULL") + " ");
            }
            System.out.println();
        }
    }

    public static boolean debug() {
        return SystemDefaults.debug(); 
        
    }

    public static void LogE(Object... msgs) {
        if (debug()) {
            for (Object msg : msgs) {
                System.err.print(" " + Objects.toString(msg, "NULL") + " ");
            }
            System.err.println();
        }
    }

}
