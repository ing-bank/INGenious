
package com.ing.engine.galenWrapper;

import com.ing.engine.galenWrapper.SpecValidation.SpecAttribute;
import com.ing.engine.galenWrapper.SpecValidation.SpecTitle;
import com.ing.engine.galenWrapper.SpecValidation.SpecUrl;
import com.ing.engine.galenWrapper.SpecValidation.SpecValidationAttribute;
import com.ing.engine.galenWrapper.SpecValidation.SpecValidationTitle;
import com.ing.engine.galenWrapper.SpecValidation.SpecValidationUrl;
import com.galenframework.browser.SeleniumBrowser;
import com.galenframework.page.PageElement;
import com.galenframework.page.selenium.WebPageElement;
import com.galenframework.specs.Spec;
import com.galenframework.validation.PageValidation;
import com.galenframework.validation.SpecValidation;
import com.galenframework.validation.ValidationErrorException;
import com.galenframework.validation.ValidationFactory;
import com.galenframework.validation.ValidationResult;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebElement;

/**
 *
 * 
 */
public class PageValidationWrapper extends PageValidation {

    Map<String, WebElement> elementMap = new HashMap<>();

    public PageValidationWrapper(PageWrapper page) {
        super(new SeleniumBrowser(page.getDriver()), page, new PageSpecWrapper(), null, null);
    }

    public PageValidationWrapper(PageWrapper page, Map<String, WebElement> elementMap) {
        this(page);
        this.elementMap = elementMap;
    }

    public PageValidationWrapper(PageWrapper page, String objectName, WebElement element) {
        this(page);
        if (element != null) {
            this.elementMap.put(objectName, element);
        }
    }

    @Override
    public ValidationResult check(String objectName, Spec spec) {
        ((PageSpecWrapper) this.getPageSpec()).setObjectMap(elementMap);
        SpecValidation<?> specValidation = ValidationFactoryWrapper.getValidation(spec, this);

        ValidationResult result = check(specValidation, objectName, spec);

        if (spec.isOnlyWarn()) {
            result.getError().setOnlyWarn(true);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ValidationResult check(SpecValidation specValidation, String objectName, Spec spec) {
        try {
            return specValidation.check(this, objectName, spec);
        } catch (ValidationErrorException ex) {
            return ex.asValidationResult(spec);
        }
    }

    @Override
    public PageElement findPageElement(String objectName) {
        if (elementMap.get(objectName) != null) {
            return new WebPageElement(((PageWrapper) getPage()).getDriver(), objectName, elementMap.get(objectName), null);
        } else {
            return super.findPageElement(objectName);
        }
    }

}

class ValidationFactoryWrapper {

    public static SpecValidation<? extends Spec> getValidation(Spec spec, PageValidation pageValidation) {
        try {
            return ValidationFactory.getValidation(spec, pageValidation);
        } catch (Exception ex) {
            if (spec.getClass().equals(SpecUrl.class)) {
                return new SpecValidationUrl();
            } else if (spec.getClass().equals(SpecTitle.class)) {
                return new SpecValidationTitle();
            } else if (spec.getClass().equals(SpecAttribute.class)) {
                return new SpecValidationAttribute();
            }
            throw ex;
        }
    }
}
