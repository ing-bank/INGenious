
package com.ing.ide.main.dashboard.server.websocket;

import com.ing.ide.main.dashboard.server.Handler;
import com.ing.ide.main.dashboard.server.HarCompareHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.simple.JSONObject;

/**
 *
 * 
 */
public class HarAdapter extends WebSocketAdapter {

    private static final Logger LOG = Logger.getLogger(HarAdapter.class.getName());

    Session session;
    Handler handler;

    @Override
    public void onWebSocketConnect(Session sess) {
        this.session = sess;
        HarCompareHandler.onConnect(this);
    }

    @Override
    public void onWebSocketText(String message) {
        try {
            this.handler.onMessage(this, message);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        HarCompareHandler.onClose(this, reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOG.log(Level.SEVERE, "{0} : {1}", new Object[]{cause.getMessage(), session.getRemoteAddress().getHostString()});
    }

    @Override
    public Session getSession() {
        return session;
    }

    public void serHandler(Handler h) {
        this.handler = h;
    }

    public void send(String data) {
        try {
            getSession().getRemote().sendStringByFuture(data);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public String IP() {
        if (getSession() != null) {
            return getSession().getRemoteAddress().getAddress().getHostAddress();
        } else {
            return "NA";
        }
    }

    public void stopServer(JSONObject data) throws Exception {
        throw new Exception("Access restricted.");
    }

}
