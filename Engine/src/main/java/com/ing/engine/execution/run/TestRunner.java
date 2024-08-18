
package com.ing.engine.execution.run;


/**
 *
 * 
 */
public interface TestRunner {

    public Object runEnv();

    public Object dataProvider();

    public Object getProject();

    public boolean isContinueOnError();
    

}
