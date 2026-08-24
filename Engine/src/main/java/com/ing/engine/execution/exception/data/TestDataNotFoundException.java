package com.ing.engine.execution.exception.data;

import com.ing.engine.execution.run.TestCaseRunner;

/**
 *
 *
 */
@SuppressWarnings("serial")
public class TestDataNotFoundException extends DataNotFoundException {
    public String sheet;

    public TestDataNotFoundException(
        TestCaseRunner context,
        String sheet,
        String field,
        Cause c,
        String info
    ) {
        super(String.format("Test Data Not Found, %s - %s is missing.", c.name(), info));
        this.context = context;
        this.field = field;
        this.sheet = sheet;
        this.cause = new CauseInfo(c, info);
    }

    @Override
    public String toString() {
        try {
            return getFormatted(
                getTemplate(context.isReusable()),
                getMessage(),
                context.executor().runEnv(),
                scopeLabel(),
                sheet,
                field,
                context.getRoot().scenario(),
                context.getRoot().testcase(),
                context.scenario(),
                context.testcase()
            );
        } catch (Exception ex) {
            return super.toString();
        }
    }

    public static String getTemplate(Boolean isReusable) {
        return (
            "{0} \n[Env : {1} | Scope : {2} | Sheet : {3} | Field : {4} | TestCase : {5}/{6}" +
            (isReusable ? " | Reusable : {7}/{8} ]" : " ]")
        );
    }

    /**
     * "Shared" or "Project" - which Test Data location this sheet reference was resolved
     * against, based on the [Shared]/[Project] scope tag the sheet reference carries (no tag
     * defaults to Project). Included in every message so it's always clear where the lookup
     * happened, not just when the sheet couldn't be found anywhere at all.
     */
    private String scopeLabel() {
        String s = sheet == null ? "" : sheet.trim();
        return s.startsWith("[Shared]") ? "Shared" : "Project";
    }
}
