
package com.ing.engine.execution.exception.data;

import com.ing.engine.execution.run.TestCaseRunner;

/**
 *
 * 
 */
@SuppressWarnings("serial")
public class GlobalDataNotFoundException extends DataNotFoundException {   
	
	public String gid;

    public GlobalDataNotFoundException(TestCaseRunner context,
            String gid, String field) {
        super("Global Data Not Found..");
        this.context = context;
        this.field = field;
        this.gid = gid;
    }

    @Override
    public String toString() {
        try {
            return getFormatted(getTemplate(context.isReusable()), getMessage(),
                    context.executor().runEnv(), field, gid,
                    context.getRoot().scenario(), context.getRoot().testcase(),
                    context.scenario(), context.testcase());
        } catch (Exception ex) {
            return super.toString();
        }
    }

    public static String getTemplate(Boolean isReusable) {
        return "{0} \n[Env : {1} | Field : {2} | GID : {3} | TestCase : {4}/{5}"
                + (isReusable ? " | Reusabe : {6}/{7} ]" : " ]");
    }

}
