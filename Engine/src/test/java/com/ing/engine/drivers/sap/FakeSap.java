package com.ing.engine.drivers.sap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory fakes for the SAP seam, so {@code SapSessionManager} / routing can be
 * tested without SAP GUI or COM.
 */
public final class FakeSap {

    private FakeSap() {}

    /** A fake {@link SapElement} that records property writes and method invocations. */
    public static final class Element implements SapElement {
        public final Map<String, Object> properties = new HashMap<>();
        public final List<String> invocations = new ArrayList<>();

        @Override
        public SapElement findById(String id) {
            return null;
        }

        @Override
        public List<SapElement> children() {
            return Collections.emptyList();
        }

        @Override
        public int childCount() {
            return 0;
        }

        @Override
        public SapElement child(int index) {
            return null;
        }

        @Override
        public String text() {
            return Objects.toString(properties.get("Text"), "");
        }

        @Override
        public String getProperty(String name) {
            return Objects.toString(properties.get(name), null);
        }

        @Override
        public void setProperty(String name, Object value) {
            properties.put(name, value);
        }

        @Override
        public void invoke(String method, Object... args) {
            invocations.add(method);
        }

        @Override
        public Object get(String property) {
            return properties.get(property);
        }

        @Override
        public Object raw() {
            return null;
        }
    }

    /** A fake {@link SapGuiSession} that records close()/closeSessionOnly() calls. */
    public static final class Session implements SapGuiSession {
        public final AtomicInteger closeCount = new AtomicInteger();
        public final AtomicInteger sessionCloseCount = new AtomicInteger();
        public final Map<String, Element> elements = new HashMap<>();
        private boolean alive = true;
        private boolean busy = false;

        public void setBusy(boolean busy) {
            this.busy = busy;
        }

        @Override
        public SapElement findById(String id) {
            return elements.get(id);
        }

        @Override
        public void startTransaction(String tcode) {}

        @Override
        public void endTransaction() {}

        @Override
        public void refresh() {}

        @Override
        public String connectionInfo() {
            return "FAKE/000/TESTER";
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public boolean isBusy() {
            return busy;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            alive = false;
        }

        @Override
        public void closeSessionOnly() {
            sessionCloseCount.incrementAndGet();
            alive = false;
        }

        @Override
        public SapGuiSession createSibling() {
            return new Session();
        }

        @Override
        public Object raw() {
            return null;
        }
    }

    /** A fake already-open connection with one or more sessions, for adopt-path tests. */
    public static final class Connection {
        public final String name;
        public final List<Session> sessions = new ArrayList<>();

        public Connection(String name) {
            this.name = name;
            sessions.add(new Session());
        }
    }

    private static final class FakeAdoptedConnection implements SapEngineLocator.AdoptedConnection {
        private final Connection conn;

        FakeAdoptedConnection(Connection conn) {
            this.conn = conn;
        }

        @Override
        public SapGuiSession firstSession() {
            return conn.sessions.isEmpty() ? null : conn.sessions.get(0);
        }

        @Override
        public SapGuiSession firstIdleSession() {
            for (Session s : conn.sessions) {
                if (!s.isBusy()) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public SapGuiSession createSession() {
            Session s = new Session();
            conn.sessions.add(s);
            return s;
        }

        @Override
        public int sessionCount() {
            return conn.sessions.size();
        }
    }

    /** A configurable {@link SapEngineLocator}. */
    public static final class Locator implements SapEngineLocator {
        public boolean rotPresent = false;
        public boolean scriptingEnabled = true;
        public final List<Session> opened = new ArrayList<>();
        public final AtomicInteger launched = new AtomicInteger();
        public List<String> openConnectionNames = new ArrayList<>();
        /** When set (and its name matches), {@code findConnection} adopts this instead of opening. */
        public Connection existing;
        /** Copied onto every freshly-opened session's element map (logon screen / dialog fixtures). */
        public final Map<String, Element> presetElements = new HashMap<>();

        @Override
        public boolean isRunningObjectTablePresent() {
            return rotPresent;
        }

        @Override
        public boolean isScriptingEnabled() {
            return scriptingEnabled;
        }

        @Override
        public SapGuiEngine attach() {
            return new Engine();
        }

        @Override
        public Process launchSapLogon(String appPath) {
            launched.incrementAndGet();
            rotPresent = true; // client is now up
            return null;
        }

        @Override
        public SapGuiEngine awaitEngine(long timeoutMillis) {
            return new Engine();
        }

        private final class Engine implements SapGuiEngine {

            @Override
            public SapGuiSession openConnection(String connectionName) {
                Session s = new Session();
                s.elements.putAll(presetElements);
                opened.add(s);
                return s;
            }

            @Override
            public List<String> openConnectionNames() {
                return openConnectionNames;
            }

            @Override
            public AdoptedConnection findConnection(String connectionName) {
                if (existing != null && existing.name.equals(connectionName)) {
                    return new FakeAdoptedConnection(existing);
                }
                return null;
            }
        }
    }
}
