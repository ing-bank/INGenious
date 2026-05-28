package com.ing.datalib.util.data;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Tests for LinkedProperties — insertion-ordered Properties subclass.
 */
public class LinkedPropertiesTest {

    @Test
    public void testPutAndGet() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("key1", "val1");
        assertThat(lp.getProperty("key1")).isEqualTo("val1");
    }

    @Test
    public void testInsertionOrderPreserved() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("c", "3");
        lp.put("a", "1");
        lp.put("b", "2");

        List<Object> keyList = Collections.list(lp.keys());
        assertThat(keyList).containsExactly("c", "a", "b");
    }

    @Test
    public void testOrderedKeysMatchesInsertionOrder() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("z", "1");
        lp.put("m", "2");
        lp.put("a", "3");

        List<Object> ordered = new ArrayList<>();
        lp.orderedKeys().forEach(ordered::add);
        assertThat(ordered).containsExactly("z", "m", "a");
    }

    @Test
    public void testUpdateMovesKeyToEnd() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("first", "1");
        lp.put("second", "2");
        lp.put("third", "3");

        lp.update("first", "1-updated");

        List<Object> keyList = Collections.list(lp.keys());
        assertThat(keyList).containsExactly("second", "third", "first");
        assertThat(lp.getProperty("first")).isEqualTo("1-updated");
    }

    @Test
    public void testClearRemovesAll() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("a", "1");
        lp.put("b", "2");

        lp.clear();

        assertThat(lp).isEmpty();
        List<Object> keyList = Collections.list(lp.keys());
        assertThat(keyList).isEmpty();
    }

    @Test
    public void testDuplicatePutOverwritesValue() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("key", "old");
        lp.put("key", "new");

        assertThat(lp.getProperty("key")).isEqualTo("new");
        // Key should appear only once in ordered keys
        List<Object> keyList = Collections.list(lp.keys());
        assertThat(keyList).hasSize(1);
    }

    @Test
    public void testSetPropertyUsesLinkedOrder() {
        LinkedProperties lp = new LinkedProperties();
        lp.setProperty("x", "1");
        lp.setProperty("y", "2");

        List<Object> keyList = Collections.list(lp.keys());
        assertThat(keyList).containsExactly("x", "y");
    }

    @Test
    public void testIsEmptyInitially() {
        LinkedProperties lp = new LinkedProperties();
        assertThat(lp.isEmpty()).isTrue();
    }

    @Test
    public void testSize() {
        LinkedProperties lp = new LinkedProperties();
        lp.put("a", "1");
        lp.put("b", "2");
        assertThat(lp.size()).isEqualTo(2);
    }
}
