package com.ing.engine.drivers.sap;

/**
 * One live SAP GUI session (a {@code GuiSession}) plus the connection it belongs
 * to. Held by {@code SapSessionManager}; opened via {@link SapEngineLocator}.
 *
 * <p>Phase 1: the ~64 {@code SAPActions} methods keep driving the raw
 * {@code com.jacob.*} objects through {@link #raw()}; the typed verbs here exist
 * for the manager, reporting and the Phase 2+ migration off {@code raw()}.
 */
public interface SapGuiSession {
    /** Resolve an element by id (session-relative, e.g. {@code wnd[0]/usr/...}). */
    SapElement findById(String id);

    void startTransaction(String tcode);

    void endTransaction();

    void refresh();

    /** Human-readable SID / client / user for the report; may be {@code ""}. */
    String connectionInfo();

    boolean isAlive();

    /** True while this session is running a step / dialog and shouldn't be driven right now. */
    boolean isBusy();

    /** Close the whole connection this session belongs to (and every session on it). */
    void close();

    /**
     * End just this session, leaving its connection (and any sibling sessions) open.
     * Used when this run owns a session it created via {@code createSession()} on a
     * connection it merely adopted.
     */
    void closeSessionOnly();

    /** The underlying {@code com.jacob.activeX.ActiveXComponent}, or {@code null} for a fake. */
    Object raw();
}
