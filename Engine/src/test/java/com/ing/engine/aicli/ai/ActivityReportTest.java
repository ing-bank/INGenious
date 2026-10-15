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
                    "{\"testcase\":\"Foo\",\"valid\":false,\"errors\":[\"bad step\"],\"warnings\":[]}"
                )
            )
        );
        assertThat(r.activities).hasSize(1);
        assertThat(r.activities.get(0).title).isEqualTo("Test case validated");
        assertThat(r.activities.get(0).status).isEqualTo(ActivityReport.Status.WARN);
        assertThat(r.warnCount).isEqualTo(1);
    }

    @Test
    public void collapsesRepeatedWholeSuiteValidationsAndLabelsThemDistinctly() {
        // No scenario/testcase filter -> a broad "validate everything" pass; repeats
        // of it should still collapse (same as a single test case's repeats do), and
        // it should read as a suite-level check, not a single test case's result.
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"checked\":5,\"valid\":false,\"errors\":[\"e1\"],\"warnings\":[]}"
                ),
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"checked\":5,\"valid\":true,\"errors\":[],\"warnings\":[]}"
                )
            )
        );
        assertThat(r.activities).hasSize(1);
        ActivityReport.Activity a = r.activities.get(0);
        assertThat(a.title).isEqualTo("Test suite validated");
        assertThat(a.status).isEqualTo(ActivityReport.Status.OK);
        assertThat(a.details).contains("Checked 5 test cases", "Resolved after 2 attempts");
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

    @Test
    public void collapsesSelfCorrectedRetriesIntoFinalState() {
        // The agent iterates: invalid -> fix a step -> invalid -> fix again -> valid.
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"testcase\":\"Pay bill\",\"valid\":false,\"errors\":[\"bad step\"],\"warnings\":[]}"
                ),
                call(
                    "ingenious_testcase_edit_step",
                    true,
                    "{\"testcase\":\"Pay bill\",\"steps\":5}"
                ),
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"testcase\":\"Pay bill\",\"valid\":false,\"errors\":[\"bad step\"],\"warnings\":[]}"
                ),
                call(
                    "ingenious_testcase_edit_step",
                    true,
                    "{\"testcase\":\"Pay bill\",\"steps\":5}"
                ),
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"testcase\":\"Pay bill\",\"valid\":true,\"errors\":[],\"warnings\":[]}"
                )
            )
        );
        // Only the final validate/steps-updated state is reported, not every intermediate attempt.
        assertThat(r.activities).hasSize(2);
        ActivityReport.Activity validate = r.activities.get(0);
        assertThat(validate.title).isEqualTo("Test case validated");
        ActivityReport.Activity steps = r.activities.get(1);
        assertThat(steps.title).isEqualTo("Test steps updated");
        assertThat(steps.details).contains("Resolved after 2 attempts");
        assertThat(validate.status).isEqualTo(ActivityReport.Status.OK);
        assertThat(validate.details).contains("Resolved after 3 attempts");
        assertThat(r.failCount).isEqualTo(0);
        assertThat(r.warnCount).isEqualTo(0);
        assertThat(r.okCount).isEqualTo(2);
        assertThat(r.retryCount).isEqualTo(2);
        assertThat(r.totalCalls).isEqualTo(5);
    }

    @Test
    public void rendersFilePathsRelativeAndShort() {
        String cwd = System.getProperty("user.dir");
        String absolute = cwd + java.io.File.separator + "Dist/release/ai/skills/example/SKILL.md";
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_db_connection_add",
                    true,
                    "{\"name\":\"example\",\"path\":\"" + absolute.replace("\\", "\\\\") + "\"}"
                )
            )
        );
        assertThat(r.activities).hasSize(1);
        List<String> details = r.activities.get(0).details;
        assertThat(details).contains("Name: example");
        assertThat(details).contains("Path: Dist/release/ai/skills/example/SKILL.md");
        assertThat(details.toString()).doesNotContain(cwd);
    }

    @Test
    public void demotesNonIngeniousToolCallsToMinor() {
        // Agent-internal housekeeping (reading a skill doc, editing a file, running a
        // shell command) isn't a user-facing test-authoring outcome; it shouldn't
        // clutter the Activity list even when it isn't a recognized read-only lookup.
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_skill_read",
                    true,
                    "{\"name\":\"example\",\"path\":\"/x/SKILL.md\"}"
                ),
                call("view", true, "file contents..."),
                call("bash", true, "done"),
                call(
                    "ingenious_testcase_create",
                    true,
                    "{\"created\":true,\"scenario\":\"Login\",\"testcase\":\"Valid login\",\"steps\":3}"
                )
            )
        );
        assertThat(r.activities).hasSize(1);
        assertThat(r.activities.get(0).title).isEqualTo("Test case created");
        assertThat(r.minorCount).isEqualTo(3);
        assertThat(r.totalCalls).isEqualTo(4);
    }

    @Test
    public void stillSurfacesFailuresOfNonIngeniousTools() {
        // A failing agent-internal tool call (e.g. a shell command) still matters and
        // must not be silently swallowed, unlike its successful counterpart.
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(call("bash", false, "command not found"))
        );
        assertThat(r.activities).hasSize(1);
        assertThat(r.activities.get(0).status).isEqualTo(ActivityReport.Status.FAIL);
        assertThat(r.failCount).isEqualTo(1);
    }

    @Test
    public void collapsesRealisticStepAndValidateSequenceForSameTestCase() {
        // Mirrors the actual ingenious_testcase_* MCP tool result shapes: add_step ->
        // validate (fails) -> edit_step -> validate (passes), all on one test case.
        ActivityReport.Result r = ActivityReport.summarize(
            List.of(
                call(
                    "ingenious_testcase_add_step",
                    true,
                    "{\"added\":true,\"scenario\":\"Payments\",\"testcase\":\"RegisterFundAndPay\",\"totalSteps\":4}"
                ),
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"scenario\":\"Payments\",\"testcase\":\"RegisterFundAndPay\",\"checked\":1,\"valid\":false,\"errors\":[\"bad step\"],\"warnings\":[]}"
                ),
                call(
                    "ingenious_testcase_edit_step",
                    true,
                    "{\"edited\":true,\"testcase\":\"RegisterFundAndPay\",\"index\":2}"
                ),
                call(
                    "ingenious_testcase_validate",
                    true,
                    "{\"scenario\":\"Payments\",\"testcase\":\"RegisterFundAndPay\",\"checked\":1,\"valid\":true,\"errors\":[],\"warnings\":[]}"
                )
            )
        );
        assertThat(r.activities).hasSize(2);
        ActivityReport.Activity steps = r.activities.get(0);
        assertThat(steps.title).isEqualTo("Test steps updated");
        assertThat(steps.details).contains("Test case: RegisterFundAndPay");
        ActivityReport.Activity validate = r.activities.get(1);
        assertThat(validate.title).isEqualTo("Test case validated");
        assertThat(validate.status).isEqualTo(ActivityReport.Status.OK);
        assertThat(validate.details)
            .contains("Test case: RegisterFundAndPay", "Resolved after 2 attempts");
        assertThat(r.failCount).isEqualTo(0);
        assertThat(r.warnCount).isEqualTo(0);
    }
}
