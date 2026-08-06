package com.ing.ide.main.dashboard.server.websocket;

import java.time.Duration;
import org.eclipse.jetty.ee8.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee8.websocket.server.JettyWebSocketServletFactory;

@SuppressWarnings("serial")
public class HarServlet extends JettyWebSocketServlet {

    @Override
    public void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(86400000));
        factory.setMaxTextMessageSize(1000 * 1000);
        factory.register(HarAdapter.class);
    }
}
