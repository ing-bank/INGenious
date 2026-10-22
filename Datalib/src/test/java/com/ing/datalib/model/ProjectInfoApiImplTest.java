package com.ing.datalib.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import org.testng.annotations.Test;

/**
 * Tests for {@link ProjectInfoApiImpl} — verifies the data-item-names
 * accessor delegates to the wrapped {@link ProjectInfo} instance's
 * {@link Data} list.
 */
public class ProjectInfoApiImplTest {

    @Test
    public void getDataItemNamesShouldDelegateToTheUnderlyingDataItems() {
        ProjectInfo projectInfo = mock(ProjectInfo.class);
        Data data = new Data();
        DataItem item1 = new DataItem();
        item1.setName("Item1");
        DataItem item2 = new DataItem();
        item2.setName("Item2");
        data.add(item1);
        data.add(item2);
        when(projectInfo.getData()).thenReturn(data);

        assertThat(new ProjectInfoApiImpl(projectInfo).getDataItemNames())
            .containsExactly("Item1", "Item2");
    }

    @Test
    public void getDataItemNamesShouldBeEmptyWhenNoDataItems() {
        ProjectInfo projectInfo = mock(ProjectInfo.class);
        when(projectInfo.getData()).thenReturn(new Data());

        assertThat(new ProjectInfoApiImpl(projectInfo).getDataItemNames()).isEmpty();
    }
}
