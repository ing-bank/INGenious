package com.ing.engine.drivers.sap;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real COM implementation of {@link SapEngineLocator}, ported from the former
 * {@code SAPSessionFactory}. Native load is lazy (no static COM state); nothing
 * here runs until {@code SapSessionManager} calls it, and only on Windows with
 * SAP GUI installed.
 *
 * <p>Dropped from the old factory: {@code System.setProperty("jacob.dll.path" / "java.library.path")}
 * (the {@code io.github.osobolev:jacob} artifact self-extracts its native DLLs)
 * and the fixed {@code Thread.sleep(7000)} (replaced by {@link #awaitEngine(long)}).
 */
public final class JacobSapEngineLocator implements SapEngineLocator {
    private static final Logger LOG = Logger.getLogger(JacobSapEngineLocator.class.getName());

    private static final String ROT_PROG_ID = "SapROTWr.SapROTWrapper";

    @Override
    public boolean isRunningObjectTablePresent() {
        try {
            ComThread.InitSTA();
            ActiveXComponent rot = new ActiveXComponent(ROT_PROG_ID);
            Variant entry = rot.invoke("GetROTEntry", "SAPGUI");
            return entry != null && entry.toDispatch() != null;
        } catch (Throwable ex) {
            // Catches UnsatisfiedLinkError too: the jacob native library failing to load
            // (missing/mismatched platform binary) surfaces as an Error, not an Exception,
            // on this - the first - COM touch. Either way: no usable SAP engine.
            return false;
        }
    }

    @Override
    public boolean isScriptingEnabled() {
        try {
            scriptingEngine();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    public SapGuiEngine attach() {
        return new JacobEngine(scriptingEngine());
    }

    @Override
    public Process launchSapLogon(String appPath) {
        try {
            String abs = Paths.get(appPath).toAbsolutePath().toString();
            return Runtime.getRuntime().exec(abs);
        } catch (Exception ex) {
            throw new SapConnectionException(
                "Could not start SAP Logon from '" + appPath + "': " + ex.getMessage(),
                ex
            );
        }
    }

    @Override
    public SapGuiEngine awaitEngine(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                return new JacobEngine(scriptingEngine());
            } catch (Exception ex) {
                last = ex;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new SapConnectionException(
            "No response from SAP GUI within " +
            timeoutMillis +
            " ms — the client did not " +
            "come up, or a scripting notification dialog is blocking. Disable " +
            "\"Notify when a script attaches / opens a connection\" in SAP GUI Options.",
            last
        );
    }

    /** @return the {@code GuiApplication} dispatch; throws if the engine is unreachable. */
    private static Dispatch scriptingEngine() {
        ComThread.InitSTA();
        ActiveXComponent rot = new ActiveXComponent(ROT_PROG_ID);
        Dispatch rotEntry = rot.invoke("GetROTEntry", "SAPGUI").toDispatch();
        Variant engine = Dispatch.call(rotEntry, "GetScriptingEngine");
        return engine.toDispatch();
    }

    /** {@link SapGuiEngine} over a {@code GuiApplication} dispatch. */
    private static final class JacobEngine implements SapGuiEngine {
        private final ActiveXComponent guiApp;

        JacobEngine(Dispatch guiApp) {
            this.guiApp = new ActiveXComponent(guiApp);
        }

        @Override
        public SapGuiSession openConnection(String connectionName) {
            try {
                ActiveXComponent connection = new ActiveXComponent(
                    guiApp.invoke("OpenConnection", connectionName).toDispatch()
                );
                ActiveXComponent session = new ActiveXComponent(
                    connection.invoke("Children", 0).toDispatch()
                );
                return new JacobSapGuiSession(connection, session);
            } catch (Exception ex) {
                throw new SapConnectionException(
                    "Could not open SAP connection '" + connectionName + "': " + ex.getMessage(),
                    ex
                );
            }
        }

        @Override
        public List<String> openConnectionNames() {
            List<String> names = new ArrayList<>();
            try {
                Dispatch conns = Dispatch.call(guiApp, "Children").toDispatch();
                int count = Dispatch.get(conns, "Count").getInt();
                for (int i = 0; i < count; i++) {
                    Dispatch conn = Dispatch.call(conns, "Item", i).toDispatch();
                    names.add(Dispatch.get(conn, "Description").toString());
                }
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Could not enumerate open SAP connections", ex);
            }
            return names;
        }

        @Override
        public AdoptedConnection findConnection(String connectionName) {
            try {
                Dispatch conns = Dispatch.call(guiApp, "Children").toDispatch();
                int count = Dispatch.get(conns, "Count").getInt();
                for (int i = 0; i < count; i++) {
                    Dispatch conn = Dispatch.call(conns, "Item", i).toDispatch();
                    String desc = Dispatch.get(conn, "Description").toString();
                    if (connectionName.equalsIgnoreCase(desc)) {
                        return new JacobAdoptedConnection(new ActiveXComponent(conn));
                    }
                }
            } catch (Exception ex) {
                LOG.log(
                    Level.FINE,
                    "Could not search for SAP connection '" + connectionName + "'",
                    ex
                );
            }
            return null;
        }
    }

    /** {@link AdoptedConnection} over a {@code GuiConnection} dispatch we did not open. */
    private static final class JacobAdoptedConnection implements AdoptedConnection {
        private final ActiveXComponent connection;

        JacobAdoptedConnection(ActiveXComponent connection) {
            this.connection = connection;
        }

        @Override
        public SapGuiSession firstSession() {
            return sessionAt(0);
        }

        @Override
        public SapGuiSession firstIdleSession() {
            int n = sessionCount();
            for (int i = 0; i < n; i++) {
                SapGuiSession s = sessionAt(i);
                if (s != null && !s.isBusy()) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public SapGuiSession createSession() {
            try {
                Dispatch.call(connection, "CreateSession");
                // The new session lands at the end of Children after creation completes.
                int n = sessionCount();
                return sessionAt(n - 1);
            } catch (Exception ex) {
                throw new SapConnectionException(
                    "Could not create a new SAP session: " + ex.getMessage(),
                    ex
                );
            }
        }

        @Override
        public int sessionCount() {
            try {
                Dispatch children = Dispatch.call(connection, "Children").toDispatch();
                return Dispatch.get(children, "Count").getInt();
            } catch (Exception ex) {
                return 0;
            }
        }

        private SapGuiSession sessionAt(int index) {
            if (index < 0) {
                return null;
            }
            try {
                Dispatch children = Dispatch.call(connection, "Children").toDispatch();
                ActiveXComponent session = new ActiveXComponent(
                    Dispatch.call(children, "Item", index).toDispatch()
                );
                return new JacobSapGuiSession(connection, session);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
