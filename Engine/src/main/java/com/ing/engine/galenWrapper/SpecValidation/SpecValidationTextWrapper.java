
package com.ing.engine.galenWrapper.SpecValidation;

import com.galenframework.specs.SpecText;
import com.galenframework.validation.PageValidation;
import com.galenframework.validation.ValidationErrorException;
import com.galenframework.validation.ValidationResult;
import com.galenframework.validation.specs.SpecValidationText;
import com.galenframework.validation.specs.TextOperation;
import java.util.List;

/**
 *
 * 
 * @param <T>
 */
public abstract class SpecValidationTextWrapper<T extends SpecText> extends SpecValidationText<T> {

    @Override
    abstract public ValidationResult check(PageValidation pageValidation, String objectName, T spec) throws ValidationErrorException;

    public String applyOperationsTo(String text, List<String> operations) {
        if (operations != null) {
            for (String operation : operations) {
                text = TextOperation.find(operation).apply(text);
            }
        }
        return text;
    }
}
