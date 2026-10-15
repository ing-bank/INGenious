package com.ing.ide.main.mobilerecorder;

import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.mobile.MobilePlatform;
import com.ing.ide.main.mobilerecorder.MobileLocatorResolver.ResolvedElement;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

/**
 * Orchestrates a live recording session and produces NATIVE INGenious artifacts:
 * each gesture on the mirror resolves to a Mobile OR object (persisted as YAML)
 * and an appended {@link TestStep} referencing it, and is replayed against the
 * real device so the recording stays in sync with the app.
 *
 * <ul>
 *   <li>Taps are recorded as a locator-based {@code Tap} step.</li>
 *   <li>Text entry is recorded as a {@code Set} step with an {@code @literal} input.</li>
 *   <li>Swipes are classified into a direction and recorded as a {@code scroll} step.</li>
 * </ul>
 */
public final class MobileStepRecorder {
    private static final Logger LOG = Logger.getLogger(MobileStepRecorder.class.getName());

    private final AppiumDriver driver;
    private final MobilePlatform platform;
    private final TestCase testCase;
    private final MobileORPage page;
    private final String pageReference;

    private final CoordinateMapper mapper = new CoordinateMapper();
    private final MobileLocatorResolver resolver = new MobileLocatorResolver();
    private final Set<String> usedObjectNames = new HashSet<>();

    public MobileStepRecorder(
        AppiumDriver driver,
        MobilePlatform platform,
        TestCase testCase,
        MobileORPage page,
        String pageReference
    ) {
        this.driver = driver;
        this.platform = platform;
        this.testCase = testCase;
        this.page = page;
        this.pageReference = pageReference;
    }

    /**
     * Resolves, replays and records a tap given in raw screenshot pixel
     * coordinates. Returns the outcome; when {@link RecordResult#isEditable()} is
     * true the caller should prompt for text and call {@link #recordTextInput}.
     */
    public synchronized RecordResult recordTapAt(
        int screenshotX,
        int screenshotY,
        int screenshotWidth,
        int screenshotHeight
    ) {
        ensureMapper(screenshotWidth, screenshotHeight);
        CoordinateMapper.DevicePoint p = mapper.toDevicePoint(screenshotX, screenshotY);
        ResolvedElement el = resolver.resolve(driver.getPageSource(), platform, p.x(), p.y());
        if (el == null) {
            return RecordResult.failure("No actionable element found at that point");
        }
        By by = toSeleniumBy(el.getStrategy(), el.getValue());
        try {
            driver.findElement(by).click();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to tap resolved element", e);
            return RecordResult.failure("Could not tap element: " + e.getMessage());
        }
        String objectName = materializeObject(el);
        appendObjectStep(objectName, "Tap", "", "Tap " + el.getDescription());
        return RecordResult.tap(objectName, el);
    }

    /** Types text into the previously resolved editable element and records a Set step. */
    public synchronized RecordResult recordTextInput(RecordResult tap, String text) {
        if (tap == null || tap.getElement() == null) {
            return RecordResult.failure("No target element for text input");
        }
        ResolvedElement el = tap.getElement();
        By by = toSeleniumBy(el.getStrategy(), el.getValue());
        try {
            WebElement element = driver.findElement(by);
            element.sendKeys(text);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to enter text", e);
            return RecordResult.failure("Could not enter text: " + e.getMessage());
        }
        appendObjectStep(
            tap.getObjectName(),
            "Set",
            "@" + text,
            "Enter \"" + text + "\" in " + el.getDescription()
        );
        return RecordResult.success("Recorded text entry");
    }

    /** Replays and records a directional swipe as a platform-aware {@code scroll} step. */
    public synchronized RecordResult recordSwipe(
        int startX,
        int startY,
        int endX,
        int endY,
        int screenshotWidth,
        int screenshotHeight
    ) {
        ensureMapper(screenshotWidth, screenshotHeight);
        CoordinateMapper.DevicePoint start = mapper.toDevicePoint(startX, startY);
        CoordinateMapper.DevicePoint end = mapper.toDevicePoint(endX, endY);
        String direction = directionOf(start, end);
        try {
            performSwipe(start.x(), start.y(), end.x(), end.y());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to swipe", e);
            return RecordResult.failure("Could not swipe: " + e.getMessage());
        }
        appendScreenStep("scroll", "@" + direction, "Scroll " + direction);
        return RecordResult.success("Recorded scroll " + direction);
    }

    // ===================================================================
    // Materialization + step authoring
    // ===================================================================

    private String materializeObject(ResolvedElement el) {
        String objectName = uniqueObjectName(el);
        MobileORObject obj = page.addObject(objectName);
        if (obj == null) {
            // Name clash inside the page model – reuse the existing object.
            objectName = uniqueObjectName(el);
            obj = page.addObject(objectName);
        }
        applyLocator(obj, el);
        if (page.getRoot() != null && page.getRoot().getObjectRepository() != null) {
            page.getRoot().getObjectRepository().saveMobilePageNow(page);
        }
        return objectName;
    }

    private void applyLocator(MobileORObject obj, ResolvedElement el) {
        if (obj == null) {
            return;
        }
        String strategy = el.getStrategy();
        if ("id".equals(strategy)) {
            obj.setId(el.getValue());
        } else if ("Accessibility".equals(strategy)) {
            obj.setAccessibility(el.getValue());
        } else if ("class".equals(strategy)) {
            obj.setClassName(el.getValue());
        } else {
            obj.setXpath(el.getValue());
        }
    }

    private void appendObjectStep(
        String objectName,
        String action,
        String input,
        String description
    ) {
        TestStep step = testCase.addNewStep();
        step.setObject(objectName);
        step.setReference(pageReference);
        step.setAction(action);
        step.setInput(input);
        step.setDescription(description);
        step.setNewlyRecorded(true);
    }

    private void appendScreenStep(String action, String input, String description) {
        TestStep step = testCase.addNewStep();
        step.setObject("Mobile");
        step.setAction(action);
        step.setInput(input);
        step.setDescription(description);
        step.setNewlyRecorded(true);
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    private void ensureMapper(int screenshotWidth, int screenshotHeight) {
        if (!mapper.isInitialized()) {
            Dimension window = driver.manage().window().getSize();
            mapper.initialize(window, screenshotWidth, screenshotHeight);
        }
    }

    private static String directionOf(
        CoordinateMapper.DevicePoint start,
        CoordinateMapper.DevicePoint end
    ) {
        int dx = end.x() - start.x();
        int dy = end.y() - start.y();
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx < 0 ? "left" : "right";
        }
        return dy < 0 ? "up" : "down";
    }

    private void performSwipe(int startX, int startY, int endX, int endY) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);
        swipe.addAction(
            finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY)
        );
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(
            finger.createPointerMove(
                Duration.ofMillis(400),
                PointerInput.Origin.viewport(),
                endX,
                endY
            )
        );
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Collections.singletonList(swipe));
    }

    private static By toSeleniumBy(String strategy, String value) {
        if ("id".equals(strategy)) {
            return By.id(value);
        }
        if ("Accessibility".equals(strategy)) {
            return AppiumBy.accessibilityId(value);
        }
        if ("class".equals(strategy)) {
            return By.className(value);
        }
        return By.xpath(value);
    }

    private String uniqueObjectName(ResolvedElement el) {
        String base = sanitize(el.getDescription());
        if (base.isEmpty()) {
            base = sanitize(simpleName(el.getClassName()));
        }
        if (base.isEmpty()) {
            base = "Element";
        }
        String candidate = base;
        int counter = 1;
        while (
            usedObjectNames.contains(candidate) || page.getObjectGroupByName(candidate) != null
        ) {
            candidate = base + "_" + counter++;
        }
        usedObjectNames.add(candidate);
        return candidate;
    }

    private static String simpleName(String tag) {
        if (tag == null) {
            return "";
        }
        int dot = tag.lastIndexOf('.');
        String base = dot >= 0 ? tag.substring(dot + 1) : tag;
        return base.replace("XCUIElementType", "");
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean capNext = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(capNext ? Character.toUpperCase(c) : c);
                capNext = false;
            } else {
                capNext = sb.length() > 0;
            }
            if (sb.length() >= 40) {
                break;
            }
        }
        String result = sb.toString();
        if (!result.isEmpty() && Character.isDigit(result.charAt(0))) {
            result = "E" + result;
        }
        return result;
    }

    public List<TestStep> getSteps() {
        return testCase.getTestSteps();
    }

    /** Outcome of a recorded gesture. */
    public static final class RecordResult {
        private final boolean ok;
        private final String message;
        private final String objectName;
        private final ResolvedElement element;

        private RecordResult(
            boolean ok,
            String message,
            String objectName,
            ResolvedElement element
        ) {
            this.ok = ok;
            this.message = message;
            this.objectName = objectName;
            this.element = element;
        }

        static RecordResult tap(String objectName, ResolvedElement element) {
            return new RecordResult(
                true,
                "Recorded " + element.getDescription(),
                objectName,
                element
            );
        }

        static RecordResult success(String message) {
            return new RecordResult(true, message, null, null);
        }

        static RecordResult failure(String message) {
            return new RecordResult(false, message, null, null);
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }

        public String getObjectName() {
            return objectName;
        }

        public ResolvedElement getElement() {
            return element;
        }

        public boolean isEditable() {
            return element != null && element.isEditable();
        }
    }
}
