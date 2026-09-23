package com.ing.engine.commands.general;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.UserDataAccess;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.ingenious.api.status.Status;
import java.lang.reflect.Field;
import java.util.HashMap;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Tests for {@link GeneralOperations#assertNumericValue()}. */
public class GeneralOperationsActionTest {
    @Mock
    private CommandControl cc;

    @Mock
    private TestCaseReport report;

    @Mock
    private UserDataAccess userData;

    private GeneralOperations generalOps;
    private AutoCloseable mocks;
    private HashMap<String, String> runtimeVars;

    @BeforeMethod
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        runtimeVars = new HashMap<>();

        generalOps = mock(GeneralOperations.class, CALLS_REAL_METHODS);
        setField(generalOps, Command.class, "Report", report);
        setField(generalOps, Command.class, "userData", userData);
        setField(generalOps, Command.class, "Action", "assertNumericValue");

        doAnswer(inv -> runtimeVars.get(inv.getArgument(0))).when(generalOps).getVar(anyString());
        runtimeVars.put("%errorCount%", "5");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    private void setField(Object obj, Class<?> clazz, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private void setCommandFields(String data, String condition) throws Exception {
        setField(generalOps, Command.class, "Data", data);
        setField(generalOps, Command.class, "Condition", condition);
    }

    @Test
    public void notEqualsConditionPasses() throws Exception {
        setCommandFields("<>0", "%errorCount%");
        generalOps.assertNumericValue();
        verify(report)
            .updateTestLog(
                eq("assertNumericValue"),
                contains("satisfies condition"),
                eq(Status.PASSNS)
            );
    }

    @Test
    public void notEqualsConditionFails() throws Exception {
        runtimeVars.put("%errorCount%", "0");
        setCommandFields("<>0", "%errorCount%");
        generalOps.assertNumericValue();
        verify(report)
            .updateTestLog(
                eq("assertNumericValue"),
                contains("does not satisfy condition"),
                eq(Status.FAILNS)
            );
    }

    @Test
    public void greaterThanOrEqualConditionPasses() throws Exception {
        runtimeVars.put("%errorCount%", "100");
        setCommandFields(">=100", "%errorCount%");
        generalOps.assertNumericValue();
        verify(report).updateTestLog(eq("assertNumericValue"), anyString(), eq(Status.PASSNS));
    }

    @Test
    public void nonNumericVariableFailsGracefully() throws Exception {
        runtimeVars.put("%loginResult%", "Connection Error");
        setCommandFields(">0", "%loginResult%");
        generalOps.assertNumericValue();
        verify(report)
            .updateTestLog(eq("assertNumericValue"), contains("is not numeric"), eq(Status.FAILNS));
    }
}
