package com.ing.engine.aicli.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

public class PlannerJsonTest {

    @Test
    public void extractsBareJson() {
        assertThat(Planner.extractJson("{\"type\":\"answer\",\"text\":\"hi\"}"))
            .isEqualTo("{\"type\":\"answer\",\"text\":\"hi\"}");
    }

    @Test
    public void extractsFencedJson() {
        String content = "```json\n{\"type\":\"plan\",\"steps\":[]}\n```";
        assertThat(Planner.extractJson(content)).isEqualTo("{\"type\":\"plan\",\"steps\":[]}");
    }

    @Test
    public void extractsJsonEmbeddedInProse() {
        String content = "Here is the plan: {\"a\":{\"b\":\"}\"}} enjoy";
        assertThat(Planner.extractJson(content)).isEqualTo("{\"a\":{\"b\":\"}\"}}");
    }

    @Test
    public void returnsNullWhenNoJson() {
        assertThat(Planner.extractJson("no json here")).isNull();
        assertThat(Planner.extractJson(null)).isNull();
    }
}
