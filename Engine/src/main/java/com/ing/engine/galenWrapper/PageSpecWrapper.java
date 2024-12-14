
package com.ing.engine.galenWrapper;

import com.galenframework.specs.page.Locator;
import com.galenframework.specs.page.PageSpec;
import java.util.HashMap;
import java.util.Map;
import org.openqa.selenium.WebElement;

/**
 *
 * 
 */
public class PageSpecWrapper extends PageSpec {

    Map<String, WebElement> elementMap = new HashMap<>();

    public void setObjectMap(Map<String, WebElement> elementMap) {
        this.elementMap = elementMap;
    }

    @Override
    public Locator getObjectLocator(String objectName) {
        return elementMap.containsKey(objectName) ? Locator.id("") : null;
    }

}
