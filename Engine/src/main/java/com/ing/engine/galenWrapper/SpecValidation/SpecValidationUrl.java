package com.ing.engine.galenWrapper.SpecValidation;

import static java.util.Arrays.asList;

import com.galenframework.validation.PageValidation;
import com.galenframework.validation.ValidationErrorException;
import com.galenframework.validation.ValidationObject;
import com.galenframework.validation.ValidationResult;

/**
 *
 *
 */
public class SpecValidationUrl extends SpecValidationTextWrapper<SpecUrl> {

    @Override
    public ValidationResult check(PageValidation pageValidation, String objectName, SpecUrl spec)
        throws ValidationErrorException {
        String realText = pageValidation.getBrowser().getUrl();
        if (realText == null) {
            realText = "";
        }
        realText = applyOperationsTo(realText, spec.getOperations());
        checkValue(spec, objectName, realText, "Url", null);

        return new ValidationResult(spec, asList(new ValidationObject(null, objectName)));
    }
}
