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

    @Test
    public void testSetIterIgnoresBlankValueInflatingCount() {
        // a ragged/trailing row can leave an empty Iteration cell; it must not be counted
        // as a 4th distinct iteration when only 1..3 actually have data
        Set<String> iterSet = new HashSet<>();
        iterSet.add("1");
        iterSet.add("2");
        iterSet.add("3");
        iterSet.add("");
        iter.setIter("Sheet1", iterSet);
        assertThat(iter.getMaxIter()).isEqualTo(3);
    }

    @Test
    public void testSetIterUsesMaxValueNotSizeAcrossMismatchedSheets() {
        // Sheet1 has iterations {1,2,3}; Sheet2 has the same *count* (3) but a gap at 2
        // instead of 3 distinct dense values. The bound must come from the real values,
        // not merely from the two sets happening to have equal size.
        Set<String> iterSet1 = new HashSet<>();
        iterSet1.add("1");
        iterSet1.add("2");
        iterSet1.add("3");
        iter.setIter("Sheet1", iterSet1);
        assertThat(iter.getMaxIter()).isEqualTo(3);

        Set<String> iterSet2 = new HashSet<>();
        iterSet2.add("1");
        iterSet2.add("3");
        iterSet2.add("5");
        iter.setIter("Sheet2", iterSet2);
        // min(3, 5) = 3, unaffected here since the max of Sheet2 (5) isn't smaller
        assertThat(iter.getMaxIter()).isEqualTo(3);
    }
}
