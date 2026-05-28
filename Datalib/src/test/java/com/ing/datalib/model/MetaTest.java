package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

public class MetaTest {

    @Test
    public void testCreateSetsTypeAndName() {
        Meta m = Meta.create("attribute", "scenario");
        assertThat(m.getType()).isEqualTo("attribute");
        assertThat(m.getName()).isEqualTo("scenario");
    }

    @Test
    public void testScenarioFactory() {
        Meta m = Meta.scenario();
        assertThat(m.getType()).isEqualTo("attribute");
        assertThat(m.getName()).isEqualTo("scenario");
        assertThat(m.getDesc()).isNotEmpty();
        assertThat(m.getRef()).isEqualTo(Attribute.class.getName());
    }

    @Test
    public void testSmokeFactory() {
        Meta m = Meta.smoke();
        assertThat(m.getType()).isEqualTo("tag");
        assertThat(m.getName()).isEqualTo("@smoke");
        assertThat(m.getDesc()).isNotEmpty();
    }

    @Test
    public void testUatFactory() {
        Meta m = Meta.uat();
        assertThat(m.getType()).isEqualTo("tag");
        assertThat(m.getName()).isEqualTo("@uat");
    }

    @Test
    public void testLoadFactory() {
        Meta m = Meta.load();
        assertThat(m.getType()).isEqualTo("tag");
        assertThat(m.getName()).isEqualTo("@load");
    }

    @Test
    public void testCreateScenario() {
        Meta m = Meta.createScenario("LoginFlow");
        assertThat(m.getType()).isEqualTo("scenario");
        assertThat(m.getName()).isEqualTo("LoginFlow");
        assertThat(m.getRef()).isEqualTo(Attribute.class.getName());
    }

    @Test
    public void testCreateTag() {
        Meta m = Meta.createTag("@custom");
        assertThat(m.getType()).isEqualTo("tag");
        assertThat(m.getName()).isEqualTo("@custom");
        assertThat(m.getRef()).isEqualTo(Tag.class.getName());
    }

    @Test
    public void testDef() {
        assertThat(Meta.def()).hasSize(4);
    }

    @Test
    public void testEqualsSameTypeAndName() {
        Meta a = Meta.create("tag", "smoke");
        Meta b = Meta.create("tag", "smoke");
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    public void testEqualsDifferentType() {
        Meta a = Meta.create("tag", "smoke");
        Meta b = Meta.create("attribute", "smoke");
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    public void testEqualsDifferentName() {
        Meta a = Meta.create("tag", "smoke");
        Meta b = Meta.create("tag", "load");
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    public void testIsTag() {
        Meta tag = Meta.createTag("@smoke");
        Meta attr = Meta.scenario();
        assertThat(tag.isTag()).isTrue();
        assertThat(attr.isTag()).isFalse();
    }

    @Test
    public void testToTag() {
        Meta m = Meta.createTag("@smoke");
        Tag t = m.toTag();
        assertThat(t.getValue()).isEqualTo("@smoke");
    }

    @Test
    public void testGettersAndSetters() {
        Meta m = new Meta();
        m.setType("type");
        m.setName("name");
        m.setDesc("desc");
        m.setRef("ref");
        assertThat(m.getType()).isEqualTo("type");
        assertThat(m.getName()).isEqualTo("name");
        assertThat(m.getDesc()).isEqualTo("desc");
        assertThat(m.getRef()).isEqualTo("ref");
    }

    @Test
    public void testAttributesAndTagsInitialized() {
        Meta m = new Meta();
        assertThat(m.getAttributes()).isNotNull();
        assertThat(m.getTags()).isNotNull();
    }

    @Test
    public void testTagsEnumToString() {
        assertThat(Meta.Tags.smoke.toString()).isEqualTo("@smoke");
        assertThat(Meta.Tags.uat.toString()).isEqualTo("@uat");
        assertThat(Meta.Tags.load.toString()).isEqualTo("@load");
    }
}
