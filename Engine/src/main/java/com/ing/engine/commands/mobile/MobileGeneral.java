package com.ing.engine.commands.mobile;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.contract.MobilePluginApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriverException;

public class MobileGeneral extends Command implements MobilePluginApi {

    public MobileGeneral(CommandControl cc) {
        super(cc);
    }

    /**
     * Implementation of {@link GeneralMobileApi#checkIfDriverIsAlive()} for mobile operations.
     * Checks if the mobile driver is alive and responsive.
     * @return true if the driver is alive, false otherwise
     * @throws RuntimeException if connection with the driver is lost/driver is closed
     */
    @Override
    public Boolean checkIfDriverIsAlive() {
        if (mDriver != null) {
            return getMobileDriverControl().isAlive();
        } else {
            throw new RuntimeException(
                "Seems like connection with the driver is lost/driver is closed"
            );
        }
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementPresent()} for mobile operations.
     * Checks if the target mobile element is present in the DOM.
     * @return true if the element is present, false otherwise
     */
    @Override
    public Boolean elementPresent() {
        return checkIfDriverIsAlive() && Element != null;
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementSelected()} for mobile operations.
     * Checks if the target mobile element is selected (e.g., checkbox, radio button, toggle).
     * @return true if the element is selected, false otherwise
     * @throws ElementException if element is not visible
     */
    @Override
    public Boolean elementSelected() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Element.isSelected();
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementDisplayed()} for mobile operations.
     * Checks if the target mobile element is displayed (visible to the user).
     * @return true if the element is displayed, false otherwise
     * @throws ElementException if element is not found
     */
    @Override
    public Boolean elementDisplayed() {
        if (!elementPresent()) {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
        return Element.isDisplayed();
    }

    /**
     * Implementation of {@link GeneralMobileApi#elementEnabled()} for mobile operations.
     * Checks if the target mobile element is enabled (interactable).
     * @return true if the element is enabled, false otherwise
     * @throws ElementException if element is not visible
     */
    @Override
    public Boolean elementEnabled() {
        if (!elementDisplayed()) {
            throw new ElementException(ExceptionType.Element_Not_Visible, ObjectName);
        }
        return Element.isEnabled();
    }

    /**
     * Implementation of {@link GeneralMobileApi#isHScrollBarPresent()} for mobile operations.
     * Checks if a horizontal scroll bar is present on the mobile page or element.
     * @return true if a horizontal scroll bar is present, false otherwise
     */
    @Override
    public boolean isHScrollBarPresent() {
        return (boolean) ((JavascriptExecutor) mDriver).executeScript(
                "return document.documentElement.scrollWidth>document.documentElement.clientWidth;"
            );
    }

    /**
     * Implementation of {@link GeneralMobileApi#isvScrollBarPresent()} for mobile operations.
     * Checks if a vertical scroll bar is present on the mobile page or element.
     * @return true if a vertical scroll bar is present, false otherwise
     */
    @Override
    public boolean isvScrollBarPresent() {
        return (boolean) ((JavascriptExecutor) mDriver).executeScript(
                "return document.documentElement.scrollHeight>document.documentElement.clientHeight;"
            );
    }

    /**
     * Implementation of {@link GeneralMobileApi#isAlertPresent()} for mobile operations.
     * Checks if an alert dialog is present in the mobile application.
     * @return true if an alert is present, false otherwise
     */
    @Override
    public boolean isAlertPresent() {
        try {
            mDriver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            return false;
        }
    }

    @Override
    public MobileObjectApi getMObject() {
        return mObject;
    }

    @Override
    public Object getMDriver() {
        return mDriver;
    }

    @Override
    public Object getElement() {
        return Element;
    }

    /**
     * Executes a shake gesture with provider-aware fallbacks.
     *
     * <p>Order:
     * 1) LambdaTest custom executor (works for LambdaTest real devices)
     * 2) Appium native mobile: shake (when supported by the provider)
     * 3) Android-only sensor emulation fallback via mobile: sensorSet
     * </p>
     *
     * @return execution detail used in reporting
     */
    protected String executeShakeGesture() {
        if (tryLambdaExecutorShake()) {
            return "using LambdaTest executor";
        }

        try {
            ((JavascriptExecutor) mDriver).executeScript("mobile: shake");
            return "using Appium mobile: shake";
        } catch (Exception mobileShakeException) {
            if (isUnsupportedShakeCommand(mobileShakeException)) {
                if (tryAndroidSensorShake()) {
                    return "using Android sensor emulation fallback";
                }
                throw new WebDriverException(
                    "Shake is unsupported by the current device provider",
                    mobileShakeException
                );
            }
            throw mobileShakeException;
        }
    }

    private boolean tryLambdaExecutorShake() {
        String[] lambdaShakeScripts = {
            "lambda_executor: {\"action\": \"shake\"}",
            "lambda_executor: {\"action\": \"gestures\", \"arguments\": {\"shake\": true}}"
        };

        for (String lambdaScript : lambdaShakeScripts) {
            try {
                ((JavascriptExecutor) mDriver).executeScript(lambdaScript);
                return true;
            } catch (Exception e) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, null, e);
            }
        }
        return false;
    }

    private boolean tryAndroidSensorShake() {
        if (!(mDriver instanceof AndroidDriver)) {
            return false;
        }

        List<List<Double>> vectors = Arrays.asList(
            Arrays.asList(9.8, 0.0, 0.0),
            Arrays.asList(-9.8, 0.0, 0.0)
        );

        try {
            for (List<Double> vector : vectors) {
                if (!trySensorSetVariants(vector)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE, null, e);
            return false;
        }
    }

    private boolean trySensorSetVariants(List<Double> vector) {
        // Different Appium providers validate sensorSet payloads differently.
        String sensorValue = vector.get(0) + ":" + vector.get(1) + ":" + vector.get(2);
        List<Map<String, Object>> variants = Arrays.asList(
            Map.of("sensorType", "acceleration", "value", sensorValue),
            Map.of("sensorType", "accelerometer", "value", sensorValue)
        );

        for (Map<String, Object> args : variants) {
            try {
                ((JavascriptExecutor) mDriver).executeScript("mobile: sensorSet", args);
                return true;
            } catch (Exception e) {
                Logger.getLogger(this.getClass().getName()).log(Level.FINE, null, e);
            }
        }
        return false;
    }

    private boolean isUnsupportedShakeCommand(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof UnsupportedCommandException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (
                    normalized.contains("unknown mobile command \"mobile: shake\"") ||
                    normalized.contains("shake is not supported on real devices")
                ) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
