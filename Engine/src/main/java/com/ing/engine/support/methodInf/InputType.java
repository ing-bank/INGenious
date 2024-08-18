
package com.ing.engine.support.methodInf;

public enum InputType {
    YES,
    NO,
    OPTIONAL;

    public Boolean isOptional() {
        return this.equals(OPTIONAL);
    }

    public Boolean isMandatory() {
        return this.equals(YES);
    }
    
    public Boolean isNotNeeded() {
        return this.equals(NO);
    }
}
