package com.ing.engine.execution.exception;

public class ActionException extends RuntimeException  {
    public String ErrorName;
    public String ErrorDescription;
    
	public ActionException(Throwable ex) {
        super(ex);
//        this.ErrorName = errorName;
        this.ErrorDescription = ex.getMessage();
	}

}
