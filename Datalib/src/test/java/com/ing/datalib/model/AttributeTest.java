package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class AttributeTest {

    @Test
    public void testCreateWithValidNameAndValue() {
        Attribute attr = Attribute.create("browser", "chrome");
        assertThat(attr).isNotNull();
        assertThat(attr.getName()).isEqualTo("browser");
        assertThat(attr.getValue()).isEqualTo("chrome");
    }

    @Test
    public void testCreateReturnsNullWhenNameIsNull() {
        Attribute attr = Attribute.create(null, "chrome");
        assertThat(attr).isNull();
    }

    @Test
    public void testCreateReturnsNullWhenValueIsNull() {
        Attribute attr = Attribute.create("browser", null);
        assertThat(attr).isNull();
    }

    @Test
    public void testCreateReturnsNullWhenBothNull() {
        Attribute attr = Attribute.create(null, null);
        assertThat(attr).isNull();
    }

    @Test
    public void testCreateConvertsObjectsToStrings() {
        Attribute attr = Attribute.create(42, true);
        assertThat(attr).isNotNull();
        assertThat(attr.getName()).isEqualTo("42");
        assertThat(attr.getValue()).isEqualTo("true");
    }

    @Test
    public void testGettersAndSetters() {
        Attribute attr = new Attribute();
        attr.setName("key");
        attr.setValue("val");
        assertThat(attr.getName()).isEqualTo("key");
        assertThat(attr.getValue()).isEqualTo("val");
    }

    @Test
    public void testEqualsWithMatchingAttributes() {
        Attribute a = Attribute.create("name", "value");
        Attribute b = Attribute.create("name", "value");
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    public void testEqualsWithDifferentName() {
        Attribute a = Attribute.create("name1", "value");
        Attribute b = Attribute.create("name2", "value");
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    public void testEqualsWithDifferentValue() {
        Attribute a = Attribute.create("name", "value1");
        Attribute b = Attribute.create("name", "value2");
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    public void testEqualsWithNullFields() {
        Attribute a = new Attribute();
        Attribute b = new Attribute();
        assertThat(a.equals(b)).isTrue();
    }
}
