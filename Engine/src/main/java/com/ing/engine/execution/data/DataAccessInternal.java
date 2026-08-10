package com.ing.engine.execution.data;

import com.ing.datalib.component.ReusableRef;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.data.DataNotFoundException.Cause;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class DataAccessInternal {

    /**
     * if the environment in the context is valid get iterations from
     * environment else get it from default environment
     *
     * @param context the context(environment,testcase and reusable) which the
     * iterations required
     * @param sheet the test datasheet name
     * @return the iterations
     */
    public static Set<String> getIterations(TestCaseRunner context, String sheet) {
        String scopeContext = getScopeContextString(context);
        LOG.fine(
            "Fetching iterations for sheet '" +
            sheet +
            "'" +
            scopeContext +
            " in " +
            context.scenario() +
            ":" +
            context.testcase()
        );

        if (validEnv(context)) {
            return getIter(context, getModel(context, sheet), getDefModel(context, sheet));
        } else {
            return getIter(context, getDefModel(context, sheet));
        }
    }

    /**
     * if the environment in the context is valid get sub iterations from
     * environment else get it from default environment
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the sub iterations required
     * @param sheet the test datasheet name
     * @return the sub iterations
     */
    public static Set<String> getSubIterations(TestCaseRunner context, String sheet) {
        if (validEnv(context)) {
            return getSubIter(context, getModel(context, sheet), getDefModel(context, sheet));
        } else {
            return getSubIter(context, getDefModel(context, sheet));
        }
    }

    /**
     * Get data from test data model with scope filtering.
     * This method is used for reusable components to ensure they only access
     * data rows that match their scope ([Project], [Shared], or empty for test plan).
     *
     * @param model the test data model
     * @param field the field/column name
     * @param scn the scenario name
     * @param tc the testcase name
     * @param iter the iteration number
     * @param subIter the sub-iteration number
     * @param scope the scope filter ([Project], [Shared], or empty)
     * @return the field value or null if not found
     */
    protected static String getDataFromModelWithScope(
        TestDataModel model,
        String field,
        String scn,
        String tc,
        String iter,
        String subIter,
        String scope
    ) {
        try {
            if (notNull(model)) {
                return model
                    .view()
                    .withSubIterAndScope(scn, tc, iter, subIter, scope)
                    .getField(field);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Update test data in the model with scope filtering, so a same-named scenario/testcase
     * in another reusable scope is never matched/overwritten by mistake.
     */
    protected static boolean putDataToModel(
        TestDataModel model,
        String field,
        String newVal,
        String scn,
        String tc,
        String iter,
        String subIter,
        String scope
    ) {
        try {
            if (
                notNull(model) &&
                model
                    .view()
                    .withSubIterAndScope(scn, tc, iter, subIter, scope, true)
                    .update(field, newVal)
            ) {
                model.saveChanges();
                return true;
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
        return false;
    }

    private static final Logger LOG = Logger.getLogger(DataAccessInternal.class.getName());

    protected static boolean putDataToModel(
        TestDataModel env,
        TestDataModel def,
        String field,
        String newVal,
        String scn,
        String tc,
        String iter,
        String subIter,
        String scope
    ) {
        return (
            putDataToModel(env, field, newVal, scn, tc, iter, subIter, scope) ||
            putDataToModel(def, field, newVal, scn, tc, iter, subIter, scope)
        );
    }

    /**
     * resolves global data
     *
     * @param env the data model for execution environment
     * @param def the data model for default environment
     * @param gid global data id
     * @param field the column/field name
     * @return the data value
     */
    protected static Object getGlobal(
        GlobalDataModel env,
        GlobalDataModel def,
        String gid,
        String field
    ) {
        Object val = getGlobal(env, gid, field);
        if (isNull(val)) {
            val = getGlobal(def, gid, field);
        }
        return val;
    }

    /**
     * resolves global model
     *
     * @param model
     * @param gid global model id
     * @param field the column/field name
     * @return the model value
     */
    protected static Object getGlobal(GlobalDataModel model, String gid, String field) {
        if (notNull(model) && model.hasColumn(field)) {
            return model.view().withScenarioOrGID(gid).getField(field);
        }
        return null;
    }

    /**
     * iterations for env check for Testcase+env else Testcase+default
     * environment if not available check for reusable+env else Reusable+default
     * environment
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * @param def default data model
     * @param env environment data model
     * @return the iteration set
     */
    protected static Set<String> getIter(
        TestCaseRunner context,
        TestDataModel env,
        TestDataModel def
    ) {
        Set<String> val = getIterForRootTestCase(env, context, def);
        if (isNullOrEmpty(val)) {
            val = getIterForReusable(env, context, def);
        }
        return val;
    }

    private static Set<String> getIterForRootTestCase(
        TestDataModel env,
        TestCaseRunner context,
        TestDataModel def
    ) {
        Set<String> val = null;
        // The root is normally the Test Plan entry (empty scope), but when a reusable is
        // run standalone (no parent Execute step) it IS its own root, so this must honor
        // its resolved scope too - otherwise it could match a same-named row from the
        // other reusable scope.
        String rootScope = getScopeFilter(context.getRoot());

        if (notNull(env)) {
            val =
                env
                    .view()
                    .withTestcaseAndScope(
                        context.getRoot().scenario(),
                        context.getRoot().testcase(),
                        rootScope
                    )
                    .getIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val =
                def
                    .view()
                    .withTestcaseAndScope(
                        context.getRoot().scenario(),
                        context.getRoot().testcase(),
                        rootScope
                    )
                    .getIterations();
        }
        return val;
    }

    private static boolean isNullOrEmpty(Set<String> val) {
        return isNull(val) || val.isEmpty();
    }

    private static Set<String> getIterForReusable(
        TestDataModel env,
        TestCaseRunner context,
        TestDataModel def
    ) {
        Set<String> val = null;
        String scopeFilter = getScopeFilter(context);

        if (notNull(env)) {
            val =
                env
                    .view()
                    .withTestcaseAndScope(context.scenario(), context.testcase(), scopeFilter)
                    .getIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val =
                def
                    .view()
                    .withTestcaseAndScope(context.scenario(), context.testcase(), scopeFilter)
                    .getIterations();
        }
        return val;
    }

    /**
     * iterations for default environment check for testcase else get reusable
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * @param def default data model
     * @return iteration set
     */
    protected static Set<String> getIter(TestCaseRunner context, TestDataModel def) {
        if (notNull(def)) {
            // The root is normally the Test Plan entry (empty scope), but when a reusable is
            // run standalone (no parent Execute step) it IS its own root, so this must honor
            // its resolved scope too.
            String rootScope = getScopeFilter(context.getRoot());
            String scopeFilter = getScopeFilter(context);

            // Get root testcase view with null-safety check
            TestDataView rootTestcaseView = def
                .view()
                .withTestcaseAndScope(
                    context.getRoot().scenario(),
                    context.getRoot().testcase(),
                    rootScope
                );

            Set<String> val = null;
            if (notNull(rootTestcaseView)) {
                val = rootTestcaseView.getIterations();
            }

            if (isNullOrEmpty(val)) {
                // Get reusable testcase view with null-safety check
                TestDataView reusableTestcaseView = def
                    .view()
                    .withTestcaseAndScope(context.scenario(), context.testcase(), scopeFilter);

                if (notNull(reusableTestcaseView)) {
                    val = reusableTestcaseView.getIterations();
                }
            }
            return val;
        }
        return null;
    }

    /**
     * sub iterations for env check for Testcase+env else Testcase+default
     * environment if not available check for reusable+env else Reusable+default
     * environment
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * @param def default data model
     * @param env environment data model
     * @return the sub iteration set
     */
    protected static Set<String> getSubIter(
        TestCaseRunner context,
        TestDataModel env,
        TestDataModel def
    ) {
        Set<String> val = getSubIterForRootTestCase(env, context, def);
        if (isNullOrEmpty(val)) {
            val = getSubIterForReusable(env, context, def);
        }
        return val;
    }

    private static Set<String> getSubIterForRootTestCase(
        TestDataModel env,
        TestCaseRunner context,
        TestDataModel def
    ) {
        Set<String> val = null;
        // The root is normally the Test Plan entry (empty scope), but when a reusable is
        // run standalone (no parent Execute step) it IS its own root, so this must honor
        // its resolved scope too.
        String rootScope = getScopeFilter(context.getRoot());
        if (notNull(env)) {
            val =
                env
                    .view()
                    .withIterAndScope(
                        context.getRoot().scenario(),
                        context.getRoot().testcase(),
                        context.iteration(),
                        rootScope
                    )
                    .getSubIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val =
                def
                    .view()
                    .withIterAndScope(
                        context.getRoot().scenario(),
                        context.getRoot().testcase(),
                        context.iteration(),
                        rootScope
                    )
                    .getSubIterations();
        }
        return val;
    }

    private static Set<String> getSubIterForReusable(
        TestDataModel env,
        TestCaseRunner context,
        TestDataModel def
    ) {
        Set<String> val = null;
        String scopeFilter = getScopeFilter(context);
        if (notNull(env)) {
            val =
                env
                    .view()
                    .withIterAndScope(
                        context.scenario(),
                        context.testcase(),
                        context.iteration(),
                        scopeFilter
                    )
                    .getSubIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val =
                def
                    .view()
                    .withIterAndScope(
                        context.scenario(),
                        context.testcase(),
                        context.iteration(),
                        scopeFilter
                    )
                    .getSubIterations();
        }
        return val;
    }

    /**
     * sub iterations for default environment check for testcase else get
     * reusable
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * @param def default data model
     * @return the sub iteration set
     */
    protected static Set<String> getSubIter(TestCaseRunner context, TestDataModel def) {
        if (notNull(def)) {
            // The root is normally the Test Plan entry (empty scope), but when a reusable is
            // run standalone (no parent Execute step) it IS its own root, so this must honor
            // its resolved scope too.
            Set<String> val = def
                .view()
                .withIterAndScope(
                    context.getRoot().scenario(),
                    context.getRoot().testcase(),
                    context.iteration(),
                    getScopeFilter(context.getRoot())
                )
                .getSubIterations();
            if (isNullOrEmpty(val)) {
                val =
                    def
                        .view()
                        .withIterAndScope(
                            context.scenario(),
                            context.testcase(),
                            context.iteration(),
                            getScopeFilter(context)
                        )
                        .getSubIterations();
            }
            return val;
        }
        return null;
    }

    /**
     * find the cause for the data issue, one of
     *
     * missing iteration missing sub-iteration missing field
     *
     * @param context running context
     * @param sheet data-sheet name
     * @param field field name
     * @param subIter sub-iteration no
     * @throws TestDataNotFoundException detailed exception with cause
     */
    protected static void throwErrorWithCause(
        TestCaseRunner context,
        String sheet,
        String field,
        String subIter
    )
        throws TestDataNotFoundException, DataNotFoundException {
        Set<String> iterSet = getIterations(context, sheet);
        ReusableRef.Scope scope = context.getResolvedReusableScope();

        if (isNull(iterSet) || !iterSet.contains(context.iteration())) {
            // Iteration not found - enhanced with scope context
            String scopedField = buildScopeAwareErrorMessage(
                context.iteration(),
                scope,
                sheet,
                field
            );
            TestDataNotFoundException ex = new TestDataNotFoundException(
                context,
                sheet,
                field,
                Cause.Iteration,
                scopedField
            );
            if (scope != null) {
                LOG.warning(
                    "Iteration not found for [" +
                    scope +
                    "] reusable: sheet='" +
                    sheet +
                    "', iteration='" +
                    context.iteration() +
                    "'"
                );
            }
            throw ex;
        } else {
            Set<String> subIterSet = getSubIterations(context, sheet);
            if (isNull(subIterSet) || !subIterSet.contains(subIter)) {
                // End of data sheet reached
                String errorMsg = "Reached the end of data sheet.";
                if (scope != null) {
                    errorMsg = "[" + scope + "] " + errorMsg;
                    LOG.warning(
                        "End of data sheet for [" + scope + "] reusable: sheet='" + sheet + "'"
                    );
                }
                DataNotFoundException dnfe = new DataNotFoundException(errorMsg);
                DataNotFoundException.CauseInfo causeInfo =
                    dnfe.new CauseInfo(Cause.EndOfDataSheet, errorMsg);
                dnfe.cause = causeInfo;
                throw dnfe;
            } else {
                // Data field not found
                String scopedField = buildScopeAwareErrorMessage(field, scope, sheet, field);
                TestDataNotFoundException ex = new TestDataNotFoundException(
                    context,
                    sheet,
                    field,
                    Cause.Data,
                    scopedField
                );
                if (scope != null) {
                    LOG.warning(
                        "Data not found for [" +
                        scope +
                        "] reusable: sheet='" +
                        sheet +
                        "', field='" +
                        field +
                        "'"
                    );
                }
                throw ex;
            }
        }
    }

    protected static TestDataModel getModel(TestCaseRunner context, String sheet) {
        return context
            .executor()
            .dataProvider()
            .getTestDataFor(context.executor().runEnv())
            .getByName(sheet);
    }

    protected static TestDataModel getDefModel(TestCaseRunner context, String sheet) {
        return context.executor().dataProvider().defData().getByName(sheet);
    }

    protected static boolean validEnv(TestCaseRunner context) {
        return (
            !context.executor().dataProvider().defEnv().equals(context.executor().runEnv()) &&
            context
                .executor()
                .dataProvider()
                .getEnvironments()
                .contains(context.executor().runEnv())
        );
    }

    public static boolean notNull(Object ins) {
        return ins != null;
    }

    public static boolean isNull(Object ins) {
        return ins == null;
    }

    /**
     * Builds a scoped error message for data access errors.
     * Prepends scope information for reusable references.
     *
     * @param info the base error info (iteration, field name, etc.)
     * @param scope the resolved reusable scope (PROJECT, SHARED, or null)
     * @param sheet the datasheet name (for context)
     * @param field the field/column name (for context)
     * @return error message info, prefixed with scope if applicable
     */
    protected static String buildScopeAwareErrorMessage(
        String info,
        ReusableRef.Scope scope,
        String sheet,
        String field
    ) {
        if (scope == null) {
            return info;
        }

        // Return info prefixed with scope for error reporting
        return "[" + scope + "] " + info;
    }

    /**
     * Gets the scope context string for logging and diagnostics.
     *
     * @param context the test case execution context
     * @return scope context string e.g. " (scope: [Shared])" or empty string for non-reusables
     */
    protected static String getScopeContextString(TestCaseRunner context) {
        ReusableRef.Scope scope = context.getResolvedReusableScope();
        return scope != null ? " (scope: [" + scope + "])" : "";
    }

    /**
     * Converts the ReusableRef.Scope to a scope filter string for test data filtering.
     *
     * @param context the test case execution context
     * @return scope filter string ([Project], [Shared], or empty for test plan/unscoped)
     */
    protected static String getScopeFilter(TestCaseRunner context) {
        ReusableRef.Scope scope = context.getResolvedReusableScope();
        if (scope == null) {
            return ""; // Empty scope for test plan scenarios
        }
        switch (scope) {
            case PROJECT:
                return "[Project]";
            case SHARED:
                return "[Shared]";
            case UNSCOPED:
            default:
                return ""; // Empty for backward compatibility
        }
    }

    /**
     * Resolves the scope filter to use for an explicitly-named scenario/testcase (as opposed
     * to the ones implied by {@code context}/{@code context.getRoot()}) - e.g. a user command
     * reading another sheet's data by name via {@code UserDataAccess}. If the given name pair
     * matches the root or the current context, that context's own resolved scope is used;
     * otherwise there is no scope information to go on, so it falls back to unscoped (matching
     * prior behavior for references to some other scenario/testcase entirely).
     */
    protected static String getScopeFilter(TestCaseRunner context, String scn, String tc) {
        if (scn.equals(context.getRoot().scenario()) && tc.equals(context.getRoot().testcase())) {
            return getScopeFilter(context.getRoot());
        }
        if (scn.equals(context.scenario()) && tc.equals(context.testcase())) {
            return getScopeFilter(context);
        }
        return "";
    }
}
