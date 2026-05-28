package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MetaListTest {

    private MetaList metaList;

    @BeforeMethod
    public void setUp() {
        metaList = new MetaList();
    }

    @Test
    public void testAddMeta() {
        boolean added = metaList.add(Meta.create("tag", "smoke"));
        assertThat(added).isTrue();
        assertThat(metaList).hasSize(1);
    }

    @Test
    public void testAddDuplicateIsRejected() {
        metaList.add(Meta.create("tag", "smoke"));
        boolean added = metaList.add(Meta.create("tag", "smoke"));
        assertThat(added).isFalse();
        assertThat(metaList).hasSize(1);
    }

    @Test
    public void testAddDifferentTypeSameNameAllowed() {
        metaList.add(Meta.create("tag", "smoke"));
        boolean added = metaList.add(Meta.create("attribute", "smoke"));
        assertThat(added).isTrue();
        assertThat(metaList).hasSize(2);
    }

    @Test
    public void testAddByNameAndValue() {
        metaList.add("tag", "smoke");
        assertThat(metaList.contains("tag", "smoke")).isTrue();
    }

    @Test
    public void testContains() {
        metaList.add(Meta.create("tag", "smoke"));
        assertThat(metaList.contains("tag", "smoke")).isTrue();
        assertThat(metaList.contains("tag", "load")).isFalse();
    }

    @Test
    public void testFindByTypeAndName() {
        metaList.add(Meta.create("tag", "smoke"));
        assertThat(metaList.find("tag", "smoke")).isPresent();
        assertThat(metaList.find("tag", "missing")).isEmpty();
    }

    @Test
    public void testGet() {
        metaList.add(Meta.create("tag", "smoke"));
        Meta result = metaList.get("tag", "smoke");
        assertThat(result.getName()).isEqualTo("smoke");
    }

    @Test
    public void testUpdateExisting() {
        metaList.add(Meta.create("tag", "smoke"));
        Meta updated = Meta.create("tag", "smoke");
        updated.setRef("newRef");
        metaList.update(updated);
        assertThat(metaList).hasSize(1);
        assertThat(metaList.get("tag", "smoke").getRef()).isEqualTo("newRef");
    }

    @Test
    public void testUpdateAddsNew() {
        metaList.update(Meta.create("tag", "smoke"));
        assertThat(metaList).hasSize(1);
    }

    @Test
    public void testUpdateWithNullIsIgnored() {
        metaList.update(null);
        assertThat(metaList).isEmpty();
    }

    @Test
    public void testRemoveByTypeAndName() {
        metaList.add(Meta.create("tag", "smoke"));
        metaList.add(Meta.create("tag", "load"));
        metaList.remove("tag", "smoke");
        assertThat(metaList).hasSize(1);
        assertThat(metaList.contains("tag", "smoke")).isFalse();
    }

    @Test
    public void testRemoveByTag() {
        metaList.add(Meta.createTag("@smoke"));
        metaList.remove(Tag.create("@smoke"));
        assertThat(metaList).isEmpty();
    }

    @Test
    public void testAddAllWithNull() {
        assertThat(metaList.addAll(null)).isFalse();
    }
}
