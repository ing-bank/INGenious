package com.ing.ide.e2e.report;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an E2E test class as a scenario in the UI test report.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scenario {
    /**
     * @return unique scenario code, e.g. {@code SC-001}
     */
    String code();

    /**
     * @return what the scenario covers
     */
    String description();
}
