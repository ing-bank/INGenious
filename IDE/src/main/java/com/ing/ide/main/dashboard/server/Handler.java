package com.ing.ide.main.dashboard.server;

import org.eclipse.jetty.ee8.websocket.api.WebSocketAdapter;

/**
 *
 *
 */
public interface Handler {
    public void onMessage(WebSocketAdapter client, String message);
}
