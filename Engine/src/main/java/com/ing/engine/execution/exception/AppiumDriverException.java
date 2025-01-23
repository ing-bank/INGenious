
package com.ing.engine.execution.exception;

public class AppiumDriverException extends RuntimeException {

    public String ErrorName;
    public String ErrorDescription;
    

    public AppiumDriverException(String msg) {
        super(msg);
    }

}
