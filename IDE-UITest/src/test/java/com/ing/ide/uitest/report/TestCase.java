package com.ing.ide.uitest.report;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a UI test method as a test case of its {@link Scenario} in the UI test report.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestCase {
    /**
     * @return unique test case code, e.g. {@code TC-001-01}
     */
    String code();

    /**
     * @return what the test case verifies
     */
    String description();
}
