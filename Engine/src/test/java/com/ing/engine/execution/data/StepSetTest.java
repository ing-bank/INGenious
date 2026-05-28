package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class StepSetTest {

    @Test
    public void testConstructorDefaults() {
        StepSet ss = new StepSet(5);
        assertThat(ss.from).isEqualTo(5);
        assertThat(ss.to).isEqualTo(-1);
        assertThat(ss.getTimes()).isEqualTo(1);
        assertThat(ss.current()).isEqualTo(1);
        assertThat(ss.isLoop).isFalse();
        assertThat(ss.isSubIterDynamic).isFalse();
    }

    @Test
    public void testNext() {
        StepSet ss = new StepSet(0);
        ss.setTimes(3); // times = max(1,3)-1 = 2
        int nextCounter = ss.next();
        assertThat(nextCounter).isEqualTo(2);
        assertThat(ss.getTimes()).isEqualTo(1); // decremented
    }

    @Test
    public void testNextMultiple() {
        StepSet ss = new StepSet(0);
        ss.setTimes(4); // times = 3
        assertThat(ss.next()).isEqualTo(2); // counter=2, times=2
        assertThat(ss.next()).isEqualTo(3); // counter=3, times=1
        assertThat(ss.next()).isEqualTo(4); // counter=4, times=0
    }

    @Test
    public void testSetTimesPositive() {
        StepSet ss = new StepSet(0);
        ss.setTimes(5);
        assertThat(ss.getTimes()).isEqualTo(4); // max(1,5)-1 = 4
    }

    @Test
    public void testSetTimesZero() {
        StepSet ss = new StepSet(0);
        ss.setTimes(0);
        // resolvedTimes=0, 0 >= 0, so times = max(1,0)-1 = 0
        assertThat(ss.getTimes()).isEqualTo(0);
    }

    @Test
    public void testSetTimesNegativeMakesDynamic() {
        StepSet ss = new StepSet(0);
        ss.setTimes(-1);
        assertThat(ss.isSubIterDynamic).isTrue();
        assertThat(ss.getTimes()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void testBreakIt() {
        StepSet ss = new StepSet(0);
        ss.setTimes(10); // times = 9
        int counterBefore = ss.current();
        ss.breakIt();
        assertThat(ss.getTimes()).isEqualTo(0);
        // counter += times (which was 9 before breakIt set it to 0)
        assertThat(ss.current()).isEqualTo(counterBefore + 9);
    }

    @Test
    public void testToField() {
        StepSet ss = new StepSet(3);
        ss.to = 10;
        assertThat(ss.to).isEqualTo(10);
    }

    @Test
    public void testIsLoopFlag() {
        StepSet ss = new StepSet(0);
        ss.isLoop = true;
        assertThat(ss.isLoop).isTrue();
    }
}
