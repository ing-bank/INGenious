
package com.ing.engine.drivers.findObjectBy;

import com.ing.engine.drivers.findObjectBy.support.SProperty;
import io.appium.java_client.AppiumBy;
//import io.appium.java_client.MobileBy;
import org.openqa.selenium.By;

/**
 *
 * 
 */
public class DefaultFindBy {

    @SProperty(name = "id")
    public By getById(String id) {
        return By.id(id);
    }

    @SProperty(name = "name")
    public By getByName(String name) {
        return By.name(name);
    }

    @SProperty(name = "xpath")
    public By getByXpath(String xpath) {
        return By.xpath(xpath);
    }

    @SProperty(name = "relative_xpath")
    public By getByRXpath(String xpath) {
        return By.xpath(xpath);
    }

    @SProperty(name = "css")
    public By getByCss(String css) {
        return By.cssSelector(css);
    }

    @SProperty(name = "link_text")
    public By getByLinkText(String linkText) {
        return By.linkText(linkText);
    }

    @SProperty(name = "partialLinkText")
    public By getByPartialLinkText(String linkText) {
        return By.partialLinkText(linkText);
    }

    @SProperty(name = "class")
    public By getByClass(String className) {
        if (className.contains(" ")) {
            return getByXpath("//*[@className='" + className + "']");
        }
        return By.className(className);
    }
   
    @SProperty(name = "type")
    public By getByType(String tagName) {
         return By.tagName(tagName);
    }

    @SProperty(name = "Accessibility")
    public By getByAccess(String access) {
        return AppiumBy.accessibilityId(access);
    }
    
    @SProperty(name = "UiAutomator")
    public By getByUiAutomator(String uiAutomator) {
        return AppiumBy.androidUIAutomator(uiAutomator);
    }
    
/*
    @SProperty(name = "UiAutomator")
    public By getByUiAutomator(String uiAutomator) {
        return MobileBy.AndroidUIAutomator(uiAutomator);
    }

    @SProperty(name = "UiAutomation")
    public By getByUiAutomation(String uiAutomation) {
        return MobileBy.IosUIAutomation(uiAutomation);
    }
*/
}
