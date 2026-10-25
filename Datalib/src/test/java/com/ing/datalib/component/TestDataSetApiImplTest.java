package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Tests for {@link TestDataSetApiImpl} — verifies the sheet-names accessor
 * delegates, and {@code getByName} reuses {@link TestDataModel#view()}
 * rather than introducing a new view type.
 */
public class TestDataSetApiImplTest {

    @Test
    public void getTestDataNamesShouldDelegate() {
        TestData testData = mock(TestData.class);
        when(testData.getTestDataNames()).thenReturn(List.of("Sheet1", "GlobalData"));

        assertThat(new TestDataSetApiImpl(testData).getTestDataNames())
            .containsExactly("Sheet1", "GlobalData");
    }

    @Test
    public void getByNameShouldReturnTheModelsView() {
        TestData testData = mock(TestData.class);
        TestDataModel model = mock(TestDataModel.class);
        TestDataView view = mock(TestDataView.class);
        when(testData.getByName("Sheet1")).thenReturn(model);
        when(model.view()).thenReturn(view);

        assertThat(new TestDataSetApiImpl(testData).getByName("Sheet1")).isSameAs(view);
    }

    @Test
    public void getByNameShouldReturnNullWhenNotFound() {
        TestData testData = mock(TestData.class);
        when(testData.getByName("Unknown")).thenReturn(null);

        assertThat(new TestDataSetApiImpl(testData).getByName("Unknown")).isNull();
    }
}
