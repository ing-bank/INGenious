package com.ing.engine.reporting.performance.metrics;

import java.util.concurrent.Callable;

/**
 *
 *
 */
public abstract class PageMetrics implements Callable<Object> {

    @Override
    public abstract Object call() throws Exception;
}
