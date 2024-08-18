
package com.ing.engine.execution.exception.data;

import com.ing.engine.execution.run.TestCaseRunner;
import java.text.MessageFormat;

/**
 *
 * 
 */
public class DataNotFoundException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = -8606941865608503468L;
    public TestCaseRunner context;
    public String field;
    public CauseInfo cause;

    protected DataNotFoundException(String name) {
        super(name);
    }
    
    public static String getFormatted(String template, Object... args) {
        return MessageFormat.format(template, args);
    }

    public static String getTemplate(Boolean isReusable) {
        return "{0} \n[Env : {1} | Field : {2} | TestCase : {4}/{5}"
                + (isReusable ? " | Reusabe : {6}/{7} ]" : " ]");
    }

    public enum Cause {
        Data, Iteration, SubIteration
    }

    public class CauseInfo {

        public Cause type;
        public String info;

        public CauseInfo(Cause c, String info) {
            this.type = c;
            this.info = info;
        }

        public boolean isIter() {
            return type == Cause.Iteration;
        }

        public boolean isSubIter() {
            return type == Cause.SubIteration;
        }

    }

}
