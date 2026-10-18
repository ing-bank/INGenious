package com.ing.ingenious.api.contract.paths;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Contract test for {@link PathsApi}. Verifies the root interface dispatches
 * to the expected sub-accessor instances. Sub-accessor stubs are created via
 * {@link Proxy} since only identity (not behavior) matters for this test.
 */
class PathsApiTest {

    private final ProjectPathsApi project = proxy(ProjectPathsApi.class);
    private final AppPathsApi app = proxy(AppPathsApi.class);
    private final ResultsPathsApi results = proxy(ResultsPathsApi.class);
    private final ReportTemplatePathsApi reportTemplates = proxy(ReportTemplatePathsApi.class);
    private final ReportOutputPathsApi reportOutputs = proxy(ReportOutputPathsApi.class);

    private final PathsApi paths = new PathsApi() {
        @Override
        public ProjectPathsApi project() {
            return project;
        }

        @Override
        public AppPathsApi app() {
            return app;
        }

        @Override
        public ResultsPathsApi results() {
            return results;
        }

        @Override
        public ReportTemplatePathsApi reportTemplates() {
            return reportTemplates;
        }

        @Override
        public ReportOutputPathsApi reportOutputs() {
            return reportOutputs;
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
    void rootShouldDispatchToTheExpectedSubAccessors() {
        assertSame(project, paths.project());
        assertSame(app, paths.app());
        assertSame(results, paths.results());
        assertSame(reportTemplates, paths.reportTemplates());
        assertSame(reportOutputs, paths.reportOutputs());
    }
}
