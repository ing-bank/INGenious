package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AttributesTest {

    private Attributes attrs;

    @BeforeMethod
    public void setUp() {
        attrs = new Attributes();
    }

    @Test
    public void testAddAttribute() {
        boolean added = attrs.add(Attribute.create("name", "value"));
        assertThat(added).isTrue();
        assertThat(attrs).hasSize(1);
    }

    @Test
    public void testAddDuplicateNameIsRejected() {
        attrs.add(Attribute.create("name", "value1"));
        boolean added = attrs.add(Attribute.create("name", "value2"));
        assertThat(added).isFalse();
        assertThat(attrs).hasSize(1);
    }

    @Test
    public void testAddByNameAndValue() {
        boolean added = attrs.add("key", "val");
        assertThat(added).isTrue();
        assertThat(attrs.get("key").getValue()).isEqualTo("val");
    }

    @Test
    public void testContainsByName() {
        attrs.add(Attribute.create("browser", "chrome"));
        assertThat(attrs.contains("browser")).isTrue();
        assertThat(attrs.contains("nonexistent")).isFalse();
    }

    @Test
    public void testContainsByNameAndValue() {
        attrs.add(Attribute.create("browser", "chrome"));
        assertThat(attrs.contains("browser", "chrome")).isTrue();
        assertThat(attrs.contains("browser", "firefox")).isFalse();
    }

    @Test
    public void testFindByName() {
        attrs.add(Attribute.create("key", "val"));
        assertThat(attrs.find("key")).isPresent();
        assertThat(attrs.find("missing")).isEmpty();
    }

    @Test
    public void testFindByAttribute() {
        attrs.add(Attribute.create("key", "val"));
        Attribute search = Attribute.create("key", "val");
        assertThat(attrs.find(search)).isPresent();
    }

    @Test
    public void testGetByName() {
        attrs.add(Attribute.create("key", "val"));
        Attribute result = attrs.get("key");
        assertThat(result.getValue()).isEqualTo("val");
    }

    @Test
    public void testUpdateExistingAttribute() {
        attrs.add(Attribute.create("key", "old"));
        attrs.update(Attribute.create("key", "new"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("key").getValue()).isEqualTo("new");
    }

    @Test
    public void testUpdateAddsNewAttribute() {
        attrs.update(Attribute.create("key", "val"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("key").getValue()).isEqualTo("val");
    }

    @Test
    public void testUpdateWithNullIsIgnored() {
        attrs.update(null);
        assertThat(attrs).isEmpty();
    }

    @Test
    public void testAddAllWithNullReturnsFalse() {
        assertThat(attrs.addAll(null)).isFalse();
    }

    @Test
    public void testEqualsStaticHelper() {
        assertThat(Attributes.equals(() -> "abc", "abc")).isTrue();
        assertThat(Attributes.equals(() -> "abc", "xyz")).isFalse();
        assertThat(Attributes.equals(() -> null, null)).isTrue();
    }
}
