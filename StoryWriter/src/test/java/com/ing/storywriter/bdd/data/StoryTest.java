package com.ing.storywriter.bdd.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.json.simple.JSONObject;
import org.testng.annotations.Test;

public class StoryTest {

    @Test
    public void testConstructorOneArg() {
        Story s = new Story("Login Feature");
        assertThat(s.name).isEqualTo("Login Feature");
        assertThat(s.desc).isEqualTo("Login Feature");
        assertThat(s.type).isEqualTo("Feature");
    }

    @Test
    public void testConstructorTwoArgs() {
        Story s = new Story("Login", "Login description");
        assertThat(s.name).isEqualTo("Login");
        assertThat(s.desc).isEqualTo("Login");
    }

    @Test
    public void testDefaultData() {
        Story s = new Story("MyFeature", "Some desc");
        assertThat(s.getData()).contains("Feature: MyFeature");
        assertThat(s.getData()).contains("#BDD-Feature File");
    }

    @Test
    public void testToString() {
        Story s = new Story("Test");
        String ts = s.toString();
        assertThat(ts).contains("Feature: Test");
    }

    @Test
    public void testToJSON() {
        Story s = new Story("JSONTest");
        JSONObject json = s.toJSON();
        assertThat(json.get("type")).isEqualTo("Feature");
        assertThat(json.get("name")).isEqualTo("JSONTest");
        assertThat(json.get("data")).isNotNull();
    }

    @Test
    public void testMeta() {
        Story s = new Story("MetaTest");
        assertThat(s.meta()).isNotNull();
        assertThat(s.meta()).isEmpty();
    }

    @Test
    public void testAddMeta() {
        Story s = new Story("MetaTest");
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("priority", "high");
        s.addMeta(meta);
        assertThat(s.meta()).containsEntry("priority", "high");
    }

    @Test
    public void testAddMetaNull() {
        Story s = new Story("MetaTest");
        s.addMeta(null); // should not throw
        assertThat(s.meta()).isEmpty();
    }

    @Test
    public void testSetDataUpdatesName() {
        Story s = new Story("Original");
        s.setData("#BDD-Feature File\n\nFeature: Updated\nDescription");
        assertThat(s.name).isEqualTo("Updated");
    }

    @Test
    public void testSetDataPrependsNewline() {
        Story s = new Story("Test");
        s.setData("Feature: NewName\nSome line");
        // update() prepends lineSeparator when data starts with "Feature:"
        assertThat(s.getData()).startsWith(System.lineSeparator());
    }
}
