package com.ing.datalib.or.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class ORAttributeTest {

    @Test
    public void testDefaultConstructor() {
        ORAttribute attr = new ORAttribute();
        assertThat(attr.getName()).isNull();
        assertThat(attr.getValue()).isNull();
        assertThat(attr.getPreference()).isNull();
    }

    @Test
    public void testParameterizedConstructor() {
        ORAttribute attr = new ORAttribute("id", 1);
        assertThat(attr.getName()).isEqualTo("id");
        assertThat(attr.getValue()).isEmpty();
        assertThat(attr.getPreference()).isEqualTo("1");
    }

    @Test
    public void testGettersAndSetters() {
        ORAttribute attr = new ORAttribute();
        attr.setName("locator");
        attr.setValue("//div[@id='main']");
        attr.setPreference("2");
        assertThat(attr.getName()).isEqualTo("locator");
        assertThat(attr.getValue()).isEqualTo("//div[@id='main']");
        assertThat(attr.getPreference()).isEqualTo("2");
    }

    @Test
    public void testCloneAs() {
        ORAttribute original = new ORAttribute("xpath", 3);
        original.setValue("//button");

        ORAttribute clone = original.cloneAs();
        assertThat(clone).isNotSameAs(original);
        assertThat(clone.getName()).isEqualTo("xpath");
        assertThat(clone.getValue()).isEqualTo("//button");
        assertThat(clone.getPreference()).isEqualTo("3");
    }

    @Test
    public void testCloneAsIsIndependent() {
        ORAttribute original = new ORAttribute("id", 1);
        original.setValue("btn");
        ORAttribute clone = original.cloneAs();

        clone.setValue("changed");
        assertThat(original.getValue()).isEqualTo("btn");
    }

    @Test
    public void testToString() {
        ORAttribute attr = new ORAttribute("id", 1);
        attr.setValue("main");
        String str = attr.toString();
        assertThat(str).contains("ref = id");
        assertThat(str).contains("value = main");
        assertThat(str).contains("pref = 1");
    }
}
