package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.data.DataNotFoundException.Cause;
import com.ing.engine.execution.exception.data.GlobalDataNotFoundException;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;
import org.testng.annotations.Test;

/**
 * Regression tests for {@link CommandControl#getDataSheetValue(String)} and
 * {@link CommandControl#getDatasheet(String)}.
 *
 * <p>Untagged {@code Sheet:Column} references used to be resolved by a hand-rolled scan of the
 * currently selected Environment's sheet names only, bypassing the scope-aware DataAccess
 * pipeline and its fallback to the Default environment - so a sheet defined only under Default
 * (and not the selected Environment) silently resolved to nothing. These tests confirm untagged
 * refs now go through {@link UserDataAccess#getData(String, String)}, same as tagged ones.
 *
 * <p>{@code getDataSheetValue} never catches any {@link DataNotFoundException} - it is a pure
 * resolve-or-throw method. {@code getDatasheet} is the single place that catches
 * {@link TestDataNotFoundException}/{@link GlobalDataNotFoundException} and reports the
 * exception's own detailed message (Env/Scope/Sheet/Field/TestCase), returning {@code ""}. The
 * plain, un-subclassed {@code DataNotFoundException} (the {@code EndOfDataSheet} signal a
 * dynamic Param Loop relies on) is never caught by either method - it always propagates.
 */
public class CommandControlGetDataSheetValueTest {

    private static class TestControl extends CommandControl {

        TestControl() {
            super(null, null, null, null, null, null);
        }

        @Override
        public void execute(String com, int sub) {}

        @Override
        public void executeAction(String action) {}

        @Override
        public Object context() {
            return null;
        }
    }

    private CommandControl newControlWith(UserDataAccess userData) {
        CommandControl control = new TestControl();
        control.userData = userData;
        control.Report = mock(TestCaseReport.class);
        return control;
    }

    @Test
    public void untaggedReferenceDelegatesToUserDataAccessGetData() {
        UserDataAccess userData = mock(UserDataAccess.class);
        when(userData.getData("DataScope", "PVal")).thenReturn("PROJVAL");
        CommandControl control = newControlWith(userData);

        assertThat(control.getDataSheetValue("DataScope:PVal")).isEqualTo("PROJVAL");
        assertThat(control.getDataSheetValue("{DataScope:PVal}")).isEqualTo("PROJVAL");
        verify(userData, org.mockito.Mockito.times(2)).getData("DataScope", "PVal");
    }

    @Test
    public void taggedReferencesStillDelegateToUserDataAccessGetData() {
        UserDataAccess userData = mock(UserDataAccess.class);
        when(userData.getData("Basic@Project", "URL")).thenReturn("PROJVAL");
        when(userData.getData("TestData0@Shared", "Data1")).thenReturn("SHRVAL");
        CommandControl control = newControlWith(userData);

        assertThat(control.getDataSheetValue("{Basic:URL@Project}")).isEqualTo("PROJVAL");
        assertThat(control.getDataSheetValue("{TestData0:Data1@Shared}")).isEqualTo("SHRVAL");
    }

    @Test
    public void malformedReferenceReturnsNullWithoutCallingUserDataAccess() {
        UserDataAccess userData = mock(UserDataAccess.class);
        CommandControl control = newControlWith(userData);

        assertThat(control.getDataSheetValue("NotAReference")).isNull();
        verify(userData, org.mockito.Mockito.never())
            .getData(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
            );
    }

    @Test
    public void endOfDataSheetPropagatesSoTheOwningParamLoopCanDetectIt() {
        UserDataAccess userData = mock(UserDataAccess.class);
        DataNotFoundException endOfData = new DataNotFoundException(
            "Reached the end of data sheet."
        );
        endOfData.cause = endOfData.new CauseInfo(Cause.EndOfDataSheet, "end of data");
        when(userData.getData("PL_SC001", "Data2")).thenThrow(endOfData);
        CommandControl control = newControlWith(userData);

        assertThatThrownBy(() -> control.getDataSheetValue("{PL_SC001:Data2}")).isSameAs(endOfData);
        assertThatThrownBy(() -> control.getDatasheet("{PL_SC001:Data2}")).isSameAs(endOfData);
    }

    @Test
    public void getDataSheetValuePropagatesTestDataNotFound() {
        UserDataAccess userData = mock(UserDataAccess.class);
        TestDataNotFoundException notFound = new TestDataNotFoundException(
            mock(TestCaseRunner.class),
            "PL_SC001",
            "Data2",
            Cause.Data,
            "missing field"
        );
        when(userData.getData("PL_SC001", "Data2")).thenThrow(notFound);
        CommandControl control = newControlWith(userData);

        assertThatThrownBy(() -> control.getDataSheetValue("{PL_SC001:Data2}")).isSameAs(notFound);
    }

    @Test
    public void getDatasheetReportsDetailedMessageOnTestDataNotFound() {
        UserDataAccess userData = mock(UserDataAccess.class);
        TestDataNotFoundException notFound = new TestDataNotFoundException(
            mock(TestCaseRunner.class),
            "PL_SC001",
            "Data2",
            Cause.Data,
            "missing field"
        );
        when(userData.getData("PL_SC001", "Data2")).thenThrow(notFound);
        CommandControl control = newControlWith(userData);

        assertThat(control.getDatasheet("{PL_SC001:Data2}")).isEqualTo("");
        verify(control.Report).updateTestLog("Get Datasheet", notFound.toString(), Status.WARNING);
    }

    @Test
    public void getDatasheetReportsDetailedMessageOnGlobalDataNotFound() {
        UserDataAccess userData = mock(UserDataAccess.class);
        GlobalDataNotFoundException notFound = new GlobalDataNotFoundException(
            mock(TestCaseRunner.class),
            "#gid",
            "Field"
        );
        when(userData.getData("PL_SC001", "Data2")).thenThrow(notFound);
        CommandControl control = newControlWith(userData);

        assertThat(control.getDatasheet("{PL_SC001:Data2}")).isEqualTo("");
        verify(control.Report).updateTestLog("Get Datasheet", notFound.toString(), Status.WARNING);
    }
}
