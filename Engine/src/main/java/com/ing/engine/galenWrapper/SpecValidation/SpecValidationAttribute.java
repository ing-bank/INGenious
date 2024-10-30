
package com.ing.engine.galenWrapper.SpecValidation;

import com.galenframework.page.Rect;
import com.galenframework.page.selenium.WebPageElement;
import com.galenframework.validation.PageValidation;
import com.galenframework.validation.ValidationErrorException;
import com.galenframework.validation.ValidationObject;
import com.galenframework.validation.ValidationResult;
import static java.util.Arrays.asList;

/**
 *
 * 
 */
public class SpecValidationAttribute extends SpecValidationTextWrapper<SpecAttribute> {

    @Override
    public ValidationResult check(PageValidation pageValidation, String objectName, SpecAttribute spec) throws ValidationErrorException {

        WebPageElement mainObject = (WebPageElement) pageValidation.findPageElement(objectName);

        checkAvailability(mainObject, objectName);

        Rect area = mainObject.getArea();
        String realText = mainObject.getWebElement().getAttribute(spec.getAtributeName());
        if (realText == null) {
            realText = "";
        }
        realText = applyOperationsTo(realText, spec.getOperations());
        checkValue(spec, objectName, realText, "Attribute \"" + spec.getAtributeName() + "\"", area);

        return new ValidationResult(spec,asList(new ValidationObject(area, objectName)));
    }

}
