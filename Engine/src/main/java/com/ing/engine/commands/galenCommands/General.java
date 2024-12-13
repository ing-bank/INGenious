
package com.ing.engine.commands.galenCommands;

import com.ing.engine.core.CommandControl;
import com.ing.engine.galenWrapper.PageValidationWrapper;
import com.ing.engine.galenWrapper.PageWrapper;
import com.galenframework.specs.Spec;
import com.galenframework.specs.SpecImage;
import com.galenframework.validation.ValidationResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.openqa.selenium.WebElement;

public class General extends Report {

    public enum RelativeElement {

        WebElement, WebElementList, None;

    };

    public General(CommandControl cc) {
        super(cc);
    }

    private Map<String, WebElement> getRelativeElement(RelativeElement elementType) {
        switch (elementType) {
            case WebElement:
                return getRelativeElement();
            case WebElementList:
                return getRelativeElementList();
            default:
                return new HashMap<>();
        }
    }

    private Map<String, WebElement> getRelativeElement() {
        Map<String, WebElement> elementMap = new HashMap<>();
        if (Condition != null && !Condition.trim().isEmpty()) {
            WebElement element = mObject.findElement(Condition, Reference);
            if (element != null) {
                elementMap.put(Condition, element);
            }
        }
        return elementMap;
    }

    private Map<String, WebElement> getRelativeElementList() {
        if (Condition != null && !Condition.trim().isEmpty()) {
            return getElementsfromList(Reference, Condition);
        }
        return null;
    }

    public Map<String, WebElement> getElementsfromList(String Page, String regexData) {
        return mObject.findElementsByRegex(regexData, Page);
    }

    public List<String> getElementsList() {
        if (Condition != null && !Condition.trim().isEmpty()) {
            return mObject.getObjectList(Reference, Condition);
        }
        return null;
    }

    public PageValidationWrapper getPageValidation(Spec spec, RelativeElement relativeElement) {
        Map<String, WebElement> elementMap = getRelativeElement(relativeElement);
        if (Element != null) {
            elementMap.put(ObjectName, Element);
        }
        if (spec instanceof SpecImage) {
            Optional.ofNullable(((SpecImage) spec).getIgnoredObjectExpressions())
                    .ifPresent((ioe) -> ioe.stream().flatMap((expr) -> Stream.of(expr.split(",")))
                    .forEach((String object) -> {
                        elementMap.put(object, mObject.findElement(object, Reference));
                    }));
        }
        return new PageValidationWrapper(new PageWrapper(mDriver, elementMap), elementMap);
    }

    public PageValidationWrapper getPageValidation(RelativeElement relativeElement) {
        Map<String, WebElement> elementMap = getRelativeElement(relativeElement);
        if (Element != null) {
            elementMap.put(ObjectName, Element);
        }
        return new PageValidationWrapper(new PageWrapper(mDriver, elementMap), elementMap);
    }

    public void validate(Spec spec, RelativeElement relativeElement) {
        try {
            PageValidationWrapper pageValidation = getPageValidation(spec, relativeElement);
            ValidationResult result = pageValidation.check(ObjectName, spec);
            if (result.getError() != null) {
                onError(spec, result);
            } else {
                onSuccess(spec, result);

            }
        } catch (Exception ex) {
            Logger.getLogger(General.class
                    .getName()).log(Level.SEVERE, null, ex);
            onError(ex);
        }
    }

    public void validate(Spec spec) {
        validate(spec, RelativeElement.WebElement);
    }

}
