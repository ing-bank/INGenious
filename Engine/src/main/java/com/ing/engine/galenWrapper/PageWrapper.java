
package com.ing.engine.galenWrapper;

import com.galenframework.page.PageElement;
import com.galenframework.page.selenium.SeleniumPage;
import com.galenframework.page.selenium.WebPageElement;
import com.galenframework.specs.page.Locator;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 * 
 */
public class PageWrapper extends SeleniumPage {

    Map<String, WebElement> elementMap = new HashMap<>();
    private WebDriver driver;

    public PageWrapper(WebDriver driver) {
        super(driver);
        this.driver = driver;
    }

    public PageWrapper(WebDriver driver, Map<String, WebElement> elementMap) {
        this(driver);
        this.elementMap = elementMap;
    }

    public PageWrapper(WebDriver driver, String objectName, WebElement element) {
        super(driver);
        if (element != null) {
            this.elementMap.put(objectName, element);
        }
    }

    @Override
    public PageElement getObject(String objectName, Locator lctr) {
        return new WebPageElement(driver,objectName, elementMap.get(objectName), null);
    }

    public WebDriver getDriver() {
        return driver;
    }
}
