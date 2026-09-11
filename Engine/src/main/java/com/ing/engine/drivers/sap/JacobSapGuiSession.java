package com.ing.engine.drivers.sap;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.Dispatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JACOB-backed {@link SapGuiSession}. Holds the {@code GuiConnection} and its
 * first {@code GuiSession}. {@link #raw()} returns the session component, which
 * is what {@code SAPObject} / {@code SAPActions} drive directly in Phase 1.
 */
final class JacobSapGuiSession implements SapGuiSession {
    private static final Logger LOG = Logger.getLogger(JacobSapGuiSession.class.getName());

    private final ActiveXComponent connection;
    private final ActiveXComponent session;

    JacobSapGuiSession(ActiveXComponent connection, ActiveXComponent session) {
        this.connection = connection;
        this.session = session;
    }

    @Override
    public SapElement findById(String id) {
        try {
            Dispatch d = session.invoke("FindById", id).toDispatch();
            return d == null ? null : new JacobSapElement(d);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void startTransaction(String tcode) {
        Dispatch.call(session, "startTransaction", tcode);
    }

    @Override
    public void endTransaction() {
        Dispatch.call(session, "endTransaction");
    }

    @Override
    public void refresh() {
        Dispatch.call(session, "Refresh");
    }

    @Override
    public String connectionInfo() {
        try {
            Dispatch info = Dispatch.call(session, "Info").toDispatch();
            String sys = Dispatch.get(info, "SystemName").toString();
            String client = Dispatch.get(info, "Client").toString();
            String user = Dispatch.get(info, "User").toString();
            return sys + "/" + client + "/" + user;
        } catch (Exception ex) {
            return "";
        }
    }

    @Override
    public boolean isAlive() {
        try {
            Dispatch.call(session, "Info").toDispatch();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public boolean isBusy() {
        try {
            return Dispatch.get(session, "Busy").getBoolean();
        } catch (Exception ex) {
            // If we can't tell, assume busy rather than risk driving a mid-transaction session.
            return true;
        }
    }

    @Override
    public void close() {
        try {
            Dispatch.call(connection, "CloseConnection");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error closing SAP connection", ex);
        }
    }

    @Override
    public void closeSessionOnly() {
        try {
            String sessionId = Dispatch.get(session, "Id").toString();
            Dispatch.call(connection, "CloseSession", sessionId);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error closing SAP session", ex);
        }
    }

    @Override
    public SapGuiSession createSibling() {
        try {
            Dispatch.call(connection, "CreateSession");
            // The new session lands at the end of Children after creation completes.
            Dispatch children = Dispatch.call(connection, "Children").toDispatch();
            int count = Dispatch.get(children, "Count").getInt();
            ActiveXComponent newSession = new ActiveXComponent(
                Dispatch.call(children, "Item", count - 1).toDispatch()
            );
            return new JacobSapGuiSession(connection, newSession);
        } catch (Exception ex) {
            throw new SapConnectionException(
                "Could not create a new SAP session: " + ex.getMessage(),
                ex
            );
        }
    }

    @Override
    public Object raw() {
        return session;
    }

    /** Package-visible accessor so {@link JacobSapEngineLocator} can enumerate sibling sessions. */
    ActiveXComponent connection() {
        return connection;
    }
}
