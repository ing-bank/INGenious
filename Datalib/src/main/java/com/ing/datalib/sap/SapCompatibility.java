package com.ing.datalib.sap;

import com.ing.datalib.component.TestStep;
import com.ing.ingenious.api.types.ObjectType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared compatibility matrix for mixing archetypes into a SAP test case (design doc:
 * "Guardrails - what may share a SAP test case"). One source of truth consulted both at
 * design time (IDE warning marker) and run time (engine fail-fast) so the two never drift.
 *
 * <p>Everything not listed here already works today (driverless actions, Database, Webservice,
 * Browser/Playwright/Web, Image) - only the archetypes needing a second live device, broker or
 * remote driver of their own are blocked.
 */
public final class SapCompatibility {

    private SapCompatibility() {}

    /** Object types that cannot share a test case with SAP - each needs its own live device, broker or remote driver. */
    public static final Set<String> BLOCKED_WITH_SAP = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                ObjectType.MOBILE,
                ObjectType.APP,
                ObjectType.KAFKA,
                ObjectType.QUEUE,
                ObjectType.PROTRACTORJS
            )
        )
    );

    public static boolean isSapObjectType(String objectType) {
        return ObjectType.SAP.equals(objectType) || ObjectType.SAP_OBJECT.equals(objectType);
    }

    public static boolean isBlockedWithSap(String objectType) {
        return objectType != null && BLOCKED_WITH_SAP.contains(objectType);
    }

    /** Whether any step in the list is SAP - used to scope the design-time warning and the Grid launch guard to SAP test cases only. */
    public static boolean hasSapStep(List<TestStep> steps) {
        if (steps == null) {
            return false;
        }
        for (TestStep step : steps) {
            if (step != null && isSapObjectType(step.getObject())) {
                return true;
            }
        }
        return false;
    }

    /** Human-readable reason + fix, shared by the design-time tooltip and the runtime failure message. */
    public static String blockedReasonMessage(String objectType) {
        String why;
        if (ObjectType.MOBILE.equals(objectType) || ObjectType.APP.equals(objectType)) {
            why = "needs a real device or emulator and an Appium session";
        } else if (ObjectType.KAFKA.equals(objectType)) {
            why = "needs a live Kafka broker connection";
        } else if (ObjectType.QUEUE.equals(objectType)) {
            why = "needs a live queue broker connection";
        } else if (ObjectType.PROTRACTORJS.equals(objectType)) {
            why = "runs in a separate ProtractorJS process";
        } else {
            why = "needs its own live device, broker or remote driver";
        }
        return (
            "\"" +
            objectType +
            "\" cannot run in a SAP test case - it " +
            why +
            ". Move this step to its own test case."
        );
    }
}
