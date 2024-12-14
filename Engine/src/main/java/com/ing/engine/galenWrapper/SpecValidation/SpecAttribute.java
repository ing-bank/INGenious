
package com.ing.engine.galenWrapper.SpecValidation;

import com.galenframework.specs.SpecText;

/**
 *
 * 
 */
public class SpecAttribute extends SpecText {

    private String attributeName;

    public SpecAttribute(String attributeName, Type type, String text) {
        super(type, text);
        this.attributeName = attributeName;
    }

    public String getAtributeName() {
        return attributeName;
    }

    public void setAtributeName(String attributeName) {
        this.attributeName = attributeName;
    }

}
