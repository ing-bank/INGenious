package com.ing.engine.execution.exception.data;

import com.ing.engine.execution.run.TestCaseRunner;

/**
 *
 *
 */
@SuppressWarnings("serial")
public class GlobalDataNotFoundException extends DataNotFoundException {
    public String gid;

    public GlobalDataNotFoundException(TestCaseRunner context, String gid, String field) {
        super("Global Data Not Found..");
        this.context = context;
        this.field = field;
        this.gid = gid;
        // Required: Task.runIteration() unconditionally calls ex.cause.isEndData() on any
        // caught DataNotFoundException, so cause must never be left null here.
        this.cause = new CauseInfo(Cause.Data, gid);
    }

    @Override
    public String toString() {
        try {
            return getFormatted(
                getTemplate(context.isReusable()),
                getMessage(),
                envLabel(),
                field,
                gid,
                context.getRoot().scenario(),
                context.getRoot().testcase(),
                context.scenario(),
                context.testcase()
            );
        } catch (Exception ex) {
            return super.toString();
        }
    }

    /**
     * The environment the lookup used - {@code sharedRunEnv()} for a {@code [Shared]} GID,
     * {@code runEnv()} otherwise.
     */
    private Object envLabel() {
        String g = gid == null ? "" : gid.trim();
        return g.startsWith("[Shared]")
            ? context.executor().sharedRunEnv()
            : context.executor().runEnv();
    }

    public static String getTemplate(Boolean isReusable) {
        return (
            "{0} \n[Env : {1} | Field : {2} | GID : {3} | TestCase : {4}/{5}" +
            (isReusable ? " | Reusable : {6}/{7} ]" : " ]")
        );
    }
}
