package com.ing.engine.aicli.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

public class ActivityReportTest {

    private static ActivityReport.Call call(String name, boolean ok, String result) {
        return new ActivityReport.Call(name, ok, result);
    }

    @Test
    public void mapsTestCaseCreationToActivity() {
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_testcase_create",
                    true,
                    "{\"created\":true,\"scenario\":\"Login\",\"testcase\":\"Valid login\",\"steps\":12}"
                )
            )
        );
        assertThat(r.activities).hasSize(1);
        ActivityReport.Activity a = r.activities.get(0);
        assertThat(a.title).isEqualTo("Test case created");
        assertThat(a.status).isEqualTo(ActivityReport.Status.OK);
        assertThat(a.details).contains("Scenario: Login", "Test case: Valid login", "12 steps");
        assertThat(r.okCount).isEqualTo(1);
    }

    @Test
    public void collapsesReadOnlyLookupsIntoMinorCount() {
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call("ingenious_action_search", true, "{\"actions\":[]}"),
                call("ingenious_testcase_list", true, "[]"),
                call("ingenious_object_show", true, "{\"name\":\"btn\"}")
            )
        );
        assertThat(r.activities).isEmpty();
        assertThat(r.minorCount).isEqualTo(3);
        assertThat(r.totalCalls).isEqualTo(3);
    }

    @Test
    public void flagsFailedValidationAsWarn() {
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"valid\":false,\"errors\":[\"bad step\"],\"warnings\":[]}"
                )
            )
        );
        assertThat(r.activities).hasSize(1);
        assertThat(r.activities.get(0).title).isEqualTo("Test case validated");
        assertThat(r.activities.get(0).status).isEqualTo(ActivityReport.Status.WARN);
        assertThat(r.warnCount).isEqualTo(1);
    }

    @Test
    public void mapsExecutionResultAndStatus() {
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_run",
                    true,
                    "{\"target\":\"Login/Valid login\",\"status\":\"FAIL\",\"exitCode\":1,\"durationMs\":4200}"
                )
            )
        );
        ActivityReport.Activity a = r.activities.get(0);
        assertThat(a.title).isEqualTo("Test executed");
        assertThat(a.status).isEqualTo(ActivityReport.Status.FAIL);
        assertThat(a.details).contains("Target: Login/Valid login", "Result: FAIL");
    }

    @Test
    public void surfacesFailedToolAsFailActivity() {
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(call("ingenious_testcase_create", false, "Scenario not found"))
        );
        assertThat(r.activities).hasSize(1);
        assertThat(r.activities.get(0).status).isEqualTo(ActivityReport.Status.FAIL);
        assertThat(r.failCount).isEqualTo(1);
    }

    @Test
    public void normalizesDoublePrefixedToolNames() {
        assertThat(ActivityReport.bareName("ingenious-ingenious_testcase_create"))
            .isEqualTo("testcase_create");
        assertThat(ActivityReport.bareName("ingenious_run")).isEqualTo("run");
    }
}
