package com.ing.engine.commands.mobile;

import java.time.Duration;
import java.util.Arrays;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.SupportsContextSwitching;
import io.appium.java_client.remote.SupportsRotation;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.interactions.Pause;

public class AppiumDeviceCommands extends MobileGeneral {

    public AppiumDeviceCommands(CommandControl cc) {
        super(cc);
        // TODO Auto-generated constructor stub
    }

    @Action(object = ObjectType.APP, desc = "Swipe Element to <Input> position", input = InputType.YES, condition = InputType.OPTIONAL)
    public void swipeElement() {
        try {
            int duration = 2000;
            Dimension size = mDriver.manage().window().getSize();
            Rectangle rectangle = Element.getRect();
            Point point = new Point(rectangle.x + (rectangle.width / 2), rectangle.y + (rectangle.height / 2));
            int startX = point.x;
            int startY = point.y;
            int endX = 0;
            int endY = 0;

            switch (Data) {
                case "Left":
                    endY = startY;
                    break;

                case "Right":
                    endX = (int) (0.9 * size.getWidth());
                    endY = startY;
                    break;

                case "Up":
                    endX = startX;
                    break;

                case "Down":
                    endX = startX;
                    endY = size.getHeight();
            }
            if (!Condition.equals("")) {
                duration = Integer.parseInt(Condition);
            }
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), startX, startY));
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(duration), PointerInput.Origin.viewport(), endX, endY));
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(Action, "Element Swiped to " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to Swipe the Element to" + Data + ", Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Swipe Screen to <Input> position", input = InputType.YES, condition = InputType.OPTIONAL)
    public void swipeMobileScreen() {
        try {
            int duration = 2000;
            Dimension size = mDriver.manage().window().getSize();
            int startX = 0;
            int startY = 0;
            int endX = 0;
            int endY = 0;

            switch (Data) {
                case "Left":
                    startX = (int) (0.8 * size.getWidth());
                    startY = (int) size.getHeight() / 2;
                    endX = (int) (0.2 * size.getWidth());
                    endY = startY;
                    break;

                case "Right":
                    startX = (int) (0.2 * size.getWidth());
                    startY = (int) size.getHeight() / 2;
                    endX = (int) (0.8 * size.getWidth());
                    endY = startY;
                    break;

                case "Up":
                    startX = (int) (size.getWidth() / 2);
                    startY = (int) (0.8 * size.getHeight());
                    endX = startX;
                    endY = (int) (0.2 * size.getHeight());
                    break;

                case "Down":
                    startX = (int) (size.getWidth() / 2);
                    startY = (int) (0.2 * size.getHeight());
                    endX = startX;
                    endY = (int) (0.8 * size.getHeight());
                    break;
            }
            if (!Condition.equals("")) {
                duration = Integer.parseInt(Condition);
            }
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), startX, startY));
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(duration), PointerInput.Origin.viewport(), endX, endY));
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(Action, "Screen swiped to " + Data, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to Swipe the Screen to" + Data + ", Error: " + e.getMessage(), Status.FAIL);
        }

    }

    @Action(object = ObjectType.MOBILE, desc = "Rotate Screen orientation to Landscape", input = InputType.NO, condition = InputType.NO)
    public void rotateLandscape() {
        try {
            ((SupportsRotation) mDriver).rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
            Report.updateTestLog(Action, "Screen orientation changed to Landscape ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to change the Screen orientation to Landscape, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Rotate Screen orientation to Portrait", input = InputType.NO, condition = InputType.NO)
    public void rotatePortrait() {
        try {
            ((SupportsRotation) mDriver).rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            Report.updateTestLog(Action, "Screen orientation changed to Portrait ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to change the Screen orientation to Portrait, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.APP, desc = "Long press the [<Object>]", input = InputType.YES, condition = InputType.NO)
    public void longPress() {
        try {
            int holdDuration = Integer.parseInt(Data);
            Rectangle rectangle = Element.getRect();
            Point point = new Point(rectangle.x + (rectangle.width / 2), rectangle.y + (rectangle.height / 2));
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence seq = new Sequence(finger, 1);
            seq.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), point.x, point.y));
            seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            seq.addAction(finger.createPointerMove(Duration.ofMillis(50), PointerInput.Origin.viewport(), point.x, point.y));
            seq.addAction(new Pause(finger, Duration.ofMillis(holdDuration)));
            seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            ((RemoteWebDriver) mDriver).perform(Arrays.asList(seq));
            Report.updateTestLog(Action, "Long press on " + "[" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to perform Long Press action, Error: " + e.getMessage(), Status.FAIL);
        }

    }

    @Action(object = ObjectType.MOBILE, desc = "Toggle Internet Data ", input = InputType.NO, condition = InputType.NO)
    public void toggleInternetData() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).toggleData();
            } else if (mDriver instanceof IOSDriver) {

            }
            Report.updateTestLog(Action, "Toggle Data is done ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to toggle Data, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Toggle Wifi", input = InputType.NO, condition = InputType.NO)
    public void toggleWifi() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).toggleWifi();
            } else if (mDriver instanceof IOSDriver) {

            }
            Report.updateTestLog(Action, "Toggle Wifi is done ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to toggle Wifi, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Toggle Location Services", input = InputType.NO, condition = InputType.NO)
    public void toggleLocationServices() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).toggleLocationServices();
            } else if (mDriver instanceof IOSDriver) {

            }
            Report.updateTestLog(Action, "Toggle Location Services is done ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to toggle Location Services, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Lock Mobile Device", input = InputType.NO, condition = InputType.NO)
    public void lockMobileDevice() {
        try {
            if (mDriver instanceof AndroidDriver) {
                boolean lockAndroid = ((AndroidDriver) mDriver).isDeviceLocked();
                if (!lockAndroid) {
                    ((AndroidDriver) mDriver).lockDevice();
                    Report.updateTestLog(Action, "Device is locked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is locked already ", Status.DONE);
                }
            } else if (mDriver instanceof IOSDriver) {
                boolean lockIOS = ((AndroidDriver) mDriver).isDeviceLocked();
                if (!lockIOS) {
                    ((AndroidDriver) mDriver).lockDevice();
                    Report.updateTestLog(Action, "Device is locked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is locked already ", Status.DONE);
                }
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to Lock device, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Unlock Mobile Device", input = InputType.NO, condition = InputType.NO)
    public void unlockMobileDevice() {
        try {
            if (mDriver instanceof AndroidDriver) {
                boolean lockAndroid = ((AndroidDriver) mDriver).isDeviceLocked();
                if (lockAndroid) {
                    ((AndroidDriver) mDriver).unlockDevice();
                    Report.updateTestLog(Action, "Device is unlocked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is unlocked already ", Status.DONE);
                }
            } else if (mDriver instanceof IOSDriver) {
                boolean lockIOS = ((AndroidDriver) mDriver).isDeviceLocked();
                if (lockIOS) {
                    ((AndroidDriver) mDriver).unlockDevice();
                    Report.updateTestLog(Action, "Device is unlocked successfully ", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Device is unlocked already ", Status.DONE);
                }

            }

        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to Unlock device, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Get device time in variable", input = InputType.NO, condition = InputType.YES)
    public void getDeviceTime() {
        try {
            String deviceTime = "";
            if (mDriver instanceof AndroidDriver) {
                deviceTime = ((AndroidDriver) mDriver).getDeviceTime();
                addVar(Condition, deviceTime);
            } else if (mDriver instanceof IOSDriver) {
                deviceTime = ((IOSDriver) mDriver).getDeviceTime();
                addVar(Condition, deviceTime);
            }
            Report.updateTestLog(Action, "Device time is " + deviceTime, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to get device time, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Open Notifications", input = InputType.NO, condition = InputType.NO)
    public void openNotifications() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).openNotifications();
            } else if (mDriver instanceof IOSDriver) {

            }
            Report.updateTestLog(Action, "Notification opened ", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to open Notifications, Error: " + e.getMessage(), Status.FAIL);
        }
    }
    
    @Action(object = ObjectType.MOBILE, desc = "Shake Device")
    public void shake() {
        try {
            if (mDriver instanceof AndroidDriver) {
                ((AndroidDriver) mDriver).executeScript("mobile: shake");
            } else if (mDriver instanceof IOSDriver) {
                ((IOSDriver) mDriver).executeScript("mobile: shake");

            }
            Report.updateTestLog(Action, "Performed Shake Operation", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to perform Shake operation, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Hide Keyboard", input = InputType.NO, condition = InputType.NO)
    public void hideKeyboard() {
        try {
            if (((AndroidDriver) mDriver).isKeyboardShown()) {
                if (mDriver instanceof AndroidDriver) {
                    ((AndroidDriver) mDriver).hideKeyboard();
                } else if (mDriver instanceof IOSDriver) {
                    ((IOSDriver) mDriver).hideKeyboard();
                }
                Report.updateTestLog(Action, "Keyboard hidden successfully ", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Keyboard is hidden already ", Status.DEBUG);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Unable to hide keyboard, Error: " + e.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Pinch and Zoom", input = InputType.YES, condition = InputType.NO)
    public void pinchAndZoomScreen() throws InterruptedException {
        try {
            Dimension size = mDriver.manage().window().getSize();
            Point source = new Point(size.getWidth(), size.getHeight());
            float halfY = source.y / 2;
            float halfX = source.x / 2;
            int angle = (int) Math.toDegrees(Math.atan(halfY / halfX));
            int halfDigonal = (int) Math.sqrt((Math.pow(halfX, 2) + (Math.pow(halfY, 2))));
            float percentageZoom = Float.parseFloat(Data) / 100;
            int xExtension = (int) (Math.cos(angle) * halfDigonal * percentageZoom);
            int yExtension = (int) (Math.sin(angle) * halfDigonal * percentageZoom);
            int startXFingure1 = source.x / 2;
            int endXFingure1 = (int) (source.x / 2 + xExtension);
            int startXFingure2 = source.x / 2;
            int endXFingure2 = (int) (source.x / 2 - xExtension);
            int startYFingure1 = source.y / 2;
            int endYFingure1 = (int) (source.y / 2 - yExtension);
            int startYFingure2 = source.y / 2;
            int endYFingure2 = (int) (source.y / 2 + yExtension);
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinchAndZoom1 = new Sequence(finger1, 1);
            pinchAndZoom1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startXFingure1, startYFingure1));
            pinchAndZoom1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinchAndZoom1.addAction(new Pause(finger1, Duration.ofMillis(200)));
            pinchAndZoom1.addAction(finger1.createPointerMove(Duration.ofMillis(200), PointerInput.Origin.viewport(), endXFingure1, endYFingure1));
            pinchAndZoom1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            Sequence pinchAndZoom2 = new Sequence(finger2, 1);
            pinchAndZoom2.addAction(finger2.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), startXFingure2, startYFingure2));
            pinchAndZoom2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinchAndZoom2.addAction(new Pause(finger2, Duration.ofMillis(200)));
            pinchAndZoom2.addAction(finger2.createPointerMove(Duration.ofMillis(200),
                    PointerInput.Origin.viewport(), endXFingure2, endYFingure2));
            pinchAndZoom2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((RemoteWebDriver) mDriver).perform(Arrays.asList(pinchAndZoom1, pinchAndZoom2));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Action(object = ObjectType.APP, desc = "Pinch and Zoom", input = InputType.YES, condition = InputType.NO)
    public void pinchAndZoomElement() throws InterruptedException {
        try {
            Dimension size = mDriver.manage().window().getSize();
            Point SreenSource = new Point(size.getWidth(), size.getHeight());
            Rectangle rectangle = Element.getRect();
            Point elementCentre = new Point(rectangle.x + (rectangle.width / 2), rectangle.y + (rectangle.height / 2));
            int rightSideWidth = SreenSource.x - elementCentre.x;
            int leftSideWidth = elementCentre.x;
            int longestDigonal = 0;
            int angle = 0;
            float percentageZoom = Float.parseFloat(Data) / 100;
            int xExtension = 0;
            int yExtension = 0;
            int startXFingure1 = elementCentre.x;
            int startXFingure2 = elementCentre.x;
            int startYFingure1 = elementCentre.y;
            int startYFingure2 = elementCentre.y;
            int endXFingure1 = 0;
            int endYFingure1 = 0;
            int endYFingure2 = 0;
            int endXFingure2 = 0;
            if (rightSideWidth > leftSideWidth) {
                longestDigonal = (int) Math.sqrt((Math.pow(rightSideWidth, 2) + (Math.pow(elementCentre.y, 2))));
                angle = (int) Math.toDegrees(Math.atan(elementCentre.y / rightSideWidth));
                xExtension = (int) (Math.cos(angle) * longestDigonal * percentageZoom);
                yExtension = (int) (Math.sin(angle) * longestDigonal * percentageZoom);
                endXFingure1 = (int) (elementCentre.x + xExtension);
                endXFingure2 = (int) (elementCentre.x - xExtension);
            } else {
                longestDigonal = (int) Math.sqrt((Math.pow(leftSideWidth, 2) + (Math.pow(elementCentre.y, 2))));
                angle = (int) Math.toDegrees(Math.atan(elementCentre.y / leftSideWidth));
                xExtension = (int) (Math.cos(angle) * longestDigonal * percentageZoom);
                yExtension = (int) (Math.sin(angle) * longestDigonal * percentageZoom);
                endXFingure1 = (int) (elementCentre.x - xExtension);
                endXFingure2 = (int) (elementCentre.x + xExtension);
            }
            endYFingure1 = (int) (elementCentre.y - yExtension);
            endYFingure2 = (int) (elementCentre.y + yExtension);
            PointerInput finger1 = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
            PointerInput finger2 = new PointerInput(PointerInput.Kind.TOUCH, "finger2");
            Sequence pinchAndZoom1 = new Sequence(finger1, 1);
            pinchAndZoom1.addAction(finger1.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startXFingure1, startYFingure1));
            pinchAndZoom1.addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinchAndZoom1.addAction(new Pause(finger1, Duration.ofMillis(200)));
            pinchAndZoom1.addAction(finger1.createPointerMove(Duration.ofMillis(200), PointerInput.Origin.viewport(), endXFingure1, endYFingure1));
            pinchAndZoom1.addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            Sequence pinchAndZoom2 = new Sequence(finger2, 1);
            pinchAndZoom2.addAction(finger2.createPointerMove(Duration.ZERO,
                    PointerInput.Origin.viewport(), startXFingure2, startYFingure2));
            pinchAndZoom2.addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            pinchAndZoom2.addAction(new Pause(finger2, Duration.ofMillis(200)));
            pinchAndZoom2.addAction(finger2.createPointerMove(Duration.ofMillis(200),
                    PointerInput.Origin.viewport(), endXFingure2, endYFingure2));
            pinchAndZoom2.addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            ((RemoteWebDriver) mDriver).perform(Arrays.asList(pinchAndZoom1, pinchAndZoom2));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Action(object = ObjectType.MOBILE, desc = "Switch Context When Name Contains", input = InputType.YES, condition = InputType.NO)
    public void switchContextWhenNameContains() {
        try {
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            for (String contextName : contextNames) {
                if (contextName.contains(Data)) {
                    ((SupportsContextSwitching) mDriver).context(contextName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Action(object = ObjectType.MOBILE, desc = "Switch Context When Name Equals", input = InputType.YES, condition = InputType.NO)
    public void switchContextWhenNameEquals() {
        try {
            Set<String> contextNames = ((SupportsContextSwitching) mDriver).getContextHandles();
            for (String contextName : contextNames) {
                if (contextName.equals(Data)) {
                    ((SupportsContextSwitching) mDriver).context(contextName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
