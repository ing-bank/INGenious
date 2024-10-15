
package com.ing.engine.drivers.findObjectBy;

import com.ing.engine.drivers.findObjectBy.support.SProperty;
import org.openqa.selenium.By;

/**
 *
 * 
 */
public class CustomFindBy {

    @SProperty(name = "title")
    public By getByCustomProp(String value) {
        return By.xpath("//*[@title='" + value + "']");
    }
}
