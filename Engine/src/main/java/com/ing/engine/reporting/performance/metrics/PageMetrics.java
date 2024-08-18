
package com.ing.engine.reporting.performance.metrics;

import java.util.concurrent.Callable;

/**
 *
 * 
 */
public abstract class PageMetrics implements Callable<Object> {
  
    @Override
    abstract public Object call() throws Exception;

}
