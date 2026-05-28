package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProjectInfoTest {

    private ProjectInfo projectInfo;

    @BeforeMethod
    public void setUp() {
        projectInfo = ProjectInfo.create("TestProject");
    }

    @Test
    public void testCreateSetsNameAndId() {
        assertThat(projectInfo.getName()).isEqualTo("TestProject");
        assertThat(projectInfo.getId()).isEqualTo("TestProject");
    }

    @Test
    public void testCreateInitializesDefaultMeta() {
        assertThat(projectInfo.getMeta()).isNotEmpty();
        assertThat(projectInfo.getMeta()).hasSize(4); // scenario, smoke, uat, load
    }

    @Test
    public void testCreateInitializesEmptyCollections() {
        assertThat(projectInfo.getAttributes()).isEmpty();
        assertThat(projectInfo.getTags()).isEmpty();
        assertThat(projectInfo.getData()).isEmpty();
    }

    @Test
    public void testSetAndGetVersion() {
        projectInfo.setVersion("2.0");
        assertThat(projectInfo.getVersion()).isEqualTo("2.0");
    }

    @Test
    public void testAddData() {
        DataItem di = DataItem.create("TC1");
        projectInfo.addData(di);
        assertThat(projectInfo.getData()).hasSize(1);
    }

    @Test
    public void testAddMeta() {
        Meta m = Meta.createTag("@custom");
        projectInfo.addMeta(m);
        assertThat(projectInfo.getMeta().contains("tag", "@custom")).isTrue();
    }

    @Test
    public void testFindMeta() {
        assertThat(projectInfo.findMeta("attribute", "scenario")).isPresent();
        assertThat(projectInfo.findMeta("attribute", "nonexistent")).isEmpty();
    }

    @Test
    public void testFindScenario() {
        projectInfo.addMeta(Meta.createScenario("Login"));
        assertThat(projectInfo.findScenario("Login")).isPresent();
        assertThat(projectInfo.findScenario("Missing")).isEmpty();
    }

    @Test
    public void testFindScenarioOrCreateReturnsExisting() {
        Meta existing = Meta.createScenario("Login");
        projectInfo.addMeta(existing);
        Meta result = projectInfo.findScenarioOrCreate("Login");
        assertThat(result.getName()).isEqualTo("Login");
        // should not add duplicate
        long count = projectInfo.findScenarios().count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testFindScenarioOrCreateCreatesNew() {
        Meta result = projectInfo.findScenarioOrCreate("NewScenario");
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("NewScenario");
    }

    @Test
    public void testFindScenarios() {
        projectInfo.addMeta(Meta.createScenario("Login"));
        projectInfo.addMeta(Meta.createScenario("Checkout"));
        List<Meta> scenarios = projectInfo.findScenarios().collect(Collectors.toList());
        assertThat(scenarios).hasSize(2);
    }

    @Test
    public void testGetAllTags() {
        projectInfo.getTags().add("global");
        DataItem di = DataItem.create("TC1");
        di.getTags().add("item-tag");
        projectInfo.addData(di);
        // Meta tags from default (smoke, uat, load)
        Tags allTags = (Tags) projectInfo.getAllTags(null);
        assertThat(allTags).isNotEmpty();
        assertThat(allTags.contains("global")).isTrue();
        assertThat(allTags.contains("item-tag")).isTrue();
    }

    @Test
    public void testGetAllTagsWithExistingTags() {
        Tags existing = new Tags();
        existing.add("extra");
        List<Tag> allTags = projectInfo.getAllTags(existing);
        assertThat(allTags).isNotEmpty();
    }

    @Test
    public void testRemoveAll() {
        Tag tag = Tag.create("@smoke");
        projectInfo.getTags().add(tag);
        DataItem di = DataItem.createTestCase("TC1", "Login");
        di.getTags().add(Tag.create("@smoke"));
        projectInfo.addData(di);

        projectInfo.removeAll(tag);
        assertThat(projectInfo.getTags().contains("@smoke")).isFalse();
    }

    @Test
    public void testToJsonProducesValidJson() throws JsonProcessingException {
        String json = projectInfo.toJson();
        assertThat(json).isNotEmpty();
        assertThat(json).contains("TestProject");
        assertThat(json).contains("_meta");
    }
}
