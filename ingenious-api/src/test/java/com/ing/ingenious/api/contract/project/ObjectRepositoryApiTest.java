package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ObjectRepositoryApi}. Uses a minimal in-test
 * implementation to verify the existence-check accessors behave as expected.
 */
class ObjectRepositoryApiTest {

    private final Set<String> webObjects = new HashSet<>();
    private final Set<String> mobileObjects = new HashSet<>();
    private final Set<String> structuredDataObjects = new HashSet<>();

    private final ObjectRepositoryApi or = new ObjectRepositoryApi() {
        @Override
        public boolean isObjectPresent(String pageName, String objectName) {
            return webObjects.contains(pageName + "#" + objectName);
        }

        @Override
        public boolean isMobileObjectPresent(String pageName, String objectName) {
            return mobileObjects.contains(pageName + "#" + objectName);
        }

        @Override
        public boolean isStructuredDataObjectPresent(String pageName, String objectName) {
            return structuredDataObjects.contains(pageName + "#" + objectName);
        }
    };

    @Test
    void shouldReportObjectsAsAbsentUntilRegistered() {
        assertFalse(or.isObjectPresent("Page1", "Object1"));
        webObjects.add("Page1#Object1");
        assertTrue(or.isObjectPresent("Page1", "Object1"));
    }

    @Test
    void mobileAndStructuredDataChecksShouldBeIndependent() {
        mobileObjects.add("Page1#Object1");
        structuredDataObjects.add("Page2#Object2");

        assertTrue(or.isMobileObjectPresent("Page1", "Object1"));
        assertFalse(or.isStructuredDataObjectPresent("Page1", "Object1"));
        assertTrue(or.isStructuredDataObjectPresent("Page2", "Object2"));
    }
}
