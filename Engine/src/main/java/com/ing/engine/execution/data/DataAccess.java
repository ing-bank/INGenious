
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
    public static String getData(TestCaseRunner context, String sheet, String field, String iter, String subIter)
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
    public static void putData(TestCaseRunner context, String sheet, String field, String newVal, String iter,
            String subIter) throws DataNotFoundException {
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
    public static String getData(TestCaseRunner context, String sheet, String field, String scn, String tc,
            String iter, String subIter) throws DataNotFoundException {
        Object val = null;
        TestDataModel env;
        TestDataModel def = getDefModel(context, sheet);
        if (validEnv(context)) {
            env = getModel(context, sheet);
            val = getDataFromModel(env, field, scn, tc, iter, subIter);
        }
        if (val == null) {
            val = getDataFromModel(def, field, scn, tc, iter, subIter);
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
    public static void putData(TestCaseRunner context, String sheet, String field, String newVal, String scn,
            String tc, String iter, String subIter) throws DataNotFoundException {
        boolean updated = false;
        TestDataModel def = getDefModel(context, sheet);
        if (validEnv(context)) {
            updated = putDataToModel(getModel(context, sheet), field, newVal, scn, tc, iter, subIter);
        }
        if (!updated) {
            updated = putDataToModel(def, field, newVal, scn, tc, iter, subIter);
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
            env = context.executor().dataProvider().getTestDataFor(context.executor().runEnv()).getGlobalData();
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
    public static void putGlobalData(TestCaseRunner context, String gid, String field, String value)
            throws DataNotFoundException {
        GlobalDataModel env = context.executor().dataProvider().defData().getGlobalData();
        if (validEnv(context)) {
            env = context.executor().dataProvider().getTestDataFor(context.executor().runEnv()).getGlobalData();
        } else if (isNull(env)) {
            throw new GlobalDataNotFoundException(context, gid, field);
        }
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
    private static Object getData(TestCaseRunner context, TestDataModel env, TestDataModel def, String field,
            String iter, String subIter) {
        if (notNull(env) && env.hasColumn(field)) {
            Object val = getDataFromModel(env, field, context.getRoot().scenario(), context.getRoot().testcase(), iter,
                    subIter);
            if (val == null) {
                val = getDataFromModel(def, field, context.getRoot().scenario(), context.getRoot().testcase(), iter,
                        subIter);
                if (val == null) {
                    val = getDataFromModel(env, field, context.scenario(), context.testcase(), iter, subIter);
                    if (val == null) {
                        val = getDataFromModel(def, field, context.scenario(), context.testcase(), iter, subIter);
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
    private static boolean putData(TestCaseRunner context, TestDataModel env, TestDataModel def, String field,
            String newVal, String iter, String subIter) {
        if (notNull(env) && env.hasColumn(field)) {
            return putDataToModel(env, def, field, newVal, context.getRoot().scenario(), context.getRoot().testcase(),
                    iter, subIter)
                    || putDataToModel(env, def, field, newVal, context.scenario(), context.testcase(), iter, subIter);
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
    private static Object getData(TestCaseRunner context, TestDataModel def, String field, String iter,
            String subIter) {
        if (notNull(def) && def.hasColumn(field)) {
            return Objects.toString(
                    getDataFromModel(def, field, context.getRoot().scenario(), context.getRoot().testcase(), iter,
                            subIter),
                    getDataFromModel(def, field, context.scenario(), context.testcase(), iter, subIter));
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
    private static boolean putData(TestCaseRunner context, TestDataModel model, String field, String newVal,
            String iter, String subIter) {
        return notNull(model) && model.hasColumn(field)
                && putDataToModel(model, field, newVal, context.getRoot().scenario(), context.getRoot().testcase(),
                        iter, subIter)
                || putDataToModel(model, field, newVal, context.scenario(), context.testcase(), iter, subIter);
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
