
package com.ing.engine.execution.data;

/**
 *
 * 
 */
public class StepSet {

    public final int from;
    public int to;
    private int times;
    private int counter;
    public Boolean isLoop = false;
    public Boolean isSubIterDynamic = false;

    public StepSet(int from) {
        this.from = from;
        to = -1;
        times = 1;
        counter = 1;
    }

    public int next() {
        times--;
        return ++counter;
    }

    public int current() {
        return counter;
    }

    public void breakIt() {
        counter += times;
        times = 0;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int resolvedTimes) {
        if (resolvedTimes >= 0) {
            times = Math.max(1, resolvedTimes) - 1;
        } else {
            isSubIterDynamic = true;
            times = Integer.MAX_VALUE;
        }
    }

}
