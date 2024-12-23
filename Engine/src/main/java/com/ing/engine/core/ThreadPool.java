package com.ing.engine.core;

import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 *
 */
public class ThreadPool extends ThreadPoolExecutor {

    public Boolean doSelectiveThreading = false;

    public ThreadPool(int threadCount, long keepAliveTime, boolean isGridMode) {
        super(threadCount, threadCount, keepAliveTime, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        doSelectiveThreading = threadCount > 1 && !isGridMode;
    }

    Map<Runnable, Browser> browserPool = new HashMap<>();
    Queue<Runnable> BrowserList = new LinkedList<>();

    @Override
    protected synchronized void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (doSelectiveThreading) {
            if (browserPool.containsKey(r)) {
                browserPool.remove(r); 
            }
            if (BrowserList.isEmpty()) {
                shutdown();
            } else  {
                if (getActiveCount() < getCorePoolSize()) {
                   
                    Runnable ieRun = BrowserList.remove();
                    execute(ieRun);

                }
            }
        }
    }

    public synchronized void execute(Runnable command, Browser browserName) {
        execute(command);

    }

    public void shutdownExecution() {
        if (!doSelectiveThreading) {
            shutdown();
        }
    }

}
