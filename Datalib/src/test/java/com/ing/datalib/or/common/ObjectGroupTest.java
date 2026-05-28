package com.ing.datalib.or.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ObjectGroupTest {

    private ObjectGroup<WebORObject> group;
    private ORPageInf mockPage;
    private ORRootInf mockRoot;

    @BeforeMethod
    public void setUp() {
        mockPage = mock(ORPageInf.class);
        mockRoot = mock(ORRootInf.class);
        when(mockPage.getRoot()).thenReturn(mockRoot);
        group = new ObjectGroup<>("TestGroup", mockPage);
    }

    @Test
    public void testConstructor() {
        assertThat(group.getName()).isEqualTo("TestGroup");
        assertThat(group.getObjects()).isNotNull();
        assertThat(group.getObjects()).isEmpty();
    }

    @Test
    public void testSetAndGetName() {
        group.setName("Renamed");
        assertThat(group.getName()).isEqualTo("Renamed");
    }

    @Test
    public void testGetParent() {
        assertThat(group.getParent()).isSameAs(mockPage);
    }

    @Test
    public void testSetParent() {
        ORPageInf newParent = mock(ORPageInf.class);
        group.setParent(newParent);
        assertThat(group.getParent()).isSameAs(newParent);
    }

    @Test
    public void testGetObjectByNameReturnsNull() {
        assertThat(group.getObjectByName("nonexistent")).isNull();
    }

    @Test
    public void testSetObjectsSetsParent() {
        WebORObject obj = mock(WebORObject.class);
        List<WebORObject> objects = new ArrayList<>();
        objects.add(obj);
        group.setObjects(objects);
        assertThat(group.getObjects()).hasSize(1);
    }

    @Test
    public void testDeleteObjectRemovesIt() {
        WebORObject obj = mock(WebORObject.class);
        when(obj.getName()).thenReturn("Object0");
        List<WebORObject> objects = new ArrayList<>();
        objects.add(obj);
        group.setObjects(objects);

        group.deleteObject("Object0");
        assertThat(group.getObjects()).isEmpty();
    }

    @Test
    public void testDeleteObjectNonexistentDoesNothing() {
        group.deleteObject("nonexistent");
        assertThat(group.getObjects()).isEmpty();
    }

    @Test
    public void testGetChildCountWithNullObjects() {
        ObjectGroup<WebORObject> emptyGroup = new ObjectGroup<>();
        assertThat(emptyGroup.getChildCount()).isZero();
    }

    @Test
    public void testGetChildCount() {
        assertThat(group.getChildCount()).isZero();
    }

    @Test
    public void testIsLeaf() {
        assertThat(group.isLeaf()).isTrue();
    }

    @Test
    public void testAllowsChildren() {
        assertThat(group.getAllowsChildren()).isTrue();
    }

    @Test
    public void testToString() {
        assertThat(group.toString()).isEqualTo("TestGroup");
    }
}
