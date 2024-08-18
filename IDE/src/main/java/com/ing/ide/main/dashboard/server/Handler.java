
package com.ing.ide.main.dashboard.server;

import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 *
 * 
 */
public interface Handler {
    public void onMessage(WebSocketAdapter client,String message);
}
