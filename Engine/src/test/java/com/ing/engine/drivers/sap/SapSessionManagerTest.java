package com.ing.engine.drivers.sap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.settings.SapConfigRegistry;
import com.ing.datalib.settings.SapConnections;
import com.ing.datalib.settings.SapDefaults;
import com.ing.engine.execution.run.TestCaseRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Claim counting, adopt-vs-launch, and teardown for {@link SapSessionManager}. */
public class SapSessionManagerTest {
    private final SapSessionManager mgr = SapSessionManager.INSTANCE;
    private FakeSap.Locator locator;
    private Path tempDir;
    private SapConnections store;
    private SapDefaults defaults;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("sapmgr-test");
        store = new SapConnections(tempDir.toString());
        store.addSap("QA");
        store.addSap("PROD");
        defaults = new SapDefaults(tempDir.toString());
        defaults.setDefaultConnection("QA");
        SapConfigRegistry registry = new SapConfigRegistry(store, defaults);

        locator = new FakeSap.Locator();
        mgr.setRegistrySupplier(() -> registry);
        mgr.setLocatorFactory(() -> locator);
        mgr.resetForThread();
    }

    @After
    public void tearDown() throws IOException {
        mgr.setRegistrySupplier(null);
        mgr.setLocatorFactory(null);
        mgr.resetForThread();
        Files
            .walk(tempDir)
            .sorted((a, b) -> b.compareTo(a))
            .map(Path::toFile)
            .forEach(f -> f.delete());
    }

    private static TestCaseRunner runner() {
        TestCaseRunner r = mock(TestCaseRunner.class);
        when(r.getRoot()).thenReturn(r);
        return r;
    }

    @Test
    public void blankInputUsesDefault_launchWhenNoRot() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());

        assertTrue(mgr.hasConnection());
        assertEquals("QA", mgr.currentAliasName());
        assertEquals(1, locator.launched.get());
        assertEquals(1, locator.opened.size());
    }

    @Test
    public void engineRunning_noMatchingConnection_opensOwnOnExistingEngine() {
        // Engine is up but nothing open under this name yet: open our own (owned)
        // connection on it - no launch needed, but still fully ours to close.
        locator.rotPresent = true;
        mgr.initConnection("#QA", runner());

        assertEquals(0, locator.launched.get());
        assertEquals(1, locator.opened.size());
        mgr.closeAll(null);
        assertEquals(
            "connection we opened is closed on teardown",
            1,
            locator.opened.get(0).closeCount.get()
        );
    }

    @Test
    public void adoptedConnection_newSessionMode_createsOwnSessionOnly() {
        // sessionMode = newSession (default): a matching connection is already open
        // (someone else's, or left over) -> spawn our own session on it, never touch
        // the connection or its existing session(s).
        locator.rotPresent = true;
        store.getSapPropertiesFor("QA").setProperty("connectionName", "QAS");
        FakeSap.Connection existing = new FakeSap.Connection("QAS");
        locator.existing = existing;

        mgr.initConnection("#QA", runner());

        assertEquals("adopt path never launches", 0, locator.launched.get());
        assertEquals("adopt path never opens a brand-new connection", 0, locator.opened.size());
        assertEquals("a session was created on the shared connection", 2, existing.sessions.size());

        mgr.closeAll(null);
        assertEquals(
            "the pre-existing session is left alone",
            0,
            existing.sessions.get(0).sessionCloseCount.get()
        );
        assertEquals(0, existing.sessions.get(0).closeCount.get());
        assertEquals(
            "only the session we created is ended",
            1,
            existing.sessions.get(1).sessionCloseCount.get()
        );
    }

    @Test
    public void adoptedConnection_shareExisting_neverCreatesOrCloses() {
        locator.rotPresent = true;
        store.getSapPropertiesFor("QA").setProperty("connectionName", "QAS");
        store.getSapPropertiesFor("QA").setProperty("sessionMode", "shareExisting");
        FakeSap.Connection existing = new FakeSap.Connection("QAS");
        locator.existing = existing;

        mgr.initConnection("#QA", runner());

        assertEquals("no new session created", 1, existing.sessions.size());
        mgr.closeAll(null);
        assertEquals(0, existing.sessions.get(0).closeCount.get());
        assertEquals(0, existing.sessions.get(0).sessionCloseCount.get());
    }

    @Test
    public void adoptedConnection_shareExisting_prefersIdleOverBusy() {
        locator.rotPresent = true;
        store.getSapPropertiesFor("QA").setProperty("connectionName", "QAS");
        store.getSapPropertiesFor("QA").setProperty("sessionMode", "shareExisting");
        FakeSap.Connection existing = new FakeSap.Connection("QAS");
        existing.sessions.get(0).setBusy(true);
        FakeSap.Session idle = new FakeSap.Session();
        existing.sessions.add(idle);
        locator.existing = existing;

        SapGuiSession got = mgr.initConnection("#QA", runner());
        assertSame(idle, got);
    }

    @Test(expected = SapConnectionException.class)
    public void adoptedConnection_requireOwn_refusesToTouchIt() {
        locator.rotPresent = true;
        store.getSapPropertiesFor("QA").setProperty("connectionName", "QAS");
        store.getSapPropertiesFor("QA").setProperty("sessionMode", "requireOwn");
        locator.existing = new FakeSap.Connection("QAS");
        mgr.initConnection("#QA", runner());
    }

    @Test
    public void adoptedConnection_newSessionMode_failsFastAtSixSessions() {
        locator.rotPresent = true;
        store.getSapPropertiesFor("QA").setProperty("connectionName", "QAS");
        FakeSap.Connection existing = new FakeSap.Connection("QAS");
        for (int i = 0; i < 5; i++) {
            existing.sessions.add(new FakeSap.Session());
        }
        locator.existing = existing;

        try {
            mgr.initConnection("#QA", runner());
            fail("expected SapConnectionException");
        } catch (SapConnectionException ex) {
            assertTrue(ex.getMessage().contains("6 sessions"));
        }
    }

    @Test
    public void ownedConnectionIsClosedByBackstop() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        mgr.closeAll(null);
        assertEquals(1, locator.opened.get(0).closeCount.get());
        assertFalse(mgr.hasConnection());
    }

    @Test
    public void claimCounting_reusableInitCloseDoesNotDropParentConnection() {
        locator.rotPresent = false;
        TestCaseRunner parent = runner();
        TestCaseRunner reusable = runner();

        mgr.initConnection("", parent); // [parent]
        mgr.initConnection("", reusable); // [parent, reusable] - same alias, no re-open
        assertEquals(1, locator.opened.size());

        mgr.closeConnection("", reusable); // pop reusable -> [parent]
        assertTrue("parent claim keeps it open", mgr.hasConnection());
        assertEquals(0, locator.opened.get(0).closeCount.get());

        mgr.closeConnection("", parent); // pop parent -> [] -> teardown
        assertFalse(mgr.hasConnection());
        assertEquals(1, locator.opened.get(0).closeCount.get());
    }

    @Test
    public void closeWithNoMatchingClaimIsNoOp() {
        locator.rotPresent = false;
        TestCaseRunner owner = runner();
        TestCaseRunner stranger = runner();
        mgr.initConnection("", owner);
        mgr.closeConnection("", stranger); // stranger never init'd -> ignored
        assertTrue(mgr.hasConnection());
        assertEquals(0, locator.opened.get(0).closeCount.get());
    }

    @Test
    public void switchConnectionMovesCurrentPointerOnly() {
        locator.rotPresent = false;
        TestCaseRunner r = runner();
        mgr.initConnection("#QA", r);
        mgr.initConnection("#PROD", r);
        assertEquals("PROD", mgr.currentAliasName());
        mgr.switchConnection("#QA");
        assertEquals("QA", mgr.currentAliasName());
    }

    @Test(expected = SapConnectionException.class)
    public void switchToUnopenedConnectionThrows() {
        mgr.switchConnection("#PROD");
    }

    @Test
    public void scriptingDisabledThrows() {
        locator.rotPresent = true;
        locator.scriptingEnabled = false;
        try {
            mgr.initConnection("#QA", runner());
            fail("expected SapConnectionException");
        } catch (SapConnectionException ex) {
            assertTrue(ex.getMessage().toLowerCase().contains("scripting"));
        }
    }

    @Test
    public void closeAllConnectionInputMapsToDefault() {
        locator.rotPresent = false;
        TestCaseRunner r = runner();
        mgr.initConnection("#QA", r);
        mgr.closeConnection("", r); // "" -> QA (the default)
        assertFalse(mgr.hasConnection());
    }

    @Test
    public void logonScreenFilled_whenPresentAndCredentialsConfigured() {
        locator.rotPresent = false;
        store.getSapPropertiesFor("QA").setProperty("user", "TESTER");
        store.getSapPropertiesFor("QA").setProperty("password", "secret");
        store.getSapPropertiesFor("QA").setProperty("client", "200");
        store.getSapPropertiesFor("QA").setProperty("language", "EN");

        FakeSap.Element userField = new FakeSap.Element();
        FakeSap.Element pwdField = new FakeSap.Element();
        FakeSap.Element clientField = new FakeSap.Element();
        FakeSap.Element langField = new FakeSap.Element();
        FakeSap.Element mainWindow = new FakeSap.Element();
        locator.presetElements.put(SapSessionManager.LOGON_USER_FIELD, userField);
        locator.presetElements.put(SapSessionManager.LOGON_PASSWORD_FIELD, pwdField);
        locator.presetElements.put(SapSessionManager.LOGON_CLIENT_FIELD, clientField);
        locator.presetElements.put(SapSessionManager.LOGON_LANGUAGE_FIELD, langField);
        locator.presetElements.put(SapSessionManager.LOGON_CONFIRM, mainWindow);

        mgr.initConnection("", runner());

        assertEquals("TESTER", userField.properties.get("Text"));
        assertEquals("secret", pwdField.properties.get("Text"));
        assertEquals("200", clientField.properties.get("Text"));
        assertEquals("EN", langField.properties.get("Text"));
        assertTrue(mainWindow.invocations.contains("sendVKey"));
    }

    @Test
    public void logonScreenAbsent_credentialsNeverTouched() {
        // No LOGON_USER_FIELD preset -> not on the logon screen (SSO/SNC already done).
        locator.rotPresent = false;
        store.getSapPropertiesFor("QA").setProperty("user", "TESTER");
        mgr.initConnection("", runner()); // must not throw
        assertTrue(mgr.hasConnection());
    }

    @Test
    public void logonScreenPresent_noCredentials_failsWithSsoHint() {
        locator.rotPresent = false;
        locator.presetElements.put(SapSessionManager.LOGON_USER_FIELD, new FakeSap.Element());
        try {
            mgr.initConnection("", runner());
            fail("expected SapConnectionException");
        } catch (SapConnectionException ex) {
            assertTrue(ex.getMessage().contains("SSO"));
        }
    }

    @Test
    public void multiLogonDialog_autoAnswersKeepOthersByDefault() {
        locator.rotPresent = false;
        FakeSap.Element keepRadio = new FakeSap.Element();
        FakeSap.Element confirmBtn = new FakeSap.Element();
        locator.presetElements.put(SapSessionManager.MULTI_LOGON_KEEP_OTHERS, keepRadio);
        locator.presetElements.put(SapSessionManager.MULTI_LOGON_CONFIRM, confirmBtn);

        mgr.initConnection("", runner());

        assertEquals(Boolean.TRUE, keepRadio.properties.get("Selected"));
        assertTrue(confirmBtn.invocations.contains("press"));
    }

    @Test
    public void multiLogonDialog_endOthersWhenConfigured() {
        locator.rotPresent = false;
        store.getSapPropertiesFor("QA").setProperty("multiLogon", "endOthers");
        FakeSap.Element endRadio = new FakeSap.Element();
        FakeSap.Element confirmBtn = new FakeSap.Element();
        locator.presetElements.put(SapSessionManager.MULTI_LOGON_END_OTHERS, endRadio);
        locator.presetElements.put(SapSessionManager.MULTI_LOGON_CONFIRM, confirmBtn);

        mgr.initConnection("", runner());

        assertEquals(Boolean.TRUE, endRadio.properties.get("Selected"));
        assertTrue(confirmBtn.invocations.contains("press"));
    }

    @Test(expected = SapConnectionException.class)
    public void multiLogonDialog_failModeThrowsInsteadOfAutoAnswering() {
        locator.rotPresent = false;
        store.getSapPropertiesFor("QA").setProperty("multiLogon", "fail");
        locator.presetElements.put(
            SapSessionManager.MULTI_LOGON_KEEP_OTHERS,
            new FakeSap.Element()
        );
        mgr.initConnection("", runner());
    }

    @Test
    public void noMultiLogonDialog_neverTouchesAnything() {
        locator.rotPresent = false;
        mgr.initConnection("", runner()); // no dialog elements preset -> must not throw
        assertTrue(mgr.hasConnection());
    }

    // ---- Phase 4: concurrent sessions ------------------------------------

    @Test
    public void openSession_createsANewSessionAndMakesItCurrent() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        SapGuiSession primary = mgr.current();

        SapGuiSession stock = mgr.openSession("s1");

        assertNotSame(primary, stock);
        assertSame(stock, mgr.current());
        assertEquals("s1", mgr.currentSessionLabel());
    }

    @Test(expected = SapConnectionException.class)
    public void openSession_blankLabelThrows() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        mgr.openSession("  ");
    }

    @Test(expected = SapConnectionException.class)
    public void openSession_noConnectionThrows() {
        mgr.openSession("s1");
    }

    @Test(expected = SapConnectionException.class)
    public void openSession_duplicateLabelThrows() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        mgr.openSession("s1");
        mgr.openSession("s1");
    }

    @Test
    public void openSession_capsAtSixSessionsPerConnection() {
        locator.rotPresent = false;
        mgr.initConnection("", runner()); // 1 session (primary) already open
        mgr.openSession("s1");
        mgr.openSession("s2");
        mgr.openSession("s3");
        mgr.openSession("s4");
        mgr.openSession("s5"); // now at 6

        try {
            mgr.openSession("s6");
            fail("expected the 6-session cap to be enforced");
        } catch (SapConnectionException expected) {
            assertTrue(expected.getMessage().contains("6"));
        }
    }

    @Test
    public void switchSession_blankGoesBackToThePrimarySession() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        SapGuiSession primary = mgr.current();
        mgr.openSession("s1");
        assertSame(mgr.current(), mgr.sessionByLabel("s1"));

        mgr.switchSession("");

        assertSame(primary, mgr.current());
        assertEquals("QA", mgr.currentSessionLabel()); // primary's label = the connection alias
    }

    @Test
    public void switchSession_movesBetweenTwoNamedSessions() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        SapGuiSession s1 = mgr.openSession("s1");
        SapGuiSession s2 = mgr.openSession("s2");
        assertSame(s2, mgr.current());

        mgr.switchSession("s1");

        assertSame(s1, mgr.current());
        assertEquals("s1", mgr.currentSessionLabel());
    }

    @Test(expected = SapConnectionException.class)
    public void switchSession_unknownLabelThrows() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        mgr.switchSession("neverOpened");
    }

    @Test(expected = SapConnectionException.class)
    public void switchSession_noConnectionThrows() {
        mgr.switchSession("s1");
    }

    @Test
    public void closeSession_blankClosesCurrentAndFallsBackToPreviousSession() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        mgr.openSession("s1");
        mgr.openSession("s2"); // current

        mgr.closeSession("");

        assertEquals("s1", mgr.currentSessionLabel());
        assertNull("s2 must no longer be resolvable", mgr.sessionByLabel("s2"));
    }

    @Test(expected = SapConnectionException.class)
    public void closeSession_refusesToCloseTheOnlyRemainingSession() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        mgr.closeSession(""); // only the primary session is open
    }

    @Test
    public void closeSession_ownedSessionEndsOnlyThatSessionNotTheConnection() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        FakeSap.Session stock = (FakeSap.Session) mgr.openSession("s1");

        mgr.closeSession("s1");

        assertEquals(1, stock.sessionCloseCount.get());
        assertEquals(0, stock.closeCount.get());
        assertTrue("the connection itself must still be open", mgr.hasConnection());
    }

    @Test
    public void sessionByLabel_resolvesOpenLabelsAndNullsForUnknownOnes() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        SapGuiSession s1 = mgr.openSession("s1");

        assertSame(s1, mgr.sessionByLabel("s1"));
        assertNull(mgr.sessionByLabel("neverOpened"));
        assertNull(mgr.sessionByLabel(""));
        assertNull(mgr.sessionByLabel(null));
    }

    @Test
    public void closeAll_tearsDownEveryOpenSessionOnAnOwnedConnection() {
        locator.rotPresent = false;
        mgr.initConnection("", runner());
        FakeSap.Session primary = (FakeSap.Session) mgr.current();
        mgr.openSession("s1");

        mgr.closeAll(null);

        // Closing any one session on an owned connection ends the whole connection (and every
        // session on it) - exactly like a real GuiConnection.CloseConnection() would.
        assertEquals(1, primary.closeCount.get());
        assertFalse(mgr.hasConnection());
    }

    @Test
    public void openSession_onAnAdoptedConnection_ownsOnlyTheSessionItCreated() {
        // shareExisting adopts an existing session (not ours) as the primary; openSession still
        // creates and owns a session of our own on top of that shared connection.
        locator.rotPresent = true;
        store.getSapPropertiesFor("QA").setProperty("connectionName", "QAS");
        store.getSapPropertiesFor("QA").setProperty("sessionMode", "shareExisting");
        FakeSap.Connection existing = new FakeSap.Connection("QAS");
        locator.existing = existing;

        mgr.initConnection("", runner());
        FakeSap.Session ours = (FakeSap.Session) mgr.openSession("s1");

        mgr.closeAll(null);

        assertEquals(
            "adopted primary session must never be closed by us",
            0,
            existing.sessions.get(0).closeCount.get() +
            existing.sessions.get(0).sessionCloseCount.get()
        );
        assertEquals(
            "our own session must be ended (not the connection)",
            1,
            ours.sessionCloseCount.get()
        );
        assertEquals(0, ours.closeCount.get());
    }
}
