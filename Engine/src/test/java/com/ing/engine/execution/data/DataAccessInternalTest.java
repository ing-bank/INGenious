package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.execution.run.ProjectRunner;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for DataAccessInternal static helper methods with mocked
 * TestCaseRunner, TestDataModel, and GlobalDataModel.
 */
public class DataAccessInternalTest {

    @Mock private TestCaseRunner context;
    @Mock private TestCaseRunner rootContext;
    @Mock private ProjectRunner executor;
    @Mock private TestDataModel envModel;
    @Mock private TestDataModel defModel;
    @Mock private GlobalDataModel globalEnvModel;
    @Mock private GlobalDataModel globalDefModel;
    @Mock private TestDataView envView;
    @Mock private TestDataView defView;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    // ---- notNull / isNull ----

    @Test
    public void testNotNullReturnsTrue() {
        assertThat(DataAccessInternal.notNull("hello")).isTrue();
    }

    @Test
    public void testNotNullReturnsFalse() {
        assertThat(DataAccessInternal.notNull(null)).isFalse();
    }

    @Test
    public void testIsNullReturnsTrue() {
        assertThat(DataAccessInternal.isNull(null)).isTrue();
    }

    @Test
    public void testIsNullReturnsFalse() {
        assertThat(DataAccessInternal.isNull("hello")).isFalse();
    }

    // ---- getDataFromModel ----

    @Test
    public void testGetDataFromModelNullModel() {
        String result = DataAccessInternal.getDataFromModel(null, "field", "scn", "tc", "1", "1");
        assertThat(result).isNull();
    }

    @Test
    public void testGetDataFromModelReturnsValue() {
        TestDataView subView = mock(TestDataView.class);
        when(envModel.view()).thenReturn(envView);
        when(envView.withSubIter("scn1", "tc1", "1", "1")).thenReturn(subView);
        when(subView.getField("myField")).thenReturn("foundValue");

        String result = DataAccessInternal.getDataFromModel(envModel, "myField", "scn1", "tc1", "1", "1");
        assertThat(result).isEqualTo("foundValue");
    }

    @Test
    public void testGetDataFromModelException() {
        when(envModel.view()).thenThrow(new RuntimeException("test error"));

        String result = DataAccessInternal.getDataFromModel(envModel, "field", "scn", "tc", "1", "1");
        assertThat(result).isNull();
    }

    // ---- putDataToModel ----

    @Test
    public void testPutDataToModelNullModel() {
        boolean result = DataAccessInternal.putDataToModel(null, "field", "val", "scn", "tc", "1", "1");
        assertThat(result).isFalse();
    }

    @Test
    public void testPutDataToModelSuccess() {
        TestDataView subView = mock(TestDataView.class);
        when(envModel.view()).thenReturn(envView);
        when(envView.withSubIter("scn1", "tc1", "1", "1", true)).thenReturn(subView);
        when(subView.update("myField", "newVal")).thenReturn(true);

        boolean result = DataAccessInternal.putDataToModel(envModel, "myField", "newVal", "scn1", "tc1", "1", "1");
        assertThat(result).isTrue();
        verify(envModel).saveChanges();
    }

    @Test
    public void testPutDataToModelUpdateReturnsFalse() {
        TestDataView subView = mock(TestDataView.class);
        when(envModel.view()).thenReturn(envView);
        when(envView.withSubIter("scn1", "tc1", "1", "1", true)).thenReturn(subView);
        when(subView.update("myField", "newVal")).thenReturn(false);

        boolean result = DataAccessInternal.putDataToModel(envModel, "myField", "newVal", "scn1", "tc1", "1", "1");
        assertThat(result).isFalse();
        verify(envModel, never()).saveChanges();
    }

    // ---- putDataToModel (env + def overload) ----

    @Test
    public void testPutDataToModelEnvDefFallsBackToDef() {
        // env update fails, def update succeeds
        TestDataView envSub = mock(TestDataView.class);
        TestDataView defSub = mock(TestDataView.class);

        when(envModel.view()).thenReturn(envView);
        when(defModel.view()).thenReturn(defView);
        when(envView.withSubIter("scn", "tc", "1", "1", true)).thenReturn(envSub);
        when(defView.withSubIter("scn", "tc", "1", "1", true)).thenReturn(defSub);
        when(envSub.update("field", "val")).thenReturn(false);
        when(defSub.update("field", "val")).thenReturn(true);

        boolean result = DataAccessInternal.putDataToModel(envModel, defModel, "field", "val", "scn", "tc", "1", "1");
        assertThat(result).isTrue();
    }

    // ---- getGlobal ----

    @Test
    public void testGetGlobalNullModel() {
        Object result = DataAccessInternal.getGlobal(null, "gid", "field");
        assertThat(result).isNull();
    }

    @Test
    public void testGetGlobalNoColumn() {
        when(globalDefModel.hasColumn("field")).thenReturn(false);
        Object result = DataAccessInternal.getGlobal(globalDefModel, "gid", "field");
        assertThat(result).isNull();
    }

    @Test
    public void testGetGlobalEnvDefFallback() {
        // env returns null, def returns value
        when(globalEnvModel.hasColumn("field")).thenReturn(true);
        com.ing.datalib.testdata.view.TestDataView envGView = mock(com.ing.datalib.testdata.view.TestDataView.class);
        when(globalEnvModel.view()).thenReturn(envGView);
        when(envGView.withScenarioOrGID("gid")).thenReturn(envGView);
        when(envGView.getField("field")).thenReturn(null);

        when(globalDefModel.hasColumn("field")).thenReturn(true);
        com.ing.datalib.testdata.view.TestDataView defGView = mock(com.ing.datalib.testdata.view.TestDataView.class);
        when(globalDefModel.view()).thenReturn(defGView);
        when(defGView.withScenarioOrGID("gid")).thenReturn(defGView);
        when(defGView.getField("field")).thenReturn("defValue");

        Object result = DataAccessInternal.getGlobal(globalEnvModel, globalDefModel, "gid", "field");
        assertThat(result).isEqualTo("defValue");
    }
}
