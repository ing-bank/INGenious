package com.ing.ingenious.api.contract.execution;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.ing.ingenious.api.contract.project.ProjectApi;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ExecutionContextApi}. Verifies the interface
 * dispatches to the expected sub-accessor instances.
 */
class ExecutionContextApiTest {

    private final ProjectApi project = proxy(ProjectApi.class);
    private final TestRunnerApi runner = proxy(TestRunnerApi.class);
    private final SummaryReportApi reportManager = proxy(SummaryReportApi.class);

    private final ExecutionContextApi context = new ExecutionContextApi() {
        @Override
        public String getTriggerId() {
            return "trigger-1";
        }

        @Override
        public String getExecutionStartTime() {
            return "2026-01-01T00:00:00";
        }

        @Override
        public ProjectApi getCurrentProject() {
            return project;
        }

        @Override
        public TestRunnerApi getRunner() {
            return runner;
        }

        @Override
        public SummaryReportApi getReportManager() {
            return reportManager;
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
    void shouldDispatchToTheExpectedAccessors() {
        assertSame("trigger-1", context.getTriggerId());
        assertSame("2026-01-01T00:00:00", context.getExecutionStartTime());
        assertSame(project, context.getCurrentProject());
        assertSame(runner, context.getRunner());
        assertSame(reportManager, context.getReportManager());
    }
}
