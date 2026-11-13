package com.ing.engine.aicli.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
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

    @Test
    public void absoluteClasspathKeepsWildcardsAndAbsolutizesEntries() {
        String original = System.getProperty("java.class.path");
        try {
            System.setProperty(
                "java.class.path",
                String.join(File.pathSeparator, "lib" + File.separator + "*", "extra.jar")
            );
            String root = new File("").getAbsolutePath();
            assertThat(CopilotSdkProvider.absoluteClasspath().split(File.pathSeparator))
                .containsExactly(
                    root + File.separator + "lib" + File.separator + "*",
                    root + File.separator + "extra.jar"
                );
        } finally {
            System.setProperty("java.class.path", original);
        }
    }

    @Test
    public void classpathArgFileContentQuotesAndEscapesWindowsPaths() {
        assertThat(
                CopilotSdkProvider.classpathArgFileContent(
                    "C:\\Program Files\\INGenious\\lib\\*;C:\\x.jar"
                )
            )
            .isEqualTo("-cp\n\"C:\\\\Program Files\\\\INGenious\\\\lib\\\\*;C:\\\\x.jar\"\n");
    }
}
