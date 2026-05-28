package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for RunContext POJO — fields, getName(), print(), defaults.
 */
public class RunContextTest {

    private RunContext ctx;

    @BeforeMethod
    public void setUp() {
        ctx = new RunContext();
    }

    // ---- Defaults ----

    @Test
    public void testDefaultUseExistingDriverIsFalse() {
        assertThat(ctx.useExistingDriver).isFalse();
    }

    @Test
    public void testFieldsAreNullByDefault() {
        assertThat(ctx.Scenario).isNull();
        assertThat(ctx.TestCase).isNull();
        assertThat(ctx.Description).isNull();
        assertThat(ctx.Browser).isNull();
        assertThat(ctx.BrowserName).isNull();
        assertThat(ctx.BrowserVersion).isNull();
        assertThat(ctx.Iteration).isNull();
        assertThat(ctx.PlatformValue).isNull();
        assertThat(ctx.BrowserVersionValue).isNull();
    }

    // ---- getName() ----

    @Test
    public void testGetNameFormat() {
        ctx.Scenario = "Login";
        ctx.TestCase = "TC01";
        ctx.Iteration = "1";
        ctx.BrowserName = "Chrome";
        assertThat(ctx.getName()).isEqualTo("Login_TC01_1_Chrome");
    }

    @Test
    public void testGetNameWithNulls() {
        // getName uses String.format — nulls become "null"
        assertThat(ctx.getName()).isEqualTo("null_null_null_null");
    }

    // ---- Browser enum ----

    @Test
    public void testBrowserField() {
        ctx.Browser = Browser.Chromium;
        assertThat(ctx.Browser).isEqualTo(Browser.Chromium);
    }

    // ---- print() ----

    @Test
    public void testPrintDoesNotThrow() {
        ctx.Scenario = "S1";
        ctx.TestCase = "TC1";
        ctx.Description = "desc";
        ctx.BrowserName = "Firefox";
        ctx.BrowserVersion = "100";
        ctx.useExistingDriver = true;
        // print() writes to stdout — just verify no exception
        ctx.print();
    }

    // ---- Field assignment ----

    @Test
    public void testFieldAssignment() {
        ctx.Scenario = "Checkout";
        ctx.TestCase = "TC_Buy";
        ctx.Description = "End-to-end buy flow";
        ctx.BrowserName = "Chromium";
        ctx.BrowserVersion = "120";
        ctx.Iteration = "3";
        ctx.PlatformValue = "macOS";
        ctx.BrowserVersionValue = "120.0";
        ctx.useExistingDriver = true;

        assertThat(ctx.Scenario).isEqualTo("Checkout");
        assertThat(ctx.TestCase).isEqualTo("TC_Buy");
        assertThat(ctx.Description).isEqualTo("End-to-end buy flow");
        assertThat(ctx.BrowserName).isEqualTo("Chromium");
        assertThat(ctx.BrowserVersion).isEqualTo("120");
        assertThat(ctx.Iteration).isEqualTo("3");
        assertThat(ctx.PlatformValue).isEqualTo("macOS");
        assertThat(ctx.BrowserVersionValue).isEqualTo("120.0");
        assertThat(ctx.useExistingDriver).isTrue();
    }
}
