package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ControlCliRoutingTest {

    @DataProvider
    public Object[][] modernCliCommands() {
        return new Object[][] {
            { "run" },
            { "project" },
            { "scenario" },
            { "testcase" },
            { "testset" },
            { "report" },
            { "config" },
            { "server" },
            { "shell" },
            { "help" },
            { "--help" },
            { "-h" },
            { "--version" },
            { "-v" },
            { "-V" }
        };
    }

    @Test(dataProvider = "modernCliCommands")
    public void recognizesModernCliCommands(String command) {
        assertThat(Control.isNewCLICommand(new String[] { command })).isTrue();
    }

    @Test
    public void recognizesModernCliCommandsCaseInsensitively() {
        assertThat(Control.isNewCLICommand(new String[] { "RUN" })).isTrue();
    }

    @DataProvider
    public Object[][] legacyCliCommands() {
        return new Object[][] {
            { "-run" },
            { "-project_location" },
            { "-scenario" },
            { "-testcase" },
            { "-browser" }
        };
    }

    @Test(dataProvider = "legacyCliCommands")
    public void leavesLegacyOptionsOnTheLegacyPath(String option) {
        assertThat(Control.isNewCLICommand(new String[] { option })).isFalse();
    }

    @Test
    public void rejectsMissingArguments() {
        assertThat(Control.isNewCLICommand(null)).isFalse();
        assertThat(Control.isNewCLICommand(new String[0])).isFalse();
    }

    @Test
    public void rejectsUnknownCommands() {
        assertThat(Control.isNewCLICommand(new String[] { "not-a-command" })).isFalse();
    }
}
