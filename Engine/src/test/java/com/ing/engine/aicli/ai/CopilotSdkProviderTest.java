package com.ing.engine.aicli.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.testng.annotations.Test;

public class CopilotSdkProviderTest {

    @Test
    public void keepsRequestedModelWhenAvailable() {
        assertThat(
                CopilotSdkProvider.selectSessionModel(
                    "claude-sonnet-4.5",
                    List.of("gpt-5", "claude-sonnet-4.5")
                )
            )
            .isEqualTo("claude-sonnet-4.5");
    }

    @Test
    public void usesCliDefaultWhenRequestedModelIsUnavailable() {
        assertThat(
                CopilotSdkProvider.selectSessionModel(
                    "claude-sonnet-4.5",
                    List.of("gpt-5", "claude-sonnet-4.6")
                )
            )
            .isNull();
    }

    @Test
    public void preservesRequestedModelWhenCatalogCannotConfirmAvailability() {
        assertThat(CopilotSdkProvider.selectSessionModel("claude-sonnet-4.5", List.of()))
            .isEqualTo("claude-sonnet-4.5");
    }
}
