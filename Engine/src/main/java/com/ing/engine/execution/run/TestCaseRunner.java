package com.ing.engine.execution.run;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.DataIterator;
import com.ing.engine.execution.data.Parameter;
import com.ing.engine.execution.data.StepSet;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.execution.exception.TestFailedException;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.execution.exception.AppiumDriverException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.data.GlobalDataNotFoundException;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.exception.element.ElementException;
import com.ing.engine.reporting.TestCaseReport;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * ,394173
 */
public class TestCaseRunner {

    private static final Logger LOG = Logger.getLogger(TestCaseRunner.class.getName());

    private TestCase testcase;
    private String scenario, testCase;

    private final Stack<StepSet> stepStack = new Stack<>();

    private final Parameter parameter;
    private final TestRunner exe;
    private DataIterator iterater;
    private final Map<String, Object> varMap = new HashMap<>();
    private int iter = -1;

    private TestCaseRunner context;
    private CommandControl control;

    private int currentSubIteration = -1;

    //<editor-fold defaultstate="collapsed" desc="_init_">
    public TestCaseRunner(ProjectRunner exe, String scenario, String testCase) {
        this(exe, null, null, new Parameter());
        iterater = exe.getIterater(scenario, testCase);
        this.scenario = scenario;
        this.testCase = testCase;
    }

    public TestCaseRunner(ProjectRunner exe, TestCase testcase) {
        this(exe, null, testcase, new Parameter());
        iterater = exe.getIterater(testcase);
    }

    public TestCaseRunner(TestCaseRunner parent, TestCase testcase) {
        this(parent.exe, parent, testcase, new Parameter());
    }

    public TestCaseRunner(TestCaseRunner parent, TestCase testcase,
            Parameter parameter) {
        this(parent.exe, parent, testcase, parameter);
    }

    private TestCaseRunner(TestRunner exe, TestCaseRunner parent, TestCase testcase,
            Parameter parameter) {
        this.exe = exe;
        this.context = parent;
        this.testcase = testcase;
        this.parameter = parameter;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="dependent apis">
    public void setMaxIter(int n) {
        if (n > 0) {
            iter = n;
        }
    }

    public TestCaseRunner getRoot() {
        if (context == null) {
            return this;
        } else {
            return context.getRoot();
        }
    }

    public ProjectRunner executor() {
        return (ProjectRunner) this.exe;
    }

    public Project project() {
        return executor().getProject();
    }

    public String scenario() {
        if (testcase != null) {
            return testcase.getScenario().getName();
        } else {
            return scenario;
        }
    }

    public String testcase() {
        if (testcase != null) {
            return testcase.getName();
        } else {
            return testCase;
        }
    }

    public String iteration() {
        return String.valueOf(parameter.getIteration());
    }

    public String subIteration() {
        return String.valueOf(parameter.getSubIteration());
    }

    public Parameter getParameter() {
        return parameter;
    }

    public TestCaseReport getReport() {
        return (TestCaseReport) control.Report;
    }

    public CommandControl getControl() {
        syncControl();
        return control;
    }

    private void syncControl() {
        if (this != getRoot()) {
            syncRunTimeVars();
        }
    }

    /**
     * sync runtime variable created in each context with its root
     */
    private void syncRunTimeVars() {
        getRoot().getControl().getRunTimeVars()
                .putAll(control.getRunTimeVars());
        control.getRunTimeVars()
                .putAll(getRoot().getControl().getRunTimeVars());
    }

    public CommandControl createControl(final TestCaseRunner newThis) {
        return new CommandControl(getRoot().getControl().Playwright, getRoot().getControl().Page, getRoot().getControl().BrowserContext, getRoot().getControl().webDriver, getRoot().getControl().Report) {
            @Override
            public void execute(String com, int sub) {
                newThis.runTestCase(com, sub);
            }

            @Override
            public void executeAction(String action) {
                newThis.runAction(action);
            }

            @Override
            public Object context() {
                return newThis;
            }
        };
    }

    public boolean isReusable() {
        return context != null;
    }

    public TestCase getTestCase() {
        return testcase;
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="internal apis">
    private boolean canRunStep(int currStep) {
        return currStep < testcase.getTestSteps().size() && canRun();
    }

    private boolean canRun() {
        return !SystemDefaults.stopExecution.get() && !SystemDefaults.stopCurrentIteration.get();
    }

    private void setControl(CommandControl cc) {
        this.control = cc;
    }

    private void checkForStartLoop(TestStep testStep, int currStep) {
        if (Parameter.startParamRLoop(testStep.getCondition())) {
            if (stepStack.isEmpty() || stepStack.peek().from != currStep) {
                stepStack.push(new StepSet(currStep));
                stepStack.peek().isLoop = Parameter.isLoop(testStep.getCondition());
            }
        }
    }

    private int checkForEndLoop(TestStep testStep, int currStep) {
        if (Parameter.endParamRLoop(testStep.getCondition())) {
            if (!stepStack.isEmpty()) {
                if (stepStack.peek().to != currStep) {
                    stepStack.peek().to = currStep;
                    stepStack.peek().setTimes(this.resolveNoOfTimes(testStep));
                }
                if (stepStack.peek().getTimes() == 0) {
                    stepStack.pop();
                } else {
                    currStep = stepStack.peek().from - 1;
                    stepStack.peek().next();
                }
            }
        }
        return currStep;
    }

    private int resolveNoOfTimes(TestStep testStep) {
        String condition = testStep.getCondition();
        if (condition.matches("End Loop(:@[0-9]+)?")) {
            String val = condition.replace("End Loop:@", "");
            if (val.matches("[0-9]+")) {
                return Integer.valueOf(val);
            }
        }
        if (condition.matches("End Param:@[0-9]+")) {
            return Integer.valueOf(condition.replaceAll("End Param:@", ""));
        } else {
            return -1;
        }
    }

    private Parameter resolveParam() {
        Parameter param = new Parameter();
        param.setIteration(this.parameter.getIteration());
        currentSubIteration = this.parameter.getSubIteration();
        if (!stepStack.isEmpty()) {
            if (!stepStack.peek().isLoop) {
                currentSubIteration = stepStack.peek().current();
            }
        }
        param.setSubIteration(currentSubIteration);
        return param;
    }

    public String getCurrentSubIteration() {
        return String.valueOf(currentSubIteration);
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="error handling">
    private void onError(Throwable ex) {
        if (!ex.getMessage().contains("ActionException"))
          reportOnError(getStepName(), ex.getMessage(), Status.DEBUG);
        if (exe.isContinueOnError()) {
            LOG.log(Level.SEVERE, ex.getMessage(), Optional.ofNullable(ex.getCause()).orElse(ex));
        } else {
            if (ex instanceof RuntimeException) {
                throw new TestFailedException(scenario(), testcase(), ex);
            }
            throw new UnCaughtException(ex);
        }
    }

    private void onRuntimeException(RuntimeException ex) {
        reportOnError(getStepName(), ex.getMessage(), Status.FAIL);
        if (exe.isContinueOnError()) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } else {
            throw new TestFailedException(scenario(), testcase(), ex);
        }
    }

    private void onPlaywrightException(RuntimeException ex) {
        if (exe.isContinueOnError()) {
        } else {
            throw new TestFailedException(scenario(), testcase(), ex);
        }
    }

    private String getStepName() {
        return Objects.nonNull(getControl().Action) ? getControl().Action : "Error";
    }

    private void onDataNotFoundException(DataNotFoundException ex) throws TestFailedException {
        if (ex instanceof TestDataNotFoundException) {
            if (ex.cause.isIter()) {
                reportOnError("DataNotFound", ex.toString(), Status.DEBUG);
                throw new TestFailedException(scenario(), testcase(), ex);
            } else if (!this.stepStack.isEmpty() && !this.stepStack.peek().isSubIterDynamic) {
                System.out.println(ex.toString() + ", Breaking subIteration!!");
                reportOnError("DataNotFound", ex.toString(), Status.DEBUG);
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            } else {
                /**
                 * its a dynamic sub-iteration(number of sub-iterations is not
                 * known in script) and at the end of data so break from it
                 */
                System.out.println("Breaking subIteration, End Of Input!!");
            }

        } else if (ex instanceof GlobalDataNotFoundException) {
            reportOnError("DataNotFound", ex.toString(), Status.DEBUG);
            throw new TestFailedException(scenario(), testcase(), ex);
        }
    }

    private void reportOnError(String err, String desc, Status status) {
        Optional.ofNullable(getReport()).ifPresent(
                (report) -> report.updateTestLog(err, desc, status));
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="run">
    public void run(CommandControl cc, int iter)
            throws DriverClosedException, TestFailedException {
        parameter.setIteration(iter);
        setControl(cc);
        if (testcase != null) {
            testcase.loadTableModel();
            /*
            * caution: breaking the loop will stop the iteration
             */
            for (int currStep = 0; canRunStep(currStep); currStep++) {
                TestStep testStep = testcase.getTestSteps().get(currStep);
                if (!testStep.isCommented()) {
                    checkForStartLoop(testStep, currStep);
                    try {
                        runStep(testStep);
                    } catch (DriverClosedException | TestFailedException | UnCaughtException ex) {
                        throw ex;
                    } catch (DataNotFoundException ex) {
                        onDataNotFoundException(ex);
                        currStep = breakSubIteration();
                        if (currStep >= 0) {
                            /**
                             * break out of sub-iteration and continue the
                             * execution
                             */
                            continue;
                        } else {
                            /**
                             * error while breaking the execution
                             */
                            throw new TestFailedException(scenario(), testcase(), ex);
                        }
                    } catch (ForcedException | ElementException ex) {
                        onRuntimeException(ex);
                    } catch (ActionException ex) {
                        onPlaywrightException(ex);
                    } catch (Throwable ex) {
                        onError(ex);
                    }
                    currStep = checkForEndLoop(testStep, currStep);
                }
            }
        }
    }

    /**
     *
     * @return step after current sub-Iteration
     */
    private int breakSubIteration() {
        if (!stepStack.isEmpty()) {
            return stepStack.pop().to;
        }
        return -1;
    }

    public void run() throws DataNotFoundException, DriverClosedException {
        run(createControl(this), parameter.getIteration());
    }

    public void run(CommandControl cc) throws DataNotFoundException, DriverClosedException {
        run(cc, parameter.getIteration());
    }

    private void runStep(TestStep testStep) throws DataNotFoundException, DriverClosedException, Throwable {
        new TestStepRunner(testStep, resolveParam()).run(this);
    }

    public void runStep(Step step, int subIter) throws DataNotFoundException, DriverClosedException {
        Parameter param = new Parameter();
        param.setIteration(this.parameter.getIteration());
        param.setSubIteration(subIter);
        new TestStepRunner(step.toTestStep(), param).run(this);
    }

    public void runTestCase(String com, int sub) {
        Step newStep = Step.create(0, sub, this);
        newStep.execute(com).run();
    }

    public void runAction(String action) {
        try {
            new TestStepRunner().executeAction(this, action);
        } catch (Throwable ex) {
            reportOnError(action, ex.getMessage(), Status.FAIL);
            throw new RuntimeException("Error executing " + action);
        }
    }
//</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="iteration & sub iteration">
    public boolean isIterResolved(String sheet) {
        if (this == getRoot()) {
            return iter > 0 || iterater.isIterResolved(sheet);
        } else {
            return getRoot().isIterResolved(sheet);
        }
    }

    public void setIter(String sheet, Set<String> iter) {
        getRootIterator().setIter(sheet, iter);
    }

    public Integer getMaxIter() {
        if (this == getRoot()) {
            return iter > 0 ? iter : iterater.getMaxIter();
        } else {
            return getRoot().getMaxIter();
        }
    }

    public boolean isRoot() {
        return this == getRoot();
    }

    public DataIterator getRootIterator() {
        if (this.isRoot()) {
            return this.iterater;
        } else {
            return this.getRoot().getRootIterator();
        }
    }

//</editor-fold>
    @Override
    public String toString() {
        return String.format("[%s:%s] [%s] [%s]", testcase.getScenario(), testcase,
                parameter, getRoot().iterater);
    }

    public Map<String, Object> getVarMap() {
        return varMap;
    }
}
