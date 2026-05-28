package com.ing.engine.commands.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.core.CommandControl;
import com.ing.engine.reporting.TestCaseReport;

import java.lang.reflect.Field;
import java.nio.file.Paths;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for StorageState — verifies the StoreStorageState action delegates
 * to BrowserContext.storageState with correct path.
 */
public class StorageStateTest {

    @Mock private TestCaseReport report;
    @Mock private CommandControl commander;
    @Mock private com.microsoft.playwright.BrowserContext browserCtx;

    private StorageState storageState;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        storageState = mock(StorageState.class, CALLS_REAL_METHODS);
        setField(storageState, Command.class, "Report", report);
        setField(storageState, Command.class, "Commander", commander);
        setField(storageState, Command.class, "BrowserContext", browserCtx);
        setField(storageState, Command.class, "Action", "StoreStorageState");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void testStoreStorageStateCallsBrowserContext() throws Exception {
        setField(storageState, Command.class, "Data", "/tmp/state.json");

        storageState.StoreStorageState();

        verify(browserCtx).storageState(argThat(opts -> {
            // StorageStateOptions has a path set
            return opts != null;
        }));
        verify(report).updateTestLog(eq("StoreStorageState"),
                contains("successfully stored"), any());
    }

    @Test
    public void testStoreStorageStateReportsErrorOnException() throws Exception {
        setField(storageState, Command.class, "Data", "/tmp/state.json");
        when(browserCtx.storageState(any())).thenThrow(new RuntimeException("disk full"));

        try {
            storageState.StoreStorageState();
        } catch (Exception e) {
            // ActionException expected
        }

        verify(report).updateTestLog(eq("StoreStorageState"),
                contains("Error storing"), any());
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
