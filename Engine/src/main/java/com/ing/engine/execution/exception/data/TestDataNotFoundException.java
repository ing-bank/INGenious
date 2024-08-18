
package com.ing.engine.execution.exception.data;

import com.ing.engine.execution.run.TestCaseRunner;

/**
 *
 * 
 */
@SuppressWarnings("serial")
public class TestDataNotFoundException extends DataNotFoundException {
    
    
	public String sheet;

    public TestDataNotFoundException(TestCaseRunner context, String sheet, String field, Cause c, String info) {
        super(String.format("Test Data Not Found, %s - %s is missing.", c.name(), info));
        this.context = context;
        this.field = field;
        this.sheet = sheet;
        this.cause = new CauseInfo(c, info);
    }

    @Override
    public String toString() {
        try {
            return getFormatted(getTemplate(context.isReusable()), getMessage(), context.executor().runEnv(), sheet,
                    field, context.getRoot().scenario(), context.getRoot().testcase(), context.scenario(),
                    context.testcase());
        } catch (Exception ex) {
            return super.toString();
        }
    }

    public static String getTemplate(Boolean isReusable) {
        return "{0} \n[Env : {1} | Sheet : {2} | Field : {3} | TestCase : {4}/{5}"
                + (isReusable ? " | Reusabe : {6}/{7} ]" : " ]");
    }

}
