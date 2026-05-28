package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashSet;
import java.util.Set;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DataIteratorTest {

    private DataIterator iter;

    @BeforeMethod
    public void setUp() {
        iter = new DataIterator();
    }

    @Test
    public void testInitialMaxIterIsOne() {
        // maxIter initialized to -1, but getMaxIter returns Math.max(1, -1) = 1
        assertThat(iter.getMaxIter()).isEqualTo(1);
    }

    @Test
    public void testSetMaxIter() {
        iter.setMaxIter(5);
        assertThat(iter.getMaxIter()).isEqualTo(5);
    }

    @Test
    public void testSetMaxIterMinimumIsOne() {
        iter.setMaxIter(0);
        assertThat(iter.getMaxIter()).isEqualTo(1);
    }

    @Test
    public void testSetMaxIterNegative() {
        iter.setMaxIter(-10);
        assertThat(iter.getMaxIter()).isEqualTo(1);
    }

    @Test
    public void testIsIterResolved() {
        assertThat(iter.isIterResolved("Sheet1")).isFalse();
        Set<String> iterSet = new HashSet<>();
        iterSet.add("1");
        iterSet.add("2");
        iterSet.add("3");
        iter.setIter("Sheet1", iterSet);
        assertThat(iter.isIterResolved("Sheet1")).isTrue();
    }

    @Test
    public void testSetIterSetsMaxIter() {
        Set<String> iterSet = new HashSet<>();
        iterSet.add("1");
        iterSet.add("2");
        iterSet.add("3");
        iter.setIter("Sheet1", iterSet);
        assertThat(iter.getMaxIter()).isEqualTo(3);
    }

    @Test
    public void testSetIterTakesMinimum() {
        Set<String> iterSet1 = new HashSet<>();
        iterSet1.add("1");
        iterSet1.add("2");
        iterSet1.add("3");
        iterSet1.add("4");
        iterSet1.add("5");
        iter.setIter("Sheet1", iterSet1);
        assertThat(iter.getMaxIter()).isEqualTo(5);

        Set<String> iterSet2 = new HashSet<>();
        iterSet2.add("1");
        iterSet2.add("2");
        iter.setIter("Sheet2", iterSet2);
        assertThat(iter.getMaxIter()).isEqualTo(2);
    }

    @Test
    public void testSetMaxIterThenSetIter() {
        iter.setMaxIter(3);
        Set<String> iterSet = new HashSet<>();
        iterSet.add("1");
        iterSet.add("2");
        iterSet.add("3");
        iterSet.add("4");
        iterSet.add("5");
        iter.setIter("Sheet1", iterSet);
        // min(5, 3) = 3
        assertThat(iter.getMaxIter()).isEqualTo(3);
    }

    @Test
    public void testToString() {
        iter.setMaxIter(5);
        assertThat(iter.toString()).isEqualTo("MaxIter:5");
    }
}
