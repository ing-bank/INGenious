package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.run.ProjectRunner;
import com.ing.engine.execution.run.TestCaseRunner;
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
    @Mock
    private TestCaseRunner context;

    @Mock
    private TestCaseRunner rootContext;

    @Mock
    private ProjectRunner executor;

    @Mock
    private TestDataModel envModel;

    @Mock
    private TestDataModel defModel;

    @Mock
    private GlobalDataModel globalEnvModel;

    @Mock
    private GlobalDataModel globalDefModel;

    @Mock
    private TestDataView envView;

    @Mock
    private TestDataView defView;

    @Mock
    private Project project;

    @Mock
    private EnvTestData dataProvider;

    @Mock
    private EnvTestData sharedDataProvider;

    @Mock
    private TestData projectEnvTestData;

    @Mock
    private TestData sharedEnvTestData;

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

    // ---- getDataFromModelWithScope ----

    @Test
    public void testGetDataFromModelNullModel() {
        String result = DataAccessInternal.getDataFromModelWithScope(
            null,
            "field",
            "scn",
            "tc",
            "1",
            "1",
            ""
        );
        assertThat(result).isNull();
    }

    @Test
    public void testGetDataFromModelReturnsValue() {
        TestDataView subView = mock(TestDataView.class);
        when(envModel.view()).thenReturn(envView);
        when(envView.withSubIterAndScope("scn1", "tc1", "1", "1", "")).thenReturn(subView);
        when(subView.getField("myField")).thenReturn("foundValue");

        String result = DataAccessInternal.getDataFromModelWithScope(
            envModel,
            "myField",
            "scn1",
            "tc1",
            "1",
            "1",
            ""
        );
        assertThat(result).isEqualTo("foundValue");
    }

    @Test
    public void testGetDataFromModelException() {
        when(envModel.view()).thenThrow(new RuntimeException("test error"));

        String result = DataAccessInternal.getDataFromModelWithScope(
            envModel,
            "field",
            "scn",
            "tc",
            "1",
            "1",
            ""
        );
        assertThat(result).isNull();
    }

    // ---- putDataToModel ----

    @Test
    public void testPutDataToModelNullModel() {
        boolean result = DataAccessInternal.putDataToModel(
            null,
            "field",
            "val",
            "scn",
            "tc",
            "1",
            "1",
            ""
        );
        assertThat(result).isFalse();
    }

    @Test
    public void testPutDataToModelSuccess() {
        TestDataView subView = mock(TestDataView.class);
        when(envModel.view()).thenReturn(envView);
        when(envView.withSubIterAndScope("scn1", "tc1", "1", "1", "", true)).thenReturn(subView);
        when(subView.update("myField", "newVal")).thenReturn(true);

        boolean result = DataAccessInternal.putDataToModel(
            envModel,
            "myField",
            "newVal",
            "scn1",
            "tc1",
            "1",
            "1",
            ""
        );
        assertThat(result).isTrue();
        verify(envModel).saveChanges();
    }

    @Test
    public void testPutDataToModelUpdateReturnsFalse() {
        TestDataView subView = mock(TestDataView.class);
        when(envModel.view()).thenReturn(envView);
        when(envView.withSubIterAndScope("scn1", "tc1", "1", "1", "", true)).thenReturn(subView);
        when(subView.update("myField", "newVal")).thenReturn(false);

        boolean result = DataAccessInternal.putDataToModel(
            envModel,
            "myField",
            "newVal",
            "scn1",
            "tc1",
            "1",
            "1",
            ""
        );
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
        when(envView.withSubIterAndScope("scn", "tc", "1", "1", "", true)).thenReturn(envSub);
        when(defView.withSubIterAndScope("scn", "tc", "1", "1", "", true)).thenReturn(defSub);
        when(envSub.update("field", "val")).thenReturn(false);
        when(defSub.update("field", "val")).thenReturn(true);

        boolean result = DataAccessInternal.putDataToModel(
            envModel,
            defModel,
            "field",
            "val",
            "scn",
            "tc",
            "1",
            "1",
            ""
        );
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
        com.ing.datalib.testdata.view.TestDataView envGView = mock(
            com.ing.datalib.testdata.view.TestDataView.class
        );
        when(globalEnvModel.view()).thenReturn(envGView);
        when(envGView.withScenarioOrGID("gid")).thenReturn(envGView);
        when(envGView.getField("field")).thenReturn(null);

        when(globalDefModel.hasColumn("field")).thenReturn(true);
        com.ing.datalib.testdata.view.TestDataView defGView = mock(
            com.ing.datalib.testdata.view.TestDataView.class
        );
        when(globalDefModel.view()).thenReturn(defGView);
        when(defGView.withScenarioOrGID("gid")).thenReturn(defGView);
        when(defGView.getField("field")).thenReturn("defValue");

        Object result = DataAccessInternal.getGlobal(
            globalEnvModel,
            globalDefModel,
            "gid",
            "field"
        );
        assertThat(result).isEqualTo("defValue");
    }

    // ---- getModel / getDefModel: [Shared]/[Project] scope tag resolution ----

    @Test
    public void testGetModelUntaggedResolvesFromProjectOnlyAndNeverTouchesShared() {
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("QA");
        when(executor.dataProvider()).thenReturn(dataProvider);
        when(dataProvider.getTestDataFor("QA")).thenReturn(projectEnvTestData);
        when(projectEnvTestData.getByName("LoginData")).thenReturn(envModel);

        TestDataModel result = DataAccessInternal.getModel(context, "LoginData");

        assertThat(result).isSameAs(envModel);
        verifyNoInteractions(project);
    }

    @Test
    public void testGetModelUntaggedMissingLocallyDoesNotFallBackToShared() {
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("QA");
        when(executor.dataProvider()).thenReturn(dataProvider);
        when(dataProvider.getTestDataFor("QA")).thenReturn(projectEnvTestData);
        when(projectEnvTestData.getByName("LoginData")).thenReturn(null);

        TestDataModel result = DataAccessInternal.getModel(context, "LoginData");

        assertThat(result).isNull();
        verifyNoInteractions(project);
    }

    @Test
    public void testGetModelExplicitProjectTagBehavesLikeUntagged() {
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("QA");
        when(executor.dataProvider()).thenReturn(dataProvider);
        when(dataProvider.getTestDataFor("QA")).thenReturn(projectEnvTestData);
        when(projectEnvTestData.getByName("LoginData")).thenReturn(envModel);

        TestDataModel result = DataAccessInternal.getModel(context, "[Project] LoginData");

        assertThat(result).isSameAs(envModel);
        verifyNoInteractions(project);
    }

    @Test
    public void testGetModelSharedTagResolvesOnlyFromSharedTestData() {
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("QA");
        when(context.project()).thenReturn(project);
        when(project.getSharedTestData()).thenReturn(sharedDataProvider);
        when(sharedDataProvider.getTestDataFor("QA")).thenReturn(sharedEnvTestData);
        when(sharedEnvTestData.getByName("LoginData")).thenReturn(envModel);

        TestDataModel result = DataAccessInternal.getModel(context, "[Shared] LoginData");

        assertThat(result).isSameAs(envModel);
        // A [Shared]-tagged reference must never consult the project's own test data.
        verify(executor, never()).dataProvider();
    }

    @Test
    public void testGetModelSharedTagFallsBackToSharedDefEnvWhenEnvMissing() {
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("QA");
        when(context.project()).thenReturn(project);
        when(project.getSharedTestData()).thenReturn(sharedDataProvider);
        when(sharedDataProvider.getTestDataFor("QA")).thenReturn(null);
        when(sharedDataProvider.defData()).thenReturn(sharedEnvTestData);
        when(sharedEnvTestData.getByName("LoginData")).thenReturn(envModel);

        TestDataModel result = DataAccessInternal.getModel(context, "[Shared] LoginData");

        assertThat(result).isSameAs(envModel);
    }

    @Test
    public void testGetModelSharedTagWhenProjectHasNoSharedTestDataReturnsNull() {
        when(context.executor()).thenReturn(executor);
        when(executor.runEnv()).thenReturn("QA");
        when(context.project()).thenReturn(project);
        when(project.getSharedTestData()).thenReturn(null);

        TestDataModel result = DataAccessInternal.getModel(context, "[Shared] LoginData");

        assertThat(result).isNull();
    }

    @Test
    public void testGetDefModelUntaggedUsesProjectDefData() {
        when(context.executor()).thenReturn(executor);
        when(executor.dataProvider()).thenReturn(dataProvider);
        when(dataProvider.defData()).thenReturn(projectEnvTestData);
        when(projectEnvTestData.getByName("LoginData")).thenReturn(defModel);

        TestDataModel result = DataAccessInternal.getDefModel(context, "LoginData");

        assertThat(result).isSameAs(defModel);
        verifyNoInteractions(project);
    }

    @Test
    public void testGetDefModelSharedTagUsesSharedDefEnv() {
        when(context.executor()).thenReturn(executor);
        when(executor.dataProvider()).thenReturn(dataProvider);
        when(dataProvider.defEnv()).thenReturn("Default");
        when(context.project()).thenReturn(project);
        when(project.getSharedTestData()).thenReturn(sharedDataProvider);
        when(sharedDataProvider.getTestDataFor("Default")).thenReturn(sharedEnvTestData);
        when(sharedEnvTestData.getByName("LoginData")).thenReturn(defModel);

        TestDataModel result = DataAccessInternal.getDefModel(context, "[Shared] LoginData");

        assertThat(result).isSameAs(defModel);
    }

    // ---- inheritSheetScopeForGlobalDataRef: a bare "#gid" cell value found inside a Shared
    // sheet must default to Shared GlobalData, not silently fall back to Project GlobalData. ----

    @Test
    public void testInheritSheetScopePromotesUntaggedGidWhenContainingSheetIsShared() {
        Object result = DataAccessInternal.inheritSheetScopeForGlobalDataRef(
            "#url",
            "[Shared] TestData0"
        );
        assertThat(result).isEqualTo("[Shared] #url");
    }

    @Test
    public void testInheritSheetScopeLeavesUntaggedGidAloneWhenContainingSheetIsProject() {
        Object result = DataAccessInternal.inheritSheetScopeForGlobalDataRef(
            "#url",
            "[Project] Basic"
        );
        assertThat(result).isEqualTo("#url");
    }

    @Test
    public void testInheritSheetScopeLeavesUntaggedGidAloneWhenContainingSheetIsUnscoped() {
        Object result = DataAccessInternal.inheritSheetScopeForGlobalDataRef("#url", "Basic");
        assertThat(result).isEqualTo("#url");
    }

    @Test
    public void testInheritSheetScopeRespectsExplicitTagAlreadyOnCellValue() {
        // The cell itself already opts out of the shared sheet's default - explicit wins.
        Object result = DataAccessInternal.inheritSheetScopeForGlobalDataRef(
            "[Project] #url",
            "[Shared] TestData0"
        );
        assertThat(result).isEqualTo("[Project] #url");
    }

    @Test
    public void testInheritSheetScopeLeavesNonGlobalDataValueUnchanged() {
        Object result = DataAccessInternal.inheritSheetScopeForGlobalDataRef(
            "plain-literal-value",
            "[Shared] TestData0"
        );
        assertThat(result).isEqualTo("plain-literal-value");
    }

    @Test
    public void testInheritSheetScopeLeavesNonStringValueUnchanged() {
        Object nonString = 42;
        Object result = DataAccessInternal.inheritSheetScopeForGlobalDataRef(
            nonString,
            "[Shared] TestData0"
        );
        assertThat(result).isSameAs(nonString);
    }
}
