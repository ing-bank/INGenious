package com.ing.ide.uitest.select;

import com.ing.ide.uitest.report.TestIdentity;

/**
 * One test case of the UI test suite: its RTM codes and the Java method that implements it.
 */
public final class CatalogEntry {
    private final TestIdentity identity;
    private final Class<?> testClass;
    private final String methodName;

    CatalogEntry(final TestIdentity identity, final Class<?> testClass, final String methodName) {
        this.identity = identity;
        this.testClass = testClass;
        this.methodName = methodName;
    }

    /**
     * @return scenario and test case codes and descriptions
     */
    public TestIdentity identity() {
        return identity;
    }

    /**
     * @return {@code <class name>#<method name>}, unique per test case
     */
    public String key() {
        return key(testClass, methodName);
    }

    /**
     * @return the test method, e.g. {@code WindowMenuIT#navigateToDashboard}
     */
    public String javaName() {
        return testClass.getSimpleName() + "#" + methodName;
    }

    static String key(final Class<?> testClass, final String methodName) {
        return testClass.getName() + "#" + methodName;
    }
}
