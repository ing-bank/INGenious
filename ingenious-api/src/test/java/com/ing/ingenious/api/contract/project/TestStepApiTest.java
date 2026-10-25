package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link TestStepApi}. Uses a minimal in-test
 * implementation to verify each accessor returns the expected field value.
 */
class TestStepApiTest {

    private final TestStepApi step = new TestStepApi() {
        @Override
        public String getTestScenarioName() {
            return "Scenario1";
        }

        @Override
        public String getTestCaseName() {
            return "TestCase1";
        }

        @Override
        public String getIteration() {
            return "1";
        }

        @Override
        public String getStatus() {
            return "Pass";
        }

        @Override
        public String getExecute() {
            return "true";
        }

        @Override
        public String getPlatform() {
            return "Windows";
        }

        @Override
        public String getBrowser() {
            return "Chrome";
        }
    };

    @Test
    void shouldExposeEachFieldValue() {
        assertEquals("Scenario1", step.getTestScenarioName());
        assertEquals("TestCase1", step.getTestCaseName());
        assertEquals("1", step.getIteration());
        assertEquals("Pass", step.getStatus());
        assertEquals("true", step.getExecute());
        assertEquals("Windows", step.getPlatform());
        assertEquals("Chrome", step.getBrowser());
    }
}
