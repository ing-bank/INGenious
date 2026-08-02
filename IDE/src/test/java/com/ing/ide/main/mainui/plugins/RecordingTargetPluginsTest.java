package com.ing.ide.main.mainui.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ingenious.api.contract.ui.RecordingTarget;
import com.ing.ingenious.api.contract.ui.RecordingTargetApi;
import java.util.List;
import org.testng.Reporter;
import org.testng.annotations.Test;

/**
 * Exercises the rule the recorder relies on: a plugin may answer where a recording belongs, but
 * no plugin may prevent a recording from starting.
 */
public class RecordingTargetPluginsTest {

    @Test
    public void theFirstPluginWithAnAnswerDecidesAndTheRestAreNotAsked() {
        RecordingTarget expected = new RecordingTarget("Checkout", "Pay by card");
        boolean[] secondWasAsked = { false };

        RecordingTarget target = RecordingTargetPlugins.currentTarget(
            List.of(
                () -> expected,
                () -> {
                    secondWasAsked[0] = true;
                    return new RecordingTarget("Other", "Other");
                }
            )
        );

        assertThat(target).isSameAs(expected);
        assertThat(secondWasAsked[0])
            .as("a later plugin is not consulted once one has answered")
            .isFalse();

        Reporter.log(
            "theFirstPluginWithAnAnswerDecidesAndTheRestAreNotAsked EVIDENCE: target=" +
            target +
            ", second plugin asked=" +
            secondWasAsked[0],
            true
        );
    }

    @Test
    public void aPluginWithNoOpinionIsPassedOver() {
        RecordingTarget expected = new RecordingTarget("Checkout", "Pay by card", true);

        RecordingTarget target = RecordingTargetPlugins.currentTarget(
            List.of(() -> null, () -> expected)
        );

        assertThat(target).isSameAs(expected);
        assertThat(target.isReusableScenario()).isTrue();

        Reporter.log(
            "aPluginWithNoOpinionIsPassedOver EVIDENCE: a null answer was skipped and the next plugin supplied " +
            target,
            true
        );
    }

    @Test
    public void aThrowingPluginCannotStopARecordingFromStarting() {
        RecordingTarget expected = new RecordingTarget(
            "Checkout",
            "Pay by card",
            false,
            "https://example.invalid/checkout"
        );
        RecordingTargetApi throwing = () -> {
            throw new IllegalStateException("the tracker is unreachable");
        };

        RecordingTarget target = RecordingTargetPlugins.currentTarget(
            List.of(throwing, () -> expected)
        );

        assertThat(target).isSameAs(expected);
        assertThat(target.getStartUrl()).isEqualTo("https://example.invalid/checkout");

        Reporter.log(
            "aThrowingPluginCannotStopARecordingFromStarting EVIDENCE: a plugin that threw was skipped and the recording target became " +
            target,
            true
        );
    }

    @Test
    public void withNoAnswerAtAllTheUserIsAskedAsUsual() {
        RecordingTargetApi throwing = () -> {
            throw new IllegalStateException("the tracker is unreachable");
        };

        assertThat(RecordingTargetPlugins.currentTarget(List.of(throwing, () -> null))).isNull();
        assertThat(RecordingTargetPlugins.currentTarget(List.of())).isNull();

        Reporter.log(
            "withNoAnswerAtAllTheUserIsAskedAsUsual EVIDENCE: a throwing plugin, a silent plugin and an empty plugin list all returned null, which is what makes Studio open its own target chooser",
            true
        );
    }
}
