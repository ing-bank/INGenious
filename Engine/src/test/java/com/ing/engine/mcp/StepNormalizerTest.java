package com.ing.engine.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** Conformance tests for the shared write-path normalization. */
public class StepNormalizerTest {

    @Test
    public void bareLiteralsGetAtPrefixed() {
        StepNormalizer.Result r = StepNormalizer.normalize(
            "step 1",
            "setEndPoint",
            "Webservice",
            "https://x"
        );
        assertThat(r.input).isEqualTo("@https://x");
        assertThat(r.warnings).isEmpty();

        r = StepNormalizer.normalize("step 2", "assertResponseCode", "Webservice", "200");
        assertThat(r.input).isEqualTo("@200");

        r = StepNormalizer.normalize("step 3", "Fill", "LoginPage.user", "alice");
        assertThat(r.input).isEqualTo("@alice");
    }

    @Test
    public void grammarConformantInputsUntouched() {
        assertThat(StepNormalizer.normalize("s", "Fill", "o", "@already").input)
            .isEqualTo("@already");
        assertThat(StepNormalizer.normalize("s", "Fill", "o", "Sheet:Col").input)
            .isEqualTo("Sheet:Col");
        assertThat(StepNormalizer.normalize("s", "Fill", "o", "%var%").input).isEqualTo("%var%");
        assertThat(StepNormalizer.normalize("s", "Fill", "o", "").input).isEqualTo("");
    }

    @Test
    public void payloadBodiesUntouched() {
        String body = "{\"a\":1}";
        assertThat(StepNormalizer.normalize("s", "postRestRequest", "Webservice", body).input)
            .isEqualTo(body);
        assertThat(StepNormalizer.normalize("s", "putRestRequest", "Webservice", body).input)
            .isEqualTo(body);
    }

    @Test
    public void globalDataIdInInputWarns() {
        StepNormalizer.Result r = StepNormalizer.normalize("step 4", "Fill", "o", "#test");
        assertThat(r.input).isEqualTo("#test"); // never silently rewritten
        assertThat(r.warnings).hasSize(1);
        assertThat(r.warnings.get(0)).contains("[E6]");
    }

    @Test
    public void atPrefixedObjectWarns() {
        StepNormalizer.Result r = StepNormalizer.normalize("step 5", "Click", "@LoginPage.btn", "");
        assertThat(r.warnings).hasSize(1);
        assertThat(r.warnings.get(0)).contains("[E7]");
        // Engine specials are fine:
        assertThat(StepNormalizer.normalize("s", "Open", "@Browser", "").warnings).isEmpty();
    }

    @Test
    public void literalSummary() {
        assertThat(StepNormalizer.literalSummary(0)).isNull();
        assertThat(StepNormalizer.literalSummary(2))
            .contains("2 step(s)")
            .contains("ingenious_testcase_parameterize")
            .contains("[W1]");
    }
}
