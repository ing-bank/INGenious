
package com.ing.engine.execution.data;

import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
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
        if (validEnv(context)) {
            return getIter(context, getModel(context, sheet),
                    getDefModel(context, sheet));
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

    protected static String getDataFromModel(TestDataModel model, String field,
            String scn, String tc, String iter, String subIter) {
        try {
            if (notNull(model)) {
                return model.view().withSubIter(scn, tc, iter, subIter)
                        .getField(field);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }

    protected static boolean putDataToModel(TestDataModel model, String field, String newVal,
            String scn, String tc, String iter, String subIter) {
        try {
            if (notNull(model) && model.view().withSubIter(scn, tc, iter, subIter, true)
                    .update(field, newVal)) {
                model.saveChanges();
                return true;
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex.getMessage(), ex);
        }
        return false;
    }
    private static final Logger LOG = Logger.getLogger(DataAccessInternal.class.getName());

    protected static boolean putDataToModel(TestDataModel env, TestDataModel def, String field, String newVal,
            String scn, String tc, String iter, String subIter) {
        return putDataToModel(env, field, newVal, scn, tc, iter, subIter)
                || putDataToModel(def, field, newVal, scn, tc, iter, subIter);
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
    protected static Object getGlobal(GlobalDataModel env, GlobalDataModel def,
            String gid, String field) {
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
    protected static Set<String> getIter(TestCaseRunner context,
            TestDataModel env, TestDataModel def) {
        Set<String> val = getIterForRootTestCase(env, context, def);
        if (isNullOrEmpty(val)) {
            val = getIterForReusable(env, context, def);
        }
        return val;
    }

    private static Set<String> getIterForRootTestCase(TestDataModel env,
            TestCaseRunner context, TestDataModel def) {
        Set<String> val = null;
        if (notNull(env)) {
            val = env.view().withTestcase(context.getRoot().scenario(),
                    context.getRoot().testcase()).getIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val = def.view().withTestcase(context.getRoot().scenario(),
                    context.getRoot().testcase()).getIterations();
        }
        return val;
    }

    private static boolean isNullOrEmpty(Set<String> val) {
        return isNull(val) || val.isEmpty();
    }

    private static Set<String> getIterForReusable(TestDataModel env,
            TestCaseRunner context, TestDataModel def) {
        Set<String> val = null;
        if (notNull(env)) {
            val = env.view().withTestcase(context.scenario(),
                    context.testcase()).getIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val = def.view().withTestcase(context.scenario(),
                    context.testcase()).getIterations();
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
            Set<String> val = def.view().withTestcase(context.getRoot().scenario(),
                    context.getRoot().testcase()).getIterations();
            if (isNullOrEmpty(val)) {
                val = def.view().withTestcase(context.scenario(), context.testcase()).getIterations();
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
    protected static Set<String> getSubIter(TestCaseRunner context, TestDataModel env, TestDataModel def) {
        Set<String> val = getSubIterForRootTestCase(env, context, def);
        if (isNullOrEmpty(val)) {
            val = getSubIterForReusable(env, context, def);
        }
        return val;
    }

    private static Set<String> getSubIterForRootTestCase(TestDataModel env, TestCaseRunner context, TestDataModel def) {
        Set<String> val = null;
        if (notNull(env)) {
            val = env.view().withIter(context.getRoot().scenario(), context.getRoot().testcase(), context.iteration())
                    .getSubIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val = def.view().withIter(context.getRoot().scenario(), context.getRoot().testcase(), context.iteration())
                    .getSubIterations();
        }
        return val;
    }

    private static Set<String> getSubIterForReusable(TestDataModel env, TestCaseRunner context, TestDataModel def) {
        Set<String> val = null;
        if (notNull(env)) {
            val = env.view().withIter(context.scenario(), context.testcase(),
                    context.iteration()).getSubIterations();
        }
        if (isNullOrEmpty(val) && notNull(def)) {
            val = def.view().withIter(context.scenario(), context.testcase(),
                    context.iteration()).getSubIterations();
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
            Set<String> val = def.view()
                    .withIter(context.getRoot().scenario(), context.getRoot().testcase(), context.iteration())
                    .getSubIterations();
            if (isNullOrEmpty(val)) {
                val = def.view()
                        .withIter(context.scenario(), context.testcase(), context.iteration())
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
    protected static void throwErrorWithCause(TestCaseRunner context,
            String sheet, String field, String subIter) throws TestDataNotFoundException {
        Set<String> iterSet = getIterations(context, sheet);
        if (isNull(iterSet) || !iterSet.contains(context.iteration())) {
            throw new TestDataNotFoundException(context, sheet, field, Cause.Iteration, context.iteration());
        } else {
            Set<String> subIterSet = getSubIterations(context, sheet);
            if (isNull(subIterSet) || !subIterSet.contains(subIter)) {
                throw new TestDataNotFoundException(context, sheet, field, Cause.SubIteration,
                        String.format("%s:%s", context.iteration(), subIter));
            } else {
                throw new TestDataNotFoundException(context, sheet, field, Cause.Data, field);
            }
        }
    }

    protected static TestDataModel getModel(TestCaseRunner context, String sheet) {
        return context.executor().dataProvider().getTestDataFor(
                context.executor().runEnv()).getByName(sheet);
    }

    protected static TestDataModel getDefModel(TestCaseRunner context, String sheet) {
        return context.executor().dataProvider().defData().getByName(sheet);
    }

    protected static boolean validEnv(TestCaseRunner context) {
        return !context.executor().dataProvider().defEnv().equals(context.executor().runEnv())
                && context.executor().dataProvider().getEnvironments().contains(context.executor().runEnv());
    }

    public static boolean notNull(Object ins) {
        return ins != null;
    }

    public static boolean isNull(Object ins) {
        return ins == null;
    }
}
