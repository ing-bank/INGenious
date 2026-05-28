package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class TagTest {

    @Test
    public void testCreateSetsValue() {
        Tag t = Tag.create("smoke");
        assertThat(t.getValue()).isEqualTo("smoke");
    }

    @Test
    public void testSetAndGetValue() {
        Tag t = new Tag();
        t.setValue("regression");
        assertThat(t.getValue()).isEqualTo("regression");
    }

    @Test
    public void testToStringReturnsValue() {
        Tag t = Tag.create("load");
        assertThat(t.toString()).isEqualTo("load");
    }

    @Test
    public void testEqualsWithSameValue() {
        Tag a = Tag.create("smoke");
        Tag b = Tag.create("smoke");
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    public void testEqualsWithDifferentValue() {
        Tag a = Tag.create("smoke");
        Tag b = Tag.create("load");
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    public void testEqualsWithString() {
        Tag a = Tag.create("smoke");
        assertThat(a.equals("smoke")).isTrue();
        assertThat(a.equals("load")).isFalse();
    }

    @Test
    public void testHashCodeConsistent() {
        Tag a = Tag.create("smoke");
        Tag b = Tag.create("smoke");
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void testHashCodeDiffersForDifferentValues() {
        Tag a = Tag.create("smoke");
        Tag b = Tag.create("load");
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    public void testStateDefaultFalse() {
        Tag t = Tag.create("test");
        assertThat(t.state).isFalse();
    }
}
