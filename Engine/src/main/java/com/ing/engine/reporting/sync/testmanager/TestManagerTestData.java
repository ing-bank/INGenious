package com.ing.engine.reporting.sync.testmanager;

import java.io.File;
import java.util.List;

/** Buffered result row queued by {@link TestManagerSync#updateResults}. */
public class TestManagerTestData {
    final String projectId;
    final String testPlanId;
    final String suite;
    final String testcase;
    final String status;
    final List<File> attach;

    public TestManagerTestData(
        String projectId,
        String testPlanId,
        String suite,
        String testcase,
        String status,
        List<File> attach
    ) {
        this.projectId = projectId;
        this.testPlanId = testPlanId;
        this.suite = suite;
        this.testcase = testcase;
        this.status = status;
        this.attach = attach;
    }
}
