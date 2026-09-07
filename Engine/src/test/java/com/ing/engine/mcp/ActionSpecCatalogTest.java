package com.ing.engine.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

/**
 * Conformance tests for the per-action {@link ArgSpec} catalog.
 *
 * <ul>
 *   <li><b>Coverage</b> - reference-category actions must be specced (strict);
 *       the overall gap is reported but tolerated (Phase 0 rollout).</li>
 *   <li><b>Golden round-trip</b> - every explicit spec's own examples must pass
 *       normalization and validation, so a spec can never be self-inconsistent.</li>
 *   <li><b>Determinism</b> - known formats auto-correct; clearly-wrong values are
 *       rejected with an error.</li>
 * </ul>
 */
public class ActionSpecCatalogTest {
    /** Reference-category actions that MUST carry an explicit @Args spec. */
    private static final String[] REFERENCE_ACTIONS = {
        "setEndPoint",
        "assertResponseCode",
        "postRestRequest",
        "getRestRequest",
        "addHeader",
        "assertJSONelementEquals",
        "initDBConnection",
        "executeSelectQuery",
        "executeDMLQuery",
        "storeDBValueinDataSheet",
        "setText",
        "setPort",
        "Open"
    };

    @Test
    public void referenceCategoriesAreSpecced() {
        for (String action : REFERENCE_ACTIONS) {
            assertThat(ActionSpecCatalog.hasSpec(action))
                .as("reference action '%s' must have an explicit @Args spec", action)
                .isTrue();
        }
    }

    @Test
    public void coverageIsReported() {
        // Report-only in Phase 0: the gap must not be negative and is logged so
        // the rollout can track it. It becomes a hard gate once all categories ship.
        List<String> gaps = ActionSpecCatalog.unspecifiedActions();
        System.out.println(
            "[ActionSpec] " + gaps.size() + " action(s) without an explicit format spec."
        );
        assertThat(gaps).isNotNull();
    }

    @Test
    public void everyExplicitSpecRoundTrips() {
        for (ArgSpec spec : ActionSpecCatalog.all()) {
            if (!spec.isExplicit()) {
                continue;
            }
            String inEx = spec.inputExample();
            String condEx = spec.conditionExample();
            ArgSpec.Result r = spec.normalize(inEx, condEx);
            assertThat(r.errors)
                .as("spec example for '%s' must normalize without error", spec.action())
                .isEmpty();
            assertThat(spec.validate(r.input, r.condition))
                .as("spec example for '%s' must validate clean", spec.action())
                .isEmpty();
        }
    }

    @Test
    public void httpStatusAutoCorrectsAndRejects() {
        ArgSpec spec = ActionSpecCatalog.forAction("assertResponseCode");
        // bare valid literal -> @-prefixed, no error
        ArgSpec.Result ok = spec.normalize("200", "");
        assertThat(ok.input).isEqualTo("@200");
        assertThat(ok.errors).isEmpty();
        // already-@ valid literal -> untouched
        assertThat(spec.normalize("@404", "").input).isEqualTo("@404");
        // clearly wrong -> rejected
        ArgSpec.Result bad = spec.normalize("ok", "");
        assertThat(bad.errors).isNotEmpty();
        assertThat(bad.errors.get(0)).contains("HTTP status code");
        // dynamic references are always accepted
        assertThat(spec.normalize("%code%", "").errors).isEmpty();
        assertThat(spec.normalize("Sheet:Col", "").errors).isEmpty();
    }

    @Test
    public void stepNormalizerRejectsWrongFormat() {
        StepNormalizer.Result r = StepNormalizer.normalize(
            "step 1",
            "assertResponseCode",
            "Webservice",
            "ok",
            ""
        );
        assertThat(r.errors).isNotEmpty();
        // a valid one passes and is @-prefixed
        StepNormalizer.Result ok = StepNormalizer.normalize(
            "step 1",
            "assertResponseCode",
            "Webservice",
            "200",
            ""
        );
        assertThat(ok.input).isEqualTo("@200");
        assertThat(ok.errors).isEmpty();
    }

    @Test
    public void unknownActionInfersTolerantSpec() {
        ArgSpec spec = ActionSpecCatalog.forAction("someCustomUserAction");
        assertThat(spec.isExplicit()).isFalse();
        // inferred specs never reject
        assertThat(spec.normalize("anything", "whatever").errors).isEmpty();
    }
}
