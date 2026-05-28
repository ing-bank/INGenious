package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Tests for ThreadPool — constructor logic, doSelectiveThreading flag,
 * shutdown behavior, and execute delegation.
 */
public class ThreadPoolTest {

    private ThreadPool pool;

    @AfterMethod
    public void tearDown() {
        if (pool != null && !pool.isShutdown()) {
            pool.shutdownNow();
            try {
                pool.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    // ---- Constructor: doSelectiveThreading ----

    @Test
    public void testSingleThreadNoGrid_notSelective() {
        pool = new ThreadPool(1, 1, false);
        assertThat(pool.doSelectiveThreading).isFalse();
    }

    @Test
    public void testMultiThreadNoGrid_isSelective() {
        pool = new ThreadPool(2, 1, false);
        assertThat(pool.doSelectiveThreading).isTrue();
    }

    @Test
    public void testMultiThreadGridMode_notSelective() {
        pool = new ThreadPool(3, 1, true);
        assertThat(pool.doSelectiveThreading).isFalse();
    }

    @Test
    public void testSingleThreadGridMode_notSelective() {
        pool = new ThreadPool(1, 1, true);
        assertThat(pool.doSelectiveThreading).isFalse();
    }

    // ---- Pool size ----

    @Test
    public void testCorePoolSize() {
        pool = new ThreadPool(4, 5, false);
        assertThat(pool.getCorePoolSize()).isEqualTo(4);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(4);
    }

    // ---- shutdownExecution ----

    @Test
    public void testShutdownExecutionWhenNotSelective() {
        pool = new ThreadPool(1, 1, false);
        pool.shutdownExecution();
        assertThat(pool.isShutdown()).isTrue();
    }

    @Test
    public void testShutdownExecutionWhenSelective_noShutdown() {
        pool = new ThreadPool(2, 1, false);
        pool.shutdownExecution();
        // When doSelectiveThreading is true, shutdownExecution does nothing
        assertThat(pool.isShutdown()).isFalse();
    }

    // ---- execute(Runnable, Browser) ----

    @Test
    public void testExecuteWithBrowser() throws Exception {
        pool = new ThreadPool(1, 1, true);
        final boolean[] ran = {false};
        pool.execute(() -> ran[0] = true, Browser.Chromium);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(ran[0]).isTrue();
    }

    // ---- afterExecute: selective threading shutdown when BrowserList empty ----

    @Test
    public void testAfterExecuteShutdownWhenBrowserListEmpty() throws Exception {
        pool = new ThreadPool(2, 1, false);
        // doSelectiveThreading = true, BrowserList is empty
        pool.execute(() -> { /* no-op */ });
        // The pool should auto-shutdown after the task completes because
        // BrowserList is empty and doSelectiveThreading is true
        pool.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(pool.isShutdown()).isTrue();
    }
}
