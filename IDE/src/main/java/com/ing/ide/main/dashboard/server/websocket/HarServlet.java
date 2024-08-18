
package com.ing.ide.main.dashboard.server.websocket;


import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@SuppressWarnings("serial")
public class HarServlet extends WebSocketServlet {

   
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(86400000);
        factory.getPolicy().setMaxTextMessageSize(1000 * 1000);
        factory.register(HarAdapter.class);
    }

}
