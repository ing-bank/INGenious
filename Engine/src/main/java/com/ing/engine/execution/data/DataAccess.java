package com.ing.engine.execution.data;

import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.exception.data.GlobalDataNotFoundException;
import com.ing.engine.execution.exception.data.TestDataNotFoundException;
import com.ing.engine.execution.run.TestCaseRunner;
import java.util.Objects;

/**
 * Provides unified interface to access execution data for the framework engine.
 * <br>
 * It handles use of testcase / reusable data , iterations and sub iterations,
 * environment based test data and exceptions and its causes <br>
 * Exceptions <br>
 * null {@link TestDataNotFoundException} {@link GlobalDataNotFoundException}
 * <br>
 *
 *
 *
 *
 */
public class DataAccess extends DataAccessInternal {

    /**
     * if the environment in the context is valid get data from environment else
     * get it from default environment
     *
     * if the data not found throws {@link TestDataNotFoundException} with the
     * proper cause.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param sheet data sheet name
     * @param field the field name
     * @param iter the iteration
     * @param subIter the sub iteration for the data
     * @return the test data
     * @throws DataNotFoundException if the data not present
     *
     */
    public static String getData(
        TestCaseRunner context,
        String sheet,
        String field,
        String iter,
        String subIter
    )
        throws DataNotFoundException {
        Object val;
        TestDataModel env;
        TestDataModel def = getDefModel(context, sheet);
        if (validEnv(context)) {
            env = getModel(context, sheet);
            val = getData(context, env, def, field, iter, subIter);
        } else {
            val = getData(context, def, field, iter, subIter);
        }
        if (val == null) {
            throwErrorWithCause(context, sheet, field, subIter);
        }
        return DataProcessor.resolve(val, context, field);
    }

    /**
     * Get the test data for the next iteration from the test data sheet
     * as specified sheet data that mathces the provided field, iteration and subiteration.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param sheet data sheet name
     * @param field the field name
     * @param iter the iteration
     * @param subIter the sub iteration for the data
     * @return - the test data
     * @throws DataNotFoundException if the data not present
     */
    public static String getNextData(
        TestCaseRunner context,
        String scn,
        String tc,
        String sheet,
        String field,
        String iter,
        String subIter
    )
        throws DataNotFoundException {
        String nextSubIteration = (Integer.parseInt(subIter) + 1) + "";
        String val = null;

        // The root is normally the Test Plan entry (empty scope), but when a reusable is run
        // standalone (no parent Execute step) it IS its own root, so it must be filtered by
        // its own resolved scope, not an assumed-empty one - otherwise Param Loop end-of-data
        // detection could match a same-named row from the other reusable scope.
        boolean isQueryingRoot =
            scn.equals(context.getRoot().scenario()) && tc.equals(context.getRoot().testcase());
        String scopeFilter = isQueryingRoot
            ? getScopeFilter(context.getRoot())
            : getScopeFilter(context);

        TestDataModel env;
        TestDataModel def = getDefModel(context, sheet);
        if (validEnv(context)) {
            env = getModel(context, sheet);
            val =
                getDataFromModelWithScope(env, field, scn, tc, iter, nextSubIteration, scopeFilter);
        } else {
            val =
                getDataFromModelWithScope(def, field, scn, tc, iter, nextSubIteration, scopeFilter);
        }
        return val;
    }

    /**
     * if the environment in the context is valid, then update data to
     * environment else update to default environment
     *
     * if the data not found throws {@link TestDataNotFoundException} with the
     * proper cause.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param sheet data sheet name
     * @param field the field name
     * @param newVal the new value to update
     * @param iter the iteration
     * @param subIter the sub iteration for the data
     * @throws DataNotFoundException if the data not present
     *
     */
    public static void putData(
        TestCaseRunner context,
        String sheet,
        String field,
        String newVal,
        String iter,
        String subIter
    )
        throws DataNotFoundException {
        Boolean updated;
        TestDataModel env;
        TestDataModel def = getDefModel(context, sheet);
        if (validEnv(context)) {
            env = getModel(context, sheet);
            updated = putData(context, env, def, field, newVal, iter, subIter);
        } else {
            updated = putData(context, def, field, newVal, iter, subIter);
        }
        if (!updated) {
            throwErrorWithCause(context, sheet, field, subIter);
        }
    }

    /**
     * if the environment in the context is valid get data from environment else
     * get it from default environment
     *
     * if the data not found throws {@link TestDataNotFoundException} with the
     * proper cause.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param sheet data sheet name
     * @param field the field name
     * @param scn the scenario name
     * @param tc the test case name
     * @param iter the iteration
     * @param subIter the sub iteration for the data
     * @return the test data
     * @throws DataNotFoundException if the data not present
     *
     */
    public static String getData(
        TestCaseRunner context,
        String sheet,
        String field,
        String scn,
        String tc,
        String iter,
        String subIter
    )
        throws DataNotFoundException {
        Object val = null;
        TestDataModel env;
        TestDataModel def = getDefModel(context, sheet);
        String scope = getScopeFilter(context, scn, tc);
        if (validEnv(context)) {
            env = getModel(context, sheet);
            val = getDataFromModelWithScope(env, field, scn, tc, iter, subIter, scope);
        }
        if (val == null) {
            val = getDataFromModelWithScope(def, field, scn, tc, iter, subIter, scope);
        }
        if (val == null) {
            throwErrorWithCause(context, sheet, field, subIter);
        }
        return DataProcessor.resolve(val, context, field);
    }

    /**
     * if the environment in the context is valid, then update data to
     * environment else update to default environment
     *
     * if the data not found throws {@link TestDataNotFoundException} with the
     * proper cause.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param sheet data sheet name
     * @param field the field name
     * @param newVal the new value to update
     * @param scn the scenario name
     * @param tc the test case name
     * @param iter the iteration
     * @param subIter the sub iteration for the data
     * @throws DataNotFoundException if the data not present
     *
     */
    public static void putData(
        TestCaseRunner context,
        String sheet,
        String field,
        String newVal,
        String scn,
        String tc,
        String iter,
        String subIter
    )
        throws DataNotFoundException {
        boolean updated = false;
        TestDataModel def = getDefModel(context, sheet);
        String scope = getScopeFilter(context, scn, tc);
        if (validEnv(context)) {
            updated =
                putDataToModel(
                    getModel(context, sheet),
                    field,
                    newVal,
                    scn,
                    tc,
                    iter,
                    subIter,
                    scope
                );
        }
        if (!updated) {
            updated = putDataToModel(def, field, newVal, scn, tc, iter, subIter, scope);
        }
        if (!updated) {
            throwErrorWithCause(context, sheet, field, subIter);
        }
    }

    /**
     * if the environment in the context is valid get global data from
     * environment else get it from default environment
     *
     * if the data not found throws {@link GlobalDataNotFoundException} with the
     * proper cause.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param gid global data id
     * @param field the field name
     * @return the test data
     * @throws DataNotFoundException if the data not present
     *
     */
    public static String getGlobalData(TestCaseRunner context, String gid, String field)
        throws DataNotFoundException {
        Object val;
        GlobalDataModel env;
        GlobalDataModel def = context.executor().dataProvider().defData().getGlobalData();
        if (validEnv(context)) {
            env =
                context
                    .executor()
                    .dataProvider()
                    .getTestDataFor(context.executor().runEnv())
                    .getGlobalData();
            val = getGlobal(env, def, gid, field);
        } else {
            val = getGlobal(def, gid, field);
        }
        if (val == null) {
            throw new GlobalDataNotFoundException(context, gid, field);
        }
        return DataProcessor.resolve(val, context, field);
    }

    /**
     * if the environment in the context is valid put global data to environment
     * else put it to default environment
     *
     * if the data not found throws {@link GlobalDataNotFoundException} with the
     * proper cause.
     *
     * @param context the context(environment,testcase,reusable and iteration)
     * which the data
     * @param gid global data id
     * @param field the field name
     * @param value the value
     * @throws DataNotFoundException if the data not present
     *
     */
    public static void putGlobalData(
        TestCaseRunner context,
        String gid,
        String field,
        String value
    )
        throws DataNotFoundException {
        GlobalDataModel env = context.executor().dataProvider().defData().getGlobalData();
        if (validEnv(context)) {
            env =
                context
                    .executor()
                    .dataProvider()
                    .getTestDataFor(context.executor().runEnv())
                    .getGlobalData();
        } else if (isNull(env)) {
            throw new GlobalDataNotFoundException(context, gid, field);
        }
        env.load();
        env.setValueAt(value, env.getRecordIndexByKey(gid), env.findColumn(field));
        env.saveChanges();
    }

    /**
     * resolves test data
     *
     * check for Testcase+env else Testcase+default environment if not available
     * check for reusable+env else Reusable+default environment
     *
     * @param env the data model for execution environment
     * @param def the data model for default environment
     * @param gid global data id
     * @param field the column/field name
     * @return the data value
     */
    private static Object getData(
        TestCaseRunner context,
        TestDataModel env,
        TestDataModel def,
        String field,
        String iter,
        String subIter
    ) {
        String scopeFilter = getScopeFilter(context);
        // The "root" test case is normally the Test Plan entry (always empty scope), but
        // when a reusable is executed standalone (no parent Execute step) it IS its own
        // root, so the root lookup must honor its resolved scope too - otherwise it would
        // match a same-named row from the other reusable scope before scope filtering
        // ever gets a chance to apply below.
        String rootScopeFilter = getScopeFilter(context.getRoot());

        if (notNull(env) && env.hasColumn(field)) {
            // Try root test case first
            Object val = getDataFromModelWithScope(
                env,
                field,
                context.getRoot().scenario(),
                context.getRoot().testcase(),
                iter,
                subIter,
                rootScopeFilter
            );
            if (val == null) {
                val =
                    getDataFromModelWithScope(
                        def,
                        field,
                        context.getRoot().scenario(),
                        context.getRoot().testcase(),
                        iter,
                        subIter,
                        rootScopeFilter
                    );
                if (val == null) {
                    // Try reusable data with scope filtering
                    val =
                        getDataFromModelWithScope(
                            env,
                            field,
                            context.scenario(),
                            context.testcase(),
                            iter,
                            subIter,
                            scopeFilter
                        );
                    if (val == null) {
                        val =
                            getDataFromModelWithScope(
                                def,
                                field,
                                context.scenario(),
                                context.testcase(),
                                iter,
                                subIter,
                                scopeFilter
                            );
                    }
                }
            }
            return val;
        } else {
            return getData(context, def, field, iter, subIter);
        }
    }

    /**
     * update test data
     *
     * check for Testcase+env else Testcase+default environment if not available
     * check for reusable+env else Reusable+default environment
     *
     * @param env the data model for execution environment
     * @param def the data model for default environment
     * @param gid global data id
     * @param field the column/field name
     * @return the data value
     */
    private static boolean putData(
        TestCaseRunner context,
        TestDataModel env,
        TestDataModel def,
        String field,
        String newVal,
        String iter,
        String subIter
    ) {
        if (notNull(env) && env.hasColumn(field)) {
            return (
                putDataToModel(
                    env,
                    def,
                    field,
                    newVal,
                    context.getRoot().scenario(),
                    context.getRoot().testcase(),
                    iter,
                    subIter,
                    getScopeFilter(context.getRoot())
                ) ||
                putDataToModel(
                    env,
                    def,
                    field,
                    newVal,
                    context.scenario(),
                    context.testcase(),
                    iter,
                    subIter,
                    getScopeFilter(context)
                )
            );
        } else {
            return putData(context, def, field, newVal, iter, subIter);
        }
    }

    /**
     * resolves test data for default model
     *
     * check for Testcase + env else Reusable+default environment
     *
     * @param def the data model for default environment
     * @param field the column/field name
     * @param subIter the sub iteration value
     * @return the data value
     */
    private static Object getData(
        TestCaseRunner context,
        TestDataModel def,
        String field,
        String iter,
        String subIter
    ) {
        String scopeFilter = getScopeFilter(context);
        String rootScopeFilter = getScopeFilter(context.getRoot());

        if (notNull(def) && def.hasColumn(field)) {
            // Try root test case first
            Object rootVal = getDataFromModelWithScope(
                def,
                field,
                context.getRoot().scenario(),
                context.getRoot().testcase(),
                iter,
                subIter,
                rootScopeFilter
            );
            if (rootVal != null) {
                return rootVal;
            }
            // Try reusable data with scope filtering
            return getDataFromModelWithScope(
                def,
                field,
                context.scenario(),
                context.testcase(),
                iter,
                subIter,
                scopeFilter
            );
        }
        return null;
    }

    /**
     * resolves test data
     *
     * check for Testcase+env else Testcase+default environment if not available
     * check for reusable+env else Reusable+default environment
     *
     * @param env the data model for execution environment
     * @param def the data model for default environment
     * @param gid global data id
     * @param field the column/field name
     * @return the data value
     */
    private static Object getDynamicLoopData(
        TestCaseRunner context,
        TestDataModel env,
        TestDataModel def,
        String field,
        String iter,
        String subIter
    ) {
        String scopeFilter = getScopeFilter(context);
        String rootScopeFilter = getScopeFilter(context.getRoot());

        if (notNull(env) && env.hasColumn(field)) {
            // Try root test case first
            Object val = getDataFromModelWithScope(
                env,
                field,
                context.getRoot().scenario(),
                context.getRoot().testcase(),
                iter,
                subIter,
                rootScopeFilter
            );
            if (val == null) {
                val =
                    getDataFromModelWithScope(
                        def,
                        field,
                        context.getRoot().scenario(),
                        context.getRoot().testcase(),
                        iter,
                        subIter,
                        rootScopeFilter
                    );
                if (val == null) {
                    // Try reusable data with scope filtering
                    val =
                        getDataFromModelWithScope(
                            env,
                            field,
                            context.scenario(),
                            context.testcase(),
                            iter,
                            subIter,
                            scopeFilter
                        );
                    if (val == null) {
                        val =
                            getDataFromModelWithScope(
                                def,
                                field,
                                context.scenario(),
                                context.testcase(),
                                iter,
                                subIter,
                                scopeFilter
                            );
                    }
                }
            }
            return val;
        } else {
            return getDynamicLoopData(context, def, field, iter, subIter);
        }
    }

    /**
     * resolves test data for default model
     *
     * check for Testcase + env else Reusable+default environment
     *
     * @param def the data model for default environment
     * @param field the column/field name
     * @param subIter the sub iteration value
     * @return the data value
     */
    private static Object getDynamicLoopData(
        TestCaseRunner context,
        TestDataModel def,
        String field,
        String iter,
        String subIter
    ) {
        String scopeFilter = getScopeFilter(context);

        if (notNull(def) && def.hasColumn(field)) {
            // Use scope-aware filtering for reusable data
            return getDataFromModelWithScope(
                def,
                field,
                context.scenario(),
                context.testcase(),
                iter,
                subIter,
                scopeFilter
            );
        }
        return null;
    }

    /**
     * update data for default model
     *
     * check for Testcase+env else Reusable+default environment
     *
     * @param model the data model for default environment
     * @param field the column/field name
     * @param subIter the sub iteration value
     * @return the data value
     */
    private static boolean putData(
        TestCaseRunner context,
        TestDataModel model,
        String field,
        String newVal,
        String iter,
        String subIter
    ) {
        return (
            notNull(model) &&
            model.hasColumn(field) &&
            putDataToModel(
                model,
                field,
                newVal,
                context.getRoot().scenario(),
                context.getRoot().testcase(),
                iter,
                subIter,
                getScopeFilter(context.getRoot())
            ) ||
            putDataToModel(
                model,
                field,
                newVal,
                context.scenario(),
                context.testcase(),
                iter,
                subIter,
                getScopeFilter(context)
            )
        );
    }

    public static TestDataView getTestData(TestCaseRunner context, String sheet) {
        TestDataModel env;
        TestDataModel def = getDefModel(context, sheet);
        if (validEnv(context)) {
            env = getModel(context, sheet);
        } else {
            env = def;
        }
        return env != null ? env.view() : null;
    }
}
