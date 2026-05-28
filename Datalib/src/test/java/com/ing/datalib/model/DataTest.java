package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DataTest {

    private Data data;

    @BeforeMethod
    public void setUp() {
        data = new Data();
    }

    @Test
    public void testAddDataItem() {
        boolean added = data.add(DataItem.create("TC1"));
        assertThat(added).isTrue();
        assertThat(data).hasSize(1);
    }

    @Test
    public void testAddDuplicateNameIsRejected() {
        data.add(DataItem.create("TC1"));
        boolean added = data.add(DataItem.create("TC1"));
        assertThat(added).isFalse();
        assertThat(data).hasSize(1);
    }

    @Test
    public void testContainsByName() {
        data.add(DataItem.create("TC1"));
        assertThat(data.contains("TC1")).isTrue();
        assertThat(data.contains("TC2")).isFalse();
    }

    @Test
    public void testGetByName() {
        data.add(DataItem.create("TC1"));
        DataItem result = data.getByName("TC1");
        assertThat(result.getName()).isEqualTo("TC1");
    }

    @Test
    public void testFindByName() {
        data.add(DataItem.create("TC1"));
        assertThat(data.find("TC1")).isPresent();
        assertThat(data.find("TC2")).isEmpty();
    }

    @Test
    public void testFindByNameAndScenario() {
        DataItem di = DataItem.createTestCase("TC1", "Login");
        data.add(di);
        assertThat(data.find("TC1", "Login")).isPresent();
        assertThat(data.find("TC1", "Logout")).isEmpty();
    }

    @Test
    public void testFindOrCreateReturnsExisting() {
        DataItem existing = DataItem.createTestCase("TC1", "Login");
        data.add(existing);
        DataItem result = data.findOrCreate("TC1", "Login");
        assertThat(result).isSameAs(existing);
        assertThat(data).hasSize(1);
    }

    @Test
    public void testFindOrCreateCreatesNew() {
        DataItem result = data.findOrCreate("TC1", "Login");
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("TC1");
        assertThat(result.hasScenario("Login")).isTrue();
        assertThat(data).hasSize(1);
    }

    @Test
    public void testAddAllWithNull() {
        assertThat(data.addAll(null)).isFalse();
    }

    @Test
    public void testEqualsStaticHelper() {
        assertThat(Data.equals(() -> "abc", "abc")).isTrue();
        assertThat(Data.equals(() -> "abc", "xyz")).isFalse();
    }
}
