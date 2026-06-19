package com.ing.engine.drivers;

import com.ing.engine.core.CommandControl;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class for testing SAP automation without requiring actual SAP software.
 * Provides utilities to initialize mock SAP sessions for unit/integration testing.
 *
 * <p>Usage in tests:
 * <pre>
 * // Setup mock SAP session
 * SAPSessionCreation mockSession = SAPTestHelper.createMockSAPSession("TestSAP");
 * // CommandControl now has SAPsession != null, enabling SAP mode
 * // But SAPObject.getSapObject() will return null for non-existent objects
 * // causing non-SAP actions to skip SAP object finding
 * </pre>
 */
public class SAPTestHelper {

    /**
     * Creates a mock SAP session for testing without real SAP software.
     * The mock session allows testing action routing logic in SAP mode
     * without requiring SAP COM components to be installed.
     *
     * <p>This is useful for testing that non-SAP actions (General, Database,
     * StringOperations, etc.) execute correctly within a SAP test case context.</p>
     *
     * <p>How it works:
     * <ol>
     *   <li>Creates a SAPSessionCreation with non-null parent but null session.session</li>
     *   <li>CommandControl recognizes SAPsession != null → SAP mode</li>
     *   <li>CommandControl doesn't create SAPObject (session.session is null)</li>
     *   <li>For any action: isSAPAction() returns false (SAPObject is null)</li>
     *   <li>All actions skip SAP object finding and execute normally</li>
     * </ol>
     *
     * @param sessionName the name to assign to the mock session
     * @return a mocked SAPSessionCreation object with non-null parent but null internal session
     */
    public static SAPSessionCreation createMockSAPSession(String sessionName) {
        SAPSessionCreation mockSession = new SAPSessionCreation();

        try {
            RunContext context = new RunContext();
            context.BrowserName = sessionName;
            context.BrowserVersion = "mock";

            mockSession.runContext = context;
            mockSession.session = null; // Keep null - SAPObject won't be created

            System.out.println("[TEST-MOCK] SAP Session initialized: " + sessionName);
            System.out.println("[TEST-MOCK] Mode: SAPsession != null (SAP mode active)");
            System.out.println("[TEST-MOCK] SAPObject: null (object finding disabled)");
            System.out.println("[TEST-MOCK] Result: All actions skip SAP object finding");

            return mockSession;
        } catch (Exception ex) {
            Logger
                .getLogger(SAPTestHelper.class.getName())
                .log(Level.WARNING, "Failed to create mock SAP session", ex);
            return null;
        }
    }

    /**
     * Creates a dummy Dispatch-like object for mock SAP testing.
     * This returns an Object that satisfies null checks but doesn't do real COM calls.
     * (Currently not used since we keep session null)
     */
    private static Object createDummySession() {
        return new Object() {

            @Override
            public String toString() {
                return "[MockSAPSession]";
            }
        };
    }
}
