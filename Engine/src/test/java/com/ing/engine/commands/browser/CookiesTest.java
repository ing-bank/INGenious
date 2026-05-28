package com.ing.engine.commands.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.core.CommandControl;
import com.ing.engine.reporting.TestCaseReport;
import com.microsoft.playwright.options.Cookie;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Cookies — cookie string formatting and variable pattern validation.
 */
public class CookiesTest {

    @Mock private TestCaseReport report;
    @Mock private CommandControl commander;
    @Mock private com.microsoft.playwright.BrowserContext browserCtx;

    private Cookies cookies;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        cookies = mock(Cookies.class, CALLS_REAL_METHODS);
        setField(cookies, Command.class, "Report", report);
        setField(cookies, Command.class, "Commander", commander);
        // The "BrowserContext" field on Command is public
        setField(cookies, Command.class, "BrowserContext", browserCtx);
        setField(cookies, Command.class, "Action", "storeCookiesInVariable");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void testStoreCookiesFormatsStringCorrectly() throws Exception {
        Cookie c = new Cookie("session", "abc123");
        c.domain = ".example.com";
        c.url = "https://example.com";
        c.path = "/";

        when(browserCtx.cookies()).thenReturn(Collections.singletonList(c));
        setField(cookies, Command.class, "Input", "%myCookies%");

        cookies.storeCookiesInVariable();

        verify(commander).addVar(eq("%myCookies%"), contains("Name=session"));
        verify(commander).addVar(eq("%myCookies%"), contains("Value=abc123"));
        verify(commander).addVar(eq("%myCookies%"), contains("Domain=.example.com"));
    }

    @Test
    public void testStoreCookiesMultipleCookies() throws Exception {
        Cookie c1 = new Cookie("a", "1");
        c1.domain = "d1";
        c1.url = "u1";
        c1.path = "/p1";

        Cookie c2 = new Cookie("b", "2");
        c2.domain = "d2";
        c2.url = "u2";
        c2.path = "/p2";

        when(browserCtx.cookies()).thenReturn(Arrays.asList(c1, c2));
        setField(cookies, Command.class, "Input", "%allCookies%");

        cookies.storeCookiesInVariable();

        verify(commander).addVar(eq("%allCookies%"), argThat(s ->
                s.contains("Name=a") && s.contains("Name=b")));
    }

    @Test
    public void testStoreCookiesInvalidVariableFormat() throws Exception {
        when(browserCtx.cookies()).thenReturn(Collections.emptyList());
        setField(cookies, Command.class, "Input", "noPctSigns");

        cookies.storeCookiesInVariable();

        verify(report).updateTestLog(eq("storeCookiesInVariable"),
                eq("Invalid variable format"), any());
        verify(commander, never()).addVar(anyString(), anyString());
    }

    @Test
    public void testStoreCookiesEmptyListValidVar() throws Exception {
        when(browserCtx.cookies()).thenReturn(Collections.emptyList());
        setField(cookies, Command.class, "Input", "%empty%");

        cookies.storeCookiesInVariable();

        verify(commander).addVar(eq("%empty%"), eq(""));
    }

    @Test
    public void testClearCookiesDelegatesToBrowserContext() throws Exception {
        setField(cookies, Command.class, "Action", "clearCookies");

        cookies.clearCookies();

        verify(browserCtx).clearCookies();
        verify(report).updateTestLog(eq("clearCookies"),
                contains("Cookies clear"), any());
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
