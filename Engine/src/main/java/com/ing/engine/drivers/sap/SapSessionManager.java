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

    /** Hard cap SAP GUI itself enforces per connection - {@code SAP.openSession} fails gracefully past it. */
    public static final int MAX_SESSIONS_PER_CONNECTION = 6;

    private SapSessionManager() {}

    /** One open connection plus its ownership flags, claim stack, and its open sessions by label. */
    private static final class Entry {
        boolean ownsProcess;
        boolean ownsConnection;
        Process process;
        final Deque<TestCaseRunner> claims = new ArrayDeque<>();
        /** Sessions on this connection, keyed by label; insertion order = creation order. */
        final LinkedHashMap<String, SessionEntry> sessions = new LinkedHashMap<>();
        /** The label {@code SAP.switchSession} last pointed at (or the default, from {@code initConnection}). */
        String currentSessionLabel;
    }

    /** One session on a connection, plus whether this run owns it (and so must close it on teardown). */
    private static final class SessionEntry {
        final SapGuiSession session;
        /** True when we created this session ourselves - via {@code openConnection} or {@code createSession()}/{@code createSibling()}. */
        final boolean ownsSession;

        SessionEntry(SapGuiSession session, boolean ownsSession) {
            this.session = session;
            this.ownsSession = ownsSession;
        }
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
            // Not reset to the menu / a particular session - whatever this connection's
            // current session pointer already was (its default, or wherever a prior
            // switchSession left it) stays current.
            return e.sessions.get(e.currentSessionLabel).session;
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
        return e.sessions.get(e.currentSessionLabel).session;
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

    // ---- sessions (Phase 4: concurrent sessions on one connection) ------

    /**
     * Create a new session on the current connection, labeled {@code label}, and make it
     * current. Unlike a {@code #alias} connection reference, a session label is a plain,
     * caller-invented string - the caller (an {@code @Action} method) is expected to have
     * already resolved it through the normal value pipeline (not left as {@code #...}).
     *
     * @throws SapConnectionException no current connection, blank label, a label already in
     *         use on this connection, or the 6-session cap reached.
     */
    public SapGuiSession openSession(String label) {
        Entry e = currentEntry();
        if (e == null) {
            throw new SapConnectionException("No SAP connection — add a SAP.initConnection step.");
        }
        String resolved = requireLabel(label);
        if (e.sessions.containsKey(resolved)) {
            throw new SapConnectionException(
                "SAP session '" +
                resolved +
                "' is already open on this connection — use a different label, or " +
                "SAP.switchSession to make it current."
            );
        }
        if (e.sessions.size() >= MAX_SESSIONS_PER_CONNECTION) {
            throw new SapConnectionException(
                "This SAP connection already has " +
                MAX_SESSIONS_PER_CONNECTION +
                " sessions open (SAP's own limit) — close one with SAP.closeSession first."
            );
        }
        SessionEntry current = e.sessions.get(e.currentSessionLabel);
        SapGuiSession created = current.session.createSibling();
        e.sessions.put(resolved, new SessionEntry(created, true));
        e.currentSessionLabel = resolved;
        return created;
    }

    /**
     * Make an already-open session on the current connection current. No session is created.
     * Blank {@code label} switches back to the connection's PRIMARY session - the one {@code
     * initConnection} created, labeled with the connection's own alias. That alias often isn't
     * known up front (e.g. a blank {@code initConnection} resolving to whatever the project
     * default is), so this is the only way to reliably target it without hardcoding a label.
     */
    public void switchSession(String label) {
        Entry e = currentEntry();
        if (e == null) {
            throw new SapConnectionException("No SAP connection — add a SAP.initConnection step.");
        }
        String resolved = (label == null || label.trim().isEmpty())
            ? currentAlias.get()
            : label.trim();
        if (!e.sessions.containsKey(resolved)) {
            throw new SapConnectionException(
                "SAP session '" + resolved + "' is not open — add a SAP.openSession step."
            );
        }
        e.currentSessionLabel = resolved;
    }

    /**
     * Close a session on the current connection (blank {@code label} = the current session)
     * and, if it was current, fall back to the most recently created remaining session. Refuses
     * to close a connection's only remaining session — {@code SAP.closeConnection} is for that.
     * A no-op (logged) when there is no current connection or the label isn't open.
     */
    public void closeSession(String label) {
        Entry e = currentEntry();
        if (e == null) {
            LOG.log(Level.WARNING, "closeSession: no SAP connection is open");
            return;
        }
        String resolved = (label == null || label.trim().isEmpty())
            ? e.currentSessionLabel
            : label.trim();
        SessionEntry se = e.sessions.get(resolved);
        if (se == null) {
            LOG.log(Level.WARNING, "closeSession: SAP session [{0}] is not open", resolved);
            return;
        }
        if (e.sessions.size() <= 1) {
            throw new SapConnectionException(
                "Cannot close the only open SAP session on this connection — use " +
                "SAP.closeConnection instead."
            );
        }
        teardownSession(e, se);
        e.sessions.remove(resolved);
        if (resolved.equals(e.currentSessionLabel)) {
            e.currentSessionLabel = lastKey(e.sessions);
        }
    }

    private static String requireLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            throw new SapConnectionException(
                "A session label is required, e.g. @stock — blank is only valid for " +
                "SAP connections (#alias), not SAP sessions."
            );
        }
        return label.trim();
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
        Entry e = currentEntry();
        if (e == null || e.currentSessionLabel == null) {
            return null;
        }
        SessionEntry se = e.sessions.get(e.currentSessionLabel);
        return se == null ? null : se.session;
    }

    public String currentAliasName() {
        return currentAlias.get();
    }

    /** The label of the current connection's current session, or {@code null} when there is none. */
    public String currentSessionLabel() {
        Entry e = currentEntry();
        return e == null ? null : e.currentSessionLabel;
    }

    /**
     * Resolve a specific labeled session on the CURRENT connection - the "OR object pins to a
     * session" seam ({@code SapORObject}'s {@code session} attribute). {@code null} when there
     * is no current connection, the label is blank, or nothing is open under it.
     */
    public SapGuiSession sessionByLabel(String label) {
        Entry e = currentEntry();
        if (e == null || label == null || label.trim().isEmpty()) {
            return null;
        }
        SessionEntry se = e.sessions.get(label.trim());
        return se == null ? null : se.session;
    }

    /**
     * The {@code saplogon.exe} process for the current connection, or {@code null} when
     * there is none — an adopted connection, or one opened on someone else's already-running
     * engine, owns no process of its own.
     */
    public Process currentProcess() {
        Entry e = currentEntry();
        return e == null ? null : e.process;
    }

    private Entry currentEntry() {
        String a = currentAlias.get();
        if (a == null) {
            return null;
        }
        return byAlias.get().get(a);
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
        SapGuiSession session;
        boolean ownsSession;
        if (existing == null) {
            // Nothing open with this name yet - open our own, fully owned, connection.
            session = engine.openConnection(connName);
            e.ownsConnection = true;
            ownsSession = true;
        } else {
            e.ownsConnection = false;
            Opened opened = resolveAgainstExistingConnection(connName, existing, sessionMode);
            session = opened.session;
            ownsSession = opened.ownsSession;
        }

        // No-op when the session is already past the logon screen (SSO/SNC, or a
        // session inherited from an already-authenticated adopted connection).
        attemptLogon(session, cfg);
        // Checked independently of whether a logon screen appeared: SSO can auto-authenticate
        // and still trigger this popup if the user is already logged on elsewhere.
        handleMultiLogonDialog(session, cfg);

        // initConnection labels its primary session - default: the connection alias itself,
        // the same identifier #alias / currentAliasName() already surface everywhere else.
        e.sessions.put(alias, new SessionEntry(session, ownsSession));
        e.currentSessionLabel = alias;

        LOG.log(
            Level.INFO,
            "Opened SAP connection [{0}] -> {1} (ownsConnection={2}, ownsSession={3})",
            new Object[] { alias, session.connectionInfo(), e.ownsConnection, ownsSession }
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

    /** The session (and whether we own it) resolved for a fresh {@code initConnection}. */
    private static final class Opened {
        final SapGuiSession session;
        final boolean ownsSession;

        Opened(SapGuiSession session, boolean ownsSession) {
            this.session = session;
            this.ownsSession = ownsSession;
        }
    }

    /**
     * Resolves the session (and ownership) for a connection that is already open, per the
     * {@code sessionMode} policy: {@code newSession} (default) spawns a session of our own on
     * the shared connection; {@code shareExisting} drives an idle session already there;
     * {@code requireOwn} refuses to touch a connection this run didn't open.
     */
    private static Opened resolveAgainstExistingConnection(
        String connName,
        SapEngineLocator.AdoptedConnection existing,
        String sessionMode
    ) {
        SapGuiSession session;
        boolean ownsSession;
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
                session = idle != null ? idle : existing.firstSession();
                ownsSession = false;
                break;
            case "newSession":
            default:
                if (existing.sessionCount() >= MAX_SESSIONS_PER_CONNECTION) {
                    throw new SapConnectionException(
                        "SAP connection '" +
                        connName +
                        "' already has " +
                        MAX_SESSIONS_PER_CONNECTION +
                        " sessions open - close one, or set sessionMode = shareExisting on " +
                        "this connection."
                    );
                }
                session = existing.createSession();
                ownsSession = true;
                break;
        }
        if (session == null) {
            throw new SapConnectionException(
                "Could not obtain a SAP session on the already-open connection '" + connName + "'."
            );
        }
        return new Opened(session, ownsSession);
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

    /** Tears down every session on the connection, then the connection/process itself. */
    private static void teardown(Entry e) {
        if (e.ownsConnection) {
            // Owned connection: closing any ONE session on it ends every session on it - so
            // even if we opened extra sessions ourselves via openSession, one close() suffices.
            SessionEntry any = e.sessions.isEmpty() ? null : e.sessions.values().iterator().next();
            if (any != null) {
                try {
                    any.session.close();
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Error closing SAP connection", ex);
                }
            }
        } else {
            // Adopted connection: end only the sessions we created ourselves (openConnection's
            // own createSession(), or a later openSession) - every other session, and the
            // connection itself, is left exactly as we found it.
            for (SessionEntry se : e.sessions.values()) {
                if (se.ownsSession) {
                    try {
                        se.session.closeSessionOnly();
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Error closing SAP session", ex);
                    }
                }
            }
        }
        e.sessions.clear();
        try {
            if (e.ownsProcess && e.process != null) {
                e.process.destroy();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error terminating SAP Logon process", ex);
        }
    }

    /** Tears down one session on an otherwise-still-open connection ({@code SAP.closeSession}). */
    private static void teardownSession(Entry e, SessionEntry se) {
        if (!se.ownsSession) {
            // Adopted session (shareExisting, or one opened by someone else): never ours to close.
            return;
        }
        try {
            if (e.ownsConnection && e.sessions.size() <= 1) {
                // Last session on a connection we own - closing it closes the connection too,
                // same as teardown(). closeSession's own caller already refuses this case, but
                // stay correct if that guard is ever relaxed.
                se.session.close();
            } else {
                se.session.closeSessionOnly();
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error closing SAP session", ex);
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

    private static <V> String lastKey(Map<String, V> map) {
        String k = null;
        for (String key : map.keySet()) {
            k = key;
        }
        return k;
    }
}
