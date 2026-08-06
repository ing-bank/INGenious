package com.ing.engine.execution.run;

import static java.lang.String.format;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.data.DataProcessor;
import com.ing.engine.execution.data.Parameter;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.UnKnownError;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.policy.ObjectDependencyPolicyViolationException;
import com.ing.engine.execution.policy.ObjectReferenceAnalyzer;
import com.ing.engine.execution.resolver.ExecutionResolver;
import com.ing.engine.execution.resolver.ScopedExecutionResolver;
import com.ing.engine.support.Step;
import com.ing.engine.support.reflect.MethodExecutor;
import com.ing.ingenious.api.exception.ForcedException;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.status.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestStepRunner {
    private static final Logger LOG = Logger.getLogger(TestStepRunner.class.getName());

    private final TestStep testStep;
    private final Parameter parameter;
    private Step step;

    private boolean releasedByNextStep;

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
                checkForDebug(context);
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

    private void checkForDebug(TestCaseRunner context) {
        releasedByNextStep = false;

        if (context.isDebugSuppressedForReusableStepOver()) {
            return;
        }

        SystemDefaults.nextStepflag.set(true);
        SystemDefaults.pauseExecution.set(
            getStep().hasBreakPoint() || SystemDefaults.pauseExecution.get()
        );

        while (
            SystemDefaults.pauseExecution.get() &&
            SystemDefaults.nextStepflag.get() &&
            !SystemDefaults.stopExecution.get()
        ) {
            SystemDefaults.pollWait();
        }

        releasedByNextStep =
            SystemDefaults.pauseExecution.get() &&
            !SystemDefaults.nextStepflag.get() &&
            !SystemDefaults.stopExecution.get();
    }

    private int getSubIterationFromInput(TestCaseRunner context) {
        if (!getStep().getInput().isEmpty()) {
            try {
                return Integer.valueOf(
                    DataProcessor.resolve(
                        getStep().getInput(),
                        context,
                        String.valueOf(parameter.getSubIteration())
                    )
                );
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
     * testcase context using scope-aware resolver with fallback behavior.
     *
     * Behavior:
     * - Scoped references ([Project]/[Shared]): resolve only in declared scope
     * - Unscoped (legacy): project-first fallback to shared
     * - Cross-project: block project-scope references from other projects
     *
     * @param context - current testcase context to run the reusable
     * @throws DataNotFoundException, ForcedException
     */
    private void execute(TestCaseRunner context) throws DataNotFoundException, ForcedException {
        if (getStep().isReusableStep()) {
            String refString = getStep().getAction();
            ReusableRef parsedRef;
            try {
                parsedRef = getStep().getEffectiveReusableRef();
            } catch (IllegalArgumentException ex) {
                throw new ForcedException(
                    String.format(
                        "invalid reusable [%s], expected format [scenario:reusable] or [scope] scenario:reusable",
                        getStep().getAction()
                    )
                );
            }
            if (parsedRef == null) {
                throw new ForcedException(
                    String.format(
                        "invalid reusable [%s], expected format [scenario:reusable] or [scope] scenario:reusable",
                        getStep().getAction()
                    )
                );
            }

            String effectiveRef = parsedRef.format();

            // Use scoped resolver for deterministic fallback behavior
            ExecutionResolver resolver = new ScopedExecutionResolver(context.project());
            ExecutionResolver.ResolutionResult result = resolver.resolve(
                effectiveRef,
                context.project().getName()
            );

            if (result.isSuccess()) {
                Scenario scn = result.getResolvedScenario();
                ReusableRef.Scope resolvedScope = result.getResolvedScope();
                String testcaseName = parsedRef.getTestCaseName();

                TestCase stc = scn.getTestCaseByName(testcaseName);
                if (stc != null) {
                    stc.setParentTestCase(context.getTestCase());

                    // Phase 4: Validate object references against policy before execution
                    validateObjectReferencesForPolicy(context, resolvedScope);

                    // Scope is set on the new reusableRunner only (see executeTestCase) - NOT
                    // on `context`. `context` is the CALLING runner, which may itself be the
                    // true root (TestCaseRunner.getRoot() walks the parent chain via this same
                    // `context` link). Mutating context's scope here - even temporarily - is
                    // visible through getRoot() to every data lookup made by the nested
                    // reusable while it runs, corrupting the parent identity's own scope
                    // (e.g. a Test Plan root becoming "[Project]"-scoped) for the whole nested
                    // call, which breaks parent-data resolution for nested/looped/multi-level
                    // reusables.
                    executeTestCase(context, stc, resolvedScope);
                    return;
                } else {
                    throw new ForcedException(
                        String.format(
                            "reusable testcase [%s/%s] not found in [%s] reusables",
                            scn.getName(),
                            testcaseName,
                            resolvedScope
                        )
                    );
                }
            } else {
                // Resolution failed - include scope information in error
                throw new ForcedException(
                    String.format(
                        "Failed to resolve reusable reference [%s]: %s",
                        effectiveRef,
                        result.getErrorMessage()
                    )
                );
            }
        }
        throw new ForcedException(
            String.format(
                "invalid reusable [%s], expected format [scenario:reusable] or [scope] scenario:reusable",
                getStep().getAction()
            )
        );
    }

    private void executeTestCase(
        TestCaseRunner context,
        TestCase stc,
        ReusableRef.Scope resolvedScope
    )
        throws DataNotFoundException {
        try {
            parameter.setSubIteration(getSubIterationFromInput(context));
            context
                .getReport()
                .startComponent(getStep().getAction(), getStep().getDescription(), resolvedScope);

            // The reusable's own steps execute through this runner, so preserve both
            // the resolved scope and the reusable step-over debug state.
            TestCaseRunner reusableRunner = new TestCaseRunner(context, stc, parameter);
            reusableRunner.setResolvedReusableScope(resolvedScope);
            reusableRunner.setSuppressDebugForReusableStepOver(releasedByNextStep);
            reusableRunner.run();
        } finally {
            context.getReport().endComponent(getStep().getAction());
        }
    }

    private void executeStep(TestCaseRunner context)
        throws DataNotFoundException, DriverClosedException {
        try {
            Annotation ann = new Annotation(context.getControl());
            ann.beforeStepExecution();
            executeStep(context, step, parameter);
            ann.afterStepExecution();
        } catch (
            DataNotFoundException | DriverClosedException | ForcedException | ElementException ex
        ) {
            throw ex;
        } catch (Throwable ex) {
            throw new UnKnownError(ex);
        }
    }

    private void executeStep(TestCaseRunner context, Step step, Parameter parameter)
        throws Throwable {
        step.printStep();
        if (step.ObjectName.equals("String Operations")) {
            executeStringOperation(context);
        } else {
            context.getControl().sync(step, String.valueOf(parameter.getSubIteration()));
        }
        executeAction(context, step.Action);
    }

    public void executeAction(TestCaseRunner context, String action) throws Throwable {
        if (!MethodExecutor.executeMethod(action, context.getControl())) {
            System.out.println("[ERROR][Could not find Action:" + action + "]");
            context.getReport().updateTestLog(action, "[Could not find Action]", Status.DEBUG);
        }
    }

    private void executeStringOperation(TestCaseRunner context) {
        List<String> concatList = context.getControl().smartCommaSplitter(getStep().getInput());
        List<String> result = new ArrayList();
        for (String part : concatList) {
            if (part.matches("%.*%")) result.add(
                "'" + context.getControl().getVar(part) + "'"
            ); else if (part.matches("^\\{.*:.*\\}")) result.add(
                "'" + context.getControl().getDatasheet(part) + "'"
            ); else if (part.matches("\".*\"")) result.add(
                "'" + part.substring(1, part.length() - 1) + "'"
            );
        }
        step.Data = String.join(",", result);
        context.getControl().sync(step);
    }

    /**
     * Phase 4: Validate object references in the current test step against policy constraints.
     *
     * If the step is part of a scoped reusable component, this method:
     * 1. Extracts all object references from the test step (action, input, reference fields)
     * 2. Validates each reference against the object dependency policy
     * 3. Throws ObjectDependencyPolicyViolationException if any violations found
     *
     * Violations occur when:
     * - A SHARED reusable references a PROJECT-scoped object
     *
     * Allowed:
     * - PROJECT reusable can reference PROJECT or SHARED objects
     * - SHARED reusable can only reference SHARED objects
     *
     * @param context - current testcase execution context
     * @param reusableScope - scope of the reusable being executed (PROJECT or SHARED)
     * @throws ObjectDependencyPolicyViolationException if policy violation detected
     */
    private void validateObjectReferencesForPolicy(
        TestCaseRunner context,
        ReusableRef.Scope reusableScope
    ) {
        try {
            ObjectReferenceAnalyzer.ValidationReport report = ObjectReferenceAnalyzer.analyzeStepObjectReferences(
                testStep,
                reusableScope,
                context.project()
            );

            // Throw exception if violations exist (analyzeStepObjectReferences handles this internally)
            report.throwIfViolationsExist();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Policy validation error for step: " + testStep.getAction(), ex);
            // Re-throw as-is if it's already a policy violation exception
            if (ex.getClass().getName().contains("ObjectDependencyPolicyViolationException")) {
                throw (RuntimeException) ex;
            }
            // For other exceptions, log but don't fail - validation may not be fully available
        }
    }
}
