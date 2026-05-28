package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.execution.run.ProjectRunner;
import com.ing.engine.execution.run.TestCaseRunner;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for DataAccess (extends DataAccessInternal) — focuses on
 * getTestData() and validEnv/getModel/getDefModel helper chains
 * using mocked TestCaseRunner and data providers.
 */
public class DataAccessTest {

    @Mock private TestCaseRunner context;
    @Mock private TestCaseRunner rootContext;
    @Mock private ProjectRunner executor;
    @Mock private EnvTestData dataProvider;
    @Mock private TestData defData;

    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Wire context → executor → dataProvider chain
        when(context.executor()).thenReturn(executor);
        when(executor.dataProvider()).thenReturn(dataProvider);
        when(dataProvider.defData()).thenReturn(defData);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    // ---- validEnv utility tests via getIterations ----

    @Test
    public void testValidEnvDefaultEnvReturnsFalse() {
        when(dataProvider.defEnv()).thenReturn("Default");
        when(executor.runEnv()).thenReturn("Default");

        // When defEnv equals runEnv, validEnv() returns false → takes getIter(context, def) path
        TestDataModel def = mock(TestDataModel.class);
        TestDataView view = mock(TestDataView.class);
        when(defData.getByName("Sheet1")).thenReturn(def);
        when(def.view()).thenReturn(view);
        when(context.getRoot()).thenReturn(rootContext);
        when(rootContext.scenario()).thenReturn("Scn1");
        when(rootContext.testcase()).thenReturn("TC1");
        when(context.scenario()).thenReturn("Scn1");
        when(context.testcase()).thenReturn("TC1");
        when(view.withTestcase(anyString(), anyString())).thenReturn(view);
        when(view.getIterations()).thenReturn(new java.util.LinkedHashSet<>());

        DataAccessInternal.getIterations(context, "Sheet1");
        // validEnv returns false → getTestDataFor never called for env
        verify(dataProvider, never()).getTestDataFor(anyString());
    }

    // ---- getTestData ----

    @Test
    public void testGetTestDataReturnsViewFromDefWhenNoEnv() {
        when(dataProvider.defEnv()).thenReturn("Default");
        when(executor.runEnv()).thenReturn("Default");

        TestDataModel def = mock(TestDataModel.class);
        TestDataView defView = mock(TestDataView.class);
        when(defData.getByName("Sheet1")).thenReturn(def);
        when(def.view()).thenReturn(defView);

        TestDataView result = DataAccess.getTestData(context, "Sheet1");
        assertThat(result).isSameAs(defView);
    }

    @Test
    public void testGetTestDataReturnsNullWhenDefModelIsNull() {
        when(dataProvider.defEnv()).thenReturn("Default");
        when(executor.runEnv()).thenReturn("Default");

        when(defData.getByName("Sheet1")).thenReturn(null);

        TestDataView result = DataAccess.getTestData(context, "Sheet1");
        assertThat(result).isNull();
    }
}
