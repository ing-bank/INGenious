package com.ing.engine.aicli.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.planning.Plan.PlanStep;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.Test;

public class PlanningTest {
    private static final ObjectMapper M = new ObjectMapper();

    private static PlanStep step(String id, String tool, List<String> deps) {
        return new PlanStep(id, tool, M.createObjectNode(), deps);
    }

    @Test
    public void validPlanPassesAndOrdersTopologically() {
        ToolRegistry registry = ToolRegistry.create();
        Plan plan = new Plan(
            "demo",
            List.of(
                step("s2", "testcase_validate", List.of("s1")),
                step("s1", "gen_testcase", List.of())
            )
        );
        assertThat(PlanValidator.validate(plan, registry)).isEmpty();
        List<PlanStep> order = PlanValidator.topologicalOrder(plan);
        assertThat(order.get(0).id).isEqualTo("s1");
        assertThat(order.get(1).id).isEqualTo("s2");
    }

    @Test
    public void unknownToolAndUnknownDepAreReported() {
        ToolRegistry registry = ToolRegistry.create();
        Plan plan = new Plan("bad", List.of(step("s1", "made_up_tool", List.of("ghost"))));
        List<String> errors = PlanValidator.validate(plan, registry);
        assertThat(errors).anyMatch(e -> e.contains("Unknown tool"));
        assertThat(errors).anyMatch(e -> e.contains("unknown step"));
    }

    @Test
    public void cycleIsDetected() {
        Plan plan = new Plan(
            "cycle",
            List.of(
                step("s1", "testcase_list", List.of("s2")),
                step("s2", "testcase_list", List.of("s1"))
            )
        );
        assertThatThrownBy(() -> PlanValidator.topologicalOrder(plan))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cycle");
    }

    @Test
    public void planJsonRoundTrip() {
        ObjectNode args = M.createObjectNode();
        args.put("scenario", "Login");
        Plan plan = new Plan(
            "roundtrip",
            List.of(new PlanStep("s1", "ingenious_testcase_create", args, List.of()))
        );
        Plan back = Plan.fromJson(plan.toJson(M), M);
        assertThat(back.goal).isEqualTo("roundtrip");
        assertThat(back.steps).hasSize(1);
        // qualified names are normalized to short ids
        assertThat(back.steps.get(0).tool).isEqualTo("testcase_create");
        assertThat(back.steps.get(0).args.path("scenario").asText()).isEqualTo("Login");
    }

    @Test
    public void workflowMatching() {
        assertThat(WorkflowCatalog.match("create a login test")).isPresent();
        assertThat(WorkflowCatalog.match("Create API tests for Customers")).isPresent();
        assertThat(WorkflowCatalog.match("generate 10 rows of data for login")).isPresent();
        assertThat(WorkflowCatalog.match("run all smoke tests")).isPresent();
        assertThat(WorkflowCatalog.match("generate page objects from https://x.test")).isPresent();
        assertThat(WorkflowCatalog.match("clone testcase API/PingApiTest as PingCopy")).isPresent();
        assertThat(WorkflowCatalog.match("fix failing tests")).isPresent();
        assertThat(WorkflowCatalog.match("rerun failed tests")).isPresent();
        assertThat(WorkflowCatalog.match("please explain this assertion")).isEmpty();
    }

    @Test
    public void cloneWorkflowPipesStepsReference() {
        Optional<WorkflowCatalog.Match> m = WorkflowCatalog.match(
            "clone testcase API/PingApiTest as PingCopy"
        );
        assertThat(m).isPresent();
        Map<String, String> extracted = m.get().extracted();
        assertThat(extracted)
            .containsEntry("scenario", "API")
            .containsEntry("testcase", "PingApiTest")
            .containsEntry("newname", "PingCopy");
        Plan plan = m.get().workflow().build(extracted);
        assertThat(plan.steps).hasSize(3);
        assertThat(plan.steps.get(1).args.path("steps").asText()).isEqualTo("${s1.out.steps}");
    }

    @Test
    public void rerunFailedStripsProjectFromReportTarget() {
        Optional<WorkflowCatalog.Match> m = WorkflowCatalog.match("fix failing tests");
        assertThat(m).isPresent();
        Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("target", "CLIDemo/R1/Smoke");
        Plan plan = m.get().workflow().build(values);
        assertThat(plan.steps.get(0).args.path("target").asText()).isEqualTo("R1/Smoke");
        assertThat(plan.steps.get(1).args.path("target").asText()).isEqualTo("CLIDemo/R1/Smoke");
        assertThat(plan.steps.get(1).args.path("rerun").asBoolean()).isTrue();
    }

    @Test
    public void apiWorkflowExtractsTestcaseName() {
        Optional<WorkflowCatalog.Match> m = WorkflowCatalog.match("create api tests for Customers");
        assertThat(m).isPresent();
        assertThat(m.get().extracted()).containsEntry("testcase", "CustomersApiTest");

        Map<String, String> values = new java.util.LinkedHashMap<>(m.get().extracted());
        values.put("url", "https://x/api");
        values.put("status", "200");
        values.put("scenario", "API");
        Plan plan = m.get().workflow().build(values);
        assertThat(plan.steps).hasSize(2);
        assertThat(plan.steps.get(0).tool).isEqualTo("gen_testcase");
        assertThat(plan.steps.get(1).tool).isEqualTo("testcase_validate");
    }

    @Test
    public void dataWorkflowExtractsRowsAndSheet() {
        Optional<WorkflowCatalog.Match> m = WorkflowCatalog.match(
            "generate 10 rows of data for login"
        );
        assertThat(m).isPresent();
        assertThat(m.get().extracted()).containsEntry("rows", "10");
        assertThat(m.get().extracted()).containsEntry("sheet", "login");
    }
}
