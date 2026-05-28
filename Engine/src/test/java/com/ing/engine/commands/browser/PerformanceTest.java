package com.ing.engine.commands.browser;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for Performance — focuses on the pure-function escapeName method
 * and HAR model construction helpers.
 */
public class PerformanceTest {

    // ── escapeName (private) — tested via reflection ────────────────────

    @DataProvider(name = "escapeNames")
    public Object[][] escapeNamesData() {
        return new Object[][] {
            { "Simple Page",       "Simple_Page" },
            { "hello-world",       "hello-world" },
            { "foo@bar#baz!!",     "foo_bar_baz_" },
            { "a  b",             "a_b" },          // double space → double underscore → collapsed
            { "",                 "" },
            { null,               "" },
            { "abc123",           "abc123" },
            { "Hello___World",    "Hello_World" },   // triple underscore collapsed
            { "test/page?q=1&x=2","test_page_q_1_x_2" },
        };
    }

    @Test(dataProvider = "escapeNames")
    public void testEscapeName(String input, String expected) throws Exception {
        Method m = Performance.class.getDeclaredMethod("escapeName", String.class);
        m.setAccessible(true);
        // escapeName is an instance method — need an instance; use null-commander trick
        Performance perf = createPerformanceInstance();
        String result = (String) m.invoke(perf, input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testEscapeNamePreservesDashes() throws Exception {
        Method m = Performance.class.getDeclaredMethod("escapeName", String.class);
        m.setAccessible(true);
        Performance perf = createPerformanceInstance();
        String result = (String) m.invoke(perf, "my-page-title");
        assertThat(result).isEqualTo("my-page-title");
    }

    @Test
    public void testEscapeNameCollapsesMultipleUnderscores() throws Exception {
        Method m = Performance.class.getDeclaredMethod("escapeName", String.class);
        m.setAccessible(true);
        Performance perf = createPerformanceInstance();
        // "a!!!b" → "a___b" → "a_b"
        String result = (String) m.invoke(perf, "a!!!b");
        assertThat(result).isEqualTo("a_b");
    }

    /**
     * Create a Performance instance via CALLS_REAL_METHODS mock
     * to avoid the CommandControl constructor requirement.
     */
    private Performance createPerformanceInstance() {
        return org.mockito.Mockito.mock(Performance.class,
                org.mockito.Mockito.CALLS_REAL_METHODS);
    }
}
