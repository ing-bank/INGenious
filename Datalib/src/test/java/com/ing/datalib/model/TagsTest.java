package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TagsTest {

    private Tags tags;

    @BeforeMethod
    public void setUp() {
        tags = new Tags();
    }

    @Test
    public void testAddTag() {
        boolean added = tags.add(Tag.create("smoke"));
        assertThat(added).isTrue();
        assertThat(tags).hasSize(1);
    }

    @Test
    public void testAddDuplicateIsRejected() {
        tags.add(Tag.create("smoke"));
        boolean added = tags.add(Tag.create("smoke"));
        assertThat(added).isFalse();
        assertThat(tags).hasSize(1);
    }

    @Test
    public void testAddByString() {
        boolean added = tags.add("regression");
        assertThat(added).isTrue();
        assertThat(tags.contains("regression")).isTrue();
    }

    @Test
    public void testAddByEnum() {
        boolean added = tags.add(Meta.Tags.smoke);
        assertThat(added).isTrue();
        assertThat(tags.contains("smoke")).isTrue();
    }

    @Test
    public void testContains() {
        tags.add(Tag.create("smoke"));
        assertThat(tags.contains("smoke")).isTrue();
        assertThat(tags.contains("missing")).isFalse();
    }

    @Test
    public void testFind() {
        tags.add(Tag.create("uat"));
        assertThat(tags.find("uat")).isPresent();
        assertThat(tags.find("missing")).isEmpty();
    }

    @Test
    public void testGet() {
        tags.add(Tag.create("load"));
        Tag t = tags.get("load");
        assertThat(t.getValue()).isEqualTo("load");
    }

    @Test
    public void testRemoveTag() {
        tags.add(Tag.create("smoke"));
        tags.add(Tag.create("uat"));
        tags.removeTag(Tag.create("smoke"));
        assertThat(tags).hasSize(1);
        assertThat(tags.contains("smoke")).isFalse();
        assertThat(tags.contains("uat")).isTrue();
    }

    @Test
    public void testRemoveNonexistentTagDoesNothing() {
        tags.add(Tag.create("smoke"));
        tags.removeTag(Tag.create("missing"));
        assertThat(tags).hasSize(1);
    }

    @Test
    public void testAddAllWithNull() {
        assertThat(tags.addAll(null)).isFalse();
    }
}
