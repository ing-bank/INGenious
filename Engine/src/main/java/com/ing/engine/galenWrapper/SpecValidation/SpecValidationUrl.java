
package com.ing.engine.galenWrapper.SpecValidation;

import com.galenframework.validation.PageValidation;
import com.galenframework.validation.ValidationErrorException;
import com.galenframework.validation.ValidationObject;
import com.galenframework.validation.ValidationResult;
import static java.util.Arrays.asList;

/**
 *
 * 
 */
public class SpecValidationUrl extends SpecValidationTextWrapper<SpecUrl> {

    @Override
    public ValidationResult check(PageValidation pageValidation, String objectName, SpecUrl spec) throws ValidationErrorException {

        String realText = pageValidation.getBrowser().getUrl();
        if (realText == null) {
            realText = "";
        }
        realText = applyOperationsTo(realText, spec.getOperations());
        checkValue(spec, objectName, realText, "Url", null);

        return new ValidationResult(spec, asList(new ValidationObject(null, objectName)));
    }
}
