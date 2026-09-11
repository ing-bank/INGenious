package com.ing.engine.drivers.sap;

import java.util.List;

/**
 * Discovers / launches the SAP GUI Scripting engine and opens connections.
 * The real implementation ({@code JacobSapEngineLocator}) talks COM; tests
 * inject a fake. All methods run on the caller's (STA) thread.
 */
public interface SapEngineLocator {
    /** True when a SAP GUI is already running and registered in the ROT. */
    boolean isRunningObjectTablePresent();

    /**
     * True when the scripting engine is reachable (server- and client-side
     * scripting enabled). False when the ROT is present but scripting is off.
     */
    boolean isScriptingEnabled();

    /** Attach to the already-running scripting engine. */
    SapGuiEngine attach();

    /** Start {@code saplogon.exe} (or equivalent). */
    Process launchSapLogon(String appPath);

    /** Poll the ROT until the engine is reachable or the timeout elapses. */
    SapGuiEngine awaitEngine(long timeoutMillis);

    /** The {@code GuiApplication} handle. */
    interface SapGuiEngine {
        /** Open a brand-new connection by its SAP Logon description. Always owned. */
        SapGuiSession openConnection(String connectionName);

        /** Descriptions of connections already open on this engine. */
        List<String> openConnectionNames();

        /**
         * Find a connection already open on this engine whose SAP Logon description
         * matches {@code connectionName}, or {@code null} when none is open. Never
         * opens anything itself.
         */
        AdoptedConnection findConnection(String connectionName);
    }

    /** A connection this run did not open — either someone else's, or left over from before. */
    interface AdoptedConnection {
        /** The connection's first session ({@code Children(0)}), whatever it is doing. */
        SapGuiSession firstSession();

        /** The first session that is not busy, or {@code null} when every session is busy. */
        SapGuiSession firstIdleSession();

        /** Spawn a brand-new session on this connection ({@code createSession}). */
        SapGuiSession createSession();

        /** How many sessions this connection currently has open (SAP caps this at 6). */
        int sessionCount();
    }
}
