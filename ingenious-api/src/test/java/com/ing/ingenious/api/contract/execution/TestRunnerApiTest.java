package com.ing.ingenious.api.contract.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ing.ingenious.api.contract.project.ProjectApi;
import com.ing.ingenious.api.contract.project.ProjectTestDataApi;
import com.ing.ingenious.api.contract.project.TestSetApi;
import com.ing.ingenious.api.contract.settings.ExecutionSettingsApi;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link TestRunnerApi}. Verifies the interface can be
 * implemented and dispatches to the expected sub-accessors and state.
 */
class TestRunnerApiTest {

    private final ProjectApi project = proxy(ProjectApi.class);
    private final ExecutionSettingsApi execSettings = proxy(ExecutionSettingsApi.class);
    private final TestSetApi testSet = proxy(TestSetApi.class);
    private final ProjectTestDataApi dataProvider = proxy(ProjectTestDataApi.class);
    private final Map<String, DataIteratorApi> iterators = new HashMap<>();
    private boolean continueOnError;
    private boolean retry;

    private final TestRunnerApi runner = new TestRunnerApi() {
        @Override
        public ProjectApi getProject() {
            return project;
        }

        @Override
        public ExecutionSettingsApi getExecSettings() {
            return execSettings;
        }

        @Override
        public TestSetApi getTestSet() {
            return testSet;
        }

        @Override
        public String runEnv() {
            return "QA";
        }

        @Override
        public ProjectTestDataApi dataProvider() {
            return dataProvider;
        }

        @Override
        public boolean isContinueOnError() {
            return continueOnError;
        }

        @Override
        public boolean useExistingBrowser() {
            return true;
        }

        @Override
        public boolean isDebugExe() {
            return false;
        }

        @Override
        public DataIteratorApi getIterater(String scenario, String testcase) {
            return getIterater(scenario + "#" + testcase);
        }

        @Override
        public DataIteratorApi getIterater(String key) {
            return iterators.computeIfAbsent(key, k -> proxy(DataIteratorApi.class));
        }

        @Override
        public boolean retryExecution() {
            return retry;
        }

        @Override
        public void afterExecution(boolean passed) {
            retry = !passed;
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxyInstance, method, args) -> null
        );
    }

    @Test
    void shouldDispatchToTheExpectedSubAccessors() {
        assertSame(project, runner.getProject());
        assertSame(execSettings, runner.getExecSettings());
        assertSame(testSet, runner.getTestSet());
        assertSame(dataProvider, runner.dataProvider());
        assertEquals("QA", runner.runEnv());
    }

    @Test
    void getIteraterOverloadsShouldResolveToTheSameKey() {
        DataIteratorApi byParts = runner.getIterater("Scenario1", "TestCase1");
        DataIteratorApi byKey = runner.getIterater("Scenario1#TestCase1");
        assertSame(byParts, byKey);
    }

    @Test
    void afterExecutionShouldUpdateRetryState() {
        assertFalse(runner.retryExecution());
        runner.afterExecution(false);
        assertTrue(runner.retryExecution());
        runner.afterExecution(true);
        assertFalse(runner.retryExecution());
    }
}
