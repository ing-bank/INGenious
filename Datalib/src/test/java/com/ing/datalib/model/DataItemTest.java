package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class DataItemTest {

    @Test
    public void testCreateSetsNameAndId() {
        DataItem di = DataItem.create("TestItem");
        assertThat(di.getName()).isEqualTo("TestItem");
        assertThat(di.getId()).isEqualTo("TestItem");
    }

    @Test
    public void testCreateTestCaseSetsScenarioAttribute() {
        DataItem di = DataItem.createTestCase("TC1", "Login");
        assertThat(di.getName()).isEqualTo("TC1");
        assertThat(di.hasScenario("Login")).isTrue();
        assertThat(di.getScenario()).isEqualTo("Login");
    }

    @Test
    public void testHasScenarioReturnsFalseWhenNoScenario() {
        DataItem di = DataItem.create("TC1");
        assertThat(di.hasScenario()).isFalse();
        assertThat(di.hasScenario("Login")).isFalse();
    }

    @Test
    public void testGetScenarioReturnsEmptyWhenNoScenario() {
        DataItem di = DataItem.create("TC1");
        assertThat(di.getScenario()).isEmpty();
    }

    @Test
    public void testGettersAndSetters() {
        DataItem di = new DataItem();
        di.setName("name");
        di.setId("id");
        assertThat(di.getName()).isEqualTo("name");
        assertThat(di.getId()).isEqualTo("id");
    }

    @Test
    public void testAttributesInitialized() {
        DataItem di = DataItem.create("test");
        assertThat(di.getAttributes()).isNotNull();
        assertThat(di.getAttributes()).isEmpty();
    }

    @Test
    public void testTagsInitialized() {
        DataItem di = DataItem.create("test");
        assertThat(di.getTags()).isNotNull();
        assertThat(di.getTags()).isEmpty();
    }

    @Test
    public void testSetAndGetTags() {
        DataItem di = DataItem.create("test");
        Tags tags = new Tags();
        tags.add("smoke");
        di.setTags(tags);
        assertThat(di.getTags().contains("smoke")).isTrue();
    }
}
