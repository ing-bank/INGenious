
package com.ing.engine.drivers.customWebDriver;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;


public class EmptyDriver implements WebDriver, TakesScreenshot {

    @Override
    public void get(String string) {
        //No Need
    }

    @Override
    public String getCurrentUrl() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public List<WebElement> findElements(By by) {
        return new ArrayList<>();
    }

    @Override
    public WebElement findElement(By by) {
        return null;
    }

    @Override
    public String getPageSource() {
        return null;
    }

    @Override
    public void close() {
        //No Need
    }

    @Override
    public void quit() {
        //No Need
    }

    @Override
    public Set<String> getWindowHandles() {
        return new HashSet<>();
    }

    @Override
    public String getWindowHandle() {
        return null;
    }

    @Override
    public TargetLocator switchTo() {
        return null;
    }

    @Override
    public Navigation navigate() {
        return null;
    }

    @Override
    public Options manage() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        Rectangle rectangle = new Rectangle(0, 0, screenSize.width, screenSize.height);
        try {
            File ss = new File("image");
            ImageIO.write(new Robot().createScreenCapture(rectangle), "png", ss);
            return ((X) ss);
        } catch (AWTException | IOException ex) {
            Logger.getLogger(EmptyDriver.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
