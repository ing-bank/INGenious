package com.ing.engine.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ing.engine.execution.data.UserDataAccess;
import org.testng.annotations.Test;

/**
 * Regression tests for {@link CommandControl#getDataSheetValue(String)}.
 *
 * <p>Untagged {@code Sheet:Column} references used to be resolved by a hand-rolled scan of the
 * currently selected Environment's sheet names only, bypassing the scope-aware DataAccess
 * pipeline and its fallback to the Default environment - so a sheet defined only under Default
 * (and not the selected Environment) silently resolved to nothing. These tests confirm untagged
 * refs now go through {@link UserDataAccess#getData(String, String)}, same as tagged ones.
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
}
