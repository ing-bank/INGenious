package com.ing.engine.drivers.sap;

import com.ing.datalib.settings.SapConfigException;
import com.ing.datalib.settings.SapConfigRegistry;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.core.Control;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.util.data.KeyMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-scoped registry of open SAP connections for the current test run.
 * SAP is driverless: a connection is opened by an explicit {@code SAP.initConnection}
 * step (like {@code Database.initDBConnection}), tracked here per runner thread, and
 * torn down by {@code SAP.closeConnection} or the iteration-end backstop
 * ({@link #closeAll(TestCaseRunner)} from {@code Task.runIteration}).
 *
 * <p>Aliases are {@code Settings/SAP/&lt;alias&gt;.properties}; blank input resolves to the
 * project default. {@code initConnection} is claim-counted per {@link TestCaseRunner}
 * so a nested reusable's balanced {@code init}/{@code close} never closes its caller's
 * connection.
 */
public final class SapSessionManager {
    private static final Logger LOG = Logger.getLogger(SapSessionManager.class.getName());

    public static final SapSessionManager INSTANCE = new SapSessionManager();

    /** Bounded wait for a freshly launched SAP GUI to register its scripting engine. */
    private volatile long engineTimeoutMillis = 30_000L;

    private volatile Supplier<SapEngineLocator> locatorFactory = JacobSapEngineLocator::new;

    private volatile Supplier<SapConfigRegistry> registrySupplier = () ->
        Control.getCurrentProject().getProjectSettings().getSapConfigRegistry();

    private final ThreadLocal<LinkedHashMap<String, Entry>> byAlias = ThreadLocal.withInitial(
        LinkedHashMap::new
    );
    private final ThreadLocal<String> currentAlias = new ThreadLocal<>();

    private SapSessionManager() {}

    /** One open connection plus its ownership flags and claim stack. */
    private static final class Entry {
        SapGuiSession session;
        boolean ownsProcess;
        boolean ownsConnection;
        /** True when we called {@code createSession()} for this session on someone else's connection. */
        boolean ownsSession;
        Process process;
        final Deque<TestCaseRunner> claims = new ArrayDeque<>();
    }

    // ---- test hooks -------------------------------------------------------

    public void setLocatorFactory(Supplier<SapEngineLocator> factory) {
        this.locatorFactory = factory == null ? JacobSapEngineLocator::new : factory;
    }

    public void setEngineTimeoutMillis(long millis) {
        this.engineTimeoutMillis = millis;
    }

    public void setRegistrySupplier(Supplier<SapConfigRegistry> supplier) {
        this.registrySupplier =
            supplier == null
                ? () -> Control.getCurrentProject().getProjectSettings().getSapConfigRegistry()
                : supplier;
    }

    // ---- lifecycle ------------------------------------------------------

    /**
     * Open (or adopt), or re-claim if already open, the connection named by
     * {@code rawInput} (blank = project default) and make it current.
     *
     * @throws SapConnectionException on an unknown/ambiguous alias, scripting
     *         disabled, or the engine failing to come up.
     */
    public SapGuiSession initConnection(String rawInput, TestCaseRunner runner) {
        SapConfigRegistry reg = registry();
        String alias;
        try {
            alias = reg.resolveAlias(rawInput);
        } catch (SapConfigException ex) {
            throw new SapConnectionException(ex.getMessage(), ex);
        }

        Map<String, Entry> map = byAlias.get();
        Entry e = map.get(alias);
        if (e != null) {
            e.claims.push(runner);
            currentAlias.set(alias);
            LOG.log(Level.FINE, "SAP connection [{0}] already open — re-claimed", alias);
            return e.session;
        }

        LinkedProperties cfg = reg.get(alias);
        if (cfg == null) {
            throw new SapConnectionException(
                "Unknown SAP connection '" + alias + "'. Configure it under Settings/SAP/."
            );
        }

        e = open(alias, cfg);
        e.claims.push(runner);
        map.put(alias, e);
        currentAlias.set(alias);
        return e.session;
    }

    /** Make an already-open connection current. No claim change. */
    public void switchConnection(String rawInput) {
        String alias = resolveAliasOrThrow(rawInput);
        if (!byAlias.get().containsKey(alias)) {
            throw new SapConnectionException(
                "SAP connection '" + alias + "' is not open — add a SAP.initConnection step."
            );
        }
        currentAlias.set(alias);
    }

    /**
     * Release {@code runner}'s most recent claim on the connection named by
     * {@code rawInput} (blank = default). Physically closes only when the claim
     * stack empties and the run owns the connection.
     */
    public void closeConnection(String rawInput, TestCaseRunner runner) {
        String alias;
        try {
            alias = registry().resolveAlias(rawInput);
        } catch (SapConfigException ex) {
            LOG.log(Level.WARNING, "closeConnection: {0}", ex.getMessage());
            return;
        }
        Map<String, Entry> map = byAlias.get();
        Entry e = map.get(alias);
        if (e == null) {
            LOG.log(Level.WARNING, "closeConnection: SAP connection [{0}] is not open", alias);
            return;
        }
        boolean popped = false;
        for (Iterator<TestCaseRunner> it = e.claims.iterator(); it.hasNext();) {
            if (it.next() == runner) {
                it.remove();
                popped = true;
                break;
            }
        }
        if (!popped) {
            LOG.log(
                Level.WARNING,
                "closeConnection [{0}] from a runner that never called initConnection — ignored",
                alias
            );
            return;
        }
        if (e.claims.isEmpty()) {
            teardown(e);
            map.remove(alias);
            if (alias.equals(currentAlias.get())) {
                currentAlias.set(map.isEmpty() ? null : lastKey(map));
            }
        }
    }

    /** Backstop / {@code closeAllConnection}: force-close everything this run owns. */
    public void closeAll(TestCaseRunner root) {
        Map<String, Entry> map = byAlias.get();
        for (Entry e : map.values()) {
            teardown(e);
        }
        map.clear();
        currentAlias.remove();
    }

    /** Best-effort reset for a pooled worker thread before the next test case. */
    public void resetForThread() {
        try {
            closeAll(null);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "resetForThread swallowed", ex);
        } finally {
            byAlias.remove();
            currentAlias.remove();
        }
    }

    // ---- queries ------------------------------------------------------

    public SapGuiSession current() {
        String a = currentAlias.get();
        if (a == null) {
            return null;
        }
        Entry e = byAlias.get().get(a);
        return e == null ? null : e.session;
    }

    public String currentAliasName() {
        return currentAlias.get();
    }

    /**
     * The {@code saplogon.exe} process for the current connection, or {@code null} when
     * there is none — an adopted connection, or one opened on someone else's already-running
     * engine, owns no process of its own.
     */
    public Process currentProcess() {
        String a = currentAlias.get();
        if (a == null) {
            return null;
        }
        Entry e = byAlias.get().get(a);
        return e == null ? null : e.process;
    }

    public boolean hasConnection() {
        return !byAlias.get().isEmpty();
    }

    public boolean isCurrentAliasSet() {
        return currentAlias.get() != null;
    }

    // ---- internals ------------------------------------------------------

    private Entry open(String alias, LinkedProperties cfg) {
        SapEngineLocator loc = locatorFactory.get();
        String connName = resolve(
            cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_CONNECTION_NAME)
        );
        String appPath = resolve(cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_APP));
        String sessionMode = resolveSessionMode(cfg);

        SapEngineLocator.SapGuiEngine engine;
        Entry e = new Entry();
        if (loc.isRunningObjectTablePresent()) {
            requireScripting(loc);
            engine = loc.attach();
        } else {
            e.process = loc.launchSapLogon(appPath);
            e.ownsProcess = true;
            engine = loc.awaitEngine(engineTimeoutMillis);
            requireScripting(loc);
        }

        SapEngineLocator.AdoptedConnection existing = engine.findConnection(connName);
        if (existing == null) {
            // Nothing open with this name yet - open our own, fully owned, connection.
            e.session = engine.openConnection(connName);
            e.ownsConnection = true;
            e.ownsSession = true;
        } else {
            resolveAgainstExistingConnection(alias, connName, existing, sessionMode, e);
        }

        // No-op when the session is already past the logon screen (SSO/SNC, or a
        // session inherited from an already-authenticated adopted connection).
        attemptLogon(e.session, cfg);
        // Checked independently of whether a logon screen appeared: SSO can auto-authenticate
        // and still trigger this popup if the user is already logged on elsewhere.
        handleMultiLogonDialog(e.session, cfg);

        LOG.log(
            Level.INFO,
            "Opened SAP connection [{0}] -> {1} (ownsConnection={2}, ownsSession={3})",
            new Object[] { alias, e.session.connectionInfo(), e.ownsConnection, e.ownsSession }
        );
        return e;
    }

    // ---- logon screen + multiple-logon dialog ----------------------------
    //
    // Field / dialog ids below are the standard SAP GUI Scripting ones
    // (RSYST-* logon fields; MULTI_LOGON_OPT* radio buttons on the "License
    // Information for Multiple Logon" popup) as documented across SAP GUI
    // versions. Verify against the target system if a client customises them.

    // Package-private (not private) so SapSessionManagerTest can preset fake elements at
    // these exact ids without duplicating the literals.
    static final String LOGON_USER_FIELD = "wnd[0]/usr/txtRSYST-BNAME";
    static final String LOGON_PASSWORD_FIELD = "wnd[0]/usr/pwdRSYST-BCODE";
    static final String LOGON_CLIENT_FIELD = "wnd[0]/usr/txtRSYST-MANDT";
    static final String LOGON_LANGUAGE_FIELD = "wnd[0]/usr/txtRSYST-LANGU";
    static final String LOGON_CONFIRM = "wnd[0]";
    static final String MULTI_LOGON_KEEP_OTHERS = "wnd[1]/usr/radMULTI_LOGON_OPT2";
    static final String MULTI_LOGON_END_OTHERS = "wnd[1]/usr/radMULTI_LOGON_OPT1";
    static final String MULTI_LOGON_TERMINATE_THIS = "wnd[1]/usr/radMULTI_LOGON_OPT3";
    static final String MULTI_LOGON_CONFIRM = "wnd[1]/tbar[0]/btn[0]";

    /**
     * Fills the interactive logon screen (client/user/password/language) when one is
     * showing, and does nothing when the session is already authenticated (SSO/SNC, an
     * adopted connection, or a new session on an already-logged-in connection).
     */
    private void attemptLogon(SapGuiSession session, LinkedProperties cfg) {
        SapElement userField = session.findById(LOGON_USER_FIELD);
        if (userField == null) {
            return;
        }
        String user = resolve(cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_USER));
        if (user == null || user.trim().isEmpty()) {
            throw new SapConnectionException(
                "SAP logon screen appeared but no credentials are configured and SSO did not " +
                "complete - check the SAP Logon entry's SNC settings."
            );
        }
        userField.setProperty("Text", user);
        setIfPresent(
            session,
            LOGON_CLIENT_FIELD,
            resolve(cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_CLIENT))
        );
        setIfPresent(
            session,
            LOGON_PASSWORD_FIELD,
            resolve(cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_PASSWORD))
        );
        setIfPresent(
            session,
            LOGON_LANGUAGE_FIELD,
            resolve(cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_LANGUAGE))
        );
        SapElement mainWindow = session.findById(LOGON_CONFIRM);
        if (mainWindow != null) {
            mainWindow.invoke("sendVKey", 0); // Enter
        }
        handleMultiLogonDialog(session, cfg);
    }

    private static void setIfPresent(SapGuiSession session, String fieldId, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        SapElement field = session.findById(fieldId);
        if (field != null) {
            field.setProperty("Text", value);
        }
    }

    /**
     * Auto-answers the "License Information for Multiple Logon" popup per the
     * connection's {@code multiLogon} setting, so it is never a blocker. A no-op when
     * the dialog is not showing.
     */
    private void handleMultiLogonDialog(SapGuiSession session, LinkedProperties cfg) {
        // Any one of the three radio buttons existing means the dialog is up.
        if (
            session.findById(MULTI_LOGON_KEEP_OTHERS) == null &&
            session.findById(MULTI_LOGON_END_OTHERS) == null &&
            session.findById(MULTI_LOGON_TERMINATE_THIS) == null
        ) {
            return;
        }
        String mode = resolve(
            cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_MULTI_LOGON)
        );
        if (mode == null || mode.trim().isEmpty()) {
            mode = "keepOthers";
        }
        if ("fail".equals(mode)) {
            throw new SapConnectionException(
                "Multiple-logon dialog appeared; set multiLogon on this connection " +
                "(keepOthers | endOthers | terminateThis) instead of 'fail'."
            );
        }
        String radioId;
        switch (mode) {
            case "endOthers":
                radioId = MULTI_LOGON_END_OTHERS;
                break;
            case "terminateThis":
                radioId = MULTI_LOGON_TERMINATE_THIS;
                break;
            case "keepOthers":
            default:
                radioId = MULTI_LOGON_KEEP_OTHERS;
                break;
        }
        SapElement radio = session.findById(radioId);
        if (radio != null) {
            radio.setProperty("Selected", true);
        }
        SapElement confirm = session.findById(MULTI_LOGON_CONFIRM);
        if (confirm != null) {
            confirm.invoke("press");
        }
    }

    /**
     * Fills in {@code e.session} (and ownership) for a connection that is already open,
     * per the {@code sessionMode} policy: {@code newSession} (default) spawns a session of
     * our own on the shared connection; {@code shareExisting} drives an idle session already
     * there; {@code requireOwn} refuses to touch a connection this run didn't open.
     */
    private static void resolveAgainstExistingConnection(
        String alias,
        String connName,
        SapEngineLocator.AdoptedConnection existing,
        String sessionMode,
        Entry e
    ) {
        e.ownsConnection = false;
        switch (sessionMode) {
            case "requireOwn":
                throw new SapConnectionException(
                    "SAP connection '" +
                    connName +
                    "' is already open by another session. Set sessionMode = newSession or " +
                    "shareExisting on this connection to reuse it, or free it up first."
                );
            case "shareExisting":
                SapGuiSession idle = existing.firstIdleSession();
                e.session = idle != null ? idle : existing.firstSession();
                e.ownsSession = false;
                break;
            case "newSession":
            default:
                if (existing.sessionCount() >= 6) {
                    throw new SapConnectionException(
                        "SAP connection '" +
                        connName +
                        "' already has 6 sessions open - close one, or set sessionMode = " +
                        "shareExisting on this connection."
                    );
                }
                e.session = existing.createSession();
                e.ownsSession = true;
                break;
        }
        if (e.session == null) {
            throw new SapConnectionException(
                "Could not obtain a SAP session on the already-open connection '" + connName + "'."
            );
        }
    }

    private static String resolveSessionMode(LinkedProperties cfg) {
        String v = cfg.getProperty(com.ing.datalib.settings.SapConnections.KEY_SESSION_MODE);
        return (v == null || v.trim().isEmpty()) ? "newSession" : v.trim();
    }

    private static void requireScripting(SapEngineLocator loc) {
        if (!loc.isScriptingEnabled()) {
            throw new SapConnectionException(
                "SAP GUI Scripting is disabled. Enable it in SAP GUI Options -> " +
                "Accessibility & Scripting -> Scripting (and server param " +
                "sapgui/user_scripting = TRUE)."
            );
        }
    }

    private static void teardown(Entry e) {
        try {
            if (e.ownsConnection && e.session != null) {
                // Owned connection: closing it also ends every session on it.
                e.session.close();
            } else if (e.ownsSession && e.session != null) {
                // Someone else's connection, but we created this one session on it -
                // end only ours; their connection and its other sessions are untouched.
                e.session.closeSessionOnly();
            }
            // Fully adopted (neither flag set): nothing to close, ever.
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error closing SAP session/connection", ex);
        }
        try {
            if (e.ownsProcess && e.process != null) {
                e.process.destroy();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error terminating SAP Logon process", ex);
        }
    }

    private String resolveAliasOrThrow(String rawInput) {
        try {
            return registry().resolveAlias(rawInput);
        } catch (SapConfigException ex) {
            throw new SapConnectionException(ex.getMessage(), ex);
        }
    }

    private SapConfigRegistry registry() {
        return registrySupplier.get();
    }

    private static String resolve(String value) {
        if (value == null) {
            return null;
        }
        return KeyMap.resolveEnvVars(KeyMap.resolveSystemVars(value));
    }

    private static String lastKey(Map<String, Entry> map) {
        String k = null;
        for (String key : map.keySet()) {
            k = key;
        }
        return k;
    }
}
