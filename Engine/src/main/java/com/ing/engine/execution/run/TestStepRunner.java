
package com.ing.engine.execution.run;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.execution.data.DataProcessor;
import com.ing.engine.execution.data.Parameter;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.execution.exception.UnKnownError;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import com.ing.engine.support.reflect.MethodExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.lang.String.format;

public class TestStepRunner {

    private static final Logger LOG = Logger.getLogger(TestStepRunner.class.getName());

    private final TestStep testStep;
    private final Parameter parameter;
    private Step step;

    public TestStepRunner(TestStep testStep, Parameter parameter) {
        this.parameter = parameter;
        this.testStep = testStep;
    }

    public TestStepRunner() {
        this.parameter = null;
        this.testStep = null;
    }

    public void run(TestCaseRunner context) throws DataNotFoundException, DriverClosedException {
        if (this.parameter != null && this.testStep != null) {
            if (context.executor().isDebugExe()) {
                checkForDebug();
            }
            step = new Step(testStep, context);
            context.getReport().updateStepDetails(step);
            switch (getStep().getObject()) {
                case "Execute":
                    execute(context);
                    break;
                default:
                    executeStep(context);
                    break;
            }
        } else {
            throw new RuntimeException("Not enough data to run a step");
        }
    }

    private void checkForDebug() {
        SystemDefaults.nextStepflag.set(true);
        SystemDefaults.pauseExecution.set(getStep().hasBreakPoint()
                || SystemDefaults.pauseExecution.get());
        while (SystemDefaults.pauseExecution.get() && SystemDefaults.nextStepflag.get()
                && !SystemDefaults.stopExecution.get()) {
            SystemDefaults.pollWait();
        }
    }

    private int getSubIterationFromInput(TestCaseRunner context) {
        if (!getStep().getInput().isEmpty()) {
            try {
                return Integer.valueOf(DataProcessor.resolve(getStep().getInput(), context,
                        String.valueOf(parameter.getSubIteration())));
            } catch (Exception ex) {
                System.err.println("Unable to resolve subIteration for reusable!!");
                LOG.log(Level.WARNING, ex.getMessage(), ex);
                return 1;
            }
        }
        return parameter.getSubIteration();
    }

    private TestStep getStep() {
        return testStep;
    }

    /**
     * parse the Execute action to reusable testcase and executes in the current
     * testcase context
     *
     * @param context - current testcase context to run the reusable
     * @throws DataNotFoundException, ForcedException
     */
    private void execute(TestCaseRunner context) throws DataNotFoundException, ForcedException {
        if (getStep().isReusableStep()) {
            String[] rData = getStep().getReusableData();
            String scenario = rData[0];
            String testcase = rData[1];
            Scenario scn = context.project().getScenarioByName(scenario);
            if (scn != null) {
                TestCase stc = scn.getTestCaseByName(testcase);
                if (stc != null) {
                    executeTestCase(context, stc);
                    return;
                } else {
                    throw new ForcedException(format("reusable testcase [//%s/%s] not found",
                            scenario, testcase));
                }
            } else {
                throw new ForcedException(format("scenario [%s] not found", scenario));
            }
        }
        throw new ForcedException(
                format("invalid reusable [%s], expected format [scenario:reusable]",
                        getStep().getAction()));
    }

    private void executeTestCase(TestCaseRunner context, TestCase stc) throws DataNotFoundException {
        try {
            parameter.setSubIteration(getSubIterationFromInput(context));
            context.getReport().startComponent(getStep().getAction(), getStep().getDescription());
            new TestCaseRunner(context, stc, parameter).run();
        } finally {
            context.getReport().endComponent(getStep().getAction());
        }
    }

    private void executeStep(TestCaseRunner context) throws DataNotFoundException, DriverClosedException {
        try {
            Annotation ann = new Annotation(context.getControl());
            ann.beforeStepExecution();
            executeStep(context, step, parameter);
            ann.afterStepExecution();
        } catch (DataNotFoundException | DriverClosedException
                | ForcedException | ElementException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new UnKnownError(ex);
        }
    }

    private void executeStep(TestCaseRunner context, Step step, Parameter parameter)
            throws Throwable {
        step.printStep();
        context.getControl().sync(step, String.valueOf(parameter.getSubIteration()));
        executeAction(context, step.Action);
    }

    public void executeAction(TestCaseRunner context, String action) throws Throwable {
        if (!MethodExecutor.executeMethod(action, context.getControl())) {
            System.out.println("[ERROR][Could not find Action:" + action + "]");
            context.getReport().updateTestLog(action, "[Could not find Action]",
                    Status.DEBUG);
        }
    }

}
